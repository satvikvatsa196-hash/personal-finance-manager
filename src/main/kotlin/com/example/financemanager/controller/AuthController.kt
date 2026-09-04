package com.example.financemanager.controller

import com.example.financemanager.dto.request.LoginRequest
import com.example.financemanager.dto.request.RegisterRequest
import com.example.financemanager.dto.response.MessageResponse
import com.example.financemanager.dto.response.RegisterResponse
import com.example.financemanager.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService,
    private val authenticationManager: AuthenticationManager
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<RegisterResponse> {
        val userId = userService.registerUser(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            RegisterResponse("User registered successfully", userId)
        )
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<MessageResponse> {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password)
        )

        val securityContext = SecurityContextHolder.getContext()
        securityContext.authentication = authentication

        val session = httpRequest.getSession(true)
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext)

        return ResponseEntity.ok(MessageResponse("Login successful"))
    }
}
