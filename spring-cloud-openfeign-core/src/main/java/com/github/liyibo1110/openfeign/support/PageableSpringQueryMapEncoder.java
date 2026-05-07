package com.github.liyibo1110.openfeign.support;

import feign.querymap.BeanQueryMapEncoder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支持对标注为SpringQueryMap的Pageable进行编码。
 * @author liyibo
 * @date 2026-05-06 14:43
 */
public class PageableSpringQueryMapEncoder extends BeanQueryMapEncoder {

    private String pageParameter = "page";

    private String sizeParameter = "size";

    private String sortParameter = "sort";

    private final String ignoreCase = "ignorecase";

    public void setPageParameter(String pageParameter) {
        this.pageParameter = pageParameter;
    }

    public void setSizeParameter(String sizeParameter) {
        this.sizeParameter = sizeParameter;
    }

    public void setSortParameter(String sortParameter) {
        this.sortParameter = sortParameter;
    }

    @Override
    public Map<String, Object> encode(Object object) {
        if (supports(object)) {
            Map<String, Object> queryMap = new HashMap<>();

            if (object instanceof Pageable pageable) {

                if (pageable.isPaged()) {
                    queryMap.put(pageParameter, pageable.getPageNumber());
                    queryMap.put(sizeParameter, pageable.getPageSize());
                }

                if (pageable.getSort() != null) {
                    applySort(queryMap, pageable.getSort());
                }
            } else if (object instanceof Sort sort) {
                applySort(queryMap, sort);
            }
            return queryMap;
        } else {
            return super.encode(object);
        }
    }

    private void applySort(Map<String, Object> queryMap, Sort sort) {
        List<String> sortQueries = new ArrayList<>();
        for (Sort.Order order : sort) {
            String sortQuery = order.getProperty() + "%2C" + order.getDirection();
            if (order.isIgnoreCase())
                sortQuery += "%2C" + ignoreCase;
            sortQueries.add(sortQuery);
        }
        if (!sortQueries.isEmpty())
            queryMap.put(sortParameter, sortQueries);
    }

    protected boolean supports(Object object) {
        return object instanceof Pageable || object instanceof Sort;
    }
}
