package com.xt.basecommon.http.cache.stategy;

import com.xt.basecommon.http.cache.RxCache;
import com.xt.basecommon.http.cache.model.CacheResult;

import java.lang.reflect.Type;

import io.reactivex.Observable;

/**
 * Describe: 仅缓存
 * Created by lijin on 2017/9/25.
 */

public final class OnlyCacheStrategy extends BaseStrategy {

    @Override
    public <T> Observable<CacheResult<T>> execute(RxCache rxCache, String key, long time,
                                                  Observable<T> source, Type type) {
        return loadCache(rxCache,type,key,time,false);
    }
}
