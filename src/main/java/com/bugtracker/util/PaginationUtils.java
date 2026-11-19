package com.bugtracker.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public final class PaginationUtils {

    private PaginationUtils() {
    }

    public static Pageable sanitizePageable(Pageable pageable, Sort defaultSort) {
        if (pageable == null || pageable.isUnpaged()) {
            return pageable;
        }

        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            return defaultSort != null
                    ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), defaultSort)
                    : pageable;
        }

        List<Sort.Order> validOrders = new ArrayList<>();
        for (Sort.Order order : sort) {
            String prop = order.getProperty().trim();
            if (prop.equalsIgnoreCase("asc") || prop.equalsIgnoreCase("desc")
                    || prop.equals("[\"asc\"]") || prop.equals("[\"desc\"]")
                    || prop.equals("string") || prop.isBlank()) {
                continue;
            }
            validOrders.add(order);
        }

        Sort finalSort = validOrders.isEmpty()
                ? (defaultSort != null ? defaultSort : Sort.unsorted())
                : Sort.by(validOrders);

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), finalSort);
    }
}
