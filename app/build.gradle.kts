plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
}

// Se definen en gradle.properties; ver backend/docs/despliegue.md
val apiUrl = providers.gradleProperty("nexoApiUrl").get()
val googleClientId = providers.gradleProperty("nexoGoogleClientId").get()
// Cuenta de Cloudinary donde la app sube fotos de perfil e imágenes del chat (preset sin firma: no hay secreto)
val cloudinaryNube = providers.gradleProperty("nexoCloudinaryNube").get()
val cloudinaryPreset = providers.gradleProperty("nexoCloudinaryPreset").get()

require(apiUrl.startsWith("https://") && apiUrl.endsWith("/")) {
    "nexoApiUrl debe empezar por https:// y terminar en /: la app no permite tráfico sin cifrar"
}

android {
    namespace = "com.grupo3.nexo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.grupo3.nexo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "API_URL", "\"$apiUrl\"")
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$googleClientId\"")
        buildConfigField("String", "CLOUDINARY_NUBE", "\"$cloudinaryNube\"")
        buildConfigField("String", "CLOUDINARY_PRESET", "\"$cloudinaryPreset\"")
    }

    // La llave de firma vive fuera del repositorio; solo se usa si estas variables existen
    providers.environmentVariable("NEXO_LLAVE_ARCHIVO").orNull?.let { archivo ->
        signingConfigs.create("publicacion") {
            storeFile = file(archivo)
            storePassword = providers.environmentVariable("NEXO_LLAVE_CLAVE").get()
            keyAlias = providers.environmentVariable("NEXO_LLAVE_ALIAS").get()
            keyPassword = providers.environmentVariable("NEXO_LLAVE_CLAVE").get()
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("publicacion")?.let { signingConfig = it }
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Una versión para publicar nunca sale apuntando a la dirección de relleno o sin el cliente de Google
tasks.matching { it.name == "bundleRelease" || it.name == "assembleRelease" }.configureEach {
    doFirst {
        check(!apiUrl.contains(".invalid")) { "Define nexoApiUrl en gradle.properties con la dirección del servidor" }
        check(googleClientId.isNotBlank()) { "Define nexoGoogleClientId en gradle.properties con el cliente Web de Google" }
    }
}

// Hilt genera código con kapt: evita errores de tipos sin resolver
kapt {
    correctErrorTypes = true
}

dependencies {
    // Inicio con Google
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play)
    implementation(libs.googleid)

    // Núcleo Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose (BOM gestiona versiones)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    // Navegación
    implementation(libs.androidx.navigation.compose)

    // Inyección de dependencias (Hilt)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Revisión periódica de novedades
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    kapt(libs.androidx.hilt.compiler)

    // Corrutinas
    implementation(libs.kotlinx.coroutines.android)

    // Red y sesión (backend Spring Boot)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    // Fotos de perfil e imágenes del chat desde Cloudinary y Google
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)

    // Pruebas unitarias (JVM)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
