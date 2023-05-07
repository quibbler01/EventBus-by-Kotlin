package cn.quibbler.eventbus.util

interface HasExecutionScope {

    fun getExecutionScope(): Any?

    fun setExecutionScope(executionScope: Any?)

}