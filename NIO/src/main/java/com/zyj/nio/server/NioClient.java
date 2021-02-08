package com.zyj.nio.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

/**
 * @author : zhang yijun
 * @date : 2021/2/8 11:53
 * @description : TODO
 */

public class NioClient {

    public static void main(String[] args) {
        try {
            // 得到一个网络通道
            SocketChannel socketChannel = SocketChannel.open();
            // 设置非阻塞
            socketChannel.configureBlocking(false);
            InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 6666);

            // 连接服务器，可以非阻塞
            if (!socketChannel.connect(inetSocketAddress)){
                while (!socketChannel.finishConnect()){
                    System.out.println("客户端连接需要时间，客户端不会阻塞");
                }
            }

            // 连接成功的话，
            String message = "hello, this is client!";
            ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
            // 将buffer数据写入到channel
            socketChannel.write(buffer);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
