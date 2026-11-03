# Nexo

Aplicación Android que conecta a estudiantes universitarios de Bucaramanga con tutores de apoyo académico. Permite buscar un tutor por materia, modalidad y horario, enviar una solicitud, coordinar por chat, asistir a la sesión y calificarla.

Proyecto de Ingeniería de Software de la Universidad Industrial de Santander (UIS), Grupo 3.

> **Estado:** la app funciona contra el backend de `backend/` (Spring Boot y PostgreSQL). En `debug`, `USAR_API` está en `true`; con `false` usa los datos simulados de `data/mock/MockData.kt`. El servidor está listo para desplegar, pero todavía no hay uno publicado: los pasos están en [`backend/docs/despliegue.md`](backend/docs/despliegue.md).

## Correr todo en local

Hace falta PostgreSQL con la base y el usuario `nexo` (clave `nexo`):

```bash
sudo -u postgres psql -c "CREATE ROLE nexo LOGIN PASSWORD 'nexo'" -c "CREATE DATABASE nexo OWNER nexo"
./gradlew installDebug         # app en el emulador, que llega al PC por 10.0.2.2
```


Para probar en un teléfono por WiFi, con el servidor en el PC: compila con `./gradlew assembleDebug -PnexoApiUrl=http://IP_DEL_PC:8080/`, instala `app/build/outputs/apk/debug/app-debug.apk` y permite el puerto 8080 en el firewall del PC para tu red. El teléfono y el PC deben estar en la misma red, y si la IP del PC cambia hay que volver a compilar.

Pruebas: `./gradlew test` para la app y `cd backend && ./gradlew test` para el backend, que también necesita ese PostgreSQL. El diseño de la base está en [`backend/docs/base-de-datos.md`](backend/docs/base-de-datos.md).

## Tecnologías

- Kotlin 2.0 y Jetpack Compose con Material 3
- Arquitectura en capas: UI (Compose + ViewModel) → casos de uso → repositorios → implementación
- Hilt para inyección de dependencias
- Navigation Compose
- minSdk 26, targetSdk 34, Java 17

## Requisitos

- Android Studio reciente
- JDK 17 o superior (el JDK que trae Android Studio sirve)
- Emulador con API 26 o superior, o un teléfono con depuración USB activada

## Ejecutar el proyecto

1. En Android Studio: **File ▸ Open** y selecciona la carpeta que contiene `settings.gradle.kts`.
2. Espera a que termine el *Gradle Sync*. La primera vez necesita internet.
3. Elige un dispositivo y pulsa **Run 'app'**.

Desde la terminal:

```bash
./gradlew assembleDebug
```

### Credenciales de prueba


La versión de datos simulados (`USAR_API` en `false`) acepta cualquier correo con "@" y una contraseña de 4 caracteres o más.

## Funcionalidades

| Pantalla | Qué permite |
|---|---|
| Acceso | Entrar con correo y contraseña; registro (HU-01, HU-02) |
| Inicio | Próxima sesión, accesos por área de estudio y tutores disponibles |
| Buscar | Filtrar por materia, modalidad (virtual o presencial), franja horaria y "en línea"; ordenado por calificación (HU-05) |
| Perfil del tutor | Tarifa, modalidades, horario semanal y reseñas (HU-06) |
| Solicitar tutoría | Elegir tema, día, hora del horario del tutor y modalidad (HU-07) |
| Solicitudes | Aceptar, rechazar con motivo, cancelar, expiración a 24 h y tutores alternativos (HU-08, HU-09) |
| Chat | Disponible solo con una solicitud aceptada; de solo lectura al terminar la sesión (HU-10) |
| Historial y calificación | Calificación con estrellas, reseña opcional y filtro por materia (HU-11, HU-12) |
| Perfil | Cambio entre modo estudiante y modo tutor; eliminar la cuenta (HU-03) |
| Mi disponibilidad | Marcar cada hora como virtual, presencial o ambas (HU-04) |
| Reportar usuario | Denuncia de un usuario (HU-13) |
| Avisos | Solicitudes, respuestas, mensajes y recordatorios, revisando el servidor cada 15 minutos o más |
| Panel web (`/admin`) | Reportes, revisiones de penalizaciones y suspensión de cuentas (RF12) |

Una sola cuenta puede actuar como estudiante y como tutor.

## Estructura

```
app/src/main/java/com/grupo3/nexo/
├── data/            # Implementaciones de repositorios y datos simulados (mock)
├── di/              # Módulos de Hilt
├── domain/
│   ├── model/       # Modelos del dominio
│   ├── repository/  # Interfaces de repositorios
│   └── usecase/     # Casos de uso
└── ui/
    ├── auth/        # Acceso y registro
    ├── busqueda/    # Búsqueda de tutores
    ├── chat/
    ├── components/  # Componentes compartidos
    ├── disponibilidad/
    ├── historial/
    ├── inicio/
    ├── navigation/  # Rutas y barra de navegación
    ├── perfil/
    ├── solicitudes/
    ├── theme/       # Colores, tipografía y formas
    └── tutor/
```

Las pantallas no llaman a los datos directamente: pasan por un ViewModel, que usa casos de uso, que a su vez usan las interfaces de los repositorios. Así, cuando exista el backend, solo cambia la implementación.

## Diseño

- Colores, tipografía, radios y espaciados se definen en `ui/theme/` y se usan desde ahí.
- Las cifras se muestran en formato colombiano: decimales con coma (4,9) y miles con punto ($25.000).
- Íconos Phosphor en la interfaz propia.
- Fuente Instrument Sans, incluida en `assets/fuentes/` con su licencia OFL.

## Pendiente

- Desplegar el servidor y publicar la app ([pasos](backend/docs/despliegue.md)).
- Avisos al instante con Firebase; hoy pueden tardar 15 minutos o más.
- Inicio con Google.

## Contribuir

- La rama `main` debe compilar.
- Trabaja en ramas por tarea, por ejemplo `feature/inicio-tokens`, y abre un *pull request* para revisión.
- Haz commits pequeños con mensajes claros, en español o inglés, pero consistentes.
- No subas claves, tokens ni archivos `local.properties`.
