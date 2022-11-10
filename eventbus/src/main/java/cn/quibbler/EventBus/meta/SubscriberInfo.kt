package cn.quibbler.EventBus.meta

import cn.quibbler.EventBus.SubscriberMethod

interface SubscriberInfo {

    fun getSubscriberClass(): Class<*>

    fun getSubscriberMethods(): Array<SubscriberMethod?>

    fun getSuperSubscriberInfo(): SubscriberInfo?

    fun shouldCheckSuperclass(): Boolean

}