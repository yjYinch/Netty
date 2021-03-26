package com.zyj.netty.handler;

import com.sun.org.slf4j.internal.LoggerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.util.CharsetUtil;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author : zhang yijun
 * @date : 2021/2/21 11:17
 * @description : TODO
 */

public class MyServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = Logger.getLogger("handler");

    /**
     * 读取客户端发送的消息
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;

        // channel通道
        Channel channel = ctx.channel();

        // 底层是一个双向链表，涉及到出站和入站问题
        ChannelPipeline pipeline = channel.pipeline();

        System.out.println("客户端连接地址：" + channel.localAddress() + ", 收到的消息："
                + byteBuf.toString(CharsetUtil.UTF_8));
//        ctx.channel().eventLoop().execute(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(TimeUnit.SECONDS.toMillis(30));
//                    logger.info("当前线程：" + Thread.currentThread().getName() + "执行任务");
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        });

        ctx.channel().eventLoop().schedule(new Runnable() {
            @Override
            public void run() {
                logger.info("当前线程：" + Thread.currentThread().getName() + "执行定时任务");
            }
        }, 5, TimeUnit.SECONDS);

        logger.info("执行完毕");
    }


    /**
     * channelRead完成后，所做的事情
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().writeAndFlush(Unpooled.copiedBuffer("服务器收到了你的消息，并给你发送一个ok", CharsetUtil.UTF_8));
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
