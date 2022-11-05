package cn.quibbler.EventBus

/**
 * Interface to the "main" thread, which can be whatever you like. Typically on Android, Android's main thread is used.
 */
interface MainThreadSupport {

    fun isMainThread(): Boolean

    fun createPoster(eventBus: EventBus): Poster

}