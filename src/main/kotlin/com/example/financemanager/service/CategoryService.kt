package com.example.financemanager.service

import com.example.financemanager.dto.request.CategoryRequest
import com.example.financemanager.dto.response.CategoryDto
import com.example.financemanager.dto.response.CategoryListResponse
import com.example.financemanager.exception.BadRequestException
import com.example.financemanager.exception.ConflictException
import com.example.financemanager.exception.ForbiddenException
import com.example.financemanager.exception.ResourceNotFoundException
import com.example.financemanager.model.entity.Category
import com.example.financemanager.model.entity.User
import com.example.financemanager.repository.CategoryRepository
import com.example.financemanager.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) {

    @Transactional(readOnly = true)
    fun getCategories(user: User): CategoryListResponse {
        val categories = categoryRepository.findByUserOrUserIsNull(user)
        val categoryDtos = categories.map {
            CategoryDto(
                name = it.name,
                type = it.type,
                isCustom = it.isCustom
            )
        }
        return CategoryListResponse(categoryDtos)
    }

    @Transactional
    fun createCustomCategory(user: User, request: CategoryRequest): CategoryDto {
        val categoryName = requireNotNull(request.name) { "Category name is required" }
        // Check if a category with this name already exists for the user (or as a default)
        if (categoryRepository.existsByUserAndName(user, categoryName) || 
            categoryRepository.findByUserIsNullAndName(categoryName) != null) {
            throw ConflictException("Category with this name already exists")
        }

        val category = Category(
            name = categoryName,
            type = requireNotNull(request.type) { "Category type is required" },
            isCustom = true,
            user = user
        )

        val savedCategory = categoryRepository.save(category)

        return CategoryDto(
            name = savedCategory.name,
            type = savedCategory.type,
            isCustom = savedCategory.isCustom
        )
    }

    @Transactional
    fun deleteCustomCategory(user: User, name: String) {
        val defaultCat = categoryRepository.findByUserIsNullAndName(name)
        if (defaultCat != null) {
            throw ForbiddenException("Cannot delete a default category")
        }

        val customCat = categoryRepository.findByUserAndName(user, name)
            ?: throw ResourceNotFoundException("Category not found")

        val categoryId = requireNotNull(customCat.id) { "Category ID should not be null" }
        // Check if category is referenced by any transaction
        if (transactionRepository.existsByCategoryId(categoryId)) {
            throw BadRequestException("Cannot delete category as it is referenced by transactions")
        }

        categoryRepository.delete(customCat)
    }
}
