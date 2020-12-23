package com.alibaba.csp.sentinel.transport.model;

/**
 * Rpc 消息结构体
 * @author Cheney
 * @date 2020/12/16
 */
public class TransportMessage {

    /** rpc message type */
    private byte messageType;
    /** serialization type */
    private byte codec;
    /** compress type */
    private byte compress;
    /** request id */
    private int requestId;
    /** request data */
    private Object data;

    public byte getMessageType() {
        return messageType;
    }

    public void setMessageType(byte messageType) {
        this.messageType = messageType;
    }

    public byte getCodec() {
        return codec;
    }

    public void setCodec(byte codec) {
        this.codec = codec;
    }

    public byte getCompress() {
        return compress;
    }

    public void setCompress(byte compress) {
        this.compress = compress;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
