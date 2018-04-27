package com.xt.basecommon.http.cache;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.xt.basecommon.http.utils.Utils;
import com.xt.basecommon.http.cache.converter.IDiskConverter;
import com.xt.basecommon.http.cache.converter.SerializableDiskConverter;
import com.xt.basecommon.http.cache.core.CoreCache;
import com.xt.basecommon.http.cache.core.LruDiskCache;
import com.xt.basecommon.http.cache.model.CacheMode;
import com.xt.basecommon.http.cache.model.CacheResult;
import com.xt.basecommon.http.cache.stategy.IStrategy;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.annotations.NonNull;
import io.reactivex.exceptions.Exceptions;

/**
 * Describe: 缓存入口，使用rxjava+DiskLruCache
 * 可独立使用，单独用rxcache来存储数据；
 * 采用transformer与网络请求结合，实现网络缓存，本地硬缓存
 * 异步保存缓存
 * 异步读取缓存
 * 判断缓存是否存在
 * 根据key删除缓存
 * 清除缓存
 * 缓存key进行md5加密
 * 使用说明：
 * RxCache rxCache = new RxCache（）.Builder(this)
 * .version(1) //默认为1
 * .diskDir(new File(getCacheDIr().getPath() + File,separator + "data-cache")) //路径
 * .diskConverter(new SerializableDiskConverter()) //serializable缓存
 * .diskMax(20 * 1024 * 1024) //默认50M
 * .build()
 * Created by lijin on 2017/9/22.
 */

public final class RxCache {

    private final Context context;
    private final CoreCache coreCache;
    private final String cacheKey;
    private final long cacheTime;
    private final IDiskConverter diskConverter;
    private final File diskDir;
    private final int version;
    private final long diskMaxSize;

    public RxCache() {
        this(new Builder());
    }

