package com.zyj.nio.channel;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author : zhang yijun
 * @date : 2021/2/7 14:07
 * @description : TODO
 */

public class NioFileChannelImpl3 {

    public static void main(String[] args) {
        FileInputStream fileInputStream = null;

        try {
            // 读取一个文件
            fileInputStream = new FileInputStream("E:\\Netty\\test.txt");
            FileChannel channel = fileInputStream.getChannel();

            FileOutputStream fileOutputStream = new FileOutputStream("E:\\Netty\\test1.txt");
            FileChannel channel2 = fileOutputStream.getChannel();

            ByteBuffer byteBuffer = ByteBuffer.allocate(5);
            while(true){

                byteBuffer.clear();
                int read = channel.read(byteBuffer);
                if (read == -1){
                    break;
                }
                // 读转换为写
                byteBuffer.flip();
                channel2.write(byteBuffer);
            }
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
