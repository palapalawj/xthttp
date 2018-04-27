package com.xt.basecommon.http.subsciber;

import android.content.Context;

import com.xt.basecommon.http.callback.CallBack;
import com.xt.basecommon.http.exception.ApiException;

import io.reactivex.annotations.NonNull;

/**
 * Describe: 带回调的订阅，只实现回调，无需订阅
 * Created by lijin on 2017/9/25.
 */

public class CallBackSubsciber<T> extends BaseSubscriber<T> {

    public CallBack<T> callBack;

    public CallBackSubsciber(Context context, CallBack<T> callBack) {
        super(context);
        this.callBack = callBack;
        if (callBack instanceof ProgressDialogCallBack) {
            ((ProgressDialogCallBack) callBack).subscription(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (null != callBack) {
            callBack.onStart();
        }
    }

    @Override
    public void onError(ApiException e) {
        if (callBack != null) {
            callBack.onError(e);
        }
    }

    @Override
    public void onNext(@NonNull T t) {
        super.onNext(t);
        if (callBack != null) {
            callBack.onSuccess(t);
        }
    }

    @Override
    public void onComplete() {
        super.onComplete();
        if (callBack != null) {
            callBack.onCompleted();
        }
    }
}
