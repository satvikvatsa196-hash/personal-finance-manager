package com.example.financemanager.controller

import com.example.financemanager.dto.request.CategoryRequest
import com.example.financemanager.dto.response.CategoryDto
import com.example.financemanager.dto.response.CategoryListResponse
import com.example.financemanager.dto.response.MessageResponse
import com.example.financemanager.security.UserPrincipal
import com.example.financemanager.service.CategoryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/categories")
class CategoryController(
    private val categoryService: CategoryService
) {

    @GetMapping
    fun getCategories(@AuthenticationPrincipal userPrincipal: UserPrincipal): ResponseEntity<CategoryListResponse> {
        val response = categoryService.getCategories(userPrincipal.getUser())
        return ResponseEntity.ok(response)
    }

    @PostMapping
    fun createCategory(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody request: CategoryRequest
    ): ResponseEntity<CategoryDto> {
        val response = categoryService.createCustomCategory(userPrincipal.getUser(), request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @DeleteMapping("/{name}")
    fun deleteCategory(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable name: String
    ): ResponseEntity<MessageResponse> {
        categoryService.deleteCustomCategory(userPrincipal.getUser(), name)
        return ResponseEntity.ok(MessageResponse("Category deleted successfully"))
    }
}
