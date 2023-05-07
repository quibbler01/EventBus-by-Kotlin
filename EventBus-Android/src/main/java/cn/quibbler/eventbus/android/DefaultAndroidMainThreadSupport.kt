package cn.quibbler.eventbus.android

import android.os.Looper
import cn.quibbler.eventbus.EventBus
import cn.quibbler.eventbus.HandlerPoster
import cn.quibbler.eventbus.MainThreadSupport
import cn.quibbler.eventbus.Poster

class DefaultAndroidMainThreadSupport : MainThreadSupport {

    override fun isMainThread(): Boolean {
        return Looper.getMainLooper() == Looper.myLooper()
    }

    override fun createPoster(eventBus: EventBus): Poster {
        return HandlerPoster(eventBus, Looper.getMainLooper(), 10)
    }

}