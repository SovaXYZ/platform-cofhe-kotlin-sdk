plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-library`
    `maven-publish`
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

tasks.test {
    useJUnitPlatform {
        excludeTags("integration", "e2e")
    }
}

tasks.register<Test>("integrationTest") {
    useJUnitPlatform {
        includeTags("integration")
    }
}

tasks.register<Test>("e2eTest") {
    useJUnitPlatform {
        includeTags("e2e")
    }
}

tasks.named("check") {
    dependsOn("integrationTest")
}

val ktorVersion = "2.3.12"
val coroutinesVersion = "1.8.1"
val serializationVersion = "1.6.3"

dependencies {
    // Ktor Client
    api("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

    // Coroutines
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    // Test
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.24")
    testImplementation("org.wiremock:wiremock:3.9.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")
}

group = "xyz.getsova"
version = System.getenv("SDK_VERSION") ?: "0.1.0-SNAPSHOT"

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "xyz.getsova"
            artifactId = "cofhe-sdk"
            version = project.version.toString()

            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/SovaXYZ/platform-cofhe-kotlin-sdk")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
