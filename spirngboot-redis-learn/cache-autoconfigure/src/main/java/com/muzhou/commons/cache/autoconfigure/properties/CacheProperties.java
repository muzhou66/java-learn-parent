package com.muzhou.commons.cache.autoconfigure.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * 多级缓存属性配置
 */
@Data
@ConfigurationProperties(prefix = "commons.cache")
public class CacheProperties {

    /**
     * 本地缓存配置
     */
    private Local local = new Local();

    /**
     * Redis 缓存配置
     */
    private Redis redis = new Redis();

    /**
     * 本地缓存配置项
     */
    @Data
    public static class Local {
        /**
         * 写入后过期时间，默认 10 分钟
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration expireAfterWrite = Duration.ofMinutes(10);

        /**
         * 写入后刷新时间，不配置则不刷新
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration refreshAfterWrite;

        /**
         * 缓存最大数量，默认 10000
         */
        private long maximumSize = 10000;

        /**
         * 是否记录统计信息，默认 true
         */
        private boolean recordStats = true;

        /**
         * 是否开启软引用，默认 false
         */
        private boolean softValues = false;
    }

    /**
     * Redis 缓存配置项
     */
    @Data
    public static class Redis {
        /**
         * 默认过期时间，默认1小时
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration defaultTtl = Duration.ofHours(1);

        /**
         * 随机TTL范围，默认10分钟
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration randomTtlRange = Duration.ofMinutes(10);

        /**
         * 空值缓存时间，默认5分钟
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration nullValueTtl = Duration.ofMinutes(5);

        /**
         * 缓存键前缀
         */
        private String keyPrefix = "cache:";

        /**
         * 是否缓存空值，默认true
         */
        private boolean cacheNullValues = true;
    }
}
