package com.alibaba.csp.sentinel.transport.model;

import com.alibaba.csp.sentinel.command.CommandResponse;

import java.io.Serializable;

/**
 * 消息传输响应
 *
 * @author zhangcheng.a
 * @date 2020/12/18
 */
public class TransportResponse<T> implements Serializable {

    private String requestId;

    private int code;

    private boolean success;

    private Object result;

    public static TransportResponse build() {
        return new TransportResponse();
    }

    public TransportResponse requestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public TransportResponse code(int code) {
        this.code = code;
        return this;
    }

    public TransportResponse data(CommandResponse response) {
        this.success = response.isSuccess();
        this.result = response.getResult();
        return this;
    }

    public String requestId() {
        return requestId;
    }

    public int code() {
        return code;
    }

    public CommandResponse data() {
        if (success) {
            return CommandResponse.ofSuccess(result);
        } else {
            return CommandResponse.ofFailure(null, result);
        }
    }
}
