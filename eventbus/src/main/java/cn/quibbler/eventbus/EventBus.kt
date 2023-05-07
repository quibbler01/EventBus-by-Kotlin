package cn.quibbler.eventbus

import cn.quibbler.eventbus.android.AndroidDependenciesDetector
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.logging.Level

class EventBus {

    companion object {

        /** Log tag, apps may override it.  */
        const val TAG = "EventBus"

        private val DEFAULT_BUILDER = EventBusBuilder()
        private val eventTypesCache: MutableMap<Class<*>, MutableList<Class<*>>> = HashMap()

        @Volatile
        var defaultInstance: EventBus? = null

        /** Convenience singleton for apps using a process-wide EventBus instance. */
        fun getDefault(): EventBus {
            var instance: EventBus? = defaultInstance
            if (instance == null) {
                synchronized(EventBus::class) {
                    instance = EventBus.defaultInstance
                    if (instance == null) {
                        EventBus.defaultInstance = EventBus()
                        instance = EventBus.defaultInstance
                    }
                }
            }
            return instance!!
        }

        fun builder(): EventBusBuilder = EventBusBuilder()

        /** For unit test primarily. */
        fun clearCaches() {
            SubscriberMethodFinder.clearCaches()
            eventTypesCache.clear()
        }

        /** Looks up all Class objects including super classes and interfaces. Should also work for interfaces. */
        private fun lookupAllEventTypes(eventClass: Class<*>): List<Class<*>> {
            synchronized(eventTypesCache) {
                var eventTypes: MutableList<Class<*>>? = eventTypesCache[eventClass]
                if (eventTypes == null) {
                    eventTypes = ArrayList()
                    var clazz: Class<*>? = eventClass
                    while (clazz != null) {
                        eventTypes.add(clazz)
                        addInterfaces(eventTypes, clazz.getInterfaces())
                        clazz = clazz.superclass
                    }
                }
                return eventTypes
            }
        }

        /** Recurses through super interfaces. */
        private fun addInterfaces(eventTypes: MutableList<Class<*>>, interfaces: Array<Class<*>>) {
            for (interfaceClass in interfaces) {
                if (!eventTypes.contains(interfaceClass)) {
                    eventTypes.add(interfaceClass)
                    addInterfaces(eventTypes, interfaceClass.interfaces)
                }
            }
        }

    }

    private lateinit var subscriptionsByEventType: MutableMap<Class<*>, CopyOnWriteArrayList<Subscription>>
    private lateinit var typesBySubscriber: MutableMap<Any, MutableList<Class<*>>>
    private lateinit var stickyEvents: MutableMap<Class<*>, Any>

    private val currentPostingThreadState: ThreadLocal<PostingThreadState> =
        object : ThreadLocal<PostingThreadState>() {
            override fun initialValue(): PostingThreadState {
                return PostingThreadState()
            }
        }

    // @Nullable
    private var mainThreadSupport: MainThreadSupport? = null

    // @Nullable
    private var mainThreadPoster: Poster? = null
    private var backgroundPoster: BackgroundPoster? = null
    private var asyncPoster: AsyncPoster? = null
    private lateinit var subscriberMethodFinder: SubscriberMethodFinder
    private lateinit var executorService: ExecutorService

    private var throwSubscriberException = false
    private var logSubscriberExceptions = false
    private var logNoSubscriberMessages = false
    private var sendSubscriberExceptionEvent = false
    private var sendNoSubscriberEvent = false
    private var eventInheritance = false

    private var indexCount = 0
    private lateinit var logger: Logger

    /**
     * Creates a new EventBus instance; each instance is a separate scope in which events are delivered. To use a
     * central bus, consider {@link #getDefault()}.
     */
    constructor() : this(DEFAULT_BUILDER)

