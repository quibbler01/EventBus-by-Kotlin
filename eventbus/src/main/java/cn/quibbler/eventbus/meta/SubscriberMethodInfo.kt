package cn.quibbler.eventbus.meta

import cn.quibbler.eventbus.ThreadMode

class SubscriberMethodInfo(
    val methodName: String,
    val eventType: Class<*>,
    val threadMode: ThreadMode,
    val priority: Int,
    val sticky: Boolean
) {

    constructor(methodName: String, eventType: Class<*>) : this(
        methodName,
        eventType,
        ThreadMode.POSTING,
        0,
        false
    )

    constructor(methodName: String, eventType: Class<*>, threadMode: ThreadMode) : this(
        methodName,
        eventType,
        threadMode,
        0,
        false
    )

}