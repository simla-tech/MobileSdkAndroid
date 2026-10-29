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
version = providers.gradleProperty("PUSH_FIREBASE_VERSION").get()


val publicationBrand = providers.gradleProperty("publicationBrand").orElse("simla").get()
val publicationVariant = "${publicationBrand}Release"

android {
    namespace = "com.simla.android.sdk.notifications"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        manifestPlaceholders["crmPushFcmServiceEnabled"] = "true"
    }

    flavorDimensions += "publisher"
    productFlavors {
        create("simla") {
            dimension = "publisher"
            description = "Firebase push notifications module for Simla Android SDK"
        }
        create("retailcrm") {
            dimension = "publisher"
            description = "Firebase push notifications module for RetailCRM Android SDK"
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
        freeCompilerArgs += "-opt-in=com.simla.android.sdk.CrmSdkInternalApi"
    }

    publishing {
        singleVariant(publicationVariant) {
            withSourcesJar()
        }
    }

}

dependencies {
    api(project(":sdk:core"))
    compileOnly(libs.androidx.annotation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.firebase.sdk.installations)
    api(libs.firebase.sdk.messaging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.core)
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
}


publishing {
    publications {
        register<MavenPublication>(publicationVariant) {
            artifactId = providers.gradleProperty("PUSH_FIREBASE_ARTIFACT_ID").get()
            artifact(javadocJar)
            afterEvaluate {
                from(components[publicationVariant])
            }
        }
    }
}
