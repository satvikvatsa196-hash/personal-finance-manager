package com.example.financemanager.dto.request

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "Username cannot be blank")
    val username: String?,

    @field:NotBlank(message = "Password cannot be blank")
    val password: String?
)
