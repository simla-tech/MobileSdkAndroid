import com.android.build.api.dsl.LibraryExtension
import kotlinx.validation.KotlinApiBuildTask
import kotlinx.validation.KotlinApiCompareTask
import org.gradle.jvm.tasks.Jar

plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services.gradle.plugin) apply false
    alias(libs.plugins.jreleaser)
    alias(libs.plugins.kotlin.binary.compatibility.validator) apply false
}

val publicApiProjectPaths = listOf(
    ":sdk:core",
    ":sdk:core-okhttp",
    ":sdk:consultant",
    ":sdk:tracker",
    ":sdk:recommendations",
    ":sdk:push-firebase",
)

publicApiProjectPaths.forEach { projectPath ->
    project(projectPath) {
        val apiBuild = tasks.register<KotlinApiBuildTask>("apiBuild") {
            dependsOn("compileSimlaReleaseKotlin")
            inputClassesDirs.from(layout.buildDirectory.dir("tmp/kotlin-classes/simlaRelease"))
            runtimeClasspath.from(configurations.named("bcv-rt-jvm-cp-resolver"))
            outputApiFile.set(layout.buildDirectory.file("api/current/${project.name}.api"))
            nonPublicMarkers.add("com.simla.android.sdk.CrmSdkInternalApi")
            if (project.name == "consultant") {
                ignoredPackages.addAll(
                    listOf(
                        "com.simla.android.sdk.consultant.models",
                        "com.simla.android.sdk.consultant.storage",
                        "com.simla.android.sdk.consultant.ui.models",
                    ),
                )
            }
        }
        tasks.register<Copy>("apiDump") {
            group = "verification"
            description = "Updates the public API baseline."
            dependsOn(apiBuild)
            from(apiBuild.flatMap { it.outputApiFile })
            into(layout.projectDirectory.dir("api"))
        }
        tasks.register<KotlinApiCompareTask>("apiCheck") {
            group = "verification"
            description = "Checks the public API against its baseline."
            dependsOn(apiBuild)
            projectApiFile.set(layout.projectDirectory.file("api/${project.name}.api"))
            generatedApiFile.set(apiBuild.flatMap { it.outputApiFile })
        }
    }
}

tasks.register("apiDumpAll") {
    group = "verification"
    description = "Updates public API baselines for published SDK modules."
    dependsOn(publicApiProjectPaths.map { "$it:apiDump" })
}

tasks.register("apiCheckAll") {
    group = "verification"
    description = "Checks public API baselines for published SDK modules."
    dependsOn(publicApiProjectPaths.map { "$it:apiCheck" })
}

tasks.register("sdkTest") {
    group = "verification"
    description = "Runs unit tests for one representative SDK flavor."
    dependsOn(publicApiProjectPaths.map { "$it:testSimlaDebugUnitTest" })
}

val publicationBrand = providers.gradleProperty("publicationBrand").orElse("simla").get()
val publicationGroups = mapOf(
    "simla" to providers.gradleProperty("SIMLA_GROUP_ID").get(),
    "retailcrm" to providers.gradleProperty("RETAILCRM_GROUP_ID").get(),
)
val publicationNamespaces = mapOf(
    "simla" to providers.gradleProperty("SIMLA_NAMESPACE").get(),
    "retailcrm" to providers.gradleProperty("RETAILCRM_NAMESPACE").get(),
)
val publicationGroup = requireNotNull(publicationGroups[publicationBrand]) {
    "publicationBrand must be one of: ${publicationGroups.keys.joinToString()}"
}
val publicationBundleName = publicationGroup.substringAfterLast('.')
val publicationNamespace = requireNotNull(publicationNamespaces[publicationBrand]) {
    "No Maven Central namespace configured for publicationBrand=$publicationBrand"
}
val publicationVersionProperties = linkedMapOf(
    "core" to "CORE_VERSION",
    "core-okhttp" to "CORE_OKHTTP_VERSION",
    "consultant" to "CONSULTANT_VERSION",
    "tracker" to "TRACKER_VERSION",
    "recommendations" to "RECOMMENDATIONS_VERSION",
    "push-firebase" to "PUSH_FIREBASE_VERSION",
    "bom" to "BOM_VERSION",
)
val allowedPublicationModules = publicationVersionProperties.keys
val publicationModules = providers.gradleProperty("publicationModules")
    .orElse(allowedPublicationModules.joinToString(","))
    .get()
    .split(',')
    .map(String::trim)
    .filter(String::isNotEmpty)
    .toSet()
