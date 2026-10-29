import org.gradle.jvm.tasks.Jar

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.binary.compatibility.validator)
    `maven-publish`
}

apiValidation {
    nonPublicMarkers.add("com.simla.android.sdk.CrmSdkInternalApi")
}

group = rootProject.group
version = providers.gradleProperty("CONSULTANT_VERSION").get()
description = "Online consultant module for Simla Android SDK"

val publicationBrand = providers.gradleProperty("publicationBrand").orElse("simla").get()
val publicationVariant = "${publicationBrand}Release"
val consultantUiTraceEnabled = providers.gradleProperty("ENABLE_CONSULTANT_UI_TRACE")
    .map(String::toBoolean)
    .orElse(false)

android {
    namespace = "com.simla.android.sdk.consultant"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField(
            "boolean",
            "ENABLE_CONSULTANT_UI_TRACE",
            consultantUiTraceEnabled.get().toString(),
        )
    }
    flavorDimensions += "publisher"
    productFlavors {
        create("simla") {
            dimension = "publisher"
            description = "Core runtime for Simla Android SDK"
        }
        create("retailcrm") {
            dimension = "publisher"
            description = "Core runtime for RetailCRM Android SDK"
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-opt-in=com.simla.android.sdk.CrmSdkInternalApi"
    }
    publishing { singleVariant(publicationVariant) { withSourcesJar() } }
}

dependencies {
    api(project(":sdk:core"))
    compileOnly(libs.androidx.annotation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(project(":sdk:core-okhttp"))
    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
}

val javadocJar = tasks.register<Jar>("javadocJar") { archiveClassifier.set("javadoc") }

publishing {
    publications {
        register<MavenPublication>(publicationVariant) {
            artifactId = providers.gradleProperty("CONSULTANT_ARTIFACT_ID").get()
            artifact(javadocJar)
            afterEvaluate { from(components[publicationVariant]) }
        }
    }
}
