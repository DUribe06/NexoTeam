plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.grupo3.nexo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.grupo3.nexo"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
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
        debug {
            // 10.0.2.2 es el localhost de la máquina vista desde el emulador. Para un teléfono por WiFi, con la IP del PC:
            // ./gradlew assembleDebug -PnexoApiUrl=http://192.168.x.x:8080/
            val apiUrl = providers.gradleProperty("nexoApiUrl").getOrElse("http://10.0.2.2:8080/")
            buildConfigField("String", "API_URL", "\"$apiUrl\"")
            buildConfigField("Boolean", "USAR_API", "true")
        }
        release {
            // Para publicar: ./gradlew bundleRelease -PnexoApiUrlPublicacion=https://... (ver README).
            // Sin esa propiedad sale con los datos simulados, como versión de demostración sin servidor.
            val urlPublicacion = providers.gradleProperty("nexoApiUrlPublicacion").orNull
            require(urlPublicacion == null || urlPublicacion.startsWith("https://")) {
                "nexoApiUrlPublicacion debe ser https: la versión publicada no permite tráfico sin cifrar"
            }
            buildConfigField("String", "API_URL", "\"${urlPublicacion ?: "https://api.nexo.invalid/"}\"")
            buildConfigField("Boolean", "USAR_API", (urlPublicacion != null).toString())
            signingConfigs.findByName("publicacion")?.let { signingConfig = it }
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // La versión final, reducida con R8, pero contra el servidor del PC: comprueba que R8 no rompa nada antes de publicar
        create("validacion") {
            initWith(getByName("release"))
            val apiUrl = providers.gradleProperty("nexoApiUrl").getOrElse("http://10.0.2.2:8080/")
            buildConfigField("String", "API_URL", "\"$apiUrl\"")
            buildConfigField("Boolean", "USAR_API", "true")
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }
    sourceSets.getByName("validacion") {
        manifest.srcFile("src/debug/AndroidManifest.xml")
        res.srcDir("src/debug/res")
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

// Hilt genera código con kapt: evita errores de tipos sin resolver
kapt {
    correctErrorTypes = true
}

dependencies {
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
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)

    // Pruebas unitarias (JVM)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
