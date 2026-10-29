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
version = providers.gradleProperty("CORE_VERSION").get()

val publicationBrand = providers.gradleProperty("publicationBrand").orElse("simla").get()
val publicationVariant = "${publicationBrand}Release"

android {
    namespace = "com.simla.android.sdk.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")

    }

    flavorDimensions += "publisher"
    productFlavors {
        create("simla") {
            dimension = "publisher"
            description = "Core runtime for Simla Android SDK"
            buildConfigField("String", "DEFAULT_BASE_URL", "\"https://c.simla.com\"")
        }
        create("retailcrm") {
            dimension = "publisher"
            description = "Core runtime for RetailCRM Android SDK"
            buildConfigField("String", "DEFAULT_BASE_URL", "\"https://c.retailcrm.tech\"")
        }
    }

    buildFeatures {
        buildConfig = true
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

    publishing {
        singleVariant(publicationVariant) {
            withSourcesJar()
        }
    }
}

dependencies {
    compileOnly(libs.androidx.annotation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        register<MavenPublication>(publicationVariant) {
            artifactId = providers.gradleProperty("CORE_ARTIFACT_ID").get()
            artifact(javadocJar)
            afterEvaluate {
                from(components[publicationVariant])
            }
        }
    }
}
