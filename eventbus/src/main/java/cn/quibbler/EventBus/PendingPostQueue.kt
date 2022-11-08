package cn.quibbler.EventBus

class PendingPostQueue {

    companion object {
        private val `object` = Object()
    }

    private var head: PendingPost? = null
    private var tail: PendingPost? = null

    @Synchronized
    fun enqueue(pendingPost: PendingPost?) {
        if (pendingPost == null) {
            throw NullPointerException("null cannot be enqueued")
        }
        if (tail != null) {
            tail?.next = pendingPost
            tail = pendingPost
        } else if (head == null) {
            head = pendingPost
            tail = pendingPost
        } else {
            throw IllegalStateException("")
        }
        `object`.notifyAll()
    }

    @Synchronized
    fun poll(): PendingPost? {
        val pendingPost = head
        if (head != null) {
            head = head?.next
            if (head == null) {
                tail = null
            }
        }
        return pendingPost
    }

    @Throws(InterruptedException::class)
    @Synchronized
    fun poll(maxMillisToWait: Int): PendingPost? {
        if (head == null) {
            `object`.wait(maxMillisToWait.toLong())
        }
        return poll()
    }

}