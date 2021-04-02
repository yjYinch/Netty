package com.zyj.netty.handler;

import com.sun.org.apache.xpath.internal.operations.String;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.StandardCharsets;

/**
 * @author : zhang yijun
 * @date : 2021/3/30 10:02
 * @description : TODO
 */

public class MySimpleChannelHander extends SimpleChannelInboundHandler<String> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println("已收到消息=" + msg);
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("发送消息给客户端");
        ChannelFuture channelFuture = ctx.channel()
                .writeAndFlush(Unpooled.copiedBuffer("hello, i got it".getBytes(StandardCharsets.UTF_8)));
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()){
                    System.out.println("写入成功");
                } else {
                    System.err.println("写入失败");
                    future.cause().printStackTrace();
                }
            }
        });

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
