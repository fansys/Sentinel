package com.alibaba.csp.sentinel.transport.codec;

import com.alibaba.csp.sentinel.transport.provider.CompressProvider;
import com.alibaba.csp.sentinel.transport.provider.SerializerProvider;
import com.alibaba.csp.sentinel.transport.provider.compress.Compress;
import com.alibaba.csp.sentinel.transport.provider.compress.gzip.GzipCompress;
import com.alibaba.csp.sentinel.transport.constant.TransportConstants;
import com.alibaba.csp.sentinel.transport.model.TransportMessage;
import com.alibaba.csp.sentinel.transport.enums.CompressTypeEnum;
import com.alibaba.csp.sentinel.transport.enums.SerializationTypeEnum;
import com.alibaba.csp.sentinel.transport.provider.serialize.Serializer;
import com.alibaba.csp.sentinel.transport.provider.serialize.kyro.KryoSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * custom protocol decoder
 * <p>
 * <pre>
 *   0     1     2     3     4        5     6     7     8         9          10      11     12  13  14   15 16
 *   +-----+-----+-----+-----+--------+----+----+----+------+-----------+-------+----- --+-----+-----+-------+
 *   |   magic   code        |version | full length         | messageType| codec|compress|    RequestId       |
 *   +-----------------------+--------+---------------------+-----------+-----------+-----------+------------+
 *   |                                                                                                       |
 *   |                                         body                                                          |
 *   |                                                                                                       |
 *   |                                        ... ...                                                        |
 *   +-------------------------------------------------------------------------------------------------------+
 * 4B  magic code（魔法数）   1B version（版本）   4B full length（消息长度）    1B messageType（消息类型）
 * 1B compress（压缩类型） 1B codec（序列化类型）    4B  requestId（请求的Id）
 * body（object类型数据）
 * </pre>
 *
 * @author WangTao
 * @createTime on 2020/10/2
 * @see <a href="https://zhuanlan.zhihu.com/p/95621344">LengthFieldBasedFrameDecoder解码器</a>
 */
public class TransportMessageEncoder extends MessageToByteEncoder<TransportMessage> {

    private static final Logger logger = LoggerFactory.getLogger(TransportMessageEncoder.class);

    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    @Override
    protected void encode(ChannelHandlerContext ctx, TransportMessage message, ByteBuf out) {
        try {
            out.writeBytes(TransportConstants.MAGIC_NUMBER);
            out.writeByte(TransportConstants.VERSION);
            // leave a place to write the value of full length
            out.writerIndex(out.writerIndex() + 4);
            byte messageType = message.getMessageType();
            out.writeByte(messageType);
            out.writeByte(message.getCodec());
            out.writeByte(CompressTypeEnum.GZIP.getCode());
            out.writeInt(ATOMIC_INTEGER.getAndIncrement());
            // build full length
            byte[] bodyBytes = null;
            int fullLength = TransportConstants.HEAD_LENGTH;
            // if messageType is not heartbeat message,fullLength = head length + body length
            String codecName = SerializationTypeEnum.getName(message.getCodec());
            logger.debug("codec name: [{}] ", codecName);
            Serializer serializer = SerializerProvider.getSerializer(codecName);
            bodyBytes = serializer.serialize(message.getData());
            // compress the bytes
            String compressName = CompressTypeEnum.getName(message.getCompress());
            Compress compress = CompressProvider.getCompress(compressName);
            bodyBytes = compress.compress(bodyBytes);
            fullLength += bodyBytes.length;

            if (bodyBytes != null) {
                out.writeBytes(bodyBytes);
            }
            int writeIndex = out.writerIndex();
            out.writerIndex(writeIndex - fullLength + TransportConstants.MAGIC_NUMBER.length + 1);
            out.writeInt(fullLength);
            out.writerIndex(writeIndex);
        } catch (Exception e) {
            logger.error("Encode request error!", e);
        }

    }


}