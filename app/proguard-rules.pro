# Reglas ProGuard/R8 de la app Nexo
# Hilt y Compose incluyen sus propias reglas consumer; no se requieren reglas extra.

# Los esquemas de la API se leen por su tipo en Retrofit y kotlinx.serialization: si la app no usa una respuesta, R8 borra
# la clase y la llamada falla aunque el servidor haya respondido bien. Pasó con ReporteDto.
-keep class com.grupo3.nexo.data.remote.** { *; }
-keep class com.grupo3.nexo.data.avisos.FotoNovedades** { *; }
