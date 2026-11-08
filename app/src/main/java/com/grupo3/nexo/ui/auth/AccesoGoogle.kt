package com.grupo3.nexo.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.grupo3.nexo.BuildConfig

/** Cómo terminó pedirle a Android la cuenta de Google de la persona. */
sealed interface ResultadoGoogle {
    data class Token(val valor: String) : ResultadoGoogle

    /** Cerró el selector: no es un error, así que no se avisa nada. */
    data object Cancelado : ResultadoGoogle
    data object SinCuentas : ResultadoGoogle
    data object NoDisponible : ResultadoGoogle
}

const val MENSAJE_GOOGLE_SIN_CUENTAS = "Agrega una cuenta de Google en este teléfono o entra con tu correo."
const val MENSAJE_GOOGLE_NO_DISPONIBLE = "No pudimos abrir el inicio con Google. Inténtalo de nuevo o entra con tu correo."

/** Muestra el selector de cuentas de Google. [contexto] debe ser el de la actividad, que es la que lo presenta. */
suspend fun pedirTokenGoogle(contexto: Context): ResultadoGoogle {
    if (BuildConfig.GOOGLE_CLIENT_ID.isBlank()) return ResultadoGoogle.NoDisponible
    val solicitud = GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_CLIENT_ID).build())
        .build()
    return try {
        val credencial = CredentialManager.create(contexto).getCredential(contexto, solicitud).credential
        if (credencial is CustomCredential && credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            ResultadoGoogle.Token(GoogleIdTokenCredential.createFrom(credencial.data).idToken)
        } else {
            ResultadoGoogle.NoDisponible
        }
    } catch (e: GetCredentialCancellationException) {
        ResultadoGoogle.Cancelado
    } catch (e: NoCredentialException) {
        ResultadoGoogle.SinCuentas
    } catch (e: GetCredentialException) {
        ResultadoGoogle.NoDisponible
    } catch (e: GoogleIdTokenParsingException) {
        ResultadoGoogle.NoDisponible
    }
}
