# Tractatum on life and death of `UIViewController` and `ComposeScene` in CMP world

## Intro

### Summary

This document summarizes a set of interconnected problem related to lifetime of instances
of `UIViewController` and `ComposeScene` types in the scope of Compose Multiplatform and gives an
insight into how considered solutions to those problems are intefering with each other.

### iOS SDK overview

iOS applications are built on top of the `UIKit` framework. `CMP` just like `SwiftUI` encapsulates
and
manipulates `UIKit` objects.

There are three primary types in `UIKit` to be aware of to understand what's going on:

* `UIView` is a super class of widgets, contains logic for layout, touch handling, and drawing: e.g.
  label, text field, scroll.
* `UIViewController` represents full-fledged app components with rich semantics managing a tree
  of `UIView`s. View controllers themselves can be nested and form a tree.
* `UIWindow` representing fullscreen(for simplification) pane to which a `rootViewController` can be
  attached.

### Examples

A simple app could look like that:
![](images/simple_uiapplication.png)

But usually it's something more sophisticated, looking like that:
![](images/tabbar_uiapplication.png)

This whole thing is actually quite stateful and we can even stop the world in the middle of
navigation pop transition and it will look like that:
![](images/uiapplication_during_pop.png)

If we watch this transition in dynamic we will observe this sequence of events:
![](images/navigation_transition_details.png)

If you look closer, you will notice that there is no

```
viewControllerA.willMoveToParent(navigationController)
```

In this example it happened earlier during the app execution, when it was initially
pushed to the navigation stack, so the sequence of events looked like that:

```
navigationController.pushViewController(viewControllerA, animated: true)
// Implementation will call: 
// navigationController.addChild(viewControllerA)
// |---- viewControllerA.willMoveToParent(navigationController)
// ... animation ...
// viewControllerA.didMoveToParent(navigationController)
```

This state of affairs could possibly lead us to the idea, that `UIViewControler` being
a `rootViewController` of `UIWindow` or being a child controller of other `UIViewController` could
be something we could treat as "__state of being alive__".

## Stayin' alive

iOS SDK is designed around the languages with reference counting. Both Swift and Obj-C rely on
concepts of strong ownership and weak references. The objects that don't have anybody who owns them
strongly get deallocated, all weak references still keep the control block allocated along with the
object alive, so the weak references could be unwrapped and see that the object is indeed dead,
instead of being a dangling pointer into somewhere the deallocated object resided.

```swift
class Foo {
   private let dataBaseConnection = DataBaseConnection()
   private let composeScene = ComposeScene() 
   
   init {
       print("hello")
   }
   
   deinit {
      print("bye")
      // composeScene closes and gets deallocated
      // dataBaseConnection is disposed and gets deallocated
   }
}

func bar() {
  let foo = Foo() // prints: hello
  // all references are released in the end of function scope
  // `foo` isn't owned by someone else anymore
  // prints: bye, cleans up stuff
}
```

Let's see what happens with a `UIViewController`, that was popped in the previous chapter:
![](images/popped_view_controller.png)
The `UIViewController` "B" gets immediately deallocated, because nobody owns it anymore. It released
the resources, that were uniquely referenced by it (let's say it had a `ComposeScene` inside).
Right?

Well. Not in our world.

#### Problem 1.

It can not happen _immediately_. Once `UIViewController` instance is passed to/created by Kotlin runtime,
it will be owned by a Kotlin wrapper object (let's call it `KViewController`, that is controlled by
Kotlin rules and will not be
released earlier, than next GC pass, which will happen whenever, so we can't release associated
resources in
_determined_ way.

#### Problem 2.

From Kotlin runtime perspective, `ComposeScene` which is transitively referenced by
this `KViewController` also references `KViewController` indirectly. We pass `LocalUIViewController`
into our composition, so that users can use its API for interop purposes
(like `presentViewController` to modally show other view controllers). But that should not be a big
deal, mark-and-sweep GC will collect circular references unless one of them is referenced by
something in the root set.

The bad news is, something in _the root set_ is after us:
![](images/dispose.png)
And we need to break this connection by calling `ComposeScene.dispose()`.
The problem is - who calls the `dispose()`?
How can `UIViewController` know that it needs to dispose the owned `ComposeScene`, its lifetime in
terms of automatic reference counting semantics is ruined in this case.

#### First approach
The initial take on implementing scene disposal
was [this PR](https://github.com/JetBrains/compose-multiplatform-core/pull/728).
It broke the strong connection between `ComposeScene` and `KViewController` via using `WeakReference` and Kotlin Native `createCleaner` that allowed us to schedule some work at GC time once the object is collected.
The view controller was collected and `createCleaner` lambda called `ComposeScene.dispose()`

This approach still had two disadvantages:
1. It happens at unknown time in the future when GC collects the `KViewController`
2. It's very fragile.

Let's have a look at this piece of code:

```kotlin
 @Composable
 private fun Foo() {
     // LocalUIViewController.current is WeakReference<UIViewController>
     val viewController = LocalUIViewController.current.get() ?: return

     // can't use LocalUIViewController.current inside onClick because onClick is not @Composable
     Button(onClick = {
         val newViewController = UIViewController()
         viewController.presentViewController(newViewController, true)
     }) {
         Text("Present")
     }
}
```

We've just created a memory leak. `viewController` which is our `KViewController` is now captured by `onClick` lambda and is hold inside Compose machinery. In order to release it, we need to call `ComposeScene.dispose()` which will be called
when when `KViewController` is released, which is never, because it's a part of Compose execution machinery, which is referenced by the root set.

#### Second approach
What [if we use](https://github.com/JetBrains/compose-multiplatform-core/pull/747) `viewWillAppear` to create `ComposeScene` and `viewDidDisappear` to destroy it.
It's determined. Seems like a good idea. There are disadvantages though:
1. We offload the responsibility to store the state of `ComposeScene` that should persist across scene resconstructions to the user. 
2. Semantically incorrect code like custom `UIViewController` container, that didn't call `viewDidDisappear` will never `dispose` the scene.
3. Edge case below:

![](images/oops_dispose.png)
Interop video player view implicitly presents a over-fullscreen modal to expand the player, causing `viewDidDisappear` call, subsequent `ComposeScene.dispose()`, destroying "interop VideoView" which was created by composition that was a source of presented controller in a first place. Undefined behavior in its finest.

## Stayin` alive and dying ASAP while being correct

Since `lifetime` becomes a blurred and smudged term in the mix of two different runtimes, we need to either:

### Enforce the lifetime semantics of automated reference counting

In order not to be tied to the garbage collector shenanigans, we can hide from it altogether.
![](images/cmp_view_controller.png)
The idea behind this approach is to design the CMP API in a way, where the `UIViewController` never shows up in the Kotlin runtime in the first place.



