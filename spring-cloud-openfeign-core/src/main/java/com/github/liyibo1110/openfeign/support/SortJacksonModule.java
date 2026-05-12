package com.github.liyibo1110.openfeign.support;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * 此Jackson模块为Spring Sort对象提供了序列化和反序列化支持。
 * @author liyibo
 * @date 2026-05-12 13:37
 */
public class SortJacksonModule extends Module {

    @Override
    public String getModuleName() {
        return "PageJacksonModule";
    }

    @Override
    public Version version() {
        return new Version(0, 1, 0, "", null, null);
    }

    @Override
    public void setupModule(SetupContext context) {
        context.setMixInAnnotations(Page.class, PageMixIn.class);
        context.setMixInAnnotations(Pageable.class, PageableMixIn.class);
    }

    private static PageRequest buildPageRequest(int number, int size, Sort sort) {
        PageRequest pageRequest;
        if (sort != null)
            pageRequest = PageRequest.of(number, size, sort);
        else
            pageRequest = PageRequest.of(number, size);
        return pageRequest;
    }

    @JsonDeserialize(as = SimplePageImpl.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    private interface PageMixIn {

    }

    @JsonDeserialize(as = SimplePageable.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    private interface PageableMixIn {

    }

    static class SimplePageImpl<T> implements Page<T> {

        private final Page<T> delegate;

        SimplePageImpl(@JsonProperty("content") List<T> content, @JsonProperty("pageable") Pageable pageable,
                       @JsonProperty("number") @JsonAlias("pageNumber") int number,
                       @JsonProperty("size") @JsonAlias("pageSize") int size,
                       @JsonProperty("totalElements") @JsonAlias({ "total-elements", "total_elements", "totalelements",
                               "TotalElements", "total" }) long totalElements,
                       @JsonProperty("sort") Sort sort) {
            if (size > 0) {
                PageRequest pageRequest = buildPageRequest(number, size, sort);
                delegate = new PageImpl<>(content, pageRequest, totalElements);
            } else if (pageable != null && pageable.getPageSize() > 0) {
                delegate = new PageImpl<>(content, pageable, totalElements);
            } else {
                delegate = new PageImpl<>(content);
            }
        }

        @JsonProperty
        @Override
        public int getTotalPages() {
            return delegate.getTotalPages();
        }

        @JsonProperty
        @Override
        public long getTotalElements() {
            return delegate.getTotalElements();
        }

        @JsonProperty
        @Override
        public int getNumber() {
            return delegate.getNumber();
        }

        @JsonProperty
        @Override
        public int getSize() {
            return delegate.getSize();
        }

        @JsonProperty
        @Override
        public int getNumberOfElements() {
            return delegate.getNumberOfElements();
        }

        @JsonProperty
        @Override
        public List<T> getContent() {
            return delegate.getContent();
        }

        @JsonProperty
        @Override
        public boolean hasContent() {
            return delegate.hasContent();
        }

        @JsonIgnore
        @Override
        public Sort getSort() {
            return delegate.getSort();
        }

        @JsonProperty
        @Override
        public boolean isFirst() {
            return delegate.isFirst();
        }

        @JsonProperty
        @Override
        public boolean isLast() {
            return delegate.isLast();
        }

        @JsonIgnore
        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @JsonIgnore
        @Override
        public boolean hasPrevious() {
            return delegate.hasPrevious();
        }

        @JsonIgnore
        @Override
        public Pageable nextPageable() {
            return delegate.nextPageable();
        }

        @JsonIgnore
        @Override
        public Pageable previousPageable() {
            return delegate.previousPageable();
        }

        @JsonIgnore
        @Override
        public <S> Page<S> map(Function<? super T, ? extends S> converter) {
            return delegate.map(converter);
        }

        @JsonIgnore
        @Override
        public Iterator<T> iterator() {
            return delegate.iterator();
        }

        @JsonIgnore
        @Override
        public Pageable getPageable() {
            return delegate.getPageable();
        }

        @JsonIgnore
        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return delegate.equals(obj);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }

    static class SimplePageable implements Pageable {
        private final PageRequest delegate;

        SimplePageable(@JsonProperty("pageNumber") int number, @JsonProperty("pageSize") int size,
                       @JsonProperty("sort") Sort sort) {
            delegate = buildPageRequest(number, size, sort);
        }

        @Override
        public int getPageNumber() {
            return delegate.getPageNumber();
        }

        @Override
        public int getPageSize() {
            return delegate.getPageSize();
        }

        @Override
        public long getOffset() {
            return delegate.getOffset();
        }

        @Override
        public Sort getSort() {
            return delegate.getSort();
        }

        @Override
        public Pageable next() {
            return delegate.next();
        }

        @Override
        public Pageable previousOrFirst() {
            return delegate.previousOrFirst();
        }

        @Override
        public Pageable first() {
            return delegate.first();
        }

        @Override
        public Pageable withPage(int pageNumber) {
            return delegate.withPage(pageNumber);
        }

        @Override
        public boolean hasPrevious() {
            return delegate.hasPrevious();
        }
    }
}
