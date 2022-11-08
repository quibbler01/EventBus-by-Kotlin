package cn.quibbler.EventBus.meta

import cn.quibbler.EventBus.SubscriberMethod

class SimpleSubscriberInfo : AbstractSubscriberInfo {

    private val methodInfos: Array<SubscriberMethodInfo>

    constructor(
        subscriberClass: Class<*>,
        shouldCheckSuperclass: Boolean,
        methodInfos: Array<SubscriberMethodInfo>
    ) : super(subscriberClass, null, shouldCheckSuperclass) {
        this.methodInfos = methodInfos
    }

    override fun getSubscriberMethods(): Array<SubscriberMethod?> {
        val length = methodInfos.size
        val methods = arrayOfNulls<SubscriberMethod>(length)
        for (i in 0 until length) {
            val info = methodInfos[i]
            methods[i] = createSubscriberMethod(
                info.methodName,
                info.eventType,
                info.threadMode,
                info.priority,
                info.sticky
            )
        }
        return methods
    }

}