package com.xt.basecommon.http.cache.converter;

import android.util.Log;

import com.xt.basecommon.http.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

/**
 * Describe: Serializable序列化对象转换器
 * Created by lijin on 2017/9/23.
 */

public class SerializableDiskConverter implements IDiskConverter {

    @Override
    public <T> T load(InputStream source, Type type) {
        T value = null;
        ObjectInputStream oin = null;
        try {
            oin = new ObjectInputStream(source);
            value = (T) oin.readObject();
        } catch (IOException | ClassNotFoundException e) {
            Log.e("xt", e.getMessage());
        } finally {
            Utils.close(oin);
        }
        return value;
    }

    @Override
    public boolean writer(OutputStream sink, Object obj) {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(sink);
            oos.writeObject(obj);
            oos.flush();
            return true;
        } catch (IOException e) {
            Log.e("xt", e.getMessage());
        } finally {
            Utils.close(oos);
        }
        return false;
    }
}
