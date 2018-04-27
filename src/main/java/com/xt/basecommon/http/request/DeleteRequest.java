package com.xt.basecommon.http.request;

import com.google.gson.reflect.TypeToken;
import com.xt.basecommon.http.cache.model.CacheResult;
import com.xt.basecommon.http.callback.CallBack;
import com.xt.basecommon.http.callback.CallBackProxy;
import com.xt.basecommon.http.func.ApiResultFunc;
import com.xt.basecommon.http.func.CacheResultFunc;
import com.xt.basecommon.http.func.RetryExceptionFunc;
import com.xt.basecommon.http.model.ApiResult;
import com.xt.basecommon.http.subsciber.CallBackSubsciber;
import com.xt.basecommon.http.utils.RxUtil;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import okhttp3.ResponseBody;

/**
 * Describe: delete请求
 * Created by lijin on 2017/9/27.
 */

public class DeleteRequest extends BaseRequest<DeleteRequest> {

    public DeleteRequest(String url) {
        super(url);
    }

    public <T> Disposable execute(CallBack<T> callBack) {
        return execute(new CallBackProxy<ApiResult<T>, T>(callBack) {
        });
    }

    public <T> Disposable execute(CallBackProxy<? extends ApiResult<T>, T> proxy) {
        Observable<CacheResult<T>> observable = build().toObservable(generateRequest(), proxy);
        if (CacheResult.class != proxy.getCallBack().getRawType()) {
            return observable.compose(new ObservableTransformer<CacheResult<T>, T>() {
                @Override
                public ObservableSource<T> apply(@NonNull Observable<CacheResult<T>> upstream) {
                    return upstream.map(new CacheResultFunc<T>());
                }
            }).subscribeWith(new CallBackSubsciber<T>(context, proxy.getCallBack()));
        } else {
            return observable.subscribeWith(new CallBackSubsciber<CacheResult<T>>(context,
                    proxy.getCallBack()));
        }
    }

    private <T> Observable<CacheResult<T>> toObservable(Observable observable,
                                                        CallBackProxy<? extends ApiResult<T>, T> proxy) {
        return observable.map(new ApiResultFunc(
                proxy != null ? proxy.getType() : new TypeToken<ResponseBody>() {}.getType()))
                .compose(isSyncRequest ? RxUtil._main() : RxUtil._io_main())
                .compose(rxCache.transformer(cacheMode, proxy.getCallBack().getType()))
                .retryWhen(new RetryExceptionFunc(retryCount, retryDelay, retryIncreaseDelay));
    }

    @Override
    protected Observable<ResponseBody> generateRequest() {
        return apiManager.delete(url, params.urlParamsMap);
    }
}
