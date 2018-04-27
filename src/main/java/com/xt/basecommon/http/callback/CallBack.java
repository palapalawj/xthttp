package com.xt.basecommon.http.callback;

import com.xt.basecommon.http.utils.Utils;
import com.xt.basecommon.http.exception.ApiException;

import java.lang.reflect.Type;

/**
 * Describe: 网络回调
 * Created by lijin on 2017/9/25.
 */

public abstract class CallBack<T> implements IType<T> {

    public abstract void onStart();
    public abstract void onCompleted();
    public abstract void onError(ApiException e);
    public abstract void onSuccess(T t);

    @Override
    public Type getType() {//获取需要解析的泛型T类型
        return Utils.findNeedClass(getClass());
    }

    public Type getRawType() {//获取需要解析的泛型T raw类型
        return Utils.findRawType(getClass());
    }
}
