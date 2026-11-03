# Publicar Nexo

El orden importa: primero el servidor, después la app. Lo que cuesta dinero o necesita una cuenta propia está marcado.

## 1. Antes de empezar

- En `src/main/resources/static/privacidad.html`, cambia `[CORREO DE CONTACTO]` por un correo del equipo y hazla revisar. No es una opinión legal.
- Une el trabajo a `main`: `render.yaml` despliega esa rama.

## 2. Correo (Brevo u otro proveedor SMTP; cuenta propia)

Crea la cuenta, verifica el remitente y copia los datos SMTP. El plan gratis de Brevo permitía 300 correos al día; compruébalo antes.

## 3. Servidor (Render; de pago)

1. En Render: **New ▸ Blueprint** y elige este repositorio. Lee `render.yaml`.
2. Escribe los valores que pide:

| Variable | Valor |
|---|---|
| `NEXO_ADMIN_CORREO` | El correo con el que entrarás al panel |
| `NEXO_ADMIN_CONTRASENA` | Una contraseña larga, solo tuya |
| `NEXO_SMTP_HOST`, `NEXO_SMTP_USUARIO`, `NEXO_SMTP_CLAVE` | Los del paso 2 |
| `NEXO_CORREO_REMITENTE` | Ejemplo: `Nexo <no-responder@tudominio.com>` |

3. Al terminar, abre `https://TU-SERVICIO.onrender.com/api/salud`: debe responder `{"estado":"ok",...}`.
4. Entra a `https://TU-SERVICIO.onrender.com/admin` con la cuenta del panel.
5. Regístrate con un correo real y comprueba que llegue el código.

Los planes `starter` y `basic-256mb` son los mínimos sin apagado ni vencimiento: la base gratis de Render vence y no tiene copias. Revisa el precio vigente y confirma en su panel que la base tenga copias de seguridad.

## 4. App (Google Play; cuenta de pago única)

Crea la llave una sola vez y guárdala fuera del repositorio. Si se pierde, no se puede actualizar la app con ella:

```bash
keytool -genkeypair -v -keystore ~/nexo-publicacion.jks -alias nexo -keyalg RSA -keysize 4096 -validity 10000
```

Comprueba la versión final contra el servidor local (`./gradlew installValidacion`) y genera el archivo para Play:

```bash
export NEXO_LLAVE_ARCHIVO=~/nexo-publicacion.jks NEXO_LLAVE_ALIAS=nexo NEXO_LLAVE_CLAVE='...'
./gradlew bundleRelease -PnexoApiUrlPublicacion=https://TU-SERVICIO.onrender.com/
```

Queda en `app/build/outputs/bundle/release/`. Sube cada versión nueva con un `versionCode` mayor en `app/build.gradle.kts`. En Play Console, la política de privacidad es `https://TU-SERVICIO.onrender.com/privacidad.html`.

Sin `-PnexoApiUrlPublicacion`, el archivo sale con los datos simulados: sirve para demostrar, no para publicar.

## Lo que falta para más adelante

- **Avisos al instante.** Hoy la app pregunta al servidor cada 15 minutos o más, según Android. Lo instantáneo exige Firebase Cloud Messaging: un proyecto de Firebase, `google-services.json`, guardar el token de cada teléfono en el servidor y enviar desde allí.
- **Entrar con Google.** Exige un cliente OAuth propio y verificar el token en el servidor. El botón solo aparece en la versión de datos simulados.
