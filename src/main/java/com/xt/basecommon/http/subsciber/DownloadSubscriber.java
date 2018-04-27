package com.xt.basecommon.http.subsciber;

import android.content.Context;
import android.text.TextUtils;

import com.xt.basecommon.http.callback.CallBack;
import com.xt.basecommon.http.callback.DownloadProgressCallBack;
import com.xt.basecommon.http.exception.ApiException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

/**
 * Describe: 下载订阅
 * Created by lijin on 2017/9/25.
 */

public class DownloadSubscriber<ResponseBody extends okhttp3.ResponseBody> extends
        BaseSubscriber<ResponseBody> {

    private Context context;
    private String path;
    private String name;
    public CallBack mCallBack;
    private static String APK_CONTENTTYPE = "application/vnd.android.package-archive";
    private static String PNG_CONTENTTYPE = "image/png";
    private static String JPG_CONTENTTYPE = "image/jpg";
    private static String TEXT_CONTENTTYPE = "text/html; charset=utf-8";
    private static String fileSuffix = "";
    private long lastRefreshUiTime;

    public DownloadSubscriber(Context context, String path, String name, CallBack callBack) {
        super(context);
        this.path = path;
        this.name = name;
        this.mCallBack = callBack;
        this.context = context;
        this.lastRefreshUiTime = System.currentTimeMillis();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mCallBack != null) {
            mCallBack.onStart();
        }
    }

    @Override
    public final void onComplete() {
       /* if (mCallBack != null) {
            mCallBack.onCompleted();
        }*/
    }

    @Override
    public void onError(final ApiException e) {
        finalonError(e);
    }

    @Override
    public void onNext(ResponseBody responseBody) {
        writeResponseBodyToDisk(path, name, context, responseBody);

    }

    private boolean writeResponseBodyToDisk(String path, String name, Context context, okhttp3.ResponseBody body) {
        if (!TextUtils.isEmpty(name)) {
            String type;
            if (!name.contains(".")) {
                type = body.contentType().toString();
                if (type.equals(APK_CONTENTTYPE)) {
                    fileSuffix = ".apk";
                } else if (type.equals(PNG_CONTENTTYPE)) {
                    fileSuffix = ".png";
                } else if (type.equals(JPG_CONTENTTYPE)) {
                    fileSuffix = ".jpg";
                } else {
                    fileSuffix = "." + body.contentType().subtype();
                }
                name = name + fileSuffix;
            }
        } else {
            name = System.currentTimeMillis() + fileSuffix;
        }

        if (path == null) {
            path = context.getExternalFilesDir(null) + File.separator + name;
        } else {
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
            path = path + File.separator + name;
            path = path.replaceAll("//", "/");
        }

        try {
            File futureStudioIconFile = new File(path);
           /* if (!futureStudioIconFile.exists()) {
                futureStudioIconFile.createNewFile();
            }*/
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                byte[] fileReader = new byte[4096];

                final long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;
                inputStream = body.byteStream();
                outputStream = new FileOutputStream(futureStudioIconFile);
                final CallBack callBack = mCallBack;
                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);
                    fileSizeDownloaded += read;
                    //下载进度
                    float progress = fileSizeDownloaded * 1.0f / fileSize;
                    long curTime = System.currentTimeMillis();
                    //每200毫秒刷新一次数据,防止频繁更新进度
                    if (curTime - lastRefreshUiTime >= 200 || progress == 1.0f) {
                        if (callBack != null) {
                            if (callBack != null) {
                                final long finalFileSizeDownloaded = fileSizeDownloaded;
                                Observable.just(finalFileSizeDownloaded).observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new Consumer<Long>() {
                                            @Override
                                            public void accept(@NonNull Long aLong) throws Exception {
                                                if (callBack instanceof DownloadProgressCallBack) {
                                                    ((DownloadProgressCallBack) callBack)
                                                            .update(finalFileSizeDownloaded, fileSize,
                                                                    finalFileSizeDownloaded == fileSize);
                                                }
                                            }
                                        }, new Consumer<Throwable>() {
                                            @Override
                                            public void accept(@NonNull Throwable throwable) throws Exception {

                                            }
                                        });
                            }
                        }
                        lastRefreshUiTime = System.currentTimeMillis();
                    }
                }

                outputStream.flush();

                if (callBack != null) {
                    final String finalPath = path;
                    Observable.just(finalPath).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<String>() {
                        @Override
                        public void accept(@NonNull String s) throws Exception {
                            if (callBack instanceof DownloadProgressCallBack) {
                                ((DownloadProgressCallBack) callBack).onComplete(finalPath);
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(@NonNull Throwable throwable) throws Exception {

                        }
                    });
                }
                return true;
            } catch (IOException e) {
                finalonError(e);
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            finalonError(e);
            return false;
        }
    }

    private void finalonError(final Exception e) {

        if (mCallBack == null) {
            return;
        }
        //if (Utils.checkMain()) {
        Observable.just(new ApiException(e, 100)).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<ApiException>() {
            @Override
            public void accept(@NonNull ApiException e) throws Exception {
                if (mCallBack != null) {
                    mCallBack.onError(e);
                }
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(@NonNull Throwable throwable) throws Exception {

            }
        });
    }
}
