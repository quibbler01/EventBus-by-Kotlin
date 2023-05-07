package cn.quibbler.eventbus.meta

import cn.quibbler.eventbus.SubscriberMethod

interface SubscriberInfo {

    fun getSubscriberClass(): Class<*>

    fun getSubscriberMethods(): Array<SubscriberMethod?>

    fun getSuperSubscriberInfo(): SubscriberInfo?

    fun shouldCheckSuperclass(): Boolean

}