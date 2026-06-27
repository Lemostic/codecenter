package com.meritdata.mdm.codecenter.common.api;

import lombok.Data;

@Data
public class PageRequest {
    private int page = 1;
    private int size = 20;
    private String sortBy;
    private String sortDir = "desc";

    public int offset() {
        return (Math.max(page, 1) - 1) * Math.max(size, 1);
    }
    public int limit() {
        return Math.min(Math.max(size, 1), 1000);
    }
}
