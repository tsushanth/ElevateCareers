package com.kreativekoala.elevatecareers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * The jobs API returns salaries as strings (for example "190900"), while older
 * payloads used numbers. Accept both, and turn anything unparseable into null
 * instead of failing the whole job.
 */
object FlexibleIntSerializer : KSerializer<Int?> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int? {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeInt()
        val element = jsonDecoder.decodeJsonElement()
        if (element is JsonNull) return null
        val primitive = element as? JsonPrimitive ?: return null
        return primitive.content.trim().toDoubleOrNull()?.takeIf { it.isFinite() }?.toInt()
    }

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value == null) encoder.encodeNull() else encoder.encodeInt(value)
    }
}
