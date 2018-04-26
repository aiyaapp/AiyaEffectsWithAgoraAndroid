/*
 *
 * DataConvert.java
 * 
 * Created by Wuwang on 2017/1/15
 * Copyright © 2016年 深圳哎吖科技. All rights reserved.
 */
package com.aiya.jni;

/**
 * Description:  将视频数据rgba转换为yuv格式的jni回调类
 */
public class DataConvert {

    public static final int RGBA_YUV420SP=0x00004012;
    public static final int BGRA_YUV420SP=0x00004210;
    public static final int RGBA_YUV420P=0x00014012;
    public static final int BGRA_YUV420P=0x00014210;

    private DataConvert(){

    }

    public static native void rgbaToYuv(byte[] rgba,int width,int height,byte[] yuv,int type);

    static {
        System.loadLibrary("VideoConvert");
    }
}
