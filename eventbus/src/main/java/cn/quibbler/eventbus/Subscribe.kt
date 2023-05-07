package cn.quibbler.eventbus

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
annotation class Subscribe(

    val threadMode: ThreadMode = ThreadMode.POSTING,

    /**
     * If true, delivers the most recent sticky event (posted with
     * [EventBus.postSticky]) to this subscriber (if event available).
     */
    val sticky: Boolean = false,

    /** Subscriber priority to influence the order of event delivery.
     * Within the same delivery thread ([ThreadMode]), higher priority subscribers will receive events before
     * others with a lower priority. The default priority is 0. Note: the priority does *NOT* affect the order of
     * delivery among subscribers with different [ThreadMode]s!  */
    val priority: Int = 0

)