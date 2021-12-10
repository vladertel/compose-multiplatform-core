package androidx.compose.mpp.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private inline fun Modifier.noRippleClickable(crossinline onClick: () -> Unit): Modifier =
    composed {
        clickable(indication = null,
            interactionSource = remember { MutableInteractionSource() }) {
            onClick()
        }
    }

private val random = Random(100)
private val angles = listOf(PI / 4, -PI / 3, 3 * PI / 4, -PI / 6, -1.1 * PI)
private val colors = listOf(Color.Red, Color.Black, Color.Green, Color.Magenta)

@Composable
fun BouncingBallsApp() {
    val iteration = remember { mutableStateOf(0) }

    Box(
        modifier = Modifier.fillMaxWidth()
            .fillMaxHeight()
            .noRippleClickable {
                data += BouncingBall(
                    Circle(
                        x = random.nextInt(100, 700).toFloat(),
                        y = random.nextInt(100, 500).toFloat(),
                        r = random.nextInt(10, 50).toFloat()
                    ),
                    velocity = random.nextInt(100, 200).toFloat(),
                    angle = angles.random(),
                    color = colors.random().copy(alpha = max(0.3f, random.nextFloat()))
                )
            }
    ) {
        var i = iteration.value // read iteration to recompose when necessary

        data.forEachIndexed { ix, ball ->
            key(ix) {
                Box(
                    modifier = Modifier
                        .padding(
                            start = (ball.circle.x - ball.circle.r).dp,
                            top = (ball.circle.y - ball.circle.r).dp
                        ).size((2 * ball.circle.r).dp)
                        .background(ball.color, CircleShape)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        var lastTime = 0L
        var dt = 0L

        while (true) {
            withFrameNanos { time ->
                dt = time - lastTime
                if (lastTime == 0L) {
                    dt = 0
                }
                lastTime = time
                data.forEach {
                    it.recalculate(800, 600, dt.toFloat())
                }
                iteration.value++
            }
        }
    }
}

private data class Circle(var x: Float, var y: Float, var r: Float)

private fun moveCircle(c: Circle, s: Float, angle: Float, width: Int, height: Int, r: Float) {
    c.x = (c.x + s * sin(angle)).coerceAtLeast(r).coerceAtMost(width.toFloat() - r)
    c.y = (c.y + s * cos(angle)).coerceAtLeast(r).coerceAtMost(height.toFloat() - r)
}

private fun calculatePosition(circle: Circle, boundingWidth: Int, boundingHeight: Int): Position {
    val southmost = circle.y + circle.r
    val northmost = circle.y - circle.r
    val westmost = circle.x - circle.r
    val eastmost = circle.x + circle.r

    return when {
        southmost >= boundingHeight -> Position.TOUCHES_SOUTH
        northmost <= 0 -> Position.TOUCHES_NORTH
        eastmost >= boundingWidth -> Position.TOUCHES_EAST
        westmost <= 0 -> Position.TOUCHES_WEST
        else -> Position.INSIDE
    }
}

private enum class Position {
    INSIDE,
    TOUCHES_SOUTH,
    TOUCHES_NORTH,
    TOUCHES_WEST,
    TOUCHES_EAST
}

private class BouncingBall(
    val circle: Circle,
    val velocity: Float,
    var angle: Double,
    val color: Color = Color.Red
) {
    fun recalculate(width: Int, height: Int, dt: Float) {
        val position = calculatePosition(circle, width, height)

        val dtMillis = dt / 1000000

        when (position) {
            Position.TOUCHES_SOUTH -> angle = PI - angle
            Position.TOUCHES_EAST -> angle = -angle
            Position.TOUCHES_WEST -> angle = -angle
            Position.TOUCHES_NORTH -> angle = PI - angle
            Position.INSIDE -> angle
        }

        moveCircle(
            circle,
            velocity * (dtMillis.coerceAtMost(500f) / 1000),
            angle.toFloat(),
            width,
            height,
            circle.r
        )
    }
}

private val data = listOf(
    BouncingBall(Circle(200f, 50f, 25f), 172f, PI / 4, colors.random()),
    BouncingBall(Circle(100f, 100f, 10f), 162f, -PI / 3, colors.random()),
    BouncingBall(Circle(150f, 120f, 30f), 168f, 3 * PI / 4, colors.random()),
    BouncingBall(Circle(100f, 100f, 25f), 208f, -PI / 6, colors.random()),
    BouncingBall(Circle(120f, 100f, 40f), 120f, -1.1 * PI, colors.random())
).toMutableList()
