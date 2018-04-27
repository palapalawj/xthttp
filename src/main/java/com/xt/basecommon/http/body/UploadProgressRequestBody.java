package com.xt.basecommon.http.body;

import android.util.Log;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * Describe: 带进度的上传请求
 * Created by lijin on 2017/9/22.
 */

public class UploadProgressRequestBody extends RequestBody {

    protected RequestBody delegate;
    protected ProgressResponseCallback callback;
    protected CountingSink countingSink;

    public UploadProgressRequestBody(ProgressResponseCallback listener) {
        this.callback = listener;
    }

    public UploadProgressRequestBody(RequestBody delegate, ProgressResponseCallback listener) {
        this.delegate = delegate;
        this.callback = listener;
    }

    public void setRequestBody(RequestBody delegate) {
        this.delegate = delegate;
    }

    @Override
    public MediaType contentType() {
        return delegate.contentType();
    }

    @Override
    public long contentLength() {
        try {
            return delegate.contentLength();
        } catch (IOException e) {
            Log.e("xt", e.getMessage());
            return -1;
        }
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        BufferedSink bufferedSink;
        countingSink = new CountingSink(sink);
        bufferedSink = Okio.buffer(countingSink);
        delegate.writeTo(bufferedSink);
        bufferedSink.flush();
    }

    protected final class CountingSink extends ForwardingSink {
        private long bytesWritten = 0;
        private long contentLength = 0;
        private long lastRefreshUiTime; //最后一次刷新时间

        public CountingSink(Sink delegate) {
            super(delegate);
        }

        @Override
        public void write(Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            if (contentLength <= 0) {
                contentLength = contentLength();
            }
            bytesWritten += byteCount;
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRefreshUiTime >= 100 || bytesWritten == contentLength) {
                callback.onResponseProgress(bytesWritten, contentLength, bytesWritten ==
                        contentLength);
                lastRefreshUiTime = System.currentTimeMillis();
            }
            Log.i("xt", "bytesWritten=" + bytesWritten +
                    ", totalBytesCount=" + contentLength);
        }
    }
}
