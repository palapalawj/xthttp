package com.xt.basecommon.http.callback;

/**
 * Describe: 下载进度回调
 * Created by lijin on 2017/9/25.
 */

public abstract class DownloadProgressCallBack<T> extends CallBack<T> {

    public DownloadProgressCallBack() {}

    @Override
    public void onSuccess(T response) {}

    public abstract void update(long bytesRead, long contentLength, boolean done);

    public abstract void onComplete(String path);

    @Override
    public void onCompleted() {}
}
