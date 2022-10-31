package cn.quibbler.EventBus.android

import android.os.Looper
import cn.quibbler.EventBus.EventBus
import cn.quibbler.EventBus.HandlerPoster
import cn.quibbler.EventBus.MainThreadSupport
import cn.quibbler.EventBus.Poster

class DefaultAndroidMainThreadSupport : MainThreadSupport {

    override fun isMainThread(): Boolean {
        return Looper.getMainLooper() == Looper.myLooper()
    }

    override fun createPoster(eventBus: EventBus): Poster {
        return HandlerPoster(eventBus, Looper.getMainLooper(), 10)
    }

}