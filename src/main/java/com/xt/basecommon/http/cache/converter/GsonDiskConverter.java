package com.xt.basecommon.http.cache.converter;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.xt.basecommon.http.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ConcurrentModificationException;

/**
 * Describe: GSON数据转换器
 * Created by lijin on 2017/9/22.
 */

public class GsonDiskConverter implements IDiskConverter {

    private Gson gson = new Gson();

    public GsonDiskConverter() {
        this.gson = new Gson();
    }

    public GsonDiskConverter(Gson gson) {
        Utils.checkNotNull(gson, "gson==null");
        this.gson = gson;
    }

    @Override
    public <T> T load(InputStream source, Type type) {
        T value = null;
        try {
            TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
            JsonReader jsonReader = gson.newJsonReader(new InputStreamReader(source));
            value = (T) adapter.read(jsonReader);
        } catch (JsonIOException | IOException | ConcurrentModificationException |
                JsonSyntaxException e) {
            Log.e("xt", e.getMessage());
        } catch (Exception e) {
            Log.e("xt", e.getMessage());
        } finally {
            Utils.close(source);
        }
        return value;
    }

    @Override
    public boolean writer(OutputStream sink, Object obj) {
        try {
            String json = gson.toJson(obj);
            byte[] bytes = json.getBytes();
            sink.write(bytes, 0, bytes.length);
            sink.flush();
            return true;
        } catch (JsonIOException | IOException | ConcurrentModificationException |
                JsonSyntaxException e) {
            Log.e("xt", e.getMessage());
        } catch (Exception e) {
            Log.e("xt", e.getMessage());
        } finally {
            Utils.close(sink);
        }
        return false;
    }
}

