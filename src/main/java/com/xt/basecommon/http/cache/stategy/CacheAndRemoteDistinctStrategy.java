package com.xt.basecommon.http.cache.stategy;

import com.xt.basecommon.http.cache.RxCache;
import com.xt.basecommon.http.cache.model.CacheResult;

import java.lang.reflect.Type;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import okio.ByteString;

/**
 * Describe: 先缓存，再请求网络
 * Created by lijin on 2017/9/25.
 */

public final class CacheAndRemoteDistinctStrategy extends BaseStrategy {

    @Override
    public <T> Observable<CacheResult<T>> execute(RxCache rxCache, String key, long time, Observable<T> source, Type type) {
        Observable<CacheResult<T>> cache = loadCache(rxCache, type, key, time,true);
        Observable<CacheResult<T>> remote = loadRemote(rxCache, key, source,false);
        return Observable.concat(cache, remote)
                .filter(new Predicate<CacheResult<T>>() {
                    @Override
                    public boolean test(@NonNull CacheResult<T> tCacheResult) throws Exception {
                        return tCacheResult != null && tCacheResult.data != null;
                    }
                }).distinctUntilChanged(new Function<CacheResult<T>, Object>() {
                    @Override
                    public String apply(@NonNull CacheResult<T> tCacheResult) throws Exception {
                        return  ByteString.of(tCacheResult.data.toString().getBytes()).md5().hex();
                    }
                });
    }
}
