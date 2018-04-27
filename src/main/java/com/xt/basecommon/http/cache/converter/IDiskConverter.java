package com.xt.basecommon.http.cache.converter;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

/**
 * Describe: 通用转换接口
 * Created by lijin on 2017/9/22.
 */

public interface IDiskConverter {
    /**
     * 读取
     * @param source 输入流
     * @param type 读取后要转换的数据类型
     * @param <T>
     * @return
     */
    <T> T load(InputStream source, Type type);

    /**
     * 写入
     * @param sink 输入流
     * @param data 保存的数据
     * @return
     */
    boolean writer(OutputStream sink, Object data);
}
