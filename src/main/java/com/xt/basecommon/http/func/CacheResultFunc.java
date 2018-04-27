package com.xt.basecommon.http.func;

import com.xt.basecommon.http.cache.model.CacheResult;

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;

/**
 * Describe: 缓存结果转换
 * Created by lijin on 2017/9/26.
 */

public class CacheResultFunc<T> implements Function<CacheResult<T>, T> {

    @Override
    public T apply(@NonNull CacheResult<T> cacheResult) throws Exception {
        return cacheResult.data;
    }
}
