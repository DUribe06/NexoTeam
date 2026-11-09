package com.grupo3.nexo.domain.repository

/** Sube una imagen del teléfono a Cloudinary y devuelve su dirección pública (https). */
interface SubidorImagenes {
    /** `origen` es el content:// que entrega el selector de fotos; se reduce a `ladoMaximo` píxeles antes de subirla. */
    suspend fun subir(origen: String, ladoMaximo: Int): String
}
