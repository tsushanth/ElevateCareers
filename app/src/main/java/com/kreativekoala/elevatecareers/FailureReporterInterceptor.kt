package com.kreativekoala.elevatecareers

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Reports backend 5xx responses, a 404 on an /api/ route (a dead or wrong backend answers 404 for everything), and
 * connection-level failures (DNS, refused, timeout) which is what a deleted Fly/Cloud Run app looks like.
 * Ignores other 4xx (auth/validation noise). Path only, never the query; numeric/uuid segments are collapsed so
 * ids never reach the report.
 */
class FailureReporterInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val flow = "${request.method} ${normalize(request.url.encodedPath)}"
        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            FailureReporter.failure(flow, null, "network failure calling ${request.url.host}: ${e.javaClass.simpleName}")
            throw e
        }
        if (response.code >= 500 || (response.code == 404 && request.url.encodedPath.startsWith("/api/"))) {
            FailureReporter.backendError(flow = flow, message = "HTTP ${response.code} from ${request.url.host}")
        }
        return response
    }

    companion object {
        private val ID = Regex("^([0-9]+|[0-9a-fA-F-]{16,})$")
        fun normalize(path: String) = path.split('/').joinToString("/") { if (ID.matches(it)) ":id" else it }
    }
}
