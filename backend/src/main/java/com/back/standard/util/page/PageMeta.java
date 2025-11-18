package com.back.standard.util.page;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record PageMeta(
        int page,           // 0-based
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext,
        boolean hasPrevious,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<SortOrder> sort // 정렬 정보(옵션)
) {
}

