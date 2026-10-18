# Nexo

Aplicación Android que conecta a estudiantes universitarios de Bucaramanga con tutores de apoyo académico. Permite buscar un tutor por materia, modalidad y horario, enviar una solicitud, coordinar por chat, asistir a la sesión y calificarla.

Proyecto de Ingeniería de Software de la Universidad Industrial de Santander (UIS), Grupo 3.

> **Estado:** prototipo de frontend. Todos los datos son simulados (`data/mock/MockData.kt`); todavía no hay backend.

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

- Correo: cualquier dirección con "@".
- Contraseña: 4 caracteres o más.
- También puedes entrar con "Continuar con Google" (simulado).

## Funcionalidades

| Pantalla | Qué permite |
|---|---|
| Acceso | Entrar con Google o con correo y contraseña; registro (HU-01, HU-02) |
| Inicio | Próxima sesión, accesos por área de estudio y tutores disponibles |
| Buscar | Filtrar por materia, modalidad (virtual o presencial), franja horaria y "en línea"; ordenado por calificación (HU-05) |
| Perfil del tutor | Tarifa, modalidades, horario semanal y reseñas (HU-06) |
| Solicitar tutoría | Elegir tema, día, hora del horario del tutor y modalidad (HU-07) |
| Solicitudes | Aceptar, rechazar con motivo, cancelar, expiración a 24 h y tutores alternativos (HU-08, HU-09) |
| Chat | Disponible solo con una solicitud aceptada; de solo lectura al terminar la sesión (HU-10) |
| Historial y calificación | Calificación con estrellas, reseña opcional y filtro por materia (HU-11, HU-12) |
| Perfil | Cambio entre modo estudiante y modo tutor (HU-03) |
| Mi disponibilidad | Marcar cada hora como virtual, presencial o ambas (HU-04) |
| Reportar usuario | Denuncia de un usuario (HU-13) |

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

## Pendiente de backend

- Notificaciones push.
- Expiración automática de solicitudes a las 24 h.
- Bloqueo tras 5 intentos fallidos de inicio de sesión.
- Persistencia real de usuarios, solicitudes, sesiones y reseñas.

## Contribuir

- La rama `main` debe compilar.
- Trabaja en ramas por tarea, por ejemplo `feature/inicio-tokens`, y abre un *pull request* para revisión.
- Haz commits pequeños con mensajes claros, en español o inglés, pero consistentes.
- No subas claves, tokens ni archivos `local.properties`.
