package cn.quibbler.eventbus

/**
 * Posts events in background.
 *
 * @author Markus
 */
class AsyncPoster(private val queue: PendingPostQueue, private val eventBus: EventBus) : Runnable,
    Poster {

    constructor(eventBus: EventBus) : this(PendingPostQueue(), eventBus)

    override fun run() {
        val pendingPost: PendingPost? = queue.poll()
        if (pendingPost == null) {
            throw IllegalStateException("No pending post available")
        }
        eventBus.invokeSubscriber(pendingPost)
    }

    override fun enqueue(subscription: Subscription, event: Any) {
        val pendingPost: PendingPost = PendingPost.obtainPendingPost(subscription, event)
        queue.enqueue(pendingPost)
        eventBus.getExecutorService().execute(this)
    }

}