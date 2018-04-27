package com.xt.basecommon.http.cache.core;

import android.util.Log;

import com.xt.basecommon.http.utils.Utils;

import java.lang.reflect.Type;

import okio.ByteString;

/**
 * Describe: 缓存核心类，使用磁盘缓存，对key进行MD5加密
 * Created by lijin on 2017/9/24.
 */

public class CoreCache {

    private LruDiskCache diskCache;

    public CoreCache(LruDiskCache diskCache) {
        this.diskCache = Utils.checkNotNull(diskCache, "disk==null");
    }

    /**
     * 读取
     * @param type
     * @param key
     * @param time
     * @param <T>
     * @return
     */
    public synchronized <T> T load(Type type, String key, long time) {
        String cacheKey = ByteString.of(key.getBytes()).md5().hex();
        Log.e("xt", "loadCache key=" + cacheKey);
        if (null != diskCache) {
            T result = diskCache.load(type, key, time);
            if (null != result) {
                return result;
            }
        }
        return null;
    }

    /**
     * 保存
     * @param key
     * @param value
     * @param <T>
     * @return
     */
    public synchronized <T> boolean save(String key, T value) {
        String cacheKey = ByteString.of(key.getBytes()).md5().hex();
        Log.d("xt", "saveCache key" + cacheKey);
        return diskCache.save(cacheKey, value);
    }

    /**
     * 判断是否包含
     * @param key
     * @return
     */
    public synchronized boolean containsKey(String key) {
        String cacheKey = ByteString.of(key.getBytes()).md5().hex();
        Log.d("xt", "containsCache key" + cacheKey);
        if (null != diskCache) {
            if (diskCache.containsKey(cacheKey))
                return true;
        }
        return false;
    }

    /**
     * 删除缓存
     * @param key
     * @return
     */
    public synchronized boolean remove(String key) {
        String cacheKey = ByteString.of(key.getBytes()).md5().hex();
        Log.d("xt", "remove key=" + cacheKey);
        if (null != diskCache)
            return diskCache.remove(cacheKey);
        return true;
    }

    /**
     * 清空缓存
     * @return
     */
    public synchronized boolean clear() {
        if (null != diskCache)
            return diskCache.clear();
        return false;
    }
}
