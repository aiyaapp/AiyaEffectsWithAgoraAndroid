package com.aiyaapp;


import android.content.Context;


/**
 * 哎吖科技  初始工具类
 */
public class MRender {

    private static AiyaProcesser mAiyaProcess;

    public static void create(final Context context) {
        if (mAiyaProcess == null) {
            mAiyaProcess = new AiyaProcesser(context);

            //设置特效路径
            mAiyaProcess.setEffect("assets/sticker/bunny/meta.json");
        }
    }



    public static void destroy() {
        if (mAiyaProcess != null) {
            mAiyaProcess.destroy();
        }
    }


    /**
     * 获取到 声网的数据
     * @param img
     * @param w
     * @param h
     */
    public static void renderToI420Image(final byte[] img, final int w, final int h) {
        System.out.println("data :" + img.length + ";" + w + ";" + h);
        //345600;640;360

        synchronized (MRender.class) {
            try {
                mAiyaProcess.pushData(img, w, h);
                MRender.class.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}
