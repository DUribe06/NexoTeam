package com.grupo3.nexo.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.grupo3.nexo.BuildConfig
import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.repository.SubidorImagenes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Sube directo a Cloudinary con un "upload preset" sin firma: la app no guarda ninguna clave secreta. La imagen se
 * reduce y se pasa a JPEG antes de salir del teléfono, así pesa poco y sube rápido con datos móviles.
 */
@Singleton
class SubidorCloudinary @Inject constructor(
    @ApplicationContext private val contexto: Context
) : SubidorImagenes {

    // Cliente propio: el de la API añade la sesión de Nexo, que Cloudinary no debe recibir
    private val cliente = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun subir(origen: String, ladoMaximo: Int): String = withContext(Dispatchers.IO) {
        val jpeg = reducir(Uri.parse(origen), ladoMaximo)
        val cuerpo = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", BuildConfig.CLOUDINARY_PRESET)
            // El preset usa el nombre del archivo como identificador y no deja sobrescribir: con un nombre fijo, Cloudinary
            // devolvía siempre la primera imagen que alguien subió. Cada subida lleva un nombre único.
            .addFormDataPart("file", "${UUID.randomUUID()}.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
            .build()
        val pedido = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/${BuildConfig.CLOUDINARY_NUBE}/image/upload")
            .post(cuerpo)
            .build()
        // Sin red sale una IOException, como en el resto de la app: la pantalla ya sabe decir "sin conexión"
        cliente.newCall(pedido).execute().use { r ->
            val texto = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw ErrorApi(CodigoError.ERROR_SERVIDOR, "Cloudinary respondió ${r.code}")
            Json.parseToJsonElement(texto).jsonObject["secure_url"]?.jsonPrimitive?.content
                ?: throw ErrorApi(CodigoError.ERROR_SERVIDOR, "Cloudinary no devolvió la dirección")
        }
    }

    private fun reducir(uri: Uri, ladoMaximo: Int): ByteArray {
        val original: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contexto.contentResolver, uri)) { decodificador, info, _ ->
                val lado = max(info.size.width, info.size.height)
                if (lado > ladoMaximo) {
                    val escala = ladoMaximo.toFloat() / lado
                    decodificador.setTargetSize((info.size.width * escala).roundToInt(), (info.size.height * escala).roundToInt())
                }
                decodificador.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            val completo = MediaStore.Images.Media.getBitmap(contexto.contentResolver, uri)
            val lado = max(completo.width, completo.height)
            if (lado <= ladoMaximo) completo else {
                val escala = ladoMaximo.toFloat() / lado
                Bitmap.createScaledBitmap(completo, (completo.width * escala).roundToInt(), (completo.height * escala).roundToInt(), true)
            }
        }
        return ByteArrayOutputStream().use { salida ->
            original.compress(Bitmap.CompressFormat.JPEG, 85, salida)
            salida.toByteArray()
        }
    }
}
