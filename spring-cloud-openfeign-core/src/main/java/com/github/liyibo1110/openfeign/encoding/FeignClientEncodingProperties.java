package com.github.liyibo1110.openfeign.encoding;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.Objects;

/**
 * feign encoding相关properties。
 * @author liyibo
 * @date 2026-05-10 16:23
 */
@ConfigurationProperties("spring.cloud.openfeign.compression.request")
public class FeignClientEncodingProperties {

    /** 支持的mime types */
    private String[] mimeTypes = new String[] { "text/xml", "application/xml", "application/json" };

    /** content size的最小阈值 */
    private int minRequestSize = 2048;

    /** content encodings的列表，即压缩encoding */
    private String[] contentEncodingTypes = new String[] { HttpEncoding.GZIP_ENCODING, HttpEncoding.DEFLATE_ENCODING };

    public String[] getMimeTypes() {
        return mimeTypes;
    }

    public void setMimeTypes(String[] mimeTypes) {
        this.mimeTypes = mimeTypes;
    }

    public int getMinRequestSize() {
        return minRequestSize;
    }

    public void setMinRequestSize(int minRequestSize) {
        this.minRequestSize = minRequestSize;
    }

    public String[] getContentEncodingTypes() {
        return contentEncodingTypes;
    }

    public void setContentEncodingTypes(String[] contentEncodingTypes) {
        this.contentEncodingTypes = contentEncodingTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        FeignClientEncodingProperties that = (FeignClientEncodingProperties) o;
        return Arrays.equals(mimeTypes, that.mimeTypes) && Objects.equals(minRequestSize, that.minRequestSize)
                && Arrays.equals(contentEncodingTypes, that.contentEncodingTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(mimeTypes), minRequestSize);
    }

    @Override
    public String toString() {
        return new StringBuilder("FeignClientEncodingProperties{").append("mimeTypes=")
                .append(Arrays.toString(mimeTypes))
                .append(", ")
                .append("minRequestSize=")
                .append(minRequestSize)
                .append(", ")
                .append("contentEncodingTypes=")
                .append(Arrays.toString(contentEncodingTypes))
                .append("}")
                .toString();
    }
}
