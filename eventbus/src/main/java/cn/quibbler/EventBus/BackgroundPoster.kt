package cn.quibbler.EventBus

import java.util.logging.Level

/**
 * Posts events in background.
 *
 * @author Markus
 */
class BackgroundPoster(private val queue: PendingPostQueue, private val eventBus: EventBus) :
    Runnable, Poster {

    @Volatile
    private var executorRunning = false

    constructor(eventBus: EventBus) : this(PendingPostQueue(), eventBus)

    override fun run() {
        try {
            try {
                var pendingPost = queue.poll(1000)
                if (pendingPost == null) {
                    synchronized(this) {
                        // Check again, this time in synchronized
                        pendingPost = queue.poll()
                        if (pendingPost == null) {
                            executorRunning = false
                            return
                        }
                    }
                    pendingPost?.let { eventBus.invokeSubscriber(it) }
                }
            } catch (e: InterruptedException) {
                eventBus.getLogger()
                    .log(Level.WARNING, Thread.currentThread().name + " was interrupted", e)
            }
        } finally {
            executorRunning = false
        }
    }

    override fun enqueue(subscription: Subscription, event: Any) {
        val pendingPost = PendingPost.obtainPendingPost(subscription, event)
        synchronized(this) {
            queue.enqueue(pendingPost)
            if (!executorRunning) {
                executorRunning = true
                eventBus.getExecutorService().execute(this)
            }
        }
    }

}