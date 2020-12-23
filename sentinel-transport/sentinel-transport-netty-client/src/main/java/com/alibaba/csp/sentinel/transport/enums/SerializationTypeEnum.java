package com.alibaba.csp.sentinel.transport.enums;

/**
 * 序列化方式枚举
 *
 * @author Cheney
 * @date 2020/12/16
 */
public enum SerializationTypeEnum {

    /**
     * Kyro 序列化
     */
    KYRO((byte) 0x01, "kyro"),

    /**
     * Protostuff 序列化
     */
    PROTOSTUFF((byte) 0x02, "protostuff");;

    private final byte code;

    private final String name;

    public static final SerializationTypeEnum DEFAULT = PROTOSTUFF;

    SerializationTypeEnum(byte code, String name) {
        this.code = code;
        this.name = name;
    }

    public byte getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static String getName(byte code) {
        for (SerializationTypeEnum c : SerializationTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }

}
