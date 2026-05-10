package com.github.liyibo1110.openfeign.encoding;

import feign.RequestTemplate;

import java.util.Collection;
import java.util.Map;

/**
 * 通过指定Content-Encoding标头来启用HTTP请求负载压缩。
 * @author liyibo
 * @date 2026-05-10 16:58
 */
public class FeignContentGzipEncodingInterceptor extends BaseRequestInterceptor {

    protected FeignContentGzipEncodingInterceptor(FeignClientEncodingProperties properties) {
        super(properties);
    }

    @Override
    public void apply(RequestTemplate template) {
        if (requiresCompression(template))
            addHeader(template, HttpEncoding.CONTENT_ENCODING_HEADER, getContentEncodings());
    }

    private String[] getContentEncodings() {
        if (getProperties().getContentEncodingTypes() == null || getProperties().getContentEncodingTypes().length == 0)
            throw new IllegalStateException("Invalid ContentEncodingTypes configuration");

        return getProperties().getContentEncodingTypes();
    }

    /**
     * 返回该请求是否需要GZIP压缩。
     */
    private boolean requiresCompression(RequestTemplate template) {
        final Map<String, Collection<String>> headers = template.headers();
        return matchesMimeType(headers.get(HttpEncoding.CONTENT_TYPE))
                && contentLengthExceedThreshold(headers.get(HttpEncoding.CONTENT_LENGTH));
    }

    /**
     * 返回请求内容长度是否超过配置的最小值。
     */
    private boolean contentLengthExceedThreshold(Collection<String> contentLength) {
        try {
            if (contentLength == null || contentLength.size() != 1)
                return false;

            final String strLen = contentLength.iterator().next();
            final long length = Long.parseLong(strLen);
            return length > getProperties().getMinRequestSize();
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * 返回内容MIME类型是否与配置的MIME类型匹配。
     */
    private boolean matchesMimeType(Collection<String> contentTypes) {
        if (contentTypes == null || contentTypes.size() == 0)
            return false;

        if (getProperties().getMimeTypes() == null || getProperties().getMimeTypes().length == 0)
            return true;

        for (String mimeType : getProperties().getMimeTypes()) {
            if (contentTypes.contains(mimeType))
                return true;
        }

        return false;
    }
}
