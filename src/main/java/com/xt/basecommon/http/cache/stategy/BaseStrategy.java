package com.xt.basecommon.http.cache.stategy;

import android.util.Log;

import com.xt.basecommon.http.cache.RxCache;
import com.xt.basecommon.http.cache.model.CacheResult;

import java.lang.reflect.Type;
import java.util.ConcurrentModificationException;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Describe: 缓存策略基类
 * Created by lijin on 2017/9/25.
 */

public abstract class BaseStrategy implements IStrategy {

    <T> Observable<CacheResult<T>> loadCache(final RxCache rxCache, Type type, final String key,
                                             final long time, final boolean needEmpty) {
        Observable<CacheResult<T>> observable = rxCache.<T>load(type, key, time).flatMap(
                new Function<T, ObservableSource<CacheResult<T>>>() {
                    @Override
                    public ObservableSource<CacheResult<T>> apply(@NonNull T t) throws Exception {
                        if (null == t) {
                            return Observable.error(new NullPointerException("Not find cache!"));
                        }
                        return Observable.just(new CacheResult<T>(true, t));
                    }
                }
        );
        if (needEmpty) {
            observable = observable.onErrorResumeNext(
                    new Function<Throwable, ObservableSource<? extends CacheResult<T>>>() {
                        @Override
                        public ObservableSource<? extends CacheResult<T>> apply(Throwable throwable)
                                throws Exception {
                            return Observable.empty();
                        }
                    }
            );
        }
        return observable;
    }

    //同步保存请求成功后数据
    <T> Observable<CacheResult<T>> loadRemote(final RxCache rxCache, final String key,
                                              Observable<T> source, final boolean needEmpty) {
        Observable<CacheResult<T>> observable = source.flatMap(
                new Function<T, ObservableSource<CacheResult<T>>>() {
                    @Override
                    public ObservableSource<CacheResult<T>> apply(final @NonNull T t) throws Exception {
                        return rxCache.save(key, t).map(new Function<Boolean, CacheResult<T>>() {
                            @Override
                            public CacheResult<T> apply(Boolean aBoolean) throws Exception {
                                Log.i("xt", "save status=" + aBoolean);
                                return new CacheResult<T>(false, t);
                            }
                        }).onErrorReturn(new Function<Throwable, CacheResult<T>>() {
                            @Override
                            public CacheResult<T> apply(@NonNull Throwable throwable) throws Exception {
                                Log.i("xt", "save status=" + throwable);
                                return new CacheResult<T>(false, t);
                            }
                        });
                    }
                }
        );
        if (needEmpty) {
            observable = observable.onErrorResumeNext(new Function<Throwable, ObservableSource<? extends CacheResult<T>>>() {
                @Override
                public ObservableSource<? extends CacheResult<T>> apply(Throwable throwable) throws Exception {
                    return Observable.empty();
                }
            });
        }
        return observable;
    }

    //异步保存请求成功后数据
    <T> Observable<CacheResult<T>> loadRemoteSync(final RxCache rxCache, final String key,
                                                  Observable<T> source, final boolean needEmpty) {
        Observable<CacheResult<T>> observable = source.map(new Function<T, CacheResult<T>>() {
            @Override
            public CacheResult<T> apply(T t) throws Exception {
                Log.i("xt", "loadRemoteSync result=" + t);
                rxCache.save(key, t).subscribeOn(Schedulers.io())
                        .subscribe(new Consumer<Boolean>() {
                            @Override
                            public void accept(@NonNull Boolean aBoolean) throws Exception {
                                Log.i("xt", "save status=" + aBoolean);
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                if (throwable instanceof ConcurrentModificationException) {
                                    Log.i("xt", "Save failed, please use a synchronized " +
                                            "cache strategy:" + throwable);
                                } else {
                                    Log.i("xt", throwable.getMessage());
                                }
                            }
                        });
                return new CacheResult<T>(false, t);
            }
        });
        if (needEmpty) {
            observable = observable.onErrorResumeNext(
                    new Function<Throwable, ObservableSource<? extends CacheResult<T>>>() {
                        @Override
                        public ObservableSource<? extends CacheResult<T>> apply(Throwable throwable) throws Exception {
                            return Observable.empty();
                        }
                    }
            );
        }
        return observable;
    }
}
