package cn.quibbler.EventBus

class SubscriberExceptionEvent(
    /** The {@link EventBus} instance to with the original event was posted to. */
    val eventBus: EventBus,

    /** The Throwable thrown by a subscriber. */
    val throwable: Throwable?,

    /** The original event that could not be delivered to any subscriber. */
    val causingEvent: Any?,

    /** The subscriber that threw the Throwable. */
    val causingSubscriber: Any?
)