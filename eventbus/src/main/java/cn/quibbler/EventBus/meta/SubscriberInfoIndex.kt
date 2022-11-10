package cn.quibbler.EventBus.meta

interface SubscriberInfoIndex {

    fun getSubscriberInfo(subscriberClass: Class<*>?): SubscriberInfo?

}