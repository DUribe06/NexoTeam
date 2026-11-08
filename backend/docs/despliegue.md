# Publicar Nexo

La API corre en Render y la base de datos en Neon, ambas en su plan gratis. El correo sale por Brevo, el inicio de sesión usa Google y la app se instala como APK en teléfonos Android. El orden importa: la app se genera al final porque necesita la dirección del servidor y el ID de Google.

Revisa los límites vigentes de cada servicio antes de empezar: cambian con el tiempo y ninguno de los planes gratuitos garantiza disponibilidad.

## 1. Antes de empezar

- La política de privacidad (`backend/src/main/resources/static/privacidad.html`) tiene como contacto `team.nexo.ia@gmail.com`. Si cambia el correo del equipo, actualízalo ahí. No es una opinión legal: conviene que alguien la revise.
- Render despliega la rama `main`. La integración continua comprueba en cada pull request que las pruebas pasen y que la imagen del servidor se construya.
- Render necesita acceso al repositorio. Quien administra `DUribe06/NexoTeam` debe autorizarlo; como el repositorio es público, también puede desplegarse con su dirección.

## 2. Correo (Brevo, plan gratis)

El servidor envía el código para confirmar la cuenta y el de recuperar la contraseña.

1. Crea la cuenta y verifica el remitente.
2. En **SMTP & API**, copia el usuario y la clave SMTP para el paso 5. El servidor y el puerto ya están en `render.yaml`: el plan gratis de Render bloquea el puerto 587, por eso se usa el 2525.

## 3. Inicio con Google (Google Cloud Console)

1. Crea un proyecto y configura la pantalla de consentimiento con el nombre de la app, un correo de contacto y la política de privacidad.
2. En **Credenciales**, crea un cliente OAuth tipo **Web**. Su ID va en `NEXO_GOOGLE_CLIENT_ID` y en `nexoGoogleClientId`. No hace falta su secreto.
3. Crea un cliente OAuth tipo **Android** con el paquete `com.grupo3.nexo` y el SHA-1 de la llave de firma. Haz uno por cada llave:
   - La de depuración, para probar desde Android Studio.
   - La de publicación del paso 6.

Para ver el SHA-1 de una llave: `keytool -list -v -keystore ARCHIVO -alias ALIAS`.

## 4. Base de datos (Neon, plan gratis)

1. Crea un proyecto con **PostgreSQL 18** en la región **AWS US East (N. Virginia)**, la misma del servidor.
2. Copia los datos de conexión con **Connection pooling desactivado**: el servidor no debe usar la dirección que contiene `-pooler`, porque fija opciones de sesión en cada conexión.
3. Neon los muestra como `postgresql://USUARIO:CLAVE@SERVIDOR/BASE?sslmode=require`. Para el paso 5, sepáralos así:

| Variable | Valor |
|---|---|
| `NEXO_DB_URL` | `jdbc:postgresql://SERVIDOR/BASE?sslmode=require` |
| `NEXO_DB_USUARIO` | `USUARIO` |
| `NEXO_DB_CLAVE` | `CLAVE` |

Las tablas se crean solas cuando el servidor arranca por primera vez.

## 5. API (Render, plan gratis)

1. En Render: **New ▸ Blueprint**, elige el repositorio y la rama. Render lee `render.yaml` y crea el servicio.
2. Escribe los valores que pide:

| Variable | Valor |
|---|---|
| `NEXO_DB_URL`, `NEXO_DB_USUARIO`, `NEXO_DB_CLAVE` | Los del paso 4 |
| `NEXO_ADMIN_CORREO` | El correo con el que entrarás al panel |
| `NEXO_ADMIN_CONTRASENA` | Una contraseña larga, solo tuya |
| `NEXO_SMTP_USUARIO`, `NEXO_SMTP_CLAVE` | Los del paso 2 |
| `NEXO_CORREO_REMITENTE` | Ejemplo: `Nexo <remitente-verificado@ejemplo.com>` |
| `NEXO_GOOGLE_CLIENT_ID` | El ID del cliente Web del paso 3 |

### Comprobar

1. `https://TU-SERVICIO.onrender.com/api/salud` responde `{"estado":"ok",...}`.
2. `https://TU-SERVICIO.onrender.com/admin` deja entrar con la cuenta del panel.
3. Al registrarte con un correo real, llega el código. Si no llega, mira el registro del servicio en Render: el puerto 2525 todavía no se ha probado desde allí.

### Cómo se mantiene encendido

Render apaga el servicio tras 15 minutos sin peticiones entrantes. Si pasan 13 sin ninguna, el propio servidor se llama por su dirección pública (`MantenerActivo`), y esa llamada cuenta como una petición más. Con actividad real no envía nada.

- **Solo un servicio gratis en el espacio de trabajo.** Render da 750 horas gratis al mes por espacio de trabajo, y este servicio consume hasta 744. Si se agotan, Render suspende el servicio hasta el mes siguiente.
- **Render puede reiniciarlo cuando quiera.** Si queda apagado, la siguiente petición lo enciende, pero tarda: la app puede mostrar un error de conexión en ese primer intento.
- **Los archivos no se conservan.** El servidor guarda todo en la base, así que esto no le afecta hoy.

### Copias de seguridad

El plan gratis no incluye copias que puedas guardar. Sácalas desde tu computador, con el cliente de PostgreSQL 18, usando la dirección `postgresql://...` del paso 4:

```bash
pg_dump "postgresql://USUARIO:CLAVE@SERVIDOR/BASE?sslmode=require" | gzip > nexo-$(date +%F).sql.gz
```

Guárdalas fuera del repositorio. **Restaura una copia de prueba al menos una vez** en otro proyecto de Neon; una copia que nunca se ha restaurado no está comprobada:

```bash
gunzip -c nexo-FECHA.sql.gz | psql "postgresql://...PROYECTO-DE-PRUEBA..."
```

### Mantenimiento

- **Actualizar Nexo:** al subir a la rama elegida, Render vuelve a desplegar. Las migraciones se aplican solas al arrancar.
- **Límites de Neon:** vigila en su panel el almacenamiento y las horas de cómputo. Si se agotan las horas, la base se detiene hasta el mes siguiente.

## 6. App (APK para Android)

Crea la llave una sola vez y guárdala fuera del repositorio. Si se pierde, los teléfonos no aceptan una versión nueva firmada con otra llave:

```bash
keytool -genkeypair -v -keystore ~/nexo-publicacion.jks -alias nexo -keyalg RSA -keysize 4096 -validity 10000
```

En `gradle.properties`, pon la dirección del servidor y el ID del cliente Web:

```properties
nexoApiUrl=https://TU-SERVICIO.onrender.com/
nexoGoogleClientId=TU-ID.apps.googleusercontent.com
```

Genera la app. Sin esos dos valores, el comando se niega a generarla:

```bash
export NEXO_LLAVE_ARCHIVO=~/nexo-publicacion.jks NEXO_LLAVE_ALIAS=nexo NEXO_LLAVE_CLAVE='...'
./gradlew assembleRelease
```

Queda en `app/build/outputs/apk/release/app-release.apk`. Se instala en el teléfono abriendo el archivo, o con `adb install app-release.apk`. Cada versión nueva lleva un `versionCode` mayor en `app/build.gradle.kts`.

La política de privacidad queda publicada en `https://TU-SERVICIO.onrender.com/privacidad.html`.
