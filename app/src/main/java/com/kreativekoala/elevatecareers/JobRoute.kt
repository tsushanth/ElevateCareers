package com.kreativekoala.elevatecareers

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A [Job] as one navigation-route argument.
 *
 * The job used to be URL-encoded into the route and URL-decoded again in the destination. Navigation Compose already decodes route
 * arguments once, so the second decode (a) threw IllegalArgumentException ("Illegal hex characters in escape (%)") for any job whose
 * text contains a "%", crashing the app on the main thread, and (b) silently turned a literal "+" (C++, A+) into a space.
 *
 * The job is now the hex of its UTF-8 JSON: only 0-9 and a-f, so no layer (Uri parsing, Navigation's own decode, a saved-state
 * round trip) can change it, and no Android API is needed (plain JVM unit tests cover it). Anything that does not decode returns
 * null and the caller closes the screen instead of crashing.
 */
object JobRoute {
    fun encode(job: Job): String =
        Json.encodeToString(job).toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    fun decode(arg: String?): Job? {
        if (arg.isNullOrEmpty() || arg.length % 2 != 0) return null
        return try {
            val bytes = ByteArray(arg.length / 2) { i -> arg.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
            Json.decodeFromString<Job>(String(bytes, Charsets.UTF_8))
        } catch (e: Exception) {
            null // NumberFormatException (not hex), SerializationException (not a Job), IllegalArgumentException
        }
    }
}
