package com.zyj.nio.channel;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author : zhang yijun
 * @date : 2021/2/7 13:52
 * @description : 从文件里面读数据并显示在控制台
 */

public class NioFileChannelImpl2 {

    public static void main(String[] args) {

        FileInputStream fileInputStream = null;
        try {
            // 读取文件
            fileInputStream = new FileInputStream("E:\\Netty\\test.txt");
            // 获取channel
            FileChannel fileChannel = fileInputStream.getChannel();

            // 初始化ByteBuffer
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            // 从通道读取数据并将其将读到byteBuffer中
            fileChannel.read(byteBuffer);

            // 将byteBuffer转为String, byteBuffer.array() 获取字节数组hb
            String s = new String(byteBuffer.array());
            System.out.println(s);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null){
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
