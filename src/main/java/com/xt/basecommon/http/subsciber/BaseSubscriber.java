package com.xt.basecommon.http.subsciber;

import android.content.Context;
import android.util.Log;

import com.xt.basecommon.http.utils.Utils;
import com.xt.basecommon.http.exception.ApiException;

import java.lang.ref.WeakReference;

import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableObserver;

/**
 * Describe: 订阅基类
 * Created by lijin on 2017/9/25.
 */

public abstract class BaseSubscriber<T> extends DisposableObserver<T> {

    public WeakReference<Context> contextWeakReference;

    public BaseSubscriber() {
    }

    @Override
    protected void onStart() {
        Log.e("xt", "http is onStart");
        if (contextWeakReference != null && contextWeakReference.get() != null && !Utils.isNetworkAvailable(contextWeakReference.get())) {
            onComplete();
        }
    }

    public BaseSubscriber(Context context) {
        if (context != null) {
            contextWeakReference = new WeakReference<>(context);
        }
    }

    @Override
    public void onNext(@NonNull T t) {
        Log.e("xt", "http is onNext");
    }

    @Override
    public final void onError(java.lang.Throwable e) {
        Log.e("xt", "http is onError");
        if (e instanceof ApiException) {
            Log.e("xt", "e instanceof ApiException err:" + e);
            onError((ApiException) e);
        } else {
            Log.e("xt", "e !instanceof ApiException err:" + e);
            onError(ApiException.handleException(e));
        }
    }

    @Override
    public void onComplete() {
        Log.e("xt", "http is onComplete");
    }


    public abstract void onError(ApiException e);
}
