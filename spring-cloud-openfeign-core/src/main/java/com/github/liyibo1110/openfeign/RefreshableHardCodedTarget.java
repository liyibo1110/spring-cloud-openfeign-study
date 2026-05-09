package com.github.liyibo1110.openfeign;

import feign.Target;

/**
 * 绑定了特定url的HardCodedTarget实现。
 * @author liyibo
 * @date 2026-05-08 11:00
 */
public class RefreshableHardCodedTarget<T> extends Target.HardCodedTarget<T> {

    private final RefreshableUrl refreshableUrl;

    private final String cleanPath;

    @SuppressWarnings("unchecked")
    public RefreshableHardCodedTarget(Class type, String name, RefreshableUrl refreshableUrl) {
        super(type, name, refreshableUrl.getUrl());
        this.refreshableUrl = refreshableUrl;
        this.cleanPath = "";
    }

    @SuppressWarnings("unchecked")
    public RefreshableHardCodedTarget(Class type, String name, RefreshableUrl refreshableUrl, String cleanPath) {
        super(type, name, refreshableUrl.getUrl());
        this.refreshableUrl = refreshableUrl;
        this.cleanPath = cleanPath;
    }

    @Override
    public String url() {
        return refreshableUrl.getUrl() + cleanPath;
    }
}
