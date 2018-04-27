package com.xt.basecommon.http.body;

/**
 * Describe: 上传进度回调接口
 * Created by lijin on 2017/9/22.
 */

public interface ProgressResponseCallback {

    /**
     *
     * @param bytesWritten 当前读取字节长度
     * @param contentLength 总长度
     * @param done 是否读取完成
     */
    void onResponseProgress(long bytesWritten, long contentLength, boolean done);
}
