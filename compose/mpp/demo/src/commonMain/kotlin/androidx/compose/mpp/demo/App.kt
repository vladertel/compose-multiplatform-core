package androidx.compose.mpp.demo

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

class App(
    private val initialScreenName: String? = null,
    private val extraScreens: List<Screen> = listOf()
) {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content() {
        val text = remember { mutableStateOf("") }
        var showContent = remember { mutableStateOf(true) }
        val focusRequester = remember { FocusRequester() }

        MaterialTheme {
            val textFieldStates = remember { List(20) { mutableStateOf("Textfield N ${it}") } }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(count = 20) { index ->
                    val textState = textFieldStates[index]
                    Spacer(modifier = Modifier.height(16.dp).pointerInput(Unit) {
                        this.awaitEachGesture {
                            val event = this.awaitFirstDown()
                            event.position
                        }
                    })

                    BasicTextField(
                        value = textState.value,
                        onValueChange = { newText ->
                            textState.value = newText
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }
            }
        }
    }
//
//
//        val navController = rememberNavController()
//        val animationSpec = tween<IntOffset>(500)
//        NavHost(
//            navController = navController,
//            startDestination = initialScreenName ?: MainScreen.title,
//
//            // Custom animations
//            enterTransition = { slideIntoContainer(SlideDirection.Left, animationSpec) },
//            exitTransition = { slideOutOfContainer(SlideDirection.Left, animationSpec) },
//            popEnterTransition = { slideIntoContainer(SlideDirection.Right, animationSpec) },
//            popExitTransition = { slideOutOfContainer(SlideDirection.Right, animationSpec) }
//        ) {
//            buildScreen(MainScreen.mergedWith(extraScreens), navController)
//        }
//    }
//
//    private fun NavGraphBuilder.buildScreen(screen: Screen, navController: NavController) {
//        if (screen is Screen.Selection) {
//            for (i in screen.screens) {
//                buildScreen(i, navController)
//            }
//        }
//        if (screen is Screen.Dialog) {
//            dialog(screen.title) { ScreenContent(screen, navController) }
//        } else {
//            composable(screen.title) { ScreenContent(screen, navController) }
//        }
//    }
//
//    @Composable
//    private fun ScreenContent(screen: Screen, navController: NavController) {
//        val lifecycle = LocalLifecycleOwner.current.lifecycle
//        val currentBackStack = remember(screen) { navController.currentBackStack.value }
//        screen.Content(
//            title = currentBackStack.drop(1)
//                .joinToString("/") { it.destination.route ?: it.destination.displayName },
//            navigate = { navController.navigate(it) },
//            back = back@{
//                // Filter multi-click by current lifecycle state: it's not [RESUMED] in case if
//                // a navigation transaction is in progress or the window is not focused.
//                if (lifecycle.currentState < Lifecycle.State.RESUMED) {
//                    return@back
//                }
//                if (navController.previousBackStackEntry != null) {
//                    navController.popBackStack()
//                }
//            }
//        )
//    }
//
//
}
