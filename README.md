# Ktor OpenApi (Swagger) Validator

![Github Actions Build](https://img.shields.io/github/actions/workflow/status/lexa-diky/ktor-openapi-validator/build.yml)
![Stars](https://img.shields.io/github/stars/lexa-diky/ktor-openapi-validator)
![License](https://img.shields.io/github/license/lexa-diky/ktor-openapi-validator)
![Maven Central Version](https://img.shields.io/maven-central/v/io.github.lexa-diky/ktor-openapi-validator)

A Ktor plugin for validating HTTP requests and responses against an OpenAPI specification.

## Installation

```kotlin
dependencies {
    testImplementation("io.github.lexa-diky:ktor-openapi-validator:0.5.0")
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

### Whitelisting

You can whitelist specific requests or responses by providing a list of paths and methods that should be ignored during
validation.

```kotlin
install(OpenApiValidator) {
    specificationUrl = "openapi.yaml"

    // Whitelist any response with error status code
    whitelist("Allow any error") {
        response.code?.isSuccess() == false
    }
}
```

### Custom reporters

You can provide a custom reporter to handle validation errors. The default reporter will use Junit5 assertions to report
errors.

```kotlin
install(OpenApiValidator) {
    specificationUrl = "openapi.yaml"
    reporter = TextReporter { messages ->
        messages.forEach { message ->
            println("OpenAPI validation error: $message")
        }
    }
}
```

## Integrations and dependencies

### Junit5

Library integrates with Junit5 via compile time dependency.
Please provide appropriate implementation on classpath if you are using default reporter.

```kotlin
dependencies {
    testImplementation("io.github.lexa-diky:ktor-openapi-validator:<latest-version>")
    testImplementation("org.junit.jupiter:junit-jupiter-api:<your-junit-version>")
}
```

### Ktor

Library integrates with Ktor Client via compile time dependency. 
It is compiled explicitly against Ktor 2.x, but works fine with Ktor 3.x as well.
Please provide appropriate implementation on classpath.

```kotlin
dependencies {
    testImplementation("io.github.lexa-diky:ktor-openapi-validator:<latest-version>")
    testImplementation("io.ktor:ktor-client-core:<your-ktor-version>")
}
```
