package com.xt.basecommon.http.cache.stategy;

import com.xt.basecommon.http.cache.RxCache;
import com.xt.basecommon.http.cache.model.CacheResult;

import java.lang.reflect.Type;

import io.reactivex.Observable;

/**
 * Describe: 缓存策略接口
 * Created by lijin on 2017/9/25.
 */

public interface IStrategy {

    /**
     * 执行缓存
     * @param rxCache 管理对象
     * @param cacheKey 缓存key
     * @param cacheTime 缓存时间
     * @param source 网络数据
     * @param type 转换的目标对象
     * @param <T> 泛型
     * @return
     */
    <T> Observable<CacheResult<T>> execute(RxCache rxCache, String cacheKey, long cacheTime,
                                           Observable<T> source, Type type);
}
