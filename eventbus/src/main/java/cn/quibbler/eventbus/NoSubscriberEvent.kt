package cn.quibbler.eventbus

/**
 * This Event is posted by EventBus when no subscriber is found for a posted event.
 *
 * @author Markus
 */
class NoSubscriberEvent(eventBus: EventBus, originalEvent: Any?)
