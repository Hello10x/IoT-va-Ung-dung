package com.example.IoT_UngDung.DTO;

public class SearchRequestDTO {
    private String filter;
    private String keyword;
    public SearchRequestDTO( String filter, String keyword) {
        this.filter = filter;
        this.keyword = keyword;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
