package com.xt.basecommon.http.body;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.Serializable;
import java.lang.ref.WeakReference;

/**
 * Describe: 更新UI回调
 * Created by lijin on 2017/9/22.
 */

public abstract class UIProgressResponseCallback implements ProgressResponseCallback {

    private static final int RESPONSE_UPDATE = 0x02;

    public class ProgressModel implements Serializable {
        private long currentBytes;  //当前读取字节
        private long contentLength; //总长度
        private boolean done;   //是否完成

        public ProgressModel(long currentBytes, long contentLength, boolean done) {
            this.currentBytes = currentBytes;
            this.contentLength = contentLength;
            this.done = done;
        }

        public long getCurrentBytes() {
            return currentBytes;
        }

        public ProgressModel setCurrentBytes(long currentBytes) {
            this.currentBytes = currentBytes;
            return this;
        }

        public long getContentLength() {
            return contentLength;
        }

        public ProgressModel setContentLength(long contentLength) {
            this.contentLength = contentLength;
            return this;
        }

        public boolean isDone() {
            return done;
        }

        public ProgressModel setDone(boolean done) {
            this.done = done;
            return this;
        }

        @Override
        public String toString() {
            return "ProgressModel{" +
                    "currentBytes=" + currentBytes +
                    ", contentLength=" + contentLength +
                    ", done=" + done + "}";
        }
    }

    /**
     * UI回调方法
     * @param bytesRead 读取字节长度
     * @param contentLength 总长度
     * @param done 是否完成
     */
    public abstract void onUIResponseProgress(long bytesRead, long contentLength, boolean done);

    private static class UIHandler extends Handler {
        private final WeakReference<UIProgressResponseCallback> uipWeakReference;

        public UIHandler(Looper looper, UIProgressResponseCallback listener) {
            super(looper);
            uipWeakReference = new WeakReference<UIProgressResponseCallback>(listener);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case RESPONSE_UPDATE:
                    UIProgressResponseCallback callback = uipWeakReference.get();
                    if (null != callback) {
                        ProgressModel model = (ProgressModel) message.obj;
                        callback.onUIResponseProgress(model.getCurrentBytes(),
                                model.getContentLength(), model.isDone());
                    }
                    break;
                default:
                      super.handleMessage(message);
                      break;
            }
        }
    }

    private final Handler mHandler = new UIHandler(Looper.getMainLooper(), this);

    @Override
    public void onResponseProgress(long bytesWritten, long contentLength, boolean done) {
        Message message = Message.obtain();
        message.obj = new ProgressModel(bytesWritten, contentLength, done);
        message.what = RESPONSE_UPDATE;
        mHandler.sendMessage(message);
    }
}
