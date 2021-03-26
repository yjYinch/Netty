package com.zyj.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;

/**
 * @author : zhang yijun
 * @date : 2021/2/21 11:46
 * @description : TODO
 */

public class MyClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("客户端已连接..");
        ctx.writeAndFlush(Unpooled.copiedBuffer("msg", CharsetUtil.UTF_8));
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 读取服务器发送的消息
        ByteBuf byteBuf = (ByteBuf) msg;
        System.out.println("接收到服务器：" + ctx.channel().remoteAddress() + "的消息：" + byteBuf.toString(CharsetUtil.UTF_8));
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(cause.getMessage());
    }
}
