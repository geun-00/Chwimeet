package com.back.standard.util.page;

import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;

public class PageUt {
    public static <T> PagePayload<T> of(Page<T> p) {
        List<SortOrder> sort = new ArrayList<>();
        p.getSort().forEach(o -> sort.add(new SortOrder(o.getProperty(), o.getDirection().name())));

        PageMeta meta = new PageMeta(
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(),
                p.isFirst(), p.isLast(), p.hasNext(), p.hasPrevious(), sort
        );
        return new PagePayload<>(p.getContent(), meta);
    }
}
