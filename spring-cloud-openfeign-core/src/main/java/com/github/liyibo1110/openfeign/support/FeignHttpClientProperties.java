package com.github.liyibo1110.openfeign.support;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * httpclient相关的配置属性
 * @author liyibo
 * @date 2026-05-06 13:46
 */
@ConfigurationProperties(prefix = "spring.cloud.openfeign.httpclient")
public class FeignHttpClientProperties {

    public static final boolean DEFAULT_DISABLE_SSL_VALIDATION = false;

    public static final int DEFAULT_MAX_CONNECTIONS = 200;

    public static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 50;

    public static final long DEFAULT_TIME_TO_LIVE = 900L;

    public static final TimeUnit DEFAULT_TIME_TO_LIVE_UNIT = TimeUnit.SECONDS;

    public static final boolean DEFAULT_FOLLOW_REDIRECTS = true;

    public static final int DEFAULT_CONNECTION_TIMEOUT = 2000;

    public static final int DEFAULT_CONNECTION_TIMER_REPEAT = 3000;

    private boolean disableSslValidation = DEFAULT_DISABLE_SSL_VALIDATION;

    private int maxConnections = DEFAULT_MAX_CONNECTIONS;

    private int maxConnectionsPerRoute = DEFAULT_MAX_CONNECTIONS_PER_ROUTE;

    private long timeToLive = DEFAULT_TIME_TO_LIVE;

    private TimeUnit timeToLiveUnit = DEFAULT_TIME_TO_LIVE_UNIT;

    private boolean followRedirects = DEFAULT_FOLLOW_REDIRECTS;

    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

    private int connectionTimerRepeat = DEFAULT_CONNECTION_TIMER_REPEAT;

    private Hc5Properties hc5 = new Hc5Properties();

    private OkHttp okHttp = new OkHttp();

    private Http2Properties http2 = new Http2Properties();

    public int getConnectionTimerRepeat() {
        return connectionTimerRepeat;
    }

    public void setConnectionTimerRepeat(int connectionTimerRepeat) {
        this.connectionTimerRepeat = connectionTimerRepeat;
    }

    public boolean isDisableSslValidation() {
        return disableSslValidation;
    }

    public void setDisableSslValidation(boolean disableSslValidation) {
        this.disableSslValidation = disableSslValidation;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getMaxConnectionsPerRoute() {
        return maxConnectionsPerRoute;
    }

    public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
        this.maxConnectionsPerRoute = maxConnectionsPerRoute;
    }

    public long getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(long timeToLive) {
        this.timeToLive = timeToLive;
    }

    public TimeUnit getTimeToLiveUnit() {
        return timeToLiveUnit;
    }

    public void setTimeToLiveUnit(TimeUnit timeToLiveUnit) {
        this.timeToLiveUnit = timeToLiveUnit;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Hc5Properties getHc5() {
        return hc5;
    }

    public void setHc5(Hc5Properties hc5) {
        this.hc5 = hc5;
    }

    public OkHttp getOkHttp() {
        return okHttp;
    }

    public void setOkHttp(OkHttp okHttp) {
        this.okHttp = okHttp;
    }

    public Http2Properties getHttp2() {
        return http2;
    }

    public void setHttp2(Http2Properties http2) {
        this.http2 = http2;
    }

    public static class Hc5Properties {
        public static final PoolConcurrencyPolicy DEFAULT_POOL_CONCURRENCY_POLICY = PoolConcurrencyPolicy.STRICT;

        public static final PoolReusePolicy DEFAULT_POOL_REUSE_POLICY = PoolReusePolicy.FIFO;

        public static final int DEFAULT_SOCKET_TIMEOUT = 5;

        public static final TimeUnit DEFAULT_SOCKET_TIMEOUT_UNIT = TimeUnit.SECONDS;

        public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 3;

        public static final TimeUnit DEFAULT_CONNECTION_REQUEST_TIMEOUT_UNIT = TimeUnit.MINUTES;

        private PoolConcurrencyPolicy poolConcurrencyPolicy = DEFAULT_POOL_CONCURRENCY_POLICY;

        private PoolReusePolicy poolReusePolicy = DEFAULT_POOL_REUSE_POLICY;

        private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;

        private TimeUnit socketTimeoutUnit = DEFAULT_SOCKET_TIMEOUT_UNIT;

        private int connectionRequestTimeout = DEFAULT_CONNECTION_REQUEST_TIMEOUT;

        private TimeUnit connectionRequestTimeoutUnit = DEFAULT_CONNECTION_REQUEST_TIMEOUT_UNIT;

        public PoolConcurrencyPolicy getPoolConcurrencyPolicy() {
            return poolConcurrencyPolicy;
        }

        public void setPoolConcurrencyPolicy(PoolConcurrencyPolicy poolConcurrencyPolicy) {
            this.poolConcurrencyPolicy = poolConcurrencyPolicy;
        }

        public PoolReusePolicy getPoolReusePolicy() {
            return poolReusePolicy;
        }

        public void setPoolReusePolicy(PoolReusePolicy poolReusePolicy) {
            this.poolReusePolicy = poolReusePolicy;
        }

        public TimeUnit getSocketTimeoutUnit() {
            return socketTimeoutUnit;
        }

        public void setSocketTimeoutUnit(TimeUnit socketTimeoutUnit) {
            this.socketTimeoutUnit = socketTimeoutUnit;
        }

        public int getSocketTimeout() {
            return socketTimeout;
        }

        public void setSocketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
        }

        public int getConnectionRequestTimeout() {
            return connectionRequestTimeout;
        }

        public void setConnectionRequestTimeout(int connectionRequestTimeout) {
            this.connectionRequestTimeout = connectionRequestTimeout;
        }
        public TimeUnit getConnectionRequestTimeoutUnit() {
            return connectionRequestTimeoutUnit;
        }

        public void setConnectionRequestTimeoutUnit(TimeUnit connectionRequestTimeoutUnit) {
            this.connectionRequestTimeoutUnit = connectionRequestTimeoutUnit;
        }

        public enum PoolConcurrencyPolicy {
            LAX,
            STRICT
        }

        public enum PoolReusePolicy {
            LIFO,
            FIFO

        }
    }

    public static class OkHttp {
        private Duration readTimeout = Duration.ofSeconds(60);

        private List<String> protocols = List.of("HTTP_2", "HTTP_1_1");

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }

        public List<String> getProtocols() {
            return protocols;
        }

        public void setProtocols(List<String> protocols) {
            this.protocols = protocols;
        }
    }

    public static class Http2Properties {
        private String version = "HTTP_2";

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}
