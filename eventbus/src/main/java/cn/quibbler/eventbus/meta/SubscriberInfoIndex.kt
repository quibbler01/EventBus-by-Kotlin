package cn.quibbler.eventbus.meta

interface SubscriberInfoIndex {

    fun getSubscriberInfo(subscriberClass: Class<*>?): SubscriberInfo?

}