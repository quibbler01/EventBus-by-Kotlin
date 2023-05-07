package cn.quibbler.eventbus

class Subscription(val subscriber: Any, val subscriberMethod: SubscriberMethod) {

    /**
     * Becomes false as soon as {@link EventBus#unregister(Object)} is called, which is checked by queued event delivery
     * {@link EventBus#invokeSubscriber(PendingPost)} to prevent race conditions.
     */
    @Volatile
    var active: Boolean = false

    override fun equals(other: Any?): Boolean {
        return if (other is Subscription) {
            subscriber == other.subscriber && subscriberMethod == other.subscriberMethod
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return subscriber.hashCode() + subscriberMethod.methodString.hashCode()
    }

}