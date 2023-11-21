# Tractatum on life and death of `UIViewController`s and `ComposeScene`s in CMP world

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

#### Examples

A simple app could look like that:
![Simple UIKit hierarchy](images/simple_uiapplication.png)

But usually it's something more sophisticated, looking like that:
![UIKit hierarchy with tab bar](images/tabbar_uiapplication.png)

This whole thing is actually quite stateful and we can even stop the world in the middle of
navigation pop transition and it will look like that:
![UIKit hierarchy with navigation controller in the middle of pop transition](images/uiapplication_during_pop.png)

If we watch this transition in dynamic we will observe this sequence of events:
![Navigation transition events sequence breakdown](images/navigation_transition_details.png)

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

