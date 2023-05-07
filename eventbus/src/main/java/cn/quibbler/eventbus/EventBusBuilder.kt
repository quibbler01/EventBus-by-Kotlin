package cn.quibbler.eventbus

import cn.quibbler.eventbus.android.AndroidComponents
import cn.quibbler.eventbus.meta.SubscriberInfoIndex
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Creates EventBus instances with custom parameters and also allows to install a custom default EventBus instance.
 * Create a new builder using {@link EventBus#builder()}.
 */
class EventBusBuilder {

    companion object {
        private val DEFAULT_EXECUTOR_SERVICE = Executors.newCachedThreadPool()
    }

    var logSubscriberExceptions = true
    var logNoSubscriberMessages = true
    var sendSubscriberExceptionEvent = true
    var sendNoSubscriberEvent = true
    var throwSubscriberException = false
    var eventInheritance = true
    var ignoreGeneratedIndex = false
    var strictMethodVerification = false
    var executorService:ExecutorService = DEFAULT_EXECUTOR_SERVICE
    var skipMethodVerificationForClasses: ArrayList<Class<*>>? = null
    var subscriberInfoIndexes: ArrayList<SubscriberInfoIndex>? = null
    var logger: Logger? = null
        get() {
            if (field != null) {
                return field
            } else {
                return Logger.Default.get()
            }
        }

    var mainThreadSupport: MainThreadSupport? = null
        get() {
            if (field != null) {
                return field
            } else if (AndroidComponents.areAvailable()) {
                return AndroidComponents.get()!!.defaultMainThreadSupport
            } else {
                return null
            }
        }

    /** Default: true */
    fun logSubscriberExceptions(logSubscriberExceptions: Boolean): EventBusBuilder {
        this.logSubscriberExceptions = logSubscriberExceptions
        return this
    }

    /** Default: true */
    fun logNoSubscriberMessages(logNoSubscriberMessages: Boolean): EventBusBuilder {
        this.logNoSubscriberMessages = logNoSubscriberMessages
        return this
    }

    /** Default: true */
    fun sendSubscriberExceptionEvent(sendSubscriberExceptionEvent: Boolean): EventBusBuilder {
        this.sendSubscriberExceptionEvent = sendSubscriberExceptionEvent
        return this
    }


    /** Default: true */
    fun sendNoSubscriberEvent(sendNoSubscriberEvent: Boolean): EventBusBuilder {
        this.sendNoSubscriberEvent = sendNoSubscriberEvent
        return this
    }

    /**
     * Fails if an subscriber throws an exception (default: false).
     * <p/>
     * Tip: Use this with BuildConfig.DEBUG to let the app crash in DEBUG mode (only). This way, you won't miss
     * exceptions during development.
     */
    fun throwSubscriberException(throwSubscriberException: Boolean): EventBusBuilder {
        this.throwSubscriberException = throwSubscriberException
        return this
    }

    /**
     * By default, EventBus considers the event class hierarchy (subscribers to super classes will be notified).
     * Switching this feature off will improve posting of events. For simple event classes extending Object directly,
     * we measured a speed up of 20% for event posting. For more complex event hierarchies, the speed up should be
     * greater than 20%.
     * <p/>
     * However, keep in mind that event posting usually consumes just a small proportion of CPU time inside an app,
     * unless it is posting at high rates, e.g. hundreds/thousands of events per second.
     */
    fun eventInheritance(eventInheritance: Boolean): EventBusBuilder {
        this.eventInheritance = eventInheritance
        return this
    }

    /**
     * Provide a custom thread pool to EventBus used for async and background event delivery. This is an advanced
     * setting to that can break things: ensure the given ExecutorService won't get stuck to avoid undefined behavior.
     */
    fun executorService(executorService: ExecutorService): EventBusBuilder {
        this.executorService = executorService
        return this
    }

    /**
     * Method name verification is done for methods starting with onEvent to avoid typos; using this method you can
     * exclude subscriber classes from this check. Also disables checks for method modifiers (public, not static nor
     * abstract).
     */
    fun skipMethodVerificationFor(clazz: Class<*>): EventBusBuilder {
        if (skipMethodVerificationForClasses == null) {
            skipMethodVerificationForClasses = ArrayList()
        }
        skipMethodVerificationForClasses?.add(clazz)
        return this
    }

    /** Forces the use of reflection even if there's a generated index (default: false). */
    fun ignoreGeneratedIndex(ignoreGeneratedIndex: Boolean): EventBusBuilder {
        this.ignoreGeneratedIndex = ignoreGeneratedIndex
        return this
    }

    /** Enables strict method verification (default: false). */
    fun strictMethodVerification(strictMethodVerification: Boolean): EventBusBuilder {
        this.strictMethodVerification = strictMethodVerification
        return this
    }

    /** Adds an index generated by EventBus' annotation preprocessor. */
    fun addIndex(index: SubscriberInfoIndex): EventBusBuilder {
        if (subscriberInfoIndexes == null) {
            subscriberInfoIndexes = ArrayList()
        }
        subscriberInfoIndexes?.add(index)
        return this
    }

    /**
     * Set a specific log handler for all EventBus logging.
     * <p/>
     * By default, all logging is via {@code android.util.Log} on Android or System.out on JVM.
     */
    fun logger(logger: Logger?): EventBusBuilder {
        this.logger = logger
        return this
    }

    fun installDefaultEventBus(): EventBus {
        synchronized(EventBus::class) {
            if (EventBus.defaultInstance != null) {
                throw  EventBusException(
                    "Default instance already exists. It may be only set once before it's used the first time to ensure consistent behavior."
                )
            }
            EventBus.defaultInstance = build()
            return EventBus.defaultInstance!!
        }
    }

    /** Builds an EventBus based on the current configuration. */
    fun build(): EventBus {
        return EventBus(this)
    }

}