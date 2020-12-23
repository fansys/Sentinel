package com.alibaba.csp.sentinel.transport.model;

import com.alibaba.csp.sentinel.command.CommandRequest;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息传输请求
 *
 * @author zhangcheng.a
 * @date 2020/12/18
 */
public class TransportRequest<T> implements Serializable {

    private String requestId;

    private final Map<String, String> metadata = new HashMap<>();
    private final Map<String, String> parameters = new HashMap<>();
    private byte[] body;

    public static TransportRequest build() {
        return new TransportRequest();
    }

    public TransportRequest requestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public TransportRequest data(CommandRequest request) {
        this.metadata.putAll(request.getMetadata());
        this.parameters.putAll(request.getParameters());
        this.body = request.getBody();
        return this;
    }

    public String requestId() {
        return requestId;
    }

    public CommandRequest data() {
        CommandRequest request = new CommandRequest();
        for (String key : metadata.keySet()) {
            request.addMetadata(key, metadata.get(key));
        }
        for (String key : parameters.keySet()) {
            request.addParam(key, parameters.get(key));
        }
        request.setBody(body);
        return request;
    }
}
