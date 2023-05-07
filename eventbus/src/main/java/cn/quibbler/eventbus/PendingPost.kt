package cn.quibbler.eventbus

class PendingPost(var event: Any?, var subscription: Subscription?) {

    var next: PendingPost? = null

    companion object {
        private val pendingPostPool: MutableList<PendingPost> = ArrayList()

        fun obtainPendingPost(subscription: Subscription, event: Any?): PendingPost {
            synchronized(pendingPostPool) {
                val size = pendingPostPool.size
                if (size > 0) {
                    val pendingPost = pendingPostPool.removeAt(size - 1)
                    pendingPost.event = event
                    pendingPost.subscription = subscription
                    pendingPost.next = null
                    return pendingPost
                }
            }
            return PendingPost(event, subscription)
        }

        fun releasePendingPost(pendingPost: PendingPost) {
            pendingPost.event = null
            pendingPost.subscription = null
            pendingPost.next = null
            synchronized(pendingPostPool) {
                // Don't let the pool grow indefinitely
                if (pendingPostPool.size < 10000) {
                    pendingPostPool.add(pendingPost)
                }
            }
        }

    }

}