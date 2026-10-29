import org.gradle.jvm.tasks.Jar

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.binary.compatibility.validator)
    `maven-publish`
}

apiValidation {
    nonPublicMarkers.add("com.simla.android.sdk.CrmSdkInternalApi")
}

group = rootProject.group
version = providers.gradleProperty("TRACKER_VERSION").get()

val publicationBrand = providers.gradleProperty("publicationBrand").orElse("simla").get()
val publicationVariant = "${publicationBrand}Release"

android {
    namespace = "com.simla.android.sdk.tracker"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    flavorDimensions += "publisher"
    productFlavors {
        create("simla") {
            dimension = "publisher"
            description = "Analytics tracker module for Simla Android SDK"
        }
        create("retailcrm") {
            dimension = "publisher"
            description = "Analytics tracker module for RetailCRM Android SDK"
        }
    }
    buildTypes { release { isMinifyEnabled = false } }
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
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

val javadocJar = tasks.register<Jar>("javadocJar") { archiveClassifier.set("javadoc") }

publishing {
    publications {
        register<MavenPublication>(publicationVariant) {
            artifactId = providers.gradleProperty("TRACKER_ARTIFACT_ID").get()
            artifact(javadocJar)
            afterEvaluate { from(components[publicationVariant]) }
        }
    }
}
