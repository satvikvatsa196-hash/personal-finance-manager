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
        if (userRepository.existsByUsername(request.username!!)) {
            throw ConflictException("Username already exists")
        }

        val user = User(
            username = request.username,
            passwordHash = passwordEncoder.encode(request.password),
            fullName = request.fullName!!,
            phoneNumber = request.phoneNumber!!
        )

        val savedUser = userRepository.save(user)
        return savedUser.id!!
    }
}
