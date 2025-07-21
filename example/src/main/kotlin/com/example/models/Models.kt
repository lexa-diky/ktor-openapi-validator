package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long,
    val name: String,
    val email: String,
    val age: Int? = null
)

@Serializable
data class UserRequest(
    val name: String,
    val email: String,
    val pika: String,
    val age: Int? = null
)

@Serializable
data class Error(
    val code: Int,
    val message: String
)
