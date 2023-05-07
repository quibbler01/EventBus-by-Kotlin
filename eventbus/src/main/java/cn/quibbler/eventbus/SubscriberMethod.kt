package cn.quibbler.eventbus

import java.lang.StringBuilder
import java.lang.reflect.Method

/** Used internally by EventBus and generated subscriber indexes. */
class SubscriberMethod(
    val method: Method,
    val eventType: Class<*>,
    val threadMode: ThreadMode,
    val priority: Int,
    val sticky: Boolean
) {

    /** Used for efficient comparison */
    var methodString: String? = null

    override fun equals(other: Any?): Boolean {
        if (other == this) {
            return true
        } else if (other is SubscriberMethod) {
            checkMethodString()
            other.checkMethodString()
            // Don't use method.equals because of http://code.google.com/p/android/issues/detail?id=7811#c6
            return methodString.equals(other.methodString)
        } else {
            return false
        }
    }

    @Synchronized
    private fun checkMethodString() {
        if (methodString == null) {
            // Method.toString has more overhead, just take relevant parts of the method
            val builder = StringBuilder(64)
            builder.append(method.declaringClass.name)
            builder.append('#').append(method.name)
            builder.append('(').append(eventType.name)
            methodString = builder.toString()
        }
    }

    override fun hashCode(): Int {
        return method.hashCode()
    }

}