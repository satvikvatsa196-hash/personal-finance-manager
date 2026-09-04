package com.example.financemanager.controller

import com.example.financemanager.dto.request.TestRequest
import com.example.financemanager.exception.*
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/test")
class TestController {

    @PostMapping("/validate")
    fun validate(@Valid @RequestBody request: TestRequest): String {
        return "Valid!"
    }

    @GetMapping("/me")
    fun me(authentication: org.springframework.security.core.Authentication): String {
        return authentication.name
    }

    @GetMapping("/bad-request")
    fun badRequest() {
        throw BadRequestException("Bad request triggered")
    }

    @GetMapping("/unauthorized")
    fun unauthorized() {
        throw UnauthorizedException("Unauthorized access")
    }

    @GetMapping("/forbidden")
    fun forbidden() {
        throw ForbiddenException("Access forbidden")
    }

    @GetMapping("/not-found")
    fun notFound() {
        throw ResourceNotFoundException("Resource not found")
    }

    @GetMapping("/conflict")
    fun conflict() {
        throw ConflictException("Conflict occurred")
    }

    @GetMapping("/internal")
    fun internal() {
        throw RuntimeException("Unexpected error")
    }
}
