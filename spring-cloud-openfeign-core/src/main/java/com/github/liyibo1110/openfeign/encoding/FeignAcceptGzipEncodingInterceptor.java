package com.github.liyibo1110.openfeign.encoding;

import feign.RequestTemplate;

/**
 * 通过指定Accept-Encoding头部启用HTTP响应负载压缩。
 * 尽管这并不意味着请求一定会被压缩，但这要求远程服务器能够识别该头部，并已配置为对响应进行压缩。
 * 此外，并非所有响应都会被压缩，这取决于媒体类型的匹配情况以及响应内容长度等其他因素。
 * @author liyibo
 * @date 2026-05-10 16:37
 */
public class FeignAcceptGzipEncodingInterceptor extends BaseRequestInterceptor {

    protected FeignAcceptGzipEncodingInterceptor(FeignClientEncodingProperties properties) {
        super(properties);
    }

    @Override
    public void apply(RequestTemplate template) {
        addHeader(template, HttpEncoding.ACCEPT_ENCODING_HEADER, HttpEncoding.GZIP_ENCODING, HttpEncoding.DEFLATE_ENCODING);
    }
}
