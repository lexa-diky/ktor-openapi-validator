package com.example

import com.example.models.User
import com.example.models.UserRequest
import io.github.lexadiky.ktor.openapivalidator.OpenApiValidator
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import io.ktor.server.engine.*
import io.ktor.server.netty.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiValidationTest {
    private lateinit var server: ApplicationEngine

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                explicitNulls = false
            })
        }

        install(OpenApiValidator) {
            specificationUrl = "openapi.yaml"

            whitelist("Ignore not found errors") {
                response.code?.value == 404
            }
        }
    }

    @BeforeAll
    fun setup() {
        // Start the server before tests
        server = embeddedServer(Netty, port = 8080, host = "localhost") {
            module()
        }
        server.start()
    }

    @AfterAll
    fun tearDown() {
        // Stop the server after tests
        server.stop(1000, 2000)
    }

    @Test
    fun `test get all users - valid response`() = runBlocking {
        val response = client.get("http://localhost:8080/users")
        assert(response.status.isSuccess())

        val users = response.body<List<User>>()
        assert(users.isNotEmpty())
    }

    @Test
    fun `test get user by id - valid response`() = runBlocking {
        val response = client.get("http://localhost:8080/users/1")
        assert(response.status.isSuccess())

        // The response body should be a user
        val user = response.body<User>()
        assert(user.id == 1L)
    }

    @Test
    fun `test create user - valid request and response`() = runBlocking {
        val userRequest = UserRequest(
            name = "Test User",
            email = "test@example.com",
            pika = "pika value",
            age = 25
        )

        val response = client.post("http://localhost:8080/users") {
            contentType(ContentType.Application.Json)
            setBody(userRequest)
        }

        assert(response.status == HttpStatusCode.Created)

        val createdUser = response.body<User>()
        assert(createdUser.name == userRequest.name)
        assert(createdUser.email == userRequest.email)
        assert(createdUser.age == userRequest.age)
    }

    @Test
    fun `test create user - invalid request`() = runBlocking {
        try {
            // Creating an invalid request by omitting the required "pika" field
            // This should be caught by the OpenAPI validator
            val invalidUserRequestJson = """
                {
                    "name": "Invalid User",
                    "email": "invalid@example.com",
                    "age": 25
                }
            """.trimIndent()

            client.post("http://localhost:8080/users") {
                header("Content-Type", "application/json")
                setBody(invalidUserRequestJson)
            }

            assert(false) { "Expected validation to fail but it passed" }
        } catch (e: Exception) {
            println("Expected validation error: ${e.message}")
        }
    }
}
