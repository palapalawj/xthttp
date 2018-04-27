package com.xt.basecommon.http.cache.core;

import com.xt.basecommon.http.utils.Utils;

import java.lang.reflect.Type;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Describe: 缓存的基类，包含锁机制，防止频繁读取造成的异常
 * Created by lijin on 2017/9/24.
 */

public abstract class BaseCache {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 读取缓存
     * @param type
     * @param key
     * @param existTime
     * @param <T>
     * @return
     */
    final <T> T load(Type type, String key, long existTime) {
        //检查key
        Utils.checkNotNull(key, "key==null");
        //判断key是否存在，没有则不读取缓存
        if (!containsKey(key)) {
            return null;
        }
        //判断是否过期
        if (isExpiry(key, existTime)) {
            remove(key);
            return null;
        }
        //开始读取缓存
        lock.readLock().lock();
        try {
            return doLoad(type, key);
        } finally {
            lock.readLock().unlock();
        }
    }

    //删除缓存
    final boolean remove(String key) {
        lock.writeLock().lock();
        try {
            return doDelete(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    //清除缓存
    final boolean clear() {
        lock.writeLock().lock();
        try {
            return doClear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    //保存缓存
    final <T> boolean save(String key, T value) {
        Utils.checkNotNull(key, "key==null");
        if (null == value) {
            return remove(key);
        }
        boolean status = false;
        lock.writeLock().lock();
        try {
            status = doSave(key, value);
        } finally {
            lock.writeLock().unlock();
        }
        return status;
    }

    /**
     * 是否包含，进行锁处理
     * @param key
     * @return
     */
    public final boolean containsKey(String key) {
        lock.readLock().lock();
        try {
            return doContainsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 是否包含
     * @param key
     * @return
     */
    protected abstract boolean doContainsKey(String key);

    /**
     * 是否过期
     * @param key
     * @param existTime
     * @return
     */
    protected abstract boolean isExpiry(String key, long existTime);

    /**
     * 读取缓存
     * @param type
     * @param key
     * @param <T>
     * @return
     */
    protected abstract <T> T doLoad(Type type, String key);

    /**
     * 保存
     * @param key
     * @param value
     * @param <T>
     * @return
     */
    protected abstract <T> boolean doSave(String key, T value);

    /**
     * 删除缓存
     * @param key
     * @return
     */
    protected abstract boolean doDelete(String key);

    /**
     * 清除缓存
     * @return
     */
    protected abstract boolean doClear();
}
