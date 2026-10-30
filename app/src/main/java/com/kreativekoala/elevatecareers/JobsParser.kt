package com.kreativekoala.elevatecareers

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * Decodes the /jobs payload one job at a time, so a single malformed job is
 * skipped instead of failing the whole list.
 */
object JobsParser {

    /** Parses `{ "jobs": [ ... ] }` (or a bare array). Throws only if the body is not JSON at all. */
    fun parseList(body: String): List<Job> {
        val root: JsonElement = ApiConfig.json.parseToJsonElement(body)
        val array: JsonArray = when (root) {
            is JsonObject -> (root["jobs"] as? JsonArray) ?: JsonArray(emptyList())
            is JsonArray -> root
            else -> JsonArray(emptyList())
        }
        return array.mapNotNull { element -> decodeJobOrNull(element) }
    }

    /** Parses a single job object (the /jobs/{id} response). */
    fun parseJob(body: String): Job? = decodeJobOrNull(ApiConfig.json.parseToJsonElement(body))

    private fun decodeJobOrNull(element: JsonElement): Job? {
        return try {
            ApiConfig.json.decodeFromJsonElement(Job.serializer(), element.jsonObject)
        } catch (e: Exception) {
            null
        }
    }
}
