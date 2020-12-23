package com.alibaba.csp.sentinel.transport.enums;

/**
 * 压缩类型枚举
 *
 * @author Cheney
 * @date 2020/12/16
 */
public enum CompressTypeEnum {

    /**
     * gzip 压缩
     */
    GZIP((byte) 0x01, "gzip");

    public static final CompressTypeEnum DEFAULT = GZIP;

    /**
     * 压缩类型代码
     */
    private final byte code;

    /**
     * 压缩类型名称
     */
    private final String name;

    CompressTypeEnum(byte code, String name) {
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
        for (CompressTypeEnum c : CompressTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }

}