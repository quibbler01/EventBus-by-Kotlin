package cn.quibbler.eventbus.util

/**
 * A generic failure event, which can be used by apps to propagate thrown exceptions.
 * Used as default failure event by {@link AsyncExecutor}.
 */
class ThrowableFailureEvent(
    val throwable: Throwable,
    val suppressErrorUi: Boolean
) : HasExecutionScope {

    var executionContext: Any? = null

    /**
     * @param suppressErrorUi
     *            true indicates to the receiver that no error UI (e.g. dialog) should now displayed.
     */
    constructor(throwable: Throwable) : this(throwable, false)

    override fun getExecutionScope(): Any? = executionContext

    override fun setExecutionScope(executionContext: Any?) {
        this.executionContext = executionContext
    }

}