require(publicationModules.isNotEmpty()) { "publicationModules must not be empty" }
require(publicationModules.all(allowedPublicationModules::contains)) {
    "publicationModules must contain only: ${allowedPublicationModules.joinToString()}"
}

group = publicationGroup
version = providers.gradleProperty(
    publicationVersionProperties.getValue(publicationModules.first())
).get()

val stagingRepositoryDirectory = layout.buildDirectory.dir("maven-central-staging")
val localMavenRepositoryDirectory = providers.gradleProperty("localMavenRepository")
    .map { file(it) }
    .orElse(layout.projectDirectory.dir("artifacts/maven").asFile)

fun publicationProperty(propertyName: String) = providers.gradleProperty(propertyName)
    .orElse(providers.environmentVariable(propertyName))
    .orElse("TODO_$propertyName")

val pomScmUrl = publicationProperty("POM_SCM_URL").get()
val pomScmConnection = publicationProperty("POM_SCM_CONNECTION").get()
val pomScmDeveloperConnection = publicationProperty("POM_SCM_DEVELOPER_CONNECTION").get()
val pomDeveloperPropertyPrefix = publicationBrand.uppercase()
val pomProjectUrlProperty = "${pomDeveloperPropertyPrefix}_POM_PROJECT_URL"
val pomDeveloperIdProperty = "${pomDeveloperPropertyPrefix}_POM_DEVELOPER_ID"
val pomDeveloperNameProperty = "${pomDeveloperPropertyPrefix}_POM_DEVELOPER_NAME"
val pomDeveloperEmailProperty = "${pomDeveloperPropertyPrefix}_POM_DEVELOPER_EMAIL"
val pomProjectUrl = publicationProperty(pomProjectUrlProperty).get()
val pomDeveloperId = publicationProperty(pomDeveloperIdProperty).get()
val pomDeveloperName = publicationProperty(pomDeveloperNameProperty).get()
val pomDeveloperEmail = publicationProperty(pomDeveloperEmailProperty).get()
val distributionLegalFiles = files(
    layout.projectDirectory.file("LICENSE"),
    layout.projectDirectory.file("NOTICE"),
)

subprojects {
    plugins.withId("com.android.library") {
        val legalResourcesDirectory = layout.buildDirectory.dir("generated/legalResources")
        val prepareLegalResources = tasks.register<Sync>("prepareLegalResources") {
            into(legalResourcesDirectory)
            from(distributionLegalFiles) {
                into("META-INF/simla/${project.name}")
            }
        }
        extensions.configure<LibraryExtension> {
            sourceSets.named("main") {
                resources.srcDir(legalResourcesDirectory)
            }
        }
        tasks.matching {
            (it.name.startsWith("process") && it.name.endsWith("JavaRes")) ||
                    (it.name.startsWith("merge") && it.name.endsWith("JavaResource"))
        }.configureEach {
            dependsOn(prepareLegalResources)
        }
    }
    plugins.withId("maven-publish") {
        tasks.withType<Jar>().configureEach {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            from(distributionLegalFiles) {
                into("META-INF")
            }
        }
        extensions.configure<PublishingExtension> {
            repositories {
                maven {
                    name = "staging"
                    url = uri(stagingRepositoryDirectory)
                }
                maven {
                    name = "localFolder"
                    url = uri(localMavenRepositoryDirectory)
                }
            }
            publications.withType(MavenPublication::class.java).configureEach {
                pom {
                    name.set(project.name)
                    description.set(project.description ?: project.name)
                    url.set(pomProjectUrl)
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set("repo")
                        }
                    }
                    developers {
                        developer {
                            id.set(pomDeveloperId)
                            name.set(pomDeveloperName)
                            email.set(pomDeveloperEmail)
                        }
                    }
                    scm {
                        connection.set(pomScmConnection)
                        developerConnection.set(pomScmDeveloperConnection)
                        url.set(pomScmUrl)
                    }
                }
            }
        }
    }
}

