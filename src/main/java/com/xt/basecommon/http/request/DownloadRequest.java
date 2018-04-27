package com.xt.basecommon.http.request;

import com.xt.basecommon.http.callback.CallBack;
import com.xt.basecommon.http.func.RetryExceptionFunc;
import com.xt.basecommon.http.subsciber.DownloadSubscriber;
import com.xt.basecommon.http.transformer.HandleErrTransformer;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

/**
 * Describe: 下载请求
 * Created by lijin on 2017/9/27.
 */

public class DownloadRequest extends BaseRequest<DownloadRequest> {

    private String savePath;
    private String saveName;

    public DownloadRequest(String url) {
        super(url);
    }

    /**
     * 下载文件路径
     * 默认在：/storage/emulated/0/Android/data/包名/files/
     */
    public DownloadRequest savePath(String savePath) {
        this.savePath = savePath;
        return this;
    }

    /**
     * 下载文件名称
     * 默认名字是时间戳生成的
     */
    public DownloadRequest saveName(String saveName) {
        this.saveName = saveName;
        return this;
    }

    public <T> Disposable execute(CallBack<T> callBack) {
        return (Disposable) build().apiManager.downloadFile(url).compose(
                new ObservableTransformer<ResponseBody, ResponseBody>() {
            @Override
            public ObservableSource<ResponseBody> apply(@NonNull Observable<ResponseBody> upstream) {
                if(isSyncRequest){
                    return upstream;
                }else {
                    return upstream.subscribeOn(Schedulers.io())
                            .unsubscribeOn(Schedulers.io())
                            .observeOn(Schedulers.io());
                }
            }
        }).compose(new HandleErrTransformer()).retryWhen(
                new RetryExceptionFunc(retryCount, retryDelay, retryIncreaseDelay))
                .subscribeWith(new DownloadSubscriber(context,savePath, saveName, callBack));
    }

    @Override
    protected Observable<ResponseBody> generateRequest() {
        return apiManager.downloadFile(url);
    }
}
