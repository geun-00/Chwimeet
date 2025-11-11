package com.back.domain.category.category.dto;

public record CategoryCreateReqBody(
        Long parentId,
        String name
) {
}
