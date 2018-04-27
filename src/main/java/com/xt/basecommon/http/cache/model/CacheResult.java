package com.xt.basecommon.http.cache.model;

import java.io.Serializable;

/**
 * Describe: 缓存对象
 * Created by lijin on 2017/9/25.
 */

public class CacheResult<T> implements Serializable {

    public boolean isFromCache;
    public T data;

    public CacheResult() {}

    public CacheResult(boolean isFromCache) {
        this.isFromCache = isFromCache;
    }

    public CacheResult(boolean isFromCache, T data) {
        this.isFromCache = isFromCache;
        this.data = data;
    }

    public boolean isCache() {
        return isFromCache;
    }

    public void setCache(boolean cache) {
        isFromCache = cache;
    }

    @Override
    public String toString() {
        return "CacheResult{" +
                "isCache=" + isFromCache +
                ",data=" + data +
                "}";
    }
}
