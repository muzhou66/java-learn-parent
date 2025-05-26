package com.muzhou.commons.cache.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.concurrent.TimeUnit;

/**
 * 热点Key配置属性
 */
@ConfigurationProperties(prefix = "multilevel.cache.hotkey")
public class HotKeyProperties {

    /**
     * 是否启用热点Key检测，默认true
     */
    private boolean enabled = true;

    /**
     * 热点Key数量阈值，默认1000
     */
    private long hotThreshold = 1000;

    /**
     * 统计TopN的Key，默认100
     */
    private int topN = 100;

    /**
     * 初始延迟时间(秒)，默认10
     */
    private long initialDelay = 10;

    /**
     * 分析间隔时间(秒)，默认300(5分钟)
     */
    private long analyzeInterval = 300;

    /**
     * 预加载间隔时间(秒)，默认60(1分钟)
     */
    private long preloadInterval = 60;

    /**
     * 是否开启调试日志，默认false
     */
    private boolean debug = false;

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getHotThreshold() {
        return hotThreshold;
    }

    public void setHotThreshold(long hotThreshold) {
        this.hotThreshold = hotThreshold;
    }

    public int getTopN() {
        return topN;
    }

    public void setTopN(int topN) {
        this.topN = topN;
    }

    public long getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(long initialDelay) {
        this.initialDelay = initialDelay;
    }

    public long getAnalyzeInterval() {
        return analyzeInterval;
    }

    public void setAnalyzeInterval(long analyzeInterval) {
        this.analyzeInterval = analyzeInterval;
    }

    public long getPreloadInterval() {
        return preloadInterval;
    }

    public void setPreloadInterval(long preloadInterval) {
        this.preloadInterval = preloadInterval;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}