package cn.quibbler.EventBus.android

/**
 * Used via reflection in the Java library by {@link AndroidDependenciesDetector}.
 */
class AndroidComponentsImpl : AndroidComponents(AndroidLogger("EventBus"), DefaultAndroidMainThreadSupport())