    constructor(builder: EventBusBuilder) {
        logger = builder.logger!!
        subscriptionsByEventType = java.util.HashMap()
        typesBySubscriber = java.util.HashMap()
        stickyEvents = ConcurrentHashMap()
        mainThreadSupport = builder.mainThreadSupport
        mainThreadPoster =
            if (mainThreadSupport != null) mainThreadSupport!!.createPoster(this) else null
        backgroundPoster = BackgroundPoster(this)
        asyncPoster = AsyncPoster(this)
        indexCount =
            if (builder.subscriberInfoIndexes != null) builder.subscriberInfoIndexes!!.size else 0
        subscriberMethodFinder = SubscriberMethodFinder(
            builder.subscriberInfoIndexes,
            builder.strictMethodVerification, builder.ignoreGeneratedIndex
        )
        logSubscriberExceptions = builder.logSubscriberExceptions
        logNoSubscriberMessages = builder.logNoSubscriberMessages
        sendSubscriberExceptionEvent = builder.sendSubscriberExceptionEvent
        sendNoSubscriberEvent = builder.sendNoSubscriberEvent
        throwSubscriberException = builder.throwSubscriberException
        eventInheritance = builder.eventInheritance
        executorService = builder.executorService
    }

    /**
     * Registers the given subscriber to receive events. Subscribers must call {@link #unregister(Object)} once they
     * are no longer interested in receiving events.
     * <p/>
     * Subscribers have event handling methods that must be annotated by {@link Subscribe}.
     * The {@link Subscribe} annotation also allows configuration like {@link
     * ThreadMode} and priority.
     */
    fun register(subscriber: Any) {
        if (AndroidDependenciesDetector.isAndroidSDKAvailable() && !AndroidDependenciesDetector.areAndroidComponentsAvailable()) {
            throw RuntimeException(
                "It looks like you are using EventBus on Android, " +
                        "make sure to add the \"eventbus\" Android library to your dependencies."
            )
        }

        val subscriberClass: Class<*> = subscriber.javaClass
        val subscriberMethods: List<SubscriberMethod> =
            subscriberMethodFinder.findSubscriberMethods(subscriberClass)
        synchronized(this) {
            for (subscriberMethod in subscriberMethods) {
                subscribe(subscriber, subscriberMethod)
            }
        }

    }

    // Must be called in synchronized block
    private fun subscribe(subscriber: Any, subscriberMethod: SubscriberMethod) {
        val eventType: Class<*> = subscriberMethod.eventType
        val newSubscription = Subscription(subscriber, subscriberMethod)
        var subscriptions: CopyOnWriteArrayList<Subscription>? = subscriptionsByEventType[eventType]
        if (subscriptions == null) {
            subscriptions = CopyOnWriteArrayList()
            subscriptionsByEventType[eventType] = subscriptions
        } else {
            if (subscriptions.contains(newSubscription)) {
                throw EventBusException("Subscriber ${subscriber.javaClass} already registered to event $eventType")
            }
        }

        val size = subscriptions.size
        for (i in 0..size) {
            if (i == size || subscriberMethod.priority > subscriptions[i].subscriberMethod.priority) {
                subscriptions[i] = newSubscription
                break
            }
        }

        var subscribedEvents: MutableList<Class<*>>? = typesBySubscriber[subscriber]
        if (subscribedEvents == null) {
            subscribedEvents = ArrayList()
            typesBySubscriber[subscriber] = subscribedEvents
        }
        subscribedEvents.add(eventType)

        if (subscriberMethod.sticky) {
            if (eventInheritance) {
                val entries = stickyEvents.entries
                for (entry in entries) {
                    val candidateEventType: Class<*> = entry.key
                    if (eventType.isAssignableFrom(candidateEventType)) {
                        val stickyEvent = stickyEvents.values
                        checkPostStickyEventToSubscription(newSubscription, stickyEvent)
                    }
                }
            } else {
                val stickyEvent = stickyEvents[eventType]
                checkPostStickyEventToSubscription(newSubscription, stickyEvent)
            }
        }
    }

