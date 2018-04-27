package com.xt.basecommon.http;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import com.xt.basecommon.http.cache.RxCache;
import com.xt.basecommon.http.cache.converter.IDiskConverter;
import com.xt.basecommon.http.cache.converter.SerializableDiskConverter;
import com.xt.basecommon.http.cache.model.CacheMode;
import com.xt.basecommon.http.cookie.CookieManger;
import com.xt.basecommon.http.https.HttpsUtil;
import com.xt.basecommon.http.interceptor.HttpLoggingInterceptor;
import com.xt.basecommon.http.model.HttpHeaders;
import com.xt.basecommon.http.model.HttpParams;
import com.xt.basecommon.http.request.CustomRequest;
import com.xt.basecommon.http.request.DeleteRequest;
import com.xt.basecommon.http.request.DownloadRequest;
import com.xt.basecommon.http.request.GetRequest;
import com.xt.basecommon.http.request.PostRequest;
import com.xt.basecommon.http.request.PutRequest;
import com.xt.basecommon.http.utils.HttpLog;
import com.xt.basecommon.http.utils.RxUtil;
import com.xt.basecommon.http.utils.Utils;

import java.io.File;
import java.io.InputStream;
import java.net.Proxy;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import okhttp3.Cache;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Describe: 网络请求入口
 * 全局设置超时时间，支持请求错误重试相关参数（重试次数、重试延时时间），缓存模式、时间、大小、缓存目录，
 * 支持get、post、delete、put请求，支持自定义请求，支持文件上传、下载，设置公共请求头，公共参数，
 * okhttp相关参数，拦截器等，支持retrofit相关参数，支持cookie管理。
 * Created by lijin on 2017/9/26.
 */

public class XTHttp {

    private static Application mContext;
    public static final int DEFAULT_MILLISECONDS = 60000;             //默认的超时时间
    private static final int DEFAULT_RETRY_COUNT = 3;                 //默认重试次数
    private static final int DEFAULT_RETRY_INCREASEDELAY = 0;         //默认重试叠加时间
    private static final int DEFAULT_RETRY_DELAY = 500;               //默认重试延时
    public static final int DEFAULT_CACHE_NEVER_EXPIRE = -1;          //缓存过期时间，默认永久缓存
    private Cache mCache = null;                                      //Okhttp缓存对象
    private CacheMode mCacheMode = CacheMode.NO_CACHE;                //缓存类型
    private long mCacheTime = -1;                                     //缓存时间
    private File mCacheDirectory;                                     //缓存目录
    private long mCacheMaxSize;                                       //缓存大小
    private String mBaseUrl;                                          //全局BaseUrl
    private int mRetryCount = DEFAULT_RETRY_COUNT;                    //重试次数默认3次
    private int mRetryDelay = DEFAULT_RETRY_DELAY;                    //延迟xxms重试
    private int mRetryIncreaseDelay = DEFAULT_RETRY_INCREASEDELAY;    //叠加延迟
    private HttpHeaders mCommonHeaders;                               //全局公共请求头
    private HttpParams mCommonParams;                                 //全局公共请求参数
    private OkHttpClient.Builder okHttpClientBuilder;                 //okhttp请求的客户端
    private Retrofit.Builder retrofitBuilder;                         //Retrofit请求Builder
    private RxCache.Builder rxCacheBuilder;                           //RxCache请求的Builder
    private CookieManger cookieJar;                                   //Cookie管理
    private volatile static XTHttp singleton = null;

    private XTHttp() {
        okHttpClientBuilder = new OkHttpClient.Builder();
        okHttpClientBuilder.hostnameVerifier(new DefaultHostnameVerifier());
        okHttpClientBuilder.connectTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        okHttpClientBuilder.readTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        okHttpClientBuilder.writeTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        retrofitBuilder = new Retrofit.Builder();
        rxCacheBuilder = new RxCache.Builder().init(mContext)
                .diskConverter(new SerializableDiskConverter());
    }

    public static XTHttp getInstance() {
        testInitialize();
        if (singleton == null) {
            synchronized (XTHttp.class) {
                if (singleton == null) {
                    singleton = new XTHttp();
                }
            }
        }
        return singleton;
    }

    //获取上下文
    public static void init(Application app) {
        mContext = app;
    }

    public static Context getContext() {
        testInitialize();
        return mContext;
    }

