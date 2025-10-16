package com.example.IoT_UngDung.DTO;

import lombok.Data;
import java.util.List;

@Data
public class PagedResponseDTO<T> {
    private int pageId;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private List<T> data;

    public PagedResponseDTO(int pageId, int pageSize, long totalElements, int totalPages, List<T> data) {
        this.pageId = pageId;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.data = data;
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }
}
