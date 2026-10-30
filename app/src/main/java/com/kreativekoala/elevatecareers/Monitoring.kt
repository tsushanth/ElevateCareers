package com.kreativekoala.elevatecareers

/**
 * Reports a handled failure with a stable message per flow. Only the exception CLASS and its stack frames are sent:
 * exception messages (server bodies, user text, urls with tokens) never leave the device.
 */
fun reportFailure(flow: String, t: Throwable? = null, message: String? = null) {
    val safe = t?.let { src -> Exception(src.javaClass.simpleName).apply { stackTrace = src.stackTrace } }
    FailureReporter.failure(flow, safe, message ?: "$flow failed${t?.let { ": " + it.javaClass.simpleName } ?: ""}")
}
