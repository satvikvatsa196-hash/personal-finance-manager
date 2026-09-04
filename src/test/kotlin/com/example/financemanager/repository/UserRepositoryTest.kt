package com.example.financemanager.repository

import com.example.financemanager.model.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private lateinit val userRepository: UserRepository

    @Test
    fun `should save and find user by username`() {
        // Arrange
        val user = User(
            username = "testuser",
            passwordHash = "hash123",
            fullName = "Test User",
            phoneNumber = "1234567890"
        )

        // Act
        userRepository.save(user)
        val foundUser = userRepository.findByUsername("testuser")

        // Assert
        assertThat(foundUser).isNotNull
        assertThat(foundUser?.username).isEqualTo("testuser")
        assertThat(foundUser?.fullName).isEqualTo("Test User")
    }

    @Test
    fun `existsByUsername should return true if user exists`() {
        // Arrange
        val user = User(
            username = "testuser2",
            passwordHash = "hash123",
            fullName = "Test User 2",
            phoneNumber = "0987654321"
        )
        userRepository.save(user)

        // Act & Assert
        assertThat(userRepository.existsByUsername("testuser2")).isTrue
        assertThat(userRepository.existsByUsername("nonexistent")).isFalse
    }
}
