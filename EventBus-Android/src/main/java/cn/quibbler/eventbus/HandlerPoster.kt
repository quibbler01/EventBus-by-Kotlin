package cn.quibbler.eventbus

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock

class HandlerPoster(
    private val eventBus: EventBus,
    looper: Looper,
    private val maxMillisInsideHandleMessage: Int,
    private val queue: PendingPostQueue = PendingPostQueue()
) : Handler(looper), Poster {

    private var handlerActive: Boolean = false

    override fun enqueue(subscription: Subscription, event: Any) {
        val pendingPost: PendingPost = PendingPost.obtainPendingPost(subscription, event)
        synchronized(this) {
            queue.enqueue(pendingPost)
            if (!handlerActive) {
                handlerActive = true
                if (!sendMessage(obtainMessage())) {
                    throw EventBusException("Could not send handler message")
                }
            }
        }
    }

    override fun handleMessage(msg: Message) {
        var rescheduled = false
        try {
            val started = SystemClock.uptimeMillis()
            while (true) {
                var pendingPost: PendingPost? = queue.poll()
                if (pendingPost == null) {
                    synchronized(this) {
                        // Check again, this time in synchronized
                        pendingPost = queue.poll()
                        if (pendingPost == null) {
                            handlerActive = false
                            return
                        }
                    }
                }
                pendingPost?.let { eventBus.invokeSubscriber(it) }
                val timeInMethod = SystemClock.uptimeMillis() - started
                if (timeInMethod >= maxMillisInsideHandleMessage) {
                    if (!sendMessage(obtainMessage())) {
                        throw EventBusException("Could not send handler message")
                    }
                    rescheduled = true
                    return
                }
            }
        } finally {
            handlerActive = rescheduled
        }
    }
}