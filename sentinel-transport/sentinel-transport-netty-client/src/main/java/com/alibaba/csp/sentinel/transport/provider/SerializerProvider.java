package com.alibaba.csp.sentinel.transport.provider;

import com.alibaba.csp.sentinel.transport.enums.SerializationTypeEnum;
import com.alibaba.csp.sentinel.transport.provider.serialize.Serializer;
import com.alibaba.csp.sentinel.transport.provider.serialize.kyro.KryoSerializer;
import com.alibaba.csp.sentinel.transport.provider.serialize.protostuff.ProtostuffSerializer;
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
public class SerializerProvider {

    private static final Logger logger = LoggerFactory.getLogger(SerializerProvider.class);

    private static Map<String, Serializer> serializerMap = new ConcurrentHashMap<>();

    private static Map<String, Class> serializerClassMap = new HashMap<>();

    public static final String DEFAULT_SERIALIZER = SerializationTypeEnum.KYRO.getName();

    static {
        serializerClassMap.put(SerializationTypeEnum.KYRO.getName(), KryoSerializer.class);
        serializerClassMap.put(SerializationTypeEnum.PROTOSTUFF.getName(), ProtostuffSerializer.class);
    }

    public static Serializer getSerializer() {
        return getSerializer(DEFAULT_SERIALIZER);
    }

    public static Serializer getSerializer(byte codecType) {
        String serializerName = SerializationTypeEnum.getName(codecType);
        if (serializerName == null) {
            logger.error("unknown serializer, use default: {}.", DEFAULT_SERIALIZER);
            serializerName = DEFAULT_SERIALIZER;
        }
        return getSerializer(serializerName);
    }

    public static Serializer getSerializer(String serializerName) {
        if (! serializerMap.containsKey(serializerName)) {
            initSerializer(serializerName);
        }
        Serializer serializer = serializerMap.get(serializerName);
        if (serializer == null) {
            logger.error("get serializer failed, use default: {}.", DEFAULT_SERIALIZER);
            return getSerializer(DEFAULT_SERIALIZER);
        }
        return serializer;
    }

    private static synchronized void initSerializer(String codeName) {
        Class serializerClass = serializerClassMap.get(codeName);
        if (serializerClass == null) {
            logger.error("unknown serializer: {}.", codeName);
        }
        try {
            serializerMap.put(codeName, (Serializer) serializerClass.newInstance());
        } catch (InstantiationException e) {
            logger.error("create {} instance error.", serializerClass.getSimpleName());
        } catch (IllegalAccessException e) {
            logger.error("create {} instance error.", serializerClass.getSimpleName());
        }
    }
}
