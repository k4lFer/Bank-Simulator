package com.pck4x.sharedcontracts.objects;

public class QueryResult<T> {
    private T results;
    private int totalCount;
    private int totalPages;
    private int pageNumber;
    private int pageSize;

    private QueryResult(T results, int totalCount, int totalPages, int pageNumber, int pageSize) {
        this.results = results;
        this.totalCount = totalCount;
        this.totalPages = totalPages;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    public static <T> QueryResult<T> of(T results, int totalCount, int totalPages, int pageNumber, int pageSize) {
        return new QueryResult<>(results, totalCount, totalPages, pageNumber, pageSize);
    }

    public T getResults() {
        return results;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }
}
