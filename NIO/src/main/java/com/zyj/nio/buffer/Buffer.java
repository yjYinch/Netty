package com.zyj.nio.buffer;

import java.nio.IntBuffer;

/**
 * @author : zhang yijun
 * @date : 2021/2/7 9:26
 * @description : TODO
 */

public class Buffer {

    public static void main(String[] args) {
        // 创建一个buffer, 大小为5，可以存放5个int
        IntBuffer intBuffer = IntBuffer.allocate(5);

        // 向buffer中存数据
        for (int i = 0; i < intBuffer.capacity(); i++) {
            intBuffer.put(i);
        }

        // 从buffer中读数据
        // 将buffer转换，读写切换
        intBuffer.flip();

        // 现在buffer已经切换到读了
        while(intBuffer.hasRemaining()){
            // 维护一个索引，每get一次，指针就后移一下
            int i = intBuffer.get();
            System.out.println("获取数据："+ i);
        }
        intBuffer.flip();

        for (int i = 0; i < 4; i++) {
            intBuffer.put(10);
        }
        System.out.println();
        //intBuffer.clear();
        //intBuffer.reset();
    }
}
