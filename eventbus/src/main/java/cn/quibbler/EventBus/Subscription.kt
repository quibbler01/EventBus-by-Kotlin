package cn.quibbler.EventBus

class Subscription(val subscriber: Any, val subscriberMethod: SubscriberMethod) {

    /**
     * Becomes false as soon as {@link EventBus#unregister(Object)} is called, which is checked by queued event delivery
     * {@link EventBus#invokeSubscriber(PendingPost)} to prevent race conditions.
     */
    @Volatile
    var active: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (other is Subscription) {
            val otherSubscription = other as Subscription
            return subscriber == otherSubscription.subscriber && subscriberMethod == otherSubscription.subscriberMethod
        } else {
            return false
        }
    }

    override fun hashCode(): Int {
        return subscriber.hashCode() + subscriberMethod.methodString.hashCode()
    }

}