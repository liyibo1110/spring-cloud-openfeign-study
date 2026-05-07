package com.github.liyibo1110.openfeign.support;

import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 支持通过组合模式实现Spring Pageable的编码。
 * @author liyibo
 * @date 2026-05-06 14:25
 */
public class PageableSpringEncoder implements Encoder {

    private final Encoder delegate;

    private String pageParameter = "page";

    private String sizeParameter = "size";

    private String sortParameter = "sort";

    private final String ignoreCase = "ignorecase";

    public PageableSpringEncoder(Encoder delegate) {
        this.delegate = delegate;
    }

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
    public void encode(Object object, Type bodyType, RequestTemplate template) throws EncodeException {
        if (supports(object)) {
            if (object instanceof Pageable pageable) {
                if (pageable.isPaged()) {
                    template.query(pageParameter, String.valueOf(pageable.getPageNumber()));
                    template.query(sizeParameter, String.valueOf(pageable.getPageSize()));
                }

                if (pageable.getSort() != null) {
                    applySort(template, pageable.getSort());
                }
            } else if (object instanceof Sort sort) {
                applySort(template, sort);
            }
        } else {
            if (delegate != null) {
                delegate.encode(object, bodyType, template);
            } else {
                throw new EncodeException("PageableSpringEncoder does not support the given object " + object.getClass()
                        + " and no delegate was provided for fallback!");
            }
        }
    }

    private void applySort(RequestTemplate template, Sort sort) {
        Collection<String> existingSorts = template.queries().get("sort");
        List<String> sortQueries = existingSorts != null ? new ArrayList<>(existingSorts) : new ArrayList<>();
        if (!sortParameter.equals("sort")) {
            existingSorts = template.queries().get(sortParameter);
            if (existingSorts != null)
                sortQueries.addAll(existingSorts);
        }
        for (Sort.Order order : sort) {
            String sortQuery = order.getProperty() + "%2C" + order.getDirection();
            if (order.isIgnoreCase())
                sortQuery += "%2C" + ignoreCase;
            sortQueries.add(sortQuery);
        }
        if (!sortQueries.isEmpty())
            template.query(sortParameter, sortQueries);
    }

    protected boolean supports(Object object) {
        return object instanceof Pageable || object instanceof Sort;
    }
}
