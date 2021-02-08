package com.zyj.nio.channel;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author : zhang yijun
 * @date : 2021/2/7 11:43
 * @description : TODO
 */

public class NioFileChannelImpl {

    public static void main(String[] args) {

        String message = "Test file channel";
        FileOutputStream fileOutputStream = null;
        try {
            // 创建一个输出流，将message写入到本地路径位置文件
            fileOutputStream = new FileOutputStream("E:\\Netty\\test.txt");
            // 获取channel, 真实类型是FileChannelImpl
            FileChannel fileChannel = fileOutputStream.getChannel();
            // 创建一个缓冲区，ByteBuffer
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            // 把数据message 流入到buffer
            byteBuffer.put(message.getBytes());

            // 反转指针，从byteBuffer读取数据并写入到fileOutputStream
            byteBuffer.flip();

            // 将缓冲区的数据写入到channel中
            fileChannel.write(byteBuffer);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null){
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
