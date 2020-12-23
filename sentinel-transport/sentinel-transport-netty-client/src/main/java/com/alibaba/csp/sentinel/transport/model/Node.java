package com.alibaba.csp.sentinel.transport.model;

import io.netty.channel.Channel;

import java.util.UUID;

/**
 * Channel 节点
 *
 * @author Cheney
 * @date 2020/12/22
 */
public class Node {

    private String id;
    private String ip;
    private Integer port;
    private Channel channel;

    public Node() {
        this.id = UUID.randomUUID().toString();
    }

    public Node(String ip, Integer port) {
        this.id = UUID.randomUUID().toString();
        this.ip = ip;
        this.port = port;
    }

    public Node(String ip, Integer port, Channel channel) {
        this.id = UUID.randomUUID().toString();
        this.ip = ip;
        this.port = port;
        this.channel = channel;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    public boolean equals(Object obj) {
        Node other;
        if (obj instanceof Node) {
            other = (Node) obj;
        } else {
            return false;
        }
        if (this.ip != null && this.port != null && this.ip.equals(other.ip) && this.port.equals(other.port)) {
            return true;
        }
        if (this.channel != null && this.channel == other.getChannel()) {
            return true;
        }
        if (this.id != null && this.id.equals(other.id)) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        // return Objects.hash(id, ip, port, channel);
        return 1;
    }
}
