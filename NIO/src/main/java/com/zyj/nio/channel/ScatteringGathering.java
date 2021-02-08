package com.zyj.nio.channel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

/**
 * @author : zhang yijun
 * @date : 2021/2/7 16:04
 * @description : TODO
 */

public class ScatteringGathering {
    public static void main(String[] args) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        InetSocketAddress socketAddress = new InetSocketAddress(7000);

        // 绑定端口到socket并启动
        serverSocketChannel.socket().bind(socketAddress);

        // 创建buffer数组
        ByteBuffer[] byteBuffer = new ByteBuffer[2];
        byteBuffer[0] = ByteBuffer.allocate(5);
        byteBuffer[1] = ByteBuffer.allocate(3);

        // 等待客户端连接
        System.out.println("等待客户端连接....");
        SocketChannel socketChannel = serverSocketChannel.accept();
        System.out.println("客户端:{" + socketChannel.getLocalAddress() + "}" + "已连接...");

        // 假设从客户端接收8个字节
        int messageLength = 8;

        while (true) {
            int byteRead = 0;
            while (byteRead < messageLength) {
                long l = socketChannel.read(byteBuffer);
                byteRead += l;
                System.out.println("byteRead = " + byteRead);

                for (ByteBuffer buffer : byteBuffer) {
                    System.out.println("position =" + buffer.position() + ", limit = " + buffer.limit());
                }
            }

            // 将所有的buffer进行反转
            for (ByteBuffer buffer : byteBuffer) {
                buffer.flip();
            }
            // 将数据读出显示给客户端
            long byteWrite = 0;
            while (byteWrite < messageLength) {
                long l = socketChannel.write(byteBuffer);
                byteWrite += l;
            }
            // 将所有的buffer进行clear操作
            for (ByteBuffer buffer : byteBuffer) {
                buffer.clear();
            }

            System.out.println("byteRead = " + byteRead + ", byteWrite" + byteWrite + ", messageLength = " + messageLength);
        }

    }
}