val publicationPropertyNames = listOf(
    pomProjectUrlProperty,
    "POM_SCM_URL",
    "POM_SCM_CONNECTION",
    "POM_SCM_DEVELOPER_CONNECTION",
    pomDeveloperIdProperty,
    pomDeveloperNameProperty,
    pomDeveloperEmailProperty,
)
val requiredVersionProperties = mapOf(
    "core" to setOf("CORE_VERSION"),
    "core-okhttp" to setOf("CORE_VERSION", "CORE_OKHTTP_VERSION"),
    "consultant" to setOf("CORE_VERSION", "CONSULTANT_VERSION"),
    "tracker" to setOf("CORE_VERSION", "TRACKER_VERSION"),
    "recommendations" to setOf(
        "CORE_VERSION",
        "TRACKER_VERSION",
        "RECOMMENDATIONS_VERSION",
    ),
    "push-firebase" to setOf("CORE_VERSION", "PUSH_FIREBASE_VERSION"),
    "bom" to publicationVersionProperties.values.toSet(),
)
val releaseVersionPropertyNames = publicationModules
    .flatMap { requiredVersionProperties.getValue(it) }
    .toSet()

val validatePublicationMetadata = tasks.register("validatePublicationMetadata") {
    group = "publishing"
    description = "Validates Maven Central metadata and release versions."
    doLast {
        val placeholders = publicationPropertyNames.filter { propertyName ->
            publicationProperty(propertyName).get().contains("TODO_")
        }
        check(placeholders.isEmpty()) {
            "Replace Maven publication placeholders: ${placeholders.joinToString()}"
        }
        val invalidVersions = releaseVersionPropertyNames.filter { propertyName ->
            providers.gradleProperty(propertyName).get().isBlank() ||
                    providers.gradleProperty(propertyName).get().endsWith("-SNAPSHOT")
        }
        check(invalidVersions.isEmpty()) {
            "Maven Central versions must be non-blank releases: ${invalidVersions.joinToString()}"
        }
    }
}

val cleanStagingRepository = tasks.register<Delete>("cleanStagingRepository") {
    group = "publishing"
    delete(stagingRepositoryDirectory)
}
val stageMavenCentral = tasks.register("stageMavenCentral") {
    group = "publishing"
    description = "Stages the selected Maven publications for JReleaser."
}

val brandPublication = "${publicationBrand}Release"
val brandPublicationTaskPart = brandPublication.replaceFirstChar(Char::uppercaseChar)
val publicationTaskPaths = buildList {
    publicationModules.forEach { moduleName ->
        if (moduleName == "bom") {
            add(":sdk:bom:publishBomPublicationToStagingRepository")
        } else {
            add(
                ":sdk:$moduleName:" +
                        "publish${brandPublicationTaskPart}PublicationToStagingRepository"
            )
        }
    }
}
val localPublicationTaskPaths = publicationTaskPaths.map { taskPath ->
    taskPath.replace("ToStagingRepository", "ToLocalFolderRepository")
}

val publishToLocalMavenRepository = tasks.register("publishToLocalMavenRepository") {
    group = "publishing"
    description =
        "Publishes the selected Maven publications to ${localMavenRepositoryDirectory.get()}."
}

