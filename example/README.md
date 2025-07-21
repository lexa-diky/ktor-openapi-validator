# Ktor OpenAPI Validator Example

This example project demonstrates how to use the Ktor OpenAPI Validator library to validate HTTP requests and responses against an OpenAPI specification.

## Project Structure

- `src/main/kotlin/com/example/Application.kt` - A simple Ktor server application with user management endpoints
- `src/main/kotlin/com/example/models/Models.kt` - Data models for the application
- `src/main/resources/openapi.yaml` - OpenAPI specification defining the API
- `src/test/kotlin/com/example/ApiValidationTest.kt` - Tests demonstrating the OpenAPI validator

## Running the Example

### Start the Server

```bash
./gradlew example:run
```

This will start the server on http://localhost:8080.

### Run the Tests

```bash
./gradlew example:test
```

This will run the tests that demonstrate the OpenAPI validator.

## API Endpoints

The example includes the following endpoints:

- `GET /users` - Get all users
- `POST /users` - Create a new user
- `GET /users/{id}` - Get a user by ID

## Using the OpenAPI Validator

The validator is configured in the `ApiValidationTest.kt` file:

```kotlin
// Install the OpenApiValidator plugin
install(OpenApiValidator) {
    // Point to our OpenAPI specification
    specificationUrl = "src/main/resources/openapi.yaml"
    
    // Optional: Whitelist example - ignore 404 responses
    whitelist("Ignore not found errors") {
        response.code?.value == 404
    }
}
```

The tests demonstrate:
1. Valid requests and responses that pass validation
2. Invalid requests that fail validation

## Key Features Demonstrated

1. **OpenAPI Specification** - Defining your API contract
2. **Request Validation** - Ensuring client requests match the specification
3. **Response Validation** - Ensuring server responses match the specification
4. **Whitelisting** - Ignoring specific validation errors when needed
5. **Error Reporting** - Using the default JUnit5 reporter for validation errors

## Notes

- This validator is intended for testing purposes only, not for production use
- The example uses an in-memory database for simplicity