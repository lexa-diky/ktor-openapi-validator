package com.example

import com.example.models.Error
import com.example.models.User
import com.example.models.UserRequest
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

private val users = mutableListOf(
    User(1, "John Doe", "john@example.com", 30),
    User(2, "Jane Smith", "jane@example.com", 25)
)

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            explicitNulls = false
        })
    }

    routing {
        get("/users") {
            call.respond(users)
        }

        post("/users") {
            try {
                val request = call.receive<UserRequest>()

                if (!request.email.contains("@")) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        Error(400, "Invalid email format")
                    )
                    return@post
                }

                // Create new user with auto-incremented ID
                val newUser = User(
                    id = users.maxOfOrNull { it.id }?.plus(1) ?: 1,
                    name = request.name,
                    email = request.email,
                    age = request.age
                )

                users.add(newUser)
                call.respond(HttpStatusCode.Created, newUser)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    Error(400, "Invalid request: ${e.message}")
                )
            }
        }

        get("/users/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    Error(400, "Invalid ID format")
                )
                return@get
            }

            val user = users.find { it.id == id }
            if (user == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    Error(404, "User not found")
                )
                return@get
            }

            call.respond(user)
        }
    }
}
