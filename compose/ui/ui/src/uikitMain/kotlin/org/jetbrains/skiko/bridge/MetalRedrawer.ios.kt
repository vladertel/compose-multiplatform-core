package org.jetbrains.skiko.bridge

import kotlin.math.roundToInt
import kotlinx.cinterop.*
import org.jetbrains.skia.*
import org.jetbrains.skiko.InternalSkikoApi
import platform.Foundation.NSRunLoop
import platform.Foundation.NSSelectorFromString
import platform.Metal.MTLDeviceProtocol
import platform.QuartzCore.*
import platform.darwin.*

private enum class DrawSchedulingState {
    AVAILABLE_ON_NEXT_FRAME,
    AVAILABLE_ON_CURRENT_FRAME,
    SCHEDULED_ON_NEXT_FRAME
}

/*
 * Represents the transient information for rendering a single frame.
 */
private data class FrameRenderTarget(
    val renderTarget: BackendRenderTarget,
    val surface: Surface,
    val metalDrawable: CAMetalDrawableProtocol
)

internal class MetalRedrawer(
    private val layer: SkiaLayer2,
    private val device: MTLDeviceProtocol,
    private val metalLayer: CAMetalLayer,
) {
    /**
     * Needs scheduling displayLink for forcing UITouch events to come at the fastest possible cadence.
     * Otherwise, touch events can come at rate lower than actual display refresh rate.
     */
    var needsProactiveDisplayLink = false
        set(value) {
            field = value

            if (value) {
                caDisplayLink.setPaused(false)
            }
        }

    var maximumFramesPerSecond: NSInteger
        get() = caDisplayLink.preferredFramesPerSecond
        set(value) {
            caDisplayLink.preferredFramesPerSecond = value
        }

    private val queue = device.newCommandQueue() ?: throw IllegalStateException("Couldn't create Metal command queue")
    private val context = DirectContext.makeMetal(device.objcPtr(), queue.objcPtr())
    private var isDisposed = false

    // Semaphore for preventing command buffers count more than swapchain size to be scheduled/executed at the same time
    private val inflightSemaphore = dispatch_semaphore_create(metalLayer.maximumDrawableCount.toLong())

    /*
     * Initial value is [DrawSchedulingState.AVAILABLE_ON_NEXT_FRAME] because voluntarily dispatching a frame
     * disregarding CADisplayLink timing (which is not accessible while it's paused) can cause frame drifting in worst
     * cases adding one frame latency due to presentation mechanism, if followed by steady draw dispatch
     * (which is often the case).
     * TODO: look closer to what happens after blank frames leave it in AVAILABLE_ON_CURRENT_FRAME. Touch driven events sequence negate that problem.
     */
    private var drawSchedulingState = DrawSchedulingState.AVAILABLE_ON_NEXT_FRAME

    private val frameListener: NSObject = FrameTickListener {
        when (drawSchedulingState) {
            DrawSchedulingState.AVAILABLE_ON_NEXT_FRAME -> {
                drawSchedulingState = DrawSchedulingState.AVAILABLE_ON_CURRENT_FRAME
            }

            DrawSchedulingState.SCHEDULED_ON_NEXT_FRAME -> {
                draw()

                drawSchedulingState = DrawSchedulingState.AVAILABLE_ON_NEXT_FRAME
            }

            DrawSchedulingState.AVAILABLE_ON_CURRENT_FRAME -> {
                // still available, do nothing
            }
        }

        if (!needsProactiveDisplayLink) {
            caDisplayLink.setPaused(true)
        }
    }

    private val caDisplayLink = CADisplayLink.displayLinkWithTarget(
        target = frameListener,
        selector = NSSelectorFromString(FrameTickListener::onDisplayLinkTick.name)
    )

    init {
        caDisplayLink.setPaused(true)
        caDisplayLink.addToRunLoop(NSRunLoop.mainRunLoop, NSRunLoop.mainRunLoop.currentMode)
    }

    /**
     * UITouch events are dispatched right before next CADisplayLink callback by iOS.
     * It's too late to encode any work for this frame after this happens.
     * Any work dispatched before the next CADisplayLink callback should be scheduled after that callback.
     */
    fun preventDrawDispatchDuringCurrentFrame() {
        if (drawSchedulingState == DrawSchedulingState.AVAILABLE_ON_CURRENT_FRAME) {
            drawSchedulingState = DrawSchedulingState.AVAILABLE_ON_NEXT_FRAME
        }
    }

    fun dispose() {
        if (!isDisposed) {
            caDisplayLink.invalidate()

            context.close()

            isDisposed = true
        }
    }

    fun needRedraw() {
        check(!isDisposed) { "MetalRedrawer is disposed" }

        drawImmediatelyIfPossible()

        if (drawSchedulingState == DrawSchedulingState.SCHEDULED_ON_NEXT_FRAME) {
            caDisplayLink.setPaused(false)
        }
    }

    /*
     * Dispatch redraw immediately during current frame if possible and updates [drawSchedulingState] to relevant value
     */
    private fun drawImmediatelyIfPossible() {
        when (drawSchedulingState) {
            DrawSchedulingState.AVAILABLE_ON_NEXT_FRAME -> {
                drawSchedulingState = DrawSchedulingState.SCHEDULED_ON_NEXT_FRAME
            }

            DrawSchedulingState.AVAILABLE_ON_CURRENT_FRAME -> {
                draw()

                drawSchedulingState = DrawSchedulingState.AVAILABLE_ON_NEXT_FRAME
            }

            DrawSchedulingState.SCHEDULED_ON_NEXT_FRAME -> {
                // already scheduled, do nothing
            }
        }
    }

    private fun prepareFrameRenderTarget(): FrameRenderTarget? {
        val (width, height) = metalLayer.drawableSize.useContents {
            width.roundToInt() to height.roundToInt()
        }

        if (width <= 0 || height <= 0) {
            return null
        }

        val metalDrawable = metalLayer.nextDrawable()!!

        val renderTarget = BackendRenderTarget.makeMetal(width, height, metalDrawable.texture.objcPtr())

        val surface = Surface.makeFromBackendRenderTarget(
            context,
            renderTarget,
            SurfaceOrigin.TOP_LEFT,
            SurfaceColorFormat.BGRA_8888,
            ColorSpace.sRGB,
            SurfaceProps(pixelGeometry = PixelGeometry.UNKNOWN)
        )

        return if (surface != null) {
            FrameRenderTarget(renderTarget, surface, metalDrawable)
        } else {
            renderTarget.close()

            // TODO manually release metalDrawable when K/N API arrives

            null
        }
    }

    private fun draw() {
        if (isDisposed) {
            return
        }

        autoreleasepool {
            dispatch_semaphore_wait(inflightSemaphore, DISPATCH_TIME_FOREVER)

            val info = prepareFrameRenderTarget()

            info?.let {
                it.surface.canvas.apply {
                    clear(Color.WHITE)
                    layer.draw(this)
                }

                context.flush()
                it.surface.flushAndSubmit()

                val commandBuffer = queue.commandBuffer()!!
                commandBuffer.label = "Present"
                commandBuffer.presentDrawable(it.metalDrawable)
                commandBuffer.addCompletedHandler {
                    // Signal work finish, allow a new command buffer to be scheduled
                    dispatch_semaphore_signal(inflightSemaphore)
                }
                commandBuffer.commit()

                it.surface.close()
                it.renderTarget.close()
                // TODO manually release it.metalDrawable when K/N API arrives
            } ?: {
                dispatch_semaphore_signal(inflightSemaphore)
            }
        }
    }
}

private class FrameTickListener(val onFrameTick: () -> Unit) : NSObject() {
    @ObjCAction
    fun onDisplayLinkTick() {
        onFrameTick()
    }
}
