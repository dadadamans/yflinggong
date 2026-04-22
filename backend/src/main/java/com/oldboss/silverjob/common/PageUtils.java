package com.oldboss.silverjob.common;

import java.util.List;

public final class PageUtils {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private PageUtils() {
    }

    public static <T> Object paginate(List<T> list, Integer page, Integer pageSize) {
        if (page == null && pageSize == null) {
            return list;
        }

        PageQuery pageQuery = normalize(page, pageSize);
        int currentPage = pageQuery.getPage();
        int currentPageSize = pageQuery.getPageSize();

        int total = list.size();
        int fromIndex = Math.min((currentPage - 1) * currentPageSize, total);
        int toIndex = Math.min(fromIndex + currentPageSize, total);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / currentPageSize);
        return new PageResult<>(list.subList(fromIndex, toIndex), total, currentPage, currentPageSize, totalPages);
    }

    public static PageQuery normalize(Integer page, Integer pageSize) {
        int currentPage = page == null ? DEFAULT_PAGE : page;
        int currentPageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        if (currentPage < 1) {
            throw new BizException("页码必须大于0");
        }
        if (currentPageSize < 1 || currentPageSize > MAX_PAGE_SIZE) {
            throw new BizException("每页数量需在1到100之间");
        }
        int offset = (currentPage - 1) * currentPageSize;
        return new PageQuery(currentPage, currentPageSize, offset);
    }

    public static <T> PageResult<T> buildPageResult(List<T> list, long total, PageQuery pageQuery) {
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / pageQuery.getPageSize());
        return new PageResult<>(list, total, pageQuery.getPage(), pageQuery.getPageSize(), totalPages);
    }
}
