package com.alibaba.csp.sentinel.transport.provider.compress;

/**
 * 消息压缩实现 SPI 接口
 * @author Cheney
 * @date 2020/12/16
 */
public interface Compress {

    /**
     * 压缩方法
     * @param bytes 待压缩字节数组
     * @return 压缩后的字节数组
     */
    byte[] compress(byte[] bytes);

    /**
     * 解压缩方法
     * @param bytes 待解压缩字节数组
     * @return 解压缩后的字节数组
     */
    byte[] decompress(byte[] bytes);
}