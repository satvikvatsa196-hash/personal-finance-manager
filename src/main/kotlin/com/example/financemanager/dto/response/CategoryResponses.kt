package com.example.financemanager.dto.response

import com.example.financemanager.model.enums.CategoryType

data class CategoryDto(
    val name: String,
    val type: CategoryType,
    val isCustom: Boolean
)

data class CategoryListResponse(
    val categories: List<CategoryDto>
)
