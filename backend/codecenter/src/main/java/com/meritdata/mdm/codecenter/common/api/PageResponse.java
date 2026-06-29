package com.meritdata.mdm.codecenter.common.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> records;
    private long total;
    private int page;
    private int size;

    /**
     * 同时输出 rows 字段作为 records 的别名 —— 兼容前端直接访问 page.data.rows
     */
    @JsonGetter("rows")
    public List<T> getRows() {
        return records;
    }

    public static <T> PageResponse<T> of(List<T> records, long total, int page, int size) {
        return new PageResponse<>(records, total, page, size);
    }
}