    private static void testInitialize() {
        if (mContext == null) {
            throw new ExceptionInInitializerError("请先在全局Application中调用 XTHttp.init() 初始化！");
        }
    }

    public static OkHttpClient getOkHttpClient() {
        return getInstance().okHttpClientBuilder.build();
    }

    public static Retrofit getRetrofit() {
        return getInstance().retrofitBuilder.build();
    }

    public static RxCache getRxCache() {
        return getInstance().rxCacheBuilder.build();
    }

    /**
     * 对外暴露 OkHttpClient,方便自定义
     */
    public static OkHttpClient.Builder getOkHttpClientBuilder() {
        return getInstance().okHttpClientBuilder;
    }

    /**
     * 对外暴露 Retrofit,方便自定义
     */
    public static Retrofit.Builder getRetrofitBuilder() {
        return getInstance().retrofitBuilder;
    }

    /**
     * 对外暴露 RxCache,方便自定义
     */
    public static RxCache.Builder getRxCacheBuilder() {
        return getInstance().rxCacheBuilder;
    }

    /**
     * 调试模式,默认打开所有的异常调试
     */
    public XTHttp debug(String tag) {
        debug(tag, true);
        return this;
    }

    /**
     * 调试模式,第二个参数表示所有catch住的log是否需要打印<br>
     * 一般来说,这些异常是由于不标准的数据格式,或者特殊需要主动产生的,
     * 并不是框架错误,如果不想每次打印,这里可以关闭异常显示
     */
    public XTHttp debug(String tag, boolean isPrintException) {
        String tempTag = TextUtils.isEmpty(tag) ? "XTHttp_" : tag;
        if (isPrintException) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(tempTag,
                    isPrintException);
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            okHttpClientBuilder.addInterceptor(loggingInterceptor);
        }
        HttpLog.customTagPrefix = tempTag;
        HttpLog.allowE = isPrintException;
        HttpLog.allowD = isPrintException;
        HttpLog.allowI = isPrintException;
        HttpLog.allowV = isPrintException;
        return this;
    }

    /**
     * 此类是用于主机名验证的基接口。 在握手期间，如果 URL 的主机名和服务器的标识主机名不匹配，
     * 则验证机制可以回调此接口的实现程序来确定是否应该允许此连接。策略可以是基于证书的或依赖于其他验证方案。
     * 当验证 URL 主机名使用的默认规则失败时使用这些回调。如果主机名是可接受的，则返回 true
     */
    public class DefaultHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    /**
     * https的全局访问规则
     */
    public XTHttp setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        okHttpClientBuilder.hostnameVerifier(hostnameVerifier);
        return this;
    }

    /**
     * https的全局自签名证书
     */
    public XTHttp setCertificates(InputStream... certificates) {
        HttpsUtil.SSLParams sslParams = HttpsUtil.getSslSocketFactory(null, null, certificates);
        okHttpClientBuilder.sslSocketFactory(sslParams.sslSocketFactory, sslParams.trustManager);
        return this;
    }

    /**
     * https双向认证证书
     */
    public XTHttp setCertificates(InputStream bksFile, String password, InputStream... certificates) {
        HttpsUtil.SSLParams sslParams = HttpsUtil.getSslSocketFactory(bksFile, password, certificates);
        okHttpClientBuilder.sslSocketFactory(sslParams.sslSocketFactory, sslParams.trustManager);
        return this;
    }

    /**
     * 全局cookie存取规则
     */
    public XTHttp setCookieStore(CookieManger cookieManager) {
        cookieJar = cookieManager;
        okHttpClientBuilder.cookieJar(cookieJar);
        return this;
    }

    /**
     * 获取全局的cookie实例
     */
    public static CookieManger getCookieJar() {
        return getInstance().cookieJar;
    }

    /**
     * 全局读取超时时间
     */
    public XTHttp setReadTimeOut(long readTimeOut) {
        okHttpClientBuilder.readTimeout(readTimeOut, TimeUnit.MILLISECONDS);
        return this;
    }

    /**
     * 全局写入超时时间
     */
    public XTHttp setWriteTimeOut(long writeTimeout) {
        okHttpClientBuilder.writeTimeout(writeTimeout, TimeUnit.MILLISECONDS);
        return this;
    }

    /**
     * 全局连接超时时间
     */
    public XTHttp setConnectTimeout(long connectTimeout) {
        okHttpClientBuilder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
        return this;
    }

    /**
     * 超时重试次数
     */
    public XTHttp setRetryCount(int retryCount) {
        if (retryCount < 0) {
            throw new IllegalArgumentException("retryCount must > 0");
        }
        mRetryCount = retryCount;
        return this;
    }

    /**
     * 超时重试次数
     */
    public static int getRetryCount() {
        return getInstance().mRetryCount;
    }

    /**
     * 超时重试延迟时间
     */
    public XTHttp setRetryDelay(int retryDelay) {
        if (retryDelay < 0) {
            throw new IllegalArgumentException("retryDelay must > 0");
        }
        mRetryDelay = retryDelay;
        return this;
    }

    /**
     * 超时重试延迟时间
     */
    public static int getRetryDelay() {
        return getInstance().mRetryDelay;
    }

    /**
     * 超时重试延迟叠加时间
     */
    public XTHttp setRetryIncreaseDelay(int retryIncreaseDelay) {
        if (retryIncreaseDelay < 0) {
            throw new IllegalArgumentException("retryIncreaseDelay must > 0");
        }
        mRetryIncreaseDelay = retryIncreaseDelay;
        return this;
    }

    /**
     * 超时重试延迟叠加时间
     */
    public static int getRetryIncreaseDelay() {
        return getInstance().mRetryIncreaseDelay;
    }

    /**
     * 全局的缓存模式
     */
    public XTHttp setCacheMode(CacheMode cacheMode) {
        mCacheMode = cacheMode;
        return this;
    }

    /**
     * 获取全局的缓存模式
     */
    public static CacheMode getCacheMode() {
        return getInstance().mCacheMode;
    }

    /**
     * 全局的缓存过期时间
     */
    public XTHttp setCacheTime(long cacheTime) {
        if (cacheTime <= -1) {
            cacheTime = DEFAULT_CACHE_NEVER_EXPIRE;
        }
        mCacheTime = cacheTime;
        return this;
    }

    /**
     * 获取全局的缓存过期时间
     */
    public static long getCacheTime() {
        return getInstance().mCacheTime;
    }

    /**
     * 全局的缓存大小,默认50M
     */
    public XTHttp setCacheMaxSize(long maxSize) {
        mCacheMaxSize = maxSize;
        return this;
    }

    /**
     * 获取全局的缓存大小
     */
    public static long getCacheMaxSize() {
        return getInstance().mCacheMaxSize;
    }

    /**
     * 全局设置缓存的版本，默认为1，缓存的版本号
     */
    public XTHttp setCacheVersion(int cacheersion) {
        if (cacheersion < 0) {
            throw new IllegalArgumentException("cacheersion must > 0");
        }
        rxCacheBuilder.version(cacheersion);
        return this;
    }

    /**
     * 全局设置缓存的路径，默认是应用包下面的缓存
     */
    public XTHttp setCacheDirectory(File directory) {
        mCacheDirectory = Utils.checkNotNull(directory, "directory == null");
        rxCacheBuilder.diskDir(directory);
        return this;
    }

    /**
     * 获取缓存的路劲
     */
    public static File getCacheDirectory() {
        return getInstance().mCacheDirectory;
    }

    /**
     * 全局设置缓存的转换器
     */
    public XTHttp setCacheDiskConverter(IDiskConverter converter) {
        rxCacheBuilder.diskConverter(Utils.checkNotNull(converter, "converter == null"));
        return this;
    }

    /**
     * 全局设置OkHttp的缓存,默认是3天
     */
    public XTHttp setHttpCache(Cache cache) {
        this.mCache = cache;
        return this;
    }

    /**
     * 获取OkHttp的缓存<br>
     */
    public static Cache getHttpCache() {
        return getInstance().mCache;
    }

    /**
     * 添加全局公共请求参数
     */
    public XTHttp addCommonParams(HttpParams commonParams) {
        if (mCommonParams == null) {
            mCommonParams = new HttpParams();
        }
        mCommonParams.put(commonParams);
        return this;
    }

    /**
     * 获取全局公共请求参数
     */
    public HttpParams getCommonParams() {
        return mCommonParams;
    }

    /**
     * 获取全局公共请求头
     */
    public HttpHeaders getCommonHeaders() {
        return mCommonHeaders;
    }

    /**
     * 添加全局公共请求参数
     */
    public XTHttp addCommonHeaders(HttpHeaders commonHeaders) {
        if (mCommonHeaders == null) {
            mCommonHeaders = new HttpHeaders();
        }
        mCommonHeaders.put(commonHeaders);
        return this;
    }

    /**
     * 添加全局拦截器
     */
    public XTHttp addInterceptor(Interceptor interceptor) {
        okHttpClientBuilder.addInterceptor(Utils.checkNotNull(interceptor,
                "interceptor == null"));
        return this;
    }

    /**
     * 添加全局网络拦截器
     */
    public XTHttp addNetworkInterceptor(Interceptor interceptor) {
        okHttpClientBuilder.addNetworkInterceptor(Utils.checkNotNull(interceptor,
                "interceptor == null"));
        return this;
    }

    /**
     * 全局设置代理
     */
    public XTHttp setOkproxy(Proxy proxy) {
        okHttpClientBuilder.proxy(Utils.checkNotNull(proxy, "proxy == null"));
        return this;
    }

    /**
     * 全局设置请求的连接池
     */
    public XTHttp setOkconnectionPool(ConnectionPool connectionPool) {
        okHttpClientBuilder.connectionPool(Utils.checkNotNull(connectionPool,
                "connectionPool == null"));
        return this;
    }

    /**
     * 全局为Retrofit设置自定义的OkHttpClient
     */
    public XTHttp setOkclient(OkHttpClient client) {
        retrofitBuilder.client(Utils.checkNotNull(client, "client == null"));
        return this;
    }

    /**
     * 全局设置Converter.Factory,默认GsonConverterFactory.create()
     */
    public XTHttp addConverterFactory(Converter.Factory factory) {
        retrofitBuilder.addConverterFactory(Utils.checkNotNull(factory, "factory == null"));
        return this;
    }

    /**
     * 全局设置CallAdapter.Factory,默认RxJavaCallAdapterFactory.create()
     */
    public XTHttp addCallAdapterFactory(CallAdapter.Factory factory) {
        retrofitBuilder.addCallAdapterFactory(Utils.checkNotNull(factory, "factory == null"));
        return this;
    }

    /**
     * 全局设置Retrofit callbackExecutor
     */
    public XTHttp setCallbackExecutor(Executor executor) {
        retrofitBuilder.callbackExecutor(Utils.checkNotNull(executor, "executor == null"));
        return this;
    }

    /**
     * 全局设置Retrofit对象Factory
     */
    public XTHttp setCallFactory(okhttp3.Call.Factory factory) {
        retrofitBuilder.callFactory(Utils.checkNotNull(factory, "factory == null"));
        return this;
    }

    /**
     * 全局设置baseurl
     */
    public XTHttp setBaseUrl(String baseUrl) {
        mBaseUrl = Utils.checkNotNull(baseUrl, "baseUrl == null");
        return this;
    }

    /**
     * 获取全局baseurl
     */
    public static String getBaseUrl() {
        return getInstance().mBaseUrl;
    }

    /**
     * get请求
     */
    public static GetRequest get(String url) {
        return new GetRequest(url);
    }

    /**
     * post请求
     */
    public static PostRequest post(String url) {
        return new PostRequest(url);
    }


    /**
     * delete请求
     */
    public static DeleteRequest delete(String url) {
        return new DeleteRequest(url);
    }

    /**
     * 自定义请求
     */
    public static CustomRequest custom() {
        return new CustomRequest();
    }

    public static DownloadRequest downLoad(String url) {
        return new DownloadRequest(url);
    }

    public static PutRequest put(String url) {
        return new PutRequest(url);
    }

    /**
     * 取消订阅
     */
    public static void cancelSubscription(Disposable disposable) {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    /**
     * 清空缓存
     */
    public static void clearCache() {
        getRxCache().clear().compose(RxUtil.<Boolean>io_main())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(@NonNull Boolean aBoolean) throws Exception {
                        HttpLog.i("clearCache success!!!");
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        HttpLog.i("clearCache err!!!");
                    }
                });
    }

    /**
     * 移除缓存（key）
     */
    public static void removeCache(String key) {
        getRxCache().remove(key)
                .compose(RxUtil.<Boolean>io_main())
                .subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(@NonNull Boolean aBoolean) throws Exception {
                HttpLog.i("removeCache success!!!");
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(@NonNull Throwable throwable) throws Exception {
                HttpLog.i("removeCache err!!!");
            }
        });
    }
}
