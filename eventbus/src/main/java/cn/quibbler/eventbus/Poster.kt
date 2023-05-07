package cn.quibbler.eventbus

/**
 * Posts events.
 *
 * @author William Ferguson
 */
interface Poster {

    /**
     * Enqueue an event to be posted for a particular subscription.
     *
     * @param subscription Subscription which will receive the event.
     * @param event        Event that will be posted to subscribers.
     */
    fun enqueue(subscription: Subscription, event: Any)

}