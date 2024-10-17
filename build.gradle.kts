import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath(libs.gradle)
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.junit5)
}

abstract class GitVersionValueSource : ValueSource<Int, ValueSourceParameters.None> {
    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): Int {
        val output = ByteArrayOutputStream()
        execOperations.exec {
            commandLine("git", "rev-list", "HEAD", "--count")
            standardOutput = output
        }
        return Integer.valueOf(String(output.toByteArray(), Charset.defaultCharset()).trim())
    }
}

val gitVersionProvider = providers.of(GitVersionValueSource::class) {}
val gitVersion = gitVersionProvider.get()

android {
    defaultConfig {
        compileSdk = 34
        minSdk = 26
        targetSdk = 34
        versionCode = 45
        versionName = "5.0.0"
        applicationId = "de.storchp.opentracks.osmplugin"

        testInstrumentationRunnerArguments += mapOf("runnerBuilder" to "de.mannodermaus.junit5.AndroidJUnit5Builder")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    signingConfigs {
        register("nightly") {
            if (System.getProperty("nightly_store_file") != null) {
                storeFile = file(System.getProperty("nightly_store_file"))
                storePassword = System.getProperty("nightly_store_password")
                keyAlias = System.getProperty("nightly_key_alias")
                keyPassword = System.getProperty("nightly_key_password")
            }
        }
        register("release") {
            if (System.getProperty("release_store_file") != null) {
                storeFile = file(System.getProperty("release_store_file"))
                storePassword = System.getProperty("release_store_password")
                keyAlias = System.getProperty("release_key_alias")
                keyPassword = System.getProperty("release_key_password")
            }
        }
    }
    compileOptions {
        // Sets Java compatibility to Java 17
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    flavorDimensions += "default"
    productFlavors {
        create("full") {
            applicationId = "de.storchp.opentracks.osmplugin"
            buildConfigField("boolean", "offline", "false")
            dimension = "default"
        }
        create("offline") {
            applicationId = "de.storchp.opentracks.osmplugin.offline"
            buildConfigField("boolean", "offline", "true")
            dimension = "default"
        }
    }

    sourceSets.getByName("full") {
        manifest.srcFile("src/full/AndroidManifest.xml")
    }
    sourceSets.getByName("offline") {
        manifest.srcFile("src/offline/AndroidManifest.xml")
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        register("nightly") {
            signingConfig = signingConfigs.getByName("nightly")
            applicationIdSuffix = ".nightly"
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    lint {
        disable.add("MissingTranslation")
    }
    androidResources {
        generateLocaleConfig = true
    }

    namespace = "de.storchp.opentracks.osmplugin"

    applicationVariants.all {
        resValue("string", "applicationId", applicationId)

        if (name == "nightly" || name == "debug") {
            outputs.forEach { output ->
                output as com.android.build.gradle.internal.api.ApkVariantOutputImpl
                output.versionCodeOverride = gitVersion
                output.versionNameOverride = "${applicationId}_${output.versionCode}"
                output.outputFileName = "${applicationId}_${versionCode}.apk"
            }
        } else {
            outputs.forEach { output ->
                output as com.android.build.gradle.internal.api.ApkVariantOutputImpl
                output.outputFileName = "${applicationId}_${versionName}.apk"
            }
        }
    }
}

dependencies {

    // Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // AndroidX
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.appcompat)
    implementation(libs.documentfile)
    implementation(libs.preference.ktx)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    // VTM
    implementation(libs.vtm)
    implementation(libs.vtm.themes)
    implementation(libs.vtm.http)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.vtm.android) {
        artifact {
            classifier = "natives-armeabi-v7a"
        }
    }
    runtimeOnly(libs.vtm.android) {
        artifact {
            classifier = "natives-arm64-v8a"
        }
    }
    runtimeOnly(libs.vtm.android) {
        artifact {
            classifier = "natives-x86"
        }
    }
    runtimeOnly(libs.vtm.android) {
        artifact {
            classifier = "natives-x86_64"
        }
    }
    implementation(libs.vtm.android)
    implementation(libs.androidsvg)
    implementation(libs.jsoup.jsoup)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)

    testRuntimeOnly(libs.junit.jupiter.engine)

    androidTestImplementation(libs.runner)
    androidTestImplementation(libs.junit.jupiter.api)
    androidTestImplementation(libs.android.test.core)

    androidTestRuntimeOnly(libs.android.test.runner)
}
