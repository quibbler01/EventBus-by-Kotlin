package cn.quibbler.EventBus

class EventBusException : RuntimeException {

    companion object {
        const val serialVersionUID = -2912559384646531479L
    }

    constructor(detailMessage: String) : super(detailMessage)

    constructor(throwable: Throwable) : super(throwable)

    constructor(detailMessage: String, throwable: Throwable?) : super(detailMessage, throwable)

}