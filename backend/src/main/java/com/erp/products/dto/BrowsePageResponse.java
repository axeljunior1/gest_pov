package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BrowsePageResponse<T> {

    private List<T> items;
    private long totalElements;
    private int page;
    private int size;
    private int totalPages;
}
