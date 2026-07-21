package com.system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class SearchRequest {
    @NotNull
    @Valid
    private List<SearchFilterDto> filters;

    @Min(0)
    private int page = 0;

    @Min(1)
    private int size = 10;

    // Getters and Setters
    public List<SearchFilterDto> getFilters() { return filters; }
    public void setFilters(List<SearchFilterDto> filters) { this.filters = filters; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
