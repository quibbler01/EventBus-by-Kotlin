package cn.quibbler.eventbus

import cn.quibbler.eventbus.meta.SubscriberInfo
import cn.quibbler.eventbus.meta.SubscriberInfoIndex
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

/*
 * In newer class files, compilers may add methods. Those are called bridge or synthetic methods.
 * EventBus must ignore both. There modifiers are not public but defined in the Java class file format:
 * http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.6-200-A.1
 */
class SubscriberMethodFinder(
    private val subscriberInfoIndexes: MutableList<SubscriberInfoIndex>?,
    private val strictMethodVerification: Boolean,
    private val ignoreGeneratedIndex: Boolean
) {

    companion object {
        private const val BRIDGE = 0x40
        private const val SYNTHETIC = 0x1000

        private const val MODIFIERS_IGNORE =
            Modifier.ABSTRACT or Modifier.STATIC or BRIDGE or SYNTHETIC
        private val METHOD_CACHE: MutableMap<Class<*>, List<SubscriberMethod>> = ConcurrentHashMap()

        private const val POOL_SIZE = 4
        private val FIND_STATE_POOL: Array<FindState?> =
            arrayOfNulls<FindState>(POOL_SIZE)

        fun clearCaches() {
            METHOD_CACHE.clear()
        }

    }

    fun findSubscriberMethods(subscriberClass: Class<*>): List<SubscriberMethod> {
        var subscriberMethods: List<SubscriberMethod>? = METHOD_CACHE[subscriberClass]
        if (subscriberMethods != null) {
            return subscriberMethods
        }

        if (ignoreGeneratedIndex) {
            subscriberMethods = findUsingReflection(subscriberClass)
        } else {
            subscriberMethods = findUsingInfo(subscriberClass)
        }

        if (subscriberMethods.isEmpty()) {
            throw EventBusException(
                "Subscriber $subscriberClass  and its super classes have no public methods with the @Subscribe annotation"
            )
        } else {
            METHOD_CACHE[subscriberClass] = subscriberMethods
            return subscriberMethods
        }
    }

    private fun findUsingInfo(subscriberClass: Class<*>): List<SubscriberMethod> {
        val findState: FindState = prepareFindState()
        findState.initForSubscriber(subscriberClass)
        while (findState.clazz != null) {
            findState.subscriberInfo = getSubscriberInfo(findState)
            if (findState.subscriberInfo != null) {
                val array: Array<SubscriberMethod?>? = findState.subscriberInfo?.getSubscriberMethods()
                if (array != null) {
                    for (subscriberMethod in array) {
                        subscriberMethod?.let {
                            if (findState.checkAdd(subscriberMethod.method, subscriberMethod.eventType)) {
                                findState.subscriberMethods.add(subscriberMethod)
                            }
                        }
                    }
                }
            } else {
                findUsingReflectionInSingleClass(findState)
            }
            findState.moveToSuperclass()
        }
        return getMethodsAndRelease(findState)
    }

    private fun getMethodsAndRelease(findState: FindState): List<SubscriberMethod> {
        val subscriberMethods: ArrayList<SubscriberMethod> =
            ArrayList<SubscriberMethod>(findState.subscriberMethods)
        findState.recycle()
        synchronized(FIND_STATE_POOL) {
            for (i in 0 until POOL_SIZE) {
                if (FIND_STATE_POOL[i] == null) {
                    FIND_STATE_POOL[i] = findState
                    break
                }
            }
        }
        return subscriberMethods
    }

    private fun prepareFindState(): FindState {
        synchronized(FIND_STATE_POOL) {
            for (i in 0 until POOL_SIZE) {
                val state = FIND_STATE_POOL[i]
                if (state != null) {
                    FIND_STATE_POOL[i] = null
                    return state
                }
            }
        }
        return FindState()
    }

    private fun getSubscriberInfo(findState: FindState): SubscriberInfo? {
        if (findState.subscriberInfo != null && findState.subscriberInfo!!.getSuperSubscriberInfo() != null) {
            val superclassInfo = findState.subscriberInfo?.getSuperSubscriberInfo()
            if (findState.clazz == superclassInfo?.getSubscriberClass()) {
                return superclassInfo
            }
            if (subscriberInfoIndexes != null) {
                for (index in subscriberInfoIndexes) {
                    val info = index.getSubscriberInfo(findState.clazz)
                    if (info != null) {
                        return info
                    }
                }
            }
        }
        return null
    }

    private fun findUsingReflection(subscriberClass: Class<*>): List<SubscriberMethod> {
        val findState: FindState = prepareFindState()
        findState.initForSubscriber(subscriberClass)
        while (findState.clazz != null) {
            findUsingReflectionInSingleClass(findState)
            findState.moveToSuperclass()
        }
        return getMethodsAndRelease(findState)
    }

    private fun findUsingReflectionInSingleClass(findState: FindState) {
        var methods: Array<Method>? = null
        try {
            // This is faster than getMethods, especially when subscribers are fat classes like Activities
            methods = findState.clazz?.declaredMethods
        } catch (th: Throwable) {
            // Workaround for java.lang.NoClassDefFoundError, see https://github.com/greenrobot/EventBus/issues/149
            try {
                methods = findState.clazz?.methods
            } catch (error: LinkageError) { // super class of NoClassDefFoundError to be a bit more broad...
                // super class of NoClassDefFoundError to be a bit more broad...
                var msg = "Could not inspect methods of ${findState.clazz?.name}"
                msg += if (ignoreGeneratedIndex) {
                    ". Please consider using EventBus annotation processor to avoid reflection."
                } else {
                    ". Please make this class visible to EventBus annotation processor to avoid reflection."
                }
            }
            findState.skipSuperClasses = true
        }
        methods?.let {
            for (method in it) {
                val modifiers: Int = method.modifiers
                if ((modifiers and Modifier.PUBLIC) != 0 && (modifiers and MODIFIERS_IGNORE) == 0) {
                    val parameterTypes: Array<Class<*>> = method.parameterTypes
                    if (parameterTypes.size == 1) {
                        val subscribeAnnotation: Subscribe? =
                            method.getAnnotation(Subscribe::class.java)
                        if (subscribeAnnotation != null) {
                            val eventType: Class<*> = parameterTypes[0]
                            if (findState.checkAdd(method, eventType)) {
                                val threadMode: ThreadMode = subscribeAnnotation.threadMode
                                findState.subscriberMethods.add(
                                    SubscriberMethod(
                                        method,
                                        eventType,
                                        threadMode,
                                        subscribeAnnotation.priority,
                                        subscribeAnnotation.sticky
                                    )
                                )
                            }
                        }
                    } else if (strictMethodVerification && method.isAnnotationPresent(Subscribe::class.java)) {
                        val methodName = "${method.declaringClass.name}.${method.name}"
                        throw EventBusException(
                            "@Subscribe method $methodName must have exactly 1 parameter but has ${parameterTypes.size}"
                        )

                    }
                } else if (strictMethodVerification && method.isAnnotationPresent(Subscribe::class.java)) {
                    val methodName: String = "${method.declaringClass.name}.${method.name}"
                    throw EventBusException("$methodName is a illegal @Subscribe method: must be public, non-static, and non-abstract")
                }
            }
        }
    }


    class FindState {
        val subscriberMethods: ArrayList<SubscriberMethod> = ArrayList()
        val anyMethodByEventType: HashMap<Class<*>, Any> = HashMap()
        val subscriberClassByMethodKey: HashMap<String, Class<*>> = HashMap()
        val methodKeyBuilder = StringBuilder(128)

        var subscriberClass: Class<*>? = null
        var clazz: Class<*>? = null
        var skipSuperClasses = false
        var subscriberInfo: SubscriberInfo? = null

        fun initForSubscriber(subscriberClass: Class<*>) {
            this.subscriberClass = subscriberClass.also { clazz = it }
            skipSuperClasses = false
            subscriberInfo = null
        }

        fun recycle() {
            subscriberMethods.clear()
            anyMethodByEventType.clear()
            subscriberClassByMethodKey.clear()
            methodKeyBuilder.setLength(0)
            subscriberClass = null
            clazz = null
            skipSuperClasses = false
            subscriberInfo = null
        }

        fun checkAdd(method: Method, eventType: Class<*>): Boolean {
            // 2 level check: 1st level with event type only (fast), 2nd level with complete signature when required.
            // Usually a subscriber doesn't have methods listening to the same event type.
            val existing: Any? = anyMethodByEventType.put(eventType, method)
            if (existing == null) {
                return true
            } else {
                if (existing is Method) {
                    if (!checkAddWithMethodSignature(existing as Method, eventType)) {
                        // Paranoia check
                        throw IllegalStateException()
                    }
                    // Put any non-Method object to "consume" the existing Method
                    // Put any non-Method object to "consume" the existing Method
                    anyMethodByEventType[eventType] = this
                }
                return checkAddWithMethodSignature(method, eventType)
            }
        }

        private fun checkAddWithMethodSignature(method: Method, eventType: Class<*>): Boolean {
            methodKeyBuilder.setLength(0)
            methodKeyBuilder.append(method.name)
            methodKeyBuilder.append('>').append(eventType.name)

            val methodKey = methodKeyBuilder.toString()
            val methodClass = method.declaringClass
            val methodClassOld: Class<*>? = subscriberClassByMethodKey.put(methodKey, methodClass)
            if (methodClassOld == null || methodClassOld.isAssignableFrom(methodClass)) {
                // Only add if not already found in a sub class
                return true
            } else {
                // Revert the put, old class is further down the class hierarchy
                subscriberClassByMethodKey.put(methodKey, methodClassOld);
                return false
            }
        }

        fun moveToSuperclass() {
            if (skipSuperClasses) {
                clazz = null
            } else {
                clazz = clazz?.superclass
                val clazzName: String? = clazz?.name
                // Skip system classes, this degrades performance.
                // Also we might avoid some ClassNotFoundException (see FAQ for background).
                // Skip system classes, this degrades performance.
                // Also we might avoid some ClassNotFoundException (see FAQ for background).
                clazzName?.let {
                    if (clazzName.startsWith("java.") || clazzName.startsWith("javax.") ||
                        clazzName.startsWith("android.") || clazzName.startsWith("androidx.")
                    ) {
                        clazz = null
                    }
                }
            }
        }

    }

}