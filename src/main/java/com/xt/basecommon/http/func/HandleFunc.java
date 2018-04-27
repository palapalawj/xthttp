package com.xt.basecommon.http.func;

import com.xt.basecommon.http.exception.ApiException;
import com.xt.basecommon.http.exception.ServerException;
import com.xt.basecommon.http.model.ApiResult;

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;

/**
 * Describe: ApiResut<T>转换T
 * Created by lijin on 2017/9/26.
 */

public class HandleFunc<T> implements Function<ApiResult<T>, T> {

    @Override
    public T apply(@NonNull ApiResult<T> apiResult) throws Exception {
        if (ApiException.isOk(apiResult)) {
            return apiResult.getData();
        } else {
            throw new ServerException(apiResult.getCode(), apiResult.getMsg());
        }
    }
}