    private fun checkPostStickyEventToSubscription(
        newSubscription: Subscription,
        stickyEvent: Any?
    ) {
        // If the subscriber is trying to abort the event, it will fail (event is not tracked in posting state)
        // --> Strange corner case, which we don't take care of here.
        if (stickyEvent != null) {
            postToSubscription(newSubscription, stickyEvent, isMainThread())
        }
    }

    private fun isMainThread(): Boolean =
        mainThreadSupport == null || mainThreadSupport!!.isMainThread()

    @Synchronized
    fun isRegistered(subscriber: Any): Boolean {
        return typesBySubscriber.containsKey(subscriber)
    }

    /** Only updates subscriptionsByEventType, not typesBySubscriber! Caller must update typesBySubscriber. */
    private fun unsubscribeByEventType(subscriber: Any, eventType: Class<*>) {
        val subscriptions = subscriptionsByEventType[eventType]
        if (subscriptions != null) {
            var size = subscriptions.size
            var i = 0
            while (i < size) {
                val subscription = subscriptions[i]
                if (subscription.subscriber == subscriber) {
                    subscription.active = false
                    subscriptions.removeAt(i)
                    i--
                    size--
                }
                ++i
            }
        }
    }

    @Synchronized
    fun unregister(subscriber: Any) {
        val subscribedTypes = typesBySubscriber[subscriber]
        if (subscribedTypes != null) {
            for (eventType in subscribedTypes) {
                unsubscribeByEventType(subscriber, eventType)
            }
            typesBySubscriber.remove(subscriber)
        } else {
            logger.log(
                Level.WARNING,
                "Subscriber to unregister was not registered before: ${subscriber.javaClass}"
            )
        }
    }

    /** Posts the given event to the event bus. */
    fun post(event: Any) {
        val postingState: PostingThreadState = currentPostingThreadState.get()
        val eventQueue = postingState.eventQueue
        eventQueue.add(event)

        if (postingState.isPosting) {
            postingState.isMainThread = isMainThread()
            postingState.isPosting = true
            if (postingState.canceled) {
                throw EventBusException("")
            }
            try {
                while (!eventQueue.isEmpty()) {
                    postSingleEvent(eventQueue.remove(0), postingState)
                }
            } finally {
                postingState.isPosting = false
                postingState.isMainThread = false
            }
        }
    }

    /**
     * Called from a subscriber's event handling method, further event delivery will be canceled. Subsequent
     * subscribers
     * won't receive the event. Events are usually canceled by higher priority subscribers (see
     * {@link Subscribe#priority()}). Canceling is restricted to event handling methods running in posting thread
     * {@link ThreadMode#POSTING}.
     */
    fun cancelEventDelivery(event: Any) {
        val postingState: PostingThreadState = currentPostingThreadState.get()
        if (!postingState.isPosting) {
            throw EventBusException("This method may only be called from inside event handling methods on the posting thread")
        } else if (event == null) {
            throw EventBusException("Event may not be null")
        } else if (postingState.event != event) {
            throw EventBusException("Only the currently handled event may be aborted")
        } else if (postingState.subscription!!.subscriberMethod.threadMode != ThreadMode.POSTING) {
            throw EventBusException(" event handlers may only abort the incoming event")
        }

        postingState.canceled = true
    }

    /**
     * Posts the given event to the event bus and holds on to the event (because it is sticky). The most recent sticky
     * event of an event's type is kept in memory for future access by subscribers using {@link Subscribe#sticky()}.
     */
    fun postSticky(event: Any) {
        synchronized(stickyEvents) {
            stickyEvents[event.javaClass] = event
        }

        // Should be posted after it is putted, in case the subscriber wants to remove immediately
        post(event)
    }

