package cn.quibbler.eventbus.util

import cn.quibbler.eventbus.EventBus
import java.lang.reflect.Constructor
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class AsyncExecutor private constructor(
    private val threadPool: Executor,
    private val eventBus: EventBus,
    failureEventType: Class<*>,
    private val scope: Any?
) {

    private val failureEventConstructor: Constructor<*>

    init {
        try {
            failureEventConstructor = failureEventType.getConstructor(Throwable::class.java)
        } catch (e: NoSuchMethodException) {
            throw RuntimeException("")
        }
    }

    companion object {

        fun builder(): Builder = Builder()

        fun create(): AsyncExecutor = Builder().build()

    }

    class Builder constructor() {

        private var threadPool: Executor? = null

        private var failureEventType: Class<*>? = null

        private var eventBus: EventBus? = null

        fun threadPool(threadPool: Executor): Builder {
            this.threadPool = threadPool
            return this
        }

        fun failureEventType(failureEventType: Class<*>): Builder {
            this.failureEventType = failureEventType
            return this
        }

        fun eventBus(eventBus: EventBus): Builder {
            this.eventBus = eventBus
            return this
        }

        fun build(): AsyncExecutor = buildForScope(null)

        fun buildForScope(executionContext: Any?): AsyncExecutor {
            if (eventBus == null) {
                eventBus = EventBus.getDefault()
            }
            if (threadPool == null) {
                threadPool = Executors.newCachedThreadPool()
            }
            if (failureEventType == null) {
                failureEventType = ThrowableFailureEvent::class.java
            }
            return AsyncExecutor(threadPool!!, eventBus!!, failureEventType!!, executionContext)
        }
    }

    interface RunnableEx {
        @Throws(Exception::class)
        fun run()
    }

}