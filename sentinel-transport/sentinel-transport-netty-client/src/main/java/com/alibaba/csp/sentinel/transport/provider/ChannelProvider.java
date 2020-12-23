package com.alibaba.csp.sentinel.transport.provider;

import com.alibaba.csp.sentinel.transport.model.Node;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelProvider {

    private static final Logger logger = LoggerFactory.getLogger(ChannelProvider.class);

    private static final Map<Node, Channel> channelMap = new ConcurrentHashMap<>();

    public static Channel get(String ip, Integer port) {
        return get(new Node(ip, port));
    }

    public static Channel get(Node node) {
        // determine if there is a connection for the corresponding address
        if (channelMap.containsKey(node)) {
            Channel channel = channelMap.get(node);
            // if so, determine if the connection is available, and if so, get it directly
            if (channel != null && channel.isActive()) {
                return channel;
            } else {
                channelMap.remove(node);
            }
        }
        return null;
    }

    public static void set(Node node) {
        channelMap.put(node, node.getChannel());
    }

    public static void remove(Node node) {
        channelMap.remove(node);
        logger.info("Channel map size :[{}]", channelMap.size());
    }

    public static Map<Node, Channel> list() {
        return channelMap;
    }

}