package com.xt.basecommon.http.cache.model;

/**
 * Describe: 缓存策略
 * Created by lijin on 2017/9/24.
 */

public enum  CacheMode {
    //不使用缓存，该模式下，cacheKey，cacheMaxAge参数无效
    NO_CACHE("NoStrategy"),
    //按照http默认缓存，即okhttp的cache缓存
    DEFAULT("NoStrategy"),
    //先网络，失败后请求缓存
    FIRSTREMOTE("FirstRemoteStrategy"),
    //先缓存，没有再请求网络
    FIRSTCACHE("FirstCacheStategy"),
    //仅网络，但数据会被缓存
    ONLYREMOTE("OnlyRemoteStrategy"),
    //仅缓存
    ONLYCACHE("OnlyCacheStrategy"),
    //先缓存，不管是否存在，仍请求网络，即调用两次
    CACHEANDREMOTE("CacheAndRemoteStrategy"),
    //先缓存，不管是否存在，仍请求网络，网络数据返回如与缓存一致就不再返回
    CACHEANDREMOTEDISTINGCT("CacheAndRemoteDistinctStrategy");

    private final String className;

    CacheMode(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