    private RxCache(Builder builder) {
        this.context = builder.context;
        this.cacheKey = builder.cachekey;
        this.cacheTime = builder.cacheTime;
        this.diskDir = builder.diskDir;
        this.version = builder.version;
        this.diskMaxSize = builder.diskMaxSize;
        this.diskConverter = builder.diskConverter;
        coreCache = new CoreCache(new LruDiskCache(diskConverter, diskDir, version, diskMaxSize));
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    /**
     * 缓存transformer
     *
     * @param cacheMode 缓存类型
     * @param type      缓存clazz
     */
    public <T> ObservableTransformer<T, CacheResult<T>> transformer(CacheMode cacheMode, final Type type) {
        final IStrategy strategy = loadStrategy(cacheMode);//获取缓存策略
        return new ObservableTransformer<T, CacheResult<T>>() {
            @Override
            public ObservableSource<CacheResult<T>> apply(Observable<T> upstream) {
                Log.i("xt,","cackeKey=" + RxCache.this.cacheKey);
                Type tempType = type;
                if (type instanceof ParameterizedType) {//自定义ApiResult
                    Class<T> cls = (Class) ((ParameterizedType) type).getRawType();
                    if (CacheResult.class.isAssignableFrom(cls)) {
                        tempType = Utils.getParameterizedType(type, 0);
                    }
                }
                return strategy.execute(RxCache.this, RxCache.this.cacheKey, RxCache.this.cacheTime, upstream, tempType);
            }
        };
    }

    private static abstract class SimpleSubscribe<T> implements ObservableOnSubscribe<T> {
        @Override
        public void subscribe(@NonNull ObservableEmitter<T> subscriber) throws Exception {
            try {
                T data = execute();
                if (!subscriber.isDisposed()) {
                    subscriber.onNext(data);
                }
            } catch (Throwable e) {
                Log.e("xt", e.getMessage());
                if (!subscriber.isDisposed()) {
                    subscriber.onError(e);
                }
                Exceptions.throwIfFatal(e);
                //RxJavaPlugins.onError(e);
                return;
            }

            if (!subscriber.isDisposed()) {
                subscriber.onComplete();
            }
        }

        abstract T execute() throws Throwable;
    }

    /**
     * 获取缓存
     * @param type 保存的类型
     * @param key 缓存key
     */
    public <T> Observable<T> load(final Type type, final String key) {
        return load(type, key, -1);
    }

    /**
     * 根据时间读取缓存
     *
     * @param type 保存的类型
     * @param key  缓存key
     * @param time 保存时间
     */
    public <T> Observable<T> load(final Type type, final String key, final long time) {
        return Observable.create(new SimpleSubscribe<T>() {
            @Override
            T execute() {
                return coreCache.load(type, key, time);
            }
        });
    }

    /**
     * 保存
     *
     * @param key   缓存key
     * @param value 缓存Value
     */
    public <T> Observable<Boolean> save(final String key, final T value) {
        return Observable.create(new SimpleSubscribe<Boolean>() {
            @Override
            Boolean execute() throws Throwable {
                coreCache.save(key, value);
                return true;
            }
        });
    }

    /**
     * 是否包含
     */
    public Observable<Boolean> containsKey(final String key) {
        return Observable.create(new SimpleSubscribe<Boolean>() {
            @Override
            Boolean execute() throws Throwable {
                return coreCache.containsKey(key);
            }
        });
    }

    /**
     * 删除缓存
     */
    public Observable<Boolean> remove(final String key) {
        return Observable.create(new SimpleSubscribe<Boolean>() {
            @Override
            Boolean execute() throws Throwable {
                return coreCache.remove(key);
            }
        });
    }

    /**
     * 清空缓存
     */
    public Observable<Boolean> clear() {
        return Observable.create(new SimpleSubscribe<Boolean>() {
            @Override
            Boolean execute() throws Throwable {
                return coreCache.clear();
            }
        });
    }

    /**
     * 利用反射，加载缓存策略模型
     */
    private IStrategy loadStrategy(CacheMode cacheMode) {
        try {
            String pkName = IStrategy.class.getPackage().getName();
            return (IStrategy) Class.forName(pkName + "." + cacheMode.getClassName()).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("loadStrategy(" + cacheMode + ") err!!" + e.getMessage());
        }
    }

    public long getCacheTime() {
        return cacheTime;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public Context getContext() {
        return context;
    }

    public CoreCache getCoreCache() {
        return coreCache;
    }

    public IDiskConverter getDiskConverter() {
        return diskConverter;
    }

    public File getDiskDir() {
        return diskDir;
    }

    public int getVersion() {
        return version;
    }

    public long getDiskMaxSize() {
        return diskMaxSize;
    }

    public static final class Builder {
        private static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024; // 5MB
        private static final int MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
        public static final long CACHE_NEVER_EXPIRE = -1;//永久不过期
        private int version;
        private long diskMaxSize;
        private File diskDir;
        private IDiskConverter diskConverter;
        private Context context;
        private String cachekey;
        private long cacheTime;

        public Builder() {
            diskConverter = new SerializableDiskConverter();
            cacheTime = CACHE_NEVER_EXPIRE;
            version = 1;
        }

        public Builder(RxCache rxCache) {
            this.context = rxCache.context;
            this.version = rxCache.version;
            this.diskMaxSize = rxCache.diskMaxSize;
            this.diskDir = rxCache.diskDir;
            this.diskConverter = rxCache.diskConverter;
            this.context = rxCache.context;
            this.cachekey = rxCache.cacheKey;
            this.cacheTime = rxCache.cacheTime;
        }

        public Builder init(Context context) {
            this.context = context;
            return this;
        }

        /**
         * 不设置，默认为1
         */
        public Builder version(int version) {
            this.version = version;
            return this;
        }

        /**
         * 默认为缓存路径
         *
         * @param directory
         * @return
         */
        public Builder diskDir(File directory) {
            this.diskDir = directory;
            return this;
        }


        public Builder diskConverter(IDiskConverter converter) {
            this.diskConverter = converter;
            return this;
        }

        /**
         * 不设置， 默为认50MB
         */
        public Builder diskMax(long maxSize) {
            this.diskMaxSize = maxSize;
            return this;
        }

        public Builder cachekey(String cachekey) {
            this.cachekey = cachekey;
            return this;
        }

        public Builder cacheTime(long cacheTime) {
            this.cacheTime = cacheTime;
            return this;
        }

        public RxCache build() {
            if (this.diskDir == null && this.context != null) {
                this.diskDir = getDiskCacheDir(this.context, "data-cache");
            }
            Utils.checkNotNull(this.diskDir, "diskDir==null");
            if (!this.diskDir.exists()) {
                this.diskDir.mkdirs();
            }
            if (this.diskConverter == null) {
                this.diskConverter = new SerializableDiskConverter();
            }
            if (diskMaxSize <= 0) {
                diskMaxSize = calculateDiskCacheSize(diskDir);
            }
            cacheTime = Math.max(CACHE_NEVER_EXPIRE, this.cacheTime);

            version = Math.max(1, this.version);

            return new RxCache(this);
        }

        private static long calculateDiskCacheSize(File dir) {
            long size = 0;
            try {
                StatFs statFs = new StatFs(dir.getAbsolutePath());
                long available = ((long) statFs.getBlockCount()) * statFs.getBlockSize();
                size = available / 50;
            } catch (IllegalArgumentException ignored) {
            }
            return Math.max(Math.min(size, MAX_DISK_CACHE_SIZE), MIN_DISK_CACHE_SIZE);
        }

        /**
         * 应用程序缓存原理：
         * 1.当SD卡存在或者SD卡不可被移除的时候，就调用getExternalCacheDir()方法来获取缓存路径，否则就调用getCacheDir()方法来获取缓存路径<br>
         * 2.前者是/sdcard/Android/data/<application package>/cache 这个路径<br>
         * 3.后者获取到的是 /data/data/<application package>/cache 这个路径<br>
         *
         * @param uniqueName 缓存目录
         */
        public File getDiskCacheDir(Context context, String uniqueName) {
            File cacheDir;
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                    || !Environment.isExternalStorageRemovable()) {
                cacheDir = context.getExternalCacheDir();
            } else {
                cacheDir = context.getCacheDir();
            }
            if (cacheDir == null) {// if cacheDir is null throws NullPointerException
                cacheDir = context.getCacheDir();
            }
            return new File(cacheDir.getPath() + File.separator + uniqueName);
        }

    }
}
