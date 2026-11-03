# Publicar Nexo

La API y la base de datos corren en un servidor gratuito de Oracle Cloud, el correo sale por Brevo, el inicio de sesión usa Google y la app se publica en Google Play. El orden importa: la app se genera al final porque necesita la dirección del servidor y el ID de Google.

Revisa los límites y precios vigentes de cada servicio antes de empezar: cambian con el tiempo y ninguno de los planes gratuitos garantiza disponibilidad.

## 1. Antes de empezar

- En `backend/src/main/resources/static/privacidad.html`, cambia `[CORREO DE CONTACTO]` por un correo del equipo y haz revisar la política. No es una opinión legal.
- Une el trabajo a `main` y comprueba que la integración continua pase: construye también la imagen del servidor.

## 2. Correo (Brevo, plan gratis)

El servidor envía el código para confirmar la cuenta y el de recuperar la contraseña.

1. Crea la cuenta y verifica el remitente.
2. En **SMTP & API**, copia el servidor, el usuario y la clave SMTP para el paso 4.

## 3. Inicio con Google (Google Cloud Console)

1. Crea un proyecto y configura la pantalla de consentimiento con el nombre de la app, un correo de contacto y la política de privacidad.
2. En **Credenciales**, crea un cliente OAuth tipo **Web**. Su ID va en `NEXO_GOOGLE_CLIENT_ID` y en `nexoGoogleClientId`. No hace falta su secreto.
3. Crea un cliente OAuth tipo **Android** con el paquete `com.grupo3.nexo` y el SHA-1 de la llave de firma. Haz uno por cada llave:
   - La de depuración, para probar desde Android Studio.
   - La de publicación del paso 5.
   - La de **Play App Signing**, que aparece en Play Console después de subir la primera versión. Si falta, Google funciona en tus pruebas y falla a quien instale desde Play.

Para ver el SHA-1 de una llave: `keytool -list -v -keystore ARCHIVO -alias ALIAS`.

## 4. API y base de datos (Oracle Cloud, Always Free)

Oracle pide una tarjeta para verificar la identidad. **La región principal no se puede cambiar después**, y las instancias gratuitas se crean en ella.

### Crear el servidor

1. Crea una instancia `VM.Standard.A1.Flex` con Ubuntu 24.04, **1 OCPU y 6 GB**, marcada como *Always Free*. Si la región no tiene capacidad, inténtalo más tarde.
2. Reserva una IP pública para que no cambie.
3. En la red de la instancia, permite la entrada a los puertos 80 y 443. Las imágenes de Oracle también los bloquean dentro del servidor:

   ```bash
   sudo iptables -I INPUT 6 -p tcp -m multiport --dports 80,443 -j ACCEPT
   sudo iptables -I INPUT 6 -p udp --dport 443 -j ACCEPT
   sudo apt install -y iptables-persistent && sudo netfilter-persistent save
   ```

4. Crea un subdominio gratuito en [DuckDNS](https://www.duckdns.org) que apunte a esa IP.

### Instalar Nexo

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker "$USER" && newgrp docker
git clone https://github.com/DUribe06/NexoTeam.git nexo
cd nexo/despliegue
cp .env.ejemplo .env && chmod 600 .env
nano .env   # cambia cada valor
docker compose up -d --build
```

### Comprobar

1. `https://TU-DOMINIO/api/salud` responde `{"estado":"ok",...}`.
2. `https://TU-DOMINIO/admin` deja entrar con la cuenta del panel.
3. Al registrarte con un correo real, llega el código.

### Copias de seguridad

Programa una copia diaria con `crontab -e`:

```cron
0 3 * * * /home/ubuntu/nexo/despliegue/copia-base.sh >> /home/ubuntu/copias.log 2>&1
```

Guarda las copias fuera del servidor: instala [rclone](https://rclone.org), configura un destino y ponlo en `NEXO_COPIAS_DESTINO`. **Restaura una copia de prueba al menos una vez**; una copia que nunca se ha restaurado no está comprobada.

```bash
gunzip -c copias/nexo-FECHA.sql.gz | docker compose exec -T base psql -U nexo nexo
```

### Mantenimiento

- **Actualizar Nexo:** `git pull && docker compose up -d --build`. Las migraciones se aplican solas al arrancar.
- **Ver qué pasa:** `docker compose ps` y `docker compose logs -f api`.
- **Parches del sistema:** `sudo apt install unattended-upgrades`.
- **Inactividad:** Oracle puede reclamar una instancia si en 7 días la CPU, la red y la memoria quedan por debajo del 20 %. La API reserva 1,5 GB para superar ese límite de memoria: compruébalo con `free -h`.

## 5. App (Google Play, pago único)

Crea la llave una sola vez y guárdala fuera del repositorio. Si se pierde, no se puede actualizar la app con ella:

```bash
keytool -genkeypair -v -keystore ~/nexo-publicacion.jks -alias nexo -keyalg RSA -keysize 4096 -validity 10000
```

En `gradle.properties`, pon la dirección del servidor y el ID del cliente Web:

```properties
nexoApiUrl=https://TU-DOMINIO/
nexoGoogleClientId=TU-ID.apps.googleusercontent.com
```

Genera el archivo para Play. Sin esos dos valores, el comando se niega a generarlo:

```bash
export NEXO_LLAVE_ARCHIVO=~/nexo-publicacion.jks NEXO_LLAVE_ALIAS=nexo NEXO_LLAVE_CLAVE='...'
./gradlew bundleRelease
```

Queda en `app/build/outputs/bundle/release/`. Sube cada versión nueva con un `versionCode` mayor en `app/build.gradle.kts`.

En Play Console, la política de privacidad es `https://TU-DOMINIO/privacidad.html`. Una cuenta personal debe hacer una prueba cerrada con al menos 12 evaluadores durante 14 días seguidos antes de pedir acceso a producción.

## Lo que falta para más adelante

- **Avisos al instante.** Hoy la app pregunta al servidor cada 15 minutos o más, según Android. Lo instantáneo exige Firebase Cloud Messaging: un proyecto de Firebase, `google-services.json`, guardar el token de cada teléfono en el servidor y enviar desde allí.
