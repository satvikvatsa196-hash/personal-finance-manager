package com.example.financemanager.service

import com.example.financemanager.dto.request.RegisterRequest
import com.example.financemanager.exception.ConflictException
import com.example.financemanager.model.entity.User
import com.example.financemanager.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional
    fun registerUser(request: RegisterRequest): Long {
        if (userRepository.existsByUsername(requireNotNull(request.username))) {
            throw ConflictException("Username already exists")
        }

        val user = User(
            username = requireNotNull(request.username),
            passwordHash = passwordEncoder.encode(requireNotNull(request.password)),
            fullName = requireNotNull(request.fullName),
            phoneNumber = requireNotNull(request.phoneNumber)
        )

        val savedUser = userRepository.save(user)
        return requireNotNull(savedUser.id) { "User ID should not be null after save" }
    }
}
