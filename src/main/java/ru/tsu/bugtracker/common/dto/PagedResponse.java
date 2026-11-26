package ru.tsu.bugtracker.common.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Generic wrapper for a single page of results.
 *
 * @param items         elements of the current page
 * @param page          zero-based page index
 * @param size          requested page size
 * @param totalElements total number of elements across all pages
 * @param totalPages    total number of pages
 * @param <T>           type of the page element
 */
public record PagedResponse<T>(
		List<T> items,
		int page,
		int size,
		long totalElements,
		int totalPages
) {

	public static <T> PagedResponse<T> from(Page<T> page) {
		return new PagedResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages()
		);
	}

}
