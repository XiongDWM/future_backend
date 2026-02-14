package com.xiongdwm.future_backend.bo;

import java.util.Map;

public class PageableParam {
    private int pageNumber;
    private int pageSize;
    private Map<String, String> filters;
    public int getPageNumber() {
        return pageNumber;
    }
    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }
    public int getPageSize() {
        return pageSize;
    }
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    public Map<String, String> getFilters() {
        return filters;
    }
    public void setFilters(Map<String, String> filters) {
        this.filters = filters;
    }

    

}
