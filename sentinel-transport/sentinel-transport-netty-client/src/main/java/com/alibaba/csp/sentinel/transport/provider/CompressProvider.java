package com.alibaba.csp.sentinel.transport.provider;

import com.alibaba.csp.sentinel.transport.provider.compress.Compress;
import com.alibaba.csp.sentinel.transport.provider.compress.gzip.GzipCompress;
import com.alibaba.csp.sentinel.transport.enums.CompressTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Title
 *
 * @author zhangcheng.a
 * @date 2020/12/22
 */
public class CompressProvider {

    private static final Logger logger = LoggerFactory.getLogger(CompressProvider.class);

    private static Map<String, Compress> compressMap = new ConcurrentHashMap<>();

    private static Map<String, Class> compressClassMap = new HashMap<>();

    public static final String DEFAULT_COMPRESS = CompressTypeEnum.GZIP.getName();

    static {
        compressClassMap.put(CompressTypeEnum.GZIP.getName(), GzipCompress.class);
    }

    public static Compress getCompress() {
        return getCompress(DEFAULT_COMPRESS);
    }

    public static Compress getCompress(byte codecType) {
        String compressName = CompressTypeEnum.getName(codecType);
        if (compressName == null) {
            logger.error("unknown compress type, use default: {}.", DEFAULT_COMPRESS);
            compressName = DEFAULT_COMPRESS;
        }
        return getCompress(compressName);
    }

    public static Compress getCompress(String compressName) {
        if (!compressMap.containsKey(compressName)) {
            initCompress(compressName);
        }
        Compress compress = compressMap.get(compressName);
        if (compress == null) {
            logger.error("get compress failed, use default: {}.", DEFAULT_COMPRESS);
            return getCompress(DEFAULT_COMPRESS);
        }
        return compress;
    }

    private static synchronized void initCompress(String codeName) {
        Class compressClass = compressClassMap.get(codeName);
        if (compressClass == null) {
            logger.error("unknown compress: {}.", codeName);
        }
        try {
            compressMap.put(codeName, (Compress) compressClass.newInstance());
        } catch (InstantiationException e) {
            logger.error("create {} instance error.", compressClass.getSimpleName());
        } catch (IllegalAccessException e) {
            logger.error("create {} instance error.", compressClass.getSimpleName());
        }
    }
}