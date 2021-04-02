package com.zyj.netty.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : zhang yijun
 * @date : 2021/2/21 11:17
 * @description : TODO
 */

public class MyServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(MyServerHandler.class);

    private static final Map<String, Channel> CHANNEL_MAP = new ConcurrentHashMap<>();

    static {
        log.info("当前集合：{}", CHANNEL_MAP);
    }

    /**
     * 读取客户端发送的消息
     *
     * @param ctx 当前handler的上下文，用于建立handler与channelPipeline沟通的桥梁
     * @param msg 客户端消息
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 由于解码器已经处理，因此这里面的msg为String类型
        String message = (String) msg;

        log.info("已收到客户端消息：{}", message);
        // TODO 业务处理

    }


    /**
     * channelRead完成后，所做的事情
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {

        // 发送消息的第一种方式：写到channel中
        //ctx.channel().writeAndFlush(Unpooled.copiedBuffer("服务器收到了你的消息，并给你发送一个ok", CharsetUtil.UTF_8));

        // 发送消息的第二种方式：写到channel关联的ChannelHandlerContext中
        //ctx.writeAndFlush(Unpooled.copiedBuffer("服务器收到了你的消息，并给你发送一个ok", CharsetUtil.UTF_8));

        // 假如给所有客户端发送消息
        CHANNEL_MAP.forEach((k, channel) -> channel.writeAndFlush(Unpooled.copiedBuffer("ok",  CharsetUtil.UTF_8)));
    }

    /**
     * 当客户端连接时调用此方法
     * @param ctx   上下文
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        String channelAddress = channel.localAddress().toString();
        log.info("客户端{}已连接", channelAddress);

        // 获取channel的唯一标识
        String channelId = channel.id().asShortText();
        // 存入map集合
        CHANNEL_MAP.putIfAbsent(channelId, channel);
        log.info("channel map = {}", CHANNEL_MAP);
    }

    /**
     * 当客户端断开连接时调用此方法，同时删除存储在map中的key
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        String channelId = channel.id().asShortText();
        log.warn("当前channel已断开连接，id = {}, channel=({})", channelId, channel.toString());
        CHANNEL_MAP.remove(channelId);
        log.info("channel map = {}", CHANNEL_MAP);
    }

    /**
     * 当系统异常时调用此方法，并且关闭上下文
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("error, {}", new String(cause.toString().getBytes(StandardCharsets.UTF_8)));
        ctx.close();
    }
}