    /**
     * Gets the most recent sticky event for the given type.
     *
     * @see #postSticky(Object)
     */
    fun <T> getStickyEvent(eventType: Class<T>): T? {
        synchronized(stickyEvents) {
            return eventType.cast(stickyEvents[eventType])
        }
    }

    /**
     * Remove and gets the recent sticky event for the given event type.
     *
     * @see #postSticky(Object)
     */
    fun <T> removeStickyEvent(eventType: Class<T>): T? {
        synchronized(stickyEvents) {
            return eventType.cast(stickyEvents.remove(eventType))
        }
    }


    fun removeStickyEvent(event: Any): Boolean {
        synchronized(stickyEvents) {
            val eventType = event.javaClass
            val existingEvent: Any? = stickyEvents[eventType]
            if (event == existingEvent) {
                stickyEvents.remove(eventType)
                return true
            } else {
                return false
            }
        }
    }

    /**
     * Removes all sticky events.
     */
    fun removeAllStickyEvents() {
        synchronized(stickyEvents) {
            stickyEvents.clear()
        }
    }

    fun hasSubscriberForEvent(eventClass: Class<*>): Boolean {
        val eventTypes: List<Class<*>>? = lookupAllEventTypes(eventClass)
        if (eventTypes != null) {
            val countTypes = eventTypes.size
            for (h in 0 until countTypes) {
                val clazz = eventTypes[h]
                var subscriptions: CopyOnWriteArrayList<Subscription>? = null
                synchronized(this) {
                    subscriptions = subscriptionsByEventType[clazz]
                }
                if (subscriptions != null && !subscriptions!!.isEmpty()) {
                    return true
                }
            }
        }
        return false
    }

    @Throws(Error::class)
    private fun postSingleEvent(event: Any, postingState: PostingThreadState) {
        val eventClass = event.javaClass
        var subscriptionFound = false
        if (eventInheritance) {
            val eventTypes: List<Class<*>> = lookupAllEventTypes(eventClass)
            val countTypes = eventTypes.size
            for (h in 0 until countTypes) {
                val clazz = eventTypes[h]
                subscriptionFound =
                    subscriptionFound or postSingleEventForEventType(event, postingState, clazz)
            }
        } else {
            subscriptionFound = postSingleEventForEventType(event, postingState, eventClass)
        }
        if (!subscriptionFound) {
            if (logNoSubscriberMessages) {
                logger.log(Level.FINE, "No subscribers registered for event " + eventClass)
            }
            if (sendNoSubscriberEvent && eventClass != NoSubscriberEvent::javaClass && eventClass != SubscriberExceptionEvent::class) {
                post(NoSubscriberEvent(this, event))
            }
        }
    }

    private fun postSingleEventForEventType(
        event: Any,
        postingState: PostingThreadState,
        eventClass: Class<*>
    ): Boolean {
        var subscriptions: CopyOnWriteArrayList<Subscription>? = null
        synchronized(this) {
            subscriptions = subscriptionsByEventType[eventClass]
        }

        if (subscriptions != null && !subscriptions!!.isEmpty()) {
            for (subscription in subscriptions!!) {
                postingState.event = event
                postingState.subscription = subscription
                var aborted = false
                try {
                    postToSubscription(subscription, event, postingState.isMainThread)
                    aborted = postingState.canceled
                } finally {
                    postingState.event = null
                    postingState.subscription = null
                    postingState.canceled = false
                }
                if (aborted) {
                    break
                }
            }
            return true
        }
        return false
    }

