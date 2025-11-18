package com.back.standard.util.page;

import java.util.List;

public record PagePayload<T>(
        List<T> content,
        PageMeta page
) {
}