gradle.projectsEvaluated {
    publicationTaskPaths.forEach { taskPath ->
        tasks.getByPath(taskPath).dependsOn(cleanStagingRepository)
        stageMavenCentral.configure { dependsOn(taskPath) }
    }
    localPublicationTaskPaths.forEach { taskPath ->
        publishToLocalMavenRepository.configure { dependsOn(taskPath) }
    }
}

val generatedJreleaserConfig = layout.buildDirectory.file("generated/jreleaser/jreleaser.yml")
val generateJreleaserConfig = tasks.register("generateJreleaserConfig") {
    group = "publishing"
    outputs.file(generatedJreleaserConfig)
    outputs.upToDateWhen { false }
    doLast {
        val coreArtifactId = providers.gradleProperty("CORE_ARTIFACT_ID").get()
        val coreOkHttpArtifactId = providers.gradleProperty("CORE_OKHTTP_ARTIFACT_ID").get()
        val consultantArtifactId = providers.gradleProperty("CONSULTANT_ARTIFACT_ID").get()
        val trackerArtifactId = providers.gradleProperty("TRACKER_ARTIFACT_ID").get()
        val recommendationsArtifactId =
            providers.gradleProperty("RECOMMENDATIONS_ARTIFACT_ID").get()
        val pushFirebaseArtifactId = providers.gradleProperty("PUSH_FIREBASE_ARTIFACT_ID").get()
        val bomArtifactId = providers.gradleProperty("BOM_ARTIFACT_ID").get()
        val artifactIds = mapOf(
            "core" to coreArtifactId,
            "core-okhttp" to coreOkHttpArtifactId,
            "consultant" to consultantArtifactId,
            "tracker" to trackerArtifactId,
            "recommendations" to recommendationsArtifactId,
            "push-firebase" to pushFirebaseArtifactId,
            "bom" to bomArtifactId,
        )
        val overrides = buildList {
            publicationModules.forEach { moduleName ->
                val artifactId = artifactIds.getValue(moduleName)
                if (moduleName == "bom") {
                    add(
                        """
                      - groupId: $publicationGroup
                        artifactId: $artifactId
                        jar: false
                        sourceJar: false
                        javadocJar: false
                        verifyPom: false
                    """.trimIndent()
                    )
                } else {
                    add(
                        """
                      - groupId: $publicationGroup
                        artifactId: $artifactId
                        jar: false
                        sourceJar: true
                        javadocJar: true
                        verifyPom: false
                    """.trimIndent()
                    )
                }
            }
        }.joinToString("\n")
        val configFile = generatedJreleaserConfig.get().asFile
        configFile.parentFile.mkdirs()
        configFile.writeText(
            """
            project:
              name: $publicationBundleName
              version: ${project.version}
              languages:
                java:
                  groupId: $publicationGroup
                  artifactId: $coreArtifactId
                  multiProject: true
            signing:
              pgp:
                active: ALWAYS
                armored: true
            deploy:
              maven:
                mavenCentral:
                  central:
                    active: ALWAYS
                    url: https://central.sonatype.com/api/v1/publisher
                    authorization: BASIC
                    stage: UPLOAD
                    sign: true
                    checksums: true
                    sourceJar: false
                    javadocJar: false
                    verifyPom: false
                    applyMavenCentralRules: false
                    namespace: $publicationNamespace
                    stagingRepositories:
                      - build/maven-central-staging
                    artifactOverrides:
${overrides.prependIndent("                      ")}
            """.trimIndent() + "\n",
        )
    }
}

jreleaser {
    configFile.set(generatedJreleaserConfig)
    dependsOnAssemble.set(false)
}

tasks.matching { it.name.startsWith("jreleaser") }.configureEach {
    dependsOn(generateJreleaserConfig)
}
tasks.named("jreleaserDeploy") {
    dependsOn(validatePublicationMetadata, stageMavenCentral)
}
tasks.register("uploadToMavenCentral") {
    group = "publishing"
    description = "Uploads a USER_MANAGED deployment to Maven Central Portal."
    dependsOn("jreleaserDeploy")
}
