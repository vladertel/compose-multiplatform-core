package androidx.compose.mpp.demo

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

enum class TestState {
    First, Second
}

@Composable
fun AnimatedContentSample() {
    Column {
        val seekingState = remember { SeekableTransitionState(TestState.First) }
        val scope = rememberCoroutineScope()
        Column {
            Row {
                Button(onClick = { scope.launch { seekingState.animateTo(TestState.First) } }) {
                    Text("First")
                }
                Button(onClick = {  scope.launch { seekingState.animateTo(TestState.Second) } }) {
                    Text("Second")
                }
            }
        }
        val transition = rememberTransition(seekingState)
        transition.AnimatedContent(transitionSpec = {
            fadeIn(tween(easing = LinearEasing)) togetherWith fadeOut(tween(easing = LinearEasing))
        }) { state ->
            println("AnimationScope: $state")
            remember { println("AnimationScope: $state remember") }
            DisposableEffect(Unit) {
                onDispose {
                    println("AnimationScope: $state onDispose")
                }
            }
        }
    }
}

class App(
    private val initialScreenName: String? = null,
    private val extraScreens: List<Screen> = listOf()
) {

    @Composable
    fun Content() {
        AnimatedContentSample()
    }
}
