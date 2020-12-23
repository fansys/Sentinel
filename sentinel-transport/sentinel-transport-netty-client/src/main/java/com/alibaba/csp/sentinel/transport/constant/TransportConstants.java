package com.alibaba.csp.sentinel.transport.constant;

import io.netty.util.AttributeKey;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Transport 常量
 *
 * @author Cheney
 * @date 2020/12/16
 */
public class TransportConstants {


    /**
     * Magic number. Verify RpcMessage
     */
    public static final byte[] MAGIC_NUMBER = {(byte) 'f', (byte) 'r', (byte) 'p', (byte) 'c'};
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * version information
     */
    public static final byte VERSION = 1;
    public static final byte TOTAL_LENGTH = 16;
    public static final byte REQUEST_TYPE = 1;
    public static final byte RESPONSE_TYPE = 2;

    /**
     * ping
     */
    public static final byte HEARTBEAT_REQUEST_TYPE = 3;

    /**
     * pong
     */
    public static final byte HEARTBEAT_RESPONSE_TYPE = 4;

    public static final int HEAD_LENGTH = 16;
    public static final String PING = "ping";
    public static final String PONG = "pong";
    public static final int MAX_FRAME_LENGTH = 8 * 1024 * 1024;

    /**
     * host对象
     */
    public static final AttributeKey TRANSPORT_CLIENT = AttributeKey.valueOf("transport_client");
    public static final AttributeKey TRANSPORT_SERVER = AttributeKey.valueOf("transport_server");

    /**
     * 默认端口
     */
    public static final int DEFAULT_PORT = 8619;

    public static final String HEART_BEAT_API = "/registry/machine";

}