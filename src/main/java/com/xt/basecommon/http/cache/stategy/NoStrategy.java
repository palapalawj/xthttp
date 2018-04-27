package com.xt.basecommon.http.cache.stategy;

import com.xt.basecommon.http.cache.RxCache;
import com.xt.basecommon.http.cache.model.CacheResult;

import java.lang.reflect.Type;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

/**
 * Describe: 仅网络，不缓存
 * Created by lijin on 2017/9/25.
 */

public class NoStrategy implements IStrategy {

    @Override
    public <T> Observable<CacheResult<T>> execute(RxCache rxCache, String key, long time,
                                                  Observable<T> source, Type type) {
        return source.map(new Function<T, CacheResult<T>>() {
            @Override
            public CacheResult<T> apply(T t) throws Exception {
                return new CacheResult<T>(false, t);
            }
        });
    }
}
