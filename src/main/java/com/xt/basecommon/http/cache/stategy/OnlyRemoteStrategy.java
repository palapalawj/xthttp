package com.xt.basecommon.http.cache.stategy;

import com.xt.basecommon.http.cache.RxCache;
import com.xt.basecommon.http.cache.model.CacheResult;

import java.lang.reflect.Type;

import io.reactivex.Observable;

/**
 * Describe: 仅网络
 * Created by lijin on 2017/9/25.
 */

public final class OnlyRemoteStrategy extends BaseStrategy {

    @Override
    public <T> Observable<CacheResult<T>> execute(RxCache rxCache, String key, long time,
                                                  Observable<T> source, Type type) {
        return loadRemote(rxCache, key, source, false);
    }
}
