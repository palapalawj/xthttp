package com.xt.basecommon.http.cache.core;

import com.jakewharton.disklrucache.DiskLruCache;
import com.xt.basecommon.http.utils.Utils;
import com.xt.basecommon.http.cache.converter.IDiskConverter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

/**
 * Describe: 磁盘缓存
 * Created by lijin on 2017/9/24.
 */

public class LruDiskCache extends BaseCache {

    private IDiskConverter diskConverter;
    private DiskLruCache diskLruCache;

    public LruDiskCache(IDiskConverter diskConverter, File diskDir, int version, long diskMaxSize) {
        this.diskConverter = Utils.checkNotNull(diskConverter, "null==diskConverter");
        try {
            diskLruCache = DiskLruCache.open(diskDir, version, 1, diskMaxSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected <T> T doLoad(Type type, String key) {
        if (null == diskLruCache) {
            return null;
        }
        try {
            DiskLruCache.Editor edit = diskLruCache.edit(key);
            if (null == edit) {
                return null;
            }
            InputStream source = edit.newInputStream(0);
            T value;
            if (null != source) {
                value = diskConverter.load(source, type);
                Utils.close(source);
                edit.commit();
                return value;
            }
            edit.abort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected <T> boolean doSave(String key, T value) {
        if (null == diskLruCache) {
            return false;
        }
        try {
            DiskLruCache.Editor edit = diskLruCache.edit(key);
            if (null == edit) {
                return false;
            }
            OutputStream sink = edit.newOutputStream(0);
            if (null != sink) {
                boolean result = diskConverter.writer(sink, value);
                Utils.close(sink);
                edit.commit();
                return result;
            }
            edit.abort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected boolean doContainsKey(String key) {
        if (null == diskLruCache) {
            return false;
        }
        try {
            return diskLruCache.get(key) != null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected boolean doDelete(String key) {
        if (null == diskLruCache) {
            return false;
        }
        try {
            return diskLruCache.remove(key);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected boolean doClear() {
        boolean status = false;
        try {
            diskLruCache.delete();
            status = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return status;
    }

    @Override
    protected boolean isExpiry(String key, long existTime) {
        if (null == diskLruCache) {
            return false;
        }
        if (existTime > -1) {
            File file = new File(diskLruCache.getDirectory(), key + "." + 0);
            if (isCacheDataFailure(file, existTime))
                return true;
        }
        return false;
    }

    private boolean isCacheDataFailure(File dataFile, long time) {
        if (!dataFile.exists()) {
            return false;
        }
        long existTime = System.currentTimeMillis() - dataFile.lastModified();
        return existTime > (time * 1000);
    }
}
