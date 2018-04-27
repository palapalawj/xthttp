package com.xt.basecommon.http.interceptor;

import com.xt.basecommon.http.utils.HttpUtil;

import java.io.IOException;
import java.nio.charset.Charset;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

/**
 * Describe: 响应有效拦截器
 * Created by lijin on 2017/9/26.
 */

public abstract class BaseExpiredInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        ResponseBody responseBody = response.body();
        BufferedSource source = responseBody.source();
        source.request(Long.MAX_VALUE);
        Buffer buffer = source.buffer();
        Charset charset = HttpUtil.UTF8;
        MediaType contentType = responseBody.contentType();
        if (null != contentType) {
            charset = contentType.charset(HttpUtil.UTF8);
        }
        String bodyStr = buffer.clone().readString(charset);
        boolean isText = isText(contentType);
        if (!isText) {
            return response;
        }
        if (isResponseExpired(response, bodyStr)) {
            return responseExpired(chain, bodyStr);
        }
        return response;
    }

    private boolean isText(MediaType mediaType) {
        if (mediaType == null) {
            return false;
        }
        if (mediaType.type() != null && mediaType.type().equals("text")) {
            return true;
        }
        if (mediaType.subtype() != null) {
            if (mediaType.subtype().equals("json")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 处理响应是否有效
     */
    public abstract boolean isResponseExpired(Response response, String bodyString);

    /**
     * 无效响应处理
     */
    public abstract Response responseExpired(Chain chain, String bodyString);
}
