package com.example.financemanager.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "Username cannot be blank")
    @field:Email(message = "Username must be a valid email address")
    val username: String?,

    @field:NotBlank(message = "Password cannot be blank")
    @field:Size(min = 8, message = "Password must be at least 8 characters long")
    val password: String?,

    @field:NotBlank(message = "Full name cannot be blank")
    val fullName: String?,

    @field:NotBlank(message = "Phone number cannot be blank")
    val phoneNumber: String?
)
