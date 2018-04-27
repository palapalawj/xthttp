package com.xt.basecommon.http.interceptor;

import android.util.Log;

import com.xt.basecommon.http.model.HttpHeaders;

import java.io.IOException;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Describe: 头部拦截器
 * Created by lijin on 2017/9/26.
 */

public class HeadersInterceptor implements Interceptor {

    private HttpHeaders headers;

    public HeadersInterceptor(HttpHeaders headers) {
        this.headers = headers;
    }


    @Override
    public Response intercept(Chain chain) throws IOException {
        Request.Builder builder = chain.request().newBuilder();
        if (headers.headersMap.isEmpty()) {
            return chain.proceed(builder.build());
        }
        try {
            for (Map.Entry<String, String> entry : headers.headersMap.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue()).build();
            }
        } catch (Exception e) {
            Log.e("xt", e.getMessage());
        }
        return chain.proceed(builder.build());

    }
}
