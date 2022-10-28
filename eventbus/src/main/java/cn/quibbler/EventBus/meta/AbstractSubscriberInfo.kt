package cn.quibbler.EventBus.meta

import cn.quibbler.EventBus.EventBusException
import cn.quibbler.EventBus.SubscriberMethod
import cn.quibbler.EventBus.ThreadMode

abstract class AbstractSubscriberInfo(
    private val subscriberClass: Class<*>,
    private val superSubscriberInfoClass: Class<out SubscriberInfo>?,
    private val shouldCheckSuperclass: Boolean
) : SubscriberInfo {

    override fun getSubscriberClass(): Class<*> = subscriberClass

    override fun getSuperSubscriberInfo(): SubscriberInfo? {
        if (superSubscriberInfoClass == null) {
            return null
        }
        try {
            return superSubscriberInfoClass.newInstance()
        } catch (e: InstantiationException) {
            throw RuntimeException()
        } catch (e: IllegalAccessException) {
            throw RuntimeException()
        }
    }

    override fun shouldCheckSuperclass(): Boolean = shouldCheckSuperclass

    protected fun createSubscriberMethod(
        methodName: String,
        eventType: Class<*>,
    ): SubscriberMethod {
        return createSubscriberMethod(methodName, eventType, ThreadMode.POSTING, 0, false)
    }

    protected fun createSubscriberMethod(
        methodName: String,
        eventType: Class<*>,
        threadMode: ThreadMode
    ): SubscriberMethod {
        return createSubscriberMethod(methodName, eventType, threadMode, 0, false)
    }

    protected fun createSubscriberMethod(
        methodName: String,
        eventType: Class<*>,
        threadMode: ThreadMode,
        priority: Int,
        sticky: Boolean
    ): SubscriberMethod {
        try {
            val method = subscriberClass.getDeclaredMethod(methodName, eventType)
            return SubscriberMethod(method, eventType, threadMode, priority, sticky)
        } catch (e: NoSuchMethodException) {
            throw EventBusException(
                "Could not find subscriber method in $subscriberClass. Maybe a missing ProGuard rule?",
                e
            )
        }
    }

}