# Ktor OpenApi (Swagger) Validator

![Github Actions Build](https://img.shields.io/github/actions/workflow/status/lexa-diky/ktor-openapi-validator/build.yml)
![Stars](https://img.shields.io/github/stars/lexa-diky/ktor-openapi-validator)
![License](https://img.shields.io/github/license/lexa-diky/ktor-openapi-validator)
![Maven Central Version](https://img.shields.io/maven-central/v/io.github.lexa-diky/ktor-openapi-validator)

A Ktor plugin for validating HTTP requests and responses against an OpenAPI specification.

## Installation

```kotlin
dependencies {
    implementation("io.github.lexa-diky:ktor-openapi-validator:0.1.0")
}
```

## Usage

> This plugin is intended for test only, please avoid using it in production code.

Install `OpenApiValidator` client plugin and specify `specificationUrl` and it will do the rest automatically.

```kotlin
val client = HttpClient {
    // ... rest of your configuration

    install(OpenApiValidator) {
        specificationUrl = "openapi.yaml"
    }
}
```
