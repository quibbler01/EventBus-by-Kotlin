package cn.quibbler.EventBus.util

interface HasExecutionScope {

    fun getExecutionScope(): Any?

    fun setExecutionScope(executionScope: Any?)

}