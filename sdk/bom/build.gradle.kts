plugins {
    `java-platform`
    `maven-publish`
}

group = rootProject.group
version = providers.gradleProperty("BOM_VERSION").get()

val publicationBrand = providers.gradleProperty("publicationBrand")
    .orElse("simla")
    .get()

description = when (publicationBrand) {
    "simla" -> "Bill of Materials for Simla Android SDK"
    "retailcrm" -> "Bill of Materials for RetailCRM Android SDK"
    else -> error("Unsupported publicationBrand: $publicationBrand")
}

val publicationGroup = rootProject.group.toString()
val coreArtifactId = providers.gradleProperty("CORE_ARTIFACT_ID").get()
val coreOkHttpArtifactId = providers.gradleProperty("CORE_OKHTTP_ARTIFACT_ID").get()
val consultantArtifactId = providers.gradleProperty("CONSULTANT_ARTIFACT_ID").get()
val trackerArtifactId = providers.gradleProperty("TRACKER_ARTIFACT_ID").get()
val recommendationsArtifactId = providers.gradleProperty("RECOMMENDATIONS_ARTIFACT_ID").get()
val pushFirebaseArtifactId = providers.gradleProperty("PUSH_FIREBASE_ARTIFACT_ID").get()
val coreVersion = providers.gradleProperty("CORE_VERSION").get()
val coreOkHttpVersion = providers.gradleProperty("CORE_OKHTTP_VERSION").get()
val consultantVersion = providers.gradleProperty("CONSULTANT_VERSION").get()
val trackerVersion = providers.gradleProperty("TRACKER_VERSION").get()
val recommendationsVersion = providers.gradleProperty("RECOMMENDATIONS_VERSION").get()
val pushFirebaseVersion = providers.gradleProperty("PUSH_FIREBASE_VERSION").get()

dependencies {
    constraints {
        api("$publicationGroup:$coreArtifactId:$coreVersion")
        api("$publicationGroup:$coreOkHttpArtifactId:$coreOkHttpVersion")
        api("$publicationGroup:$consultantArtifactId:$consultantVersion")
        api("$publicationGroup:$trackerArtifactId:$trackerVersion")
        api("$publicationGroup:$recommendationsArtifactId:$recommendationsVersion")
        api("$publicationGroup:$pushFirebaseArtifactId:$pushFirebaseVersion")
    }
}

publishing {
    publications {
        register<MavenPublication>("bom") {
            artifactId = providers.gradleProperty("BOM_ARTIFACT_ID").get()
            from(components["javaPlatform"])
        }
    }
}
