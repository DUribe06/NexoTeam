package com.grupo3.nexo.data.remote

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Lee un enum del contrato sin fallar ante un valor nuevo: si el servidor agrega un estado, ese valor llega como
 * [desconocido] y el mapeador decide qué hacer con él, en vez de que toda la respuesta se pierda.
 */
open class EnumTolerante<T : Enum<T>>(
    nombre: String,
    private val valores: List<T>,
    private val desconocido: T
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(nombre, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(value.name)

    override fun deserialize(decoder: Decoder): T {
        val texto = decoder.decodeString()
        return valores.firstOrNull { it.name == texto } ?: desconocido
    }
}
