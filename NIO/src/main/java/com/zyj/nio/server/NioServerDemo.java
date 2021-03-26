package com.zyj.nio.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : zhang yijun
 * @date : 2021/3/25 9:50
 * @description : TODO
 */

public class NioServerDemo {

    public static void main(String[] args) {
        List<SocketChannel> socketChannelList = new ArrayList<>();
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(8989));
            serverSocketChannel.configureBlocking(false);

            while (true) {
                // 这个时候accept设置为非阻塞
                SocketChannel socketChannel = serverSocketChannel.accept();
                if (socketChannel == null) {
                    // 读取数据
                    for (SocketChannel channel : socketChannelList) {
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int readLength = channel.read(buffer);
                        if (readLength > 0){
                            System.out.println("读取数据，" + new String(buffer.array()));
                        }
                    }
                } else {
                    // 获取到了客户端连接
                    System.out.println("新的客户端连接："+ socketChannel);
                    socketChannelList.add(socketChannel);

                    socketChannel.configureBlocking(false);
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    int readLength = socketChannel.read(buffer);
                    if (readLength > 0){
                        System.out.println("读取数据，" + new String(buffer.array()));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
