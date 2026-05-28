package com.xcappstore.admin.common;

import java.util.List;

public class PageResponse<T> {
    private final long total;
    private final int page;
    private final int pageSize;
    private final List<T> list;

    public PageResponse(long total, int page, int pageSize, List<T> list) {
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.list = list;
    }

    public long getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public List<T> getList() {
        return list;
    }
}
