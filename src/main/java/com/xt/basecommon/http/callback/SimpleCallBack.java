package com.xt.basecommon.http.callback;

/**
 * Describe: 默认回调
 * Created by lijin on 2017/9/25.
 */

public abstract class SimpleCallBack<T> extends CallBack<T> {

    @Override
    public void onStart() {}

    @Override
    public void onCompleted() {}
}