    private fun postToSubscription(subscription: Subscription, event: Any, isMainThread: Boolean) {
        when (subscription.subscriberMethod.threadMode) {
            ThreadMode.POSTING -> {
                invokeSubscriber(subscription, event)
            }
            ThreadMode.MAIN -> {
                if (isMainThread) {
                    invokeSubscriber(subscription, event)
                } else {
                    mainThreadPoster?.enqueue(subscription, event)
                }
            }
            ThreadMode.MAIN_ORDERED -> {
                if (mainThreadPoster != null) {
                    mainThreadPoster?.enqueue(subscription, event)
                } else {
                    // temporary: technically not correct as poster not decoupled from subscriber
                    invokeSubscriber(subscription, event)
                }
            }
            ThreadMode.BACKGROUND -> {
                if (isMainThread) {
                    backgroundPoster?.enqueue(subscription, event)
                } else {
                    invokeSubscriber(subscription, event)
                }
            }
            ThreadMode.ASYNC -> {
                asyncPoster?.enqueue(subscription, event)
            }
            else -> {
                throw IllegalStateException("Unknown thread mode: ${subscription.subscriberMethod.threadMode}")
            }
        }
    }

    /**
     * Invokes the subscriber if the subscriptions is still active. Skipping subscriptions prevents race conditions
     * between {@link #unregister(Object)} and event delivery. Otherwise the event might be delivered after the
     * subscriber unregistered. This is particularly important for main thread delivery and registrations bound to the
     * live cycle of an Activity or Fragment.
     */
    fun invokeSubscriber(pendingPost: PendingPost) {
        val event: Any? = pendingPost.event
        val subscription: Subscription? = pendingPost.subscription
        PendingPost.releasePendingPost(pendingPost)
        if (subscription?.active ?: false) {
            invokeSubscriber(subscription, event)
        }
    }

    fun invokeSubscriber(subscription: Subscription?, event: Any?) {
        try {
            subscription?.let {
                it.subscriberMethod.method.invoke(subscription.subscriber, event)
            }
        } catch (e: InvocationTargetException) {
            handleSubscriberException(subscription, event, e.cause)
        } catch (e: IllegalAccessException) {
            throw IllegalStateException("Unexpected exception", e)
        }
    }

    private fun handleSubscriberException(
        subscription: Subscription?,
        event: Any?,
        cause: Throwable?
    ) {
        if (event is SubscriberExceptionEvent) {
            if (logSubscriberExceptions) {
                // Don't send another SubscriberExceptionEvent to avoid infinite event recursion, just log
                logger.log(
                    Level.SEVERE,
                    "SubscriberExceptionEvent subscriber " + subscription?.subscriber?.javaClass
                        .toString() + " threw an exception",
                    cause
                )
                val exEvent = event
                logger.log(
                    Level.SEVERE,
                    "Initial event " + exEvent.causingEvent.toString() + " caused exception in "
                            + exEvent.causingSubscriber,
                    exEvent.throwable
                )
            }
        } else {
            if (throwSubscriberException) {
                throw EventBusException("Invoking subscriber failed", cause)
            }
            if (logSubscriberExceptions) {
                logger.log(
                    Level.SEVERE,
                    "Could not dispatch event: ${event?.javaClass} to subscribing class ${subscription?.subscriber?.javaClass}",
                    cause
                )
            }
            if (sendSubscriberExceptionEvent) {
                val exEvent = SubscriberExceptionEvent(
                    this, cause, event,
                    subscription?.subscriber
                )
                post(exEvent)
            }
        }

    }

    /** For ThreadLocal, much faster to set (and get multiple values). */
    class PostingThreadState {
        val eventQueue: MutableList<Any> = ArrayList()
        var isPosting = false
        var isMainThread = false
        var subscription: Subscription? = null
        var event: Any? = null
        var canceled = false
    }

    fun getExecutorService(): ExecutorService = executorService

    /**
     * For internal use only.
     */
    fun getLogger(): Logger = logger

    // Just an idea: we could provide a callback to post() to be notified, an alternative would be events, of course...
    /* public */
    internal interface PostCallback {
        fun onPostCompleted(exceptionEvents: List<SubscriberExceptionEvent?>?)
    }

    override fun toString(): String {
        return "EventBus[indexCount=$indexCount, eventInheritance=$eventInheritance]"
    }

}