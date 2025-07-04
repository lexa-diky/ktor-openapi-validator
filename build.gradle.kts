import io.gitlab.arturbosch.detekt.Detekt
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm") version "1.9.20"
    id("com.vanniktech.maven.publish") version "0.33.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.17.0"
}

group = "io.github.lexa-diky"
version = "0.4.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("io.ktor:ktor-client-core:2.3.13")
    compileOnly("org.junit.jupiter:junit-jupiter-api:5.8.0")
    implementation("com.atlassian.oai:swagger-request-validator-core:2.44.9")
}

kotlin {
    jvmToolchain(17)
}


tasks.withType<Detekt> {
    allRules = true
    reports {
        txt.required.set(true)
        sarif.required.set(true)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    reports {
        junitXml.required = true
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    coordinates(
        groupId = group.toString(),
        artifactId = "ktor-openapi-validator",
        version = version.toString()
    )

    pom {
        name = "ktor-openapi-validator"
        description = "Validating for ktor requests and responses against openapi specification"
        inceptionYear = "2025"
        url = "https://github.com/lexa-diky/ktor-openapi-validator"

        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/license/mit/"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "lexa-diky"
                name = "Aleksei Iakovlev"
                url = "https://github.com/lexa-diky"
            }
        }
        scm {
            url = "https://github.com/lexa-diky/ktor-openapi-validator"
        }
    }
}
