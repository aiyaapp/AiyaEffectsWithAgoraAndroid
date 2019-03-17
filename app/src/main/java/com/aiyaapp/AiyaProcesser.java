package com.aiyaapp;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.aiya.jni.DataConvert;
import com.aiyaapp.aiya.AiyaBeauty;
import com.aiyaapp.aiya.AiyaTracker;
import com.aiyaapp.aiya.filter.AyBeautyFilter;
import com.aiyaapp.aiya.filter.AyBigEyeFilter;
import com.aiyaapp.aiya.filter.AyThinFaceFilter;
import com.aiyaapp.aiya.filter.AyTrackFilter;
import com.aiyaapp.aiya.render.AiyaGiftFilter;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

/**
 * 哎吖科技 特效的具体实现类
 * AiyaGiftFilter ：礼物贴图特效的实现类，使用时要注意设置路径资源，项目中一般放在assets目录中的sticker文件夹下
 * AiyaGiftFilter :美颜实现类，共有AiyaBeauty.TYPE1到AiyaBeauty.TYPE6六中美颜，每种美颜可以设置美颜强度setDegree()取值0~1）
 * 或者单独设美白，磨皮，红润等强度（取值0~1）
 * GLEnvironment ：EGL环境的创建类
 * mShowFilter ：将纹理数据展示类
 *

 */

public class AiyaProcesser implements GLSurfaceView.Renderer {

    private final YuvFilter mYuvFilter;
    private GroupFilter mGroupFilter;
    private NoFilter mShowFilter;
    private GLEnvironment mGLEnvironment;

    private AyTrackFilter mTrackFilter;
    private AyBeautyFilter mAiyaBeautyFilter;
    private AyBigEyeFilter mAiyaBigEeyeFilter;
    private AyThinFaceFilter mAiyaThinFaceFilter;
    private AiyaGiftFilter mGiftFilter;

    public AiyaProcesser(Context context) {

        // 人脸跟踪
        mTrackFilter = new AyTrackFilter(context);

        // 美颜
        mAiyaBeautyFilter = new AyBeautyFilter(AiyaBeauty.TYPE1);

        // 大眼
        mAiyaBigEeyeFilter = new AyBigEyeFilter();

        // 瘦脸
        mAiyaThinFaceFilter = new AyThinFaceFilter();

        // 特效
        mGiftFilter = new AiyaGiftFilter(context, null);

        // yuv 转换成 rgb, 是数据输入端
        mYuvFilter = new YuvFilter(context.getResources());
        MatrixUtils.rotate(mYuvFilter.getMatrix(), 270);
        MatrixUtils.flip(mYuvFilter.getMatrix(), false, true);
        mGroupFilter = new GroupFilter(context.getResources());
        mGroupFilter.addFilter(mYuvFilter);

        // 数据输出端
        mShowFilter = new NoFilter(context.getResources());
        MatrixUtils.rotate(mShowFilter.getMatrix(), 270);

        // OpenGL 环境
        mGLEnvironment = new GLEnvironment(context);
        mGLEnvironment.setEGLContextClientVersion(2);
        mGLEnvironment.setEGLWindowSurfaceFactory(new GLEnvironment.EGLWindowSurfaceFactory() {
            @Override
            public EGLSurface createSurface(EGL10 egl, EGLDisplay display, EGLConfig
                    config, Object window) {
                return egl.eglCreatePbufferSurface(display, config, new int[]{
                        EGL10.EGL_WIDTH, 720,
                        EGL10.EGL_HEIGHT, 1280,
                        EGL10.EGL_NONE});
            }

            @Override
            public void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface) {
                egl.eglDestroySurface(display, surface);
            }
        });
        mGLEnvironment.setRenderer(this);
        mGLEnvironment.setRenderMode(GLEnvironment.RENDERMODE_WHEN_DIRTY);
        mGLEnvironment.setPreserveEGLContextOnPause(true);
        mGLEnvironment.surfaceCreated(null);
        mGLEnvironment.surfaceChanged(null, 0, 720, 1280);
        mGLEnvironment.onResume();
        mGLEnvironment.onAttachedToWindow();
    }

    private int dataWidth, dataHeight;
    private boolean dataIn = false;
    private byte[] data;

    public void pushData(final byte[] img, final int w, final int h) {
        Log.e("AiyaProcesser", "data size:" + w + "/" + h);
        this.dataWidth = h;
        this.dataHeight = w;
        dataIn = true;
        data = img;
        mGLEnvironment.requestRender();
    }

    public void setEffect(String path) {
        mGiftFilter.setEffect(path);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mTrackFilter.create();
        mAiyaBeautyFilter.create();
        mAiyaBigEeyeFilter.create();
        mAiyaThinFaceFilter.create();
        mGiftFilter.create();
        mGroupFilter.create();
        mShowFilter.create();

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

    }

    private int filterWidth, filterHeight;
    private ByteBuffer mOutPutData;
    private byte[] mOutPutByte;


    /**
     * 通话EGL绘制效果
     * @param gl
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (MRender.class) {
            if (dataIn) {
                if (filterHeight != dataHeight || filterWidth != dataWidth) {
                    filterHeight = dataHeight;
                    filterWidth = dataWidth;

                    mTrackFilter.sizeChanged(dataWidth, dataHeight);

                    mAiyaBeautyFilter.sizeChanged(dataWidth, dataHeight);
                    mAiyaBeautyFilter.setDegree(1.0f);

                    mAiyaBigEeyeFilter.sizeChanged(dataWidth, dataHeight);
                    mAiyaBigEeyeFilter.setDegree(0.5f);

                    mAiyaThinFaceFilter.sizeChanged(dataWidth, dataHeight);
                    mAiyaThinFaceFilter.setDegree(0.5f);

                    mGiftFilter.sizeChanged(dataWidth, dataHeight);

                    mGroupFilter.setSize(dataWidth, dataHeight);
                    mShowFilter.setSize(dataHeight, dataWidth);
                    if (mOutPutData != null) {
                        mOutPutData.clear();
                    }
                    mOutPutData = ByteBuffer.allocateDirect(dataWidth * dataHeight * 4);
                    mOutPutData.position(0);
                    mOutPutByte = new byte[dataWidth * dataHeight * 4];
                }

                GLES20.glViewport(0, 0, filterWidth, filterHeight);
                mYuvFilter.updateFrame(dataHeight, dataWidth, data);
                mGroupFilter.draw();

                mTrackFilter.drawToTexture(mGroupFilter.getOutputTexture());

                int texture = mAiyaBeautyFilter.drawToTexture(mGroupFilter.getOutputTexture());

                mGiftFilter.setFaceDataID(mTrackFilter.getFaceDataID());
                texture = mGiftFilter.drawToTexture(texture);

                mAiyaBigEeyeFilter.setFaceDataID(mTrackFilter.getFaceDataID());
                texture  = mAiyaBigEeyeFilter.drawToTexture(texture);

                mAiyaThinFaceFilter.setFaceDataID(mTrackFilter.getFaceDataID());
                texture = mAiyaThinFaceFilter.drawToTexture(texture);

                GLES20.glViewport(0, 0, filterHeight, filterWidth);
                mShowFilter.setTextureId(texture);
                mShowFilter.draw();
                mOutPutData.position(0);
                GLES20.glReadPixels(0, 0, filterHeight, filterWidth, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mOutPutData);
                mOutPutData.position(0);
                mOutPutData.get(mOutPutByte);

                //将yuv数据导出
                DataConvert.rgbaToYuv(mOutPutByte, dataHeight, dataWidth, data, DataConvert.RGBA_YUV420P);
                dataIn = false;
                MRender.class.notifyAll();
            }
            Log.d("AiyaProcesser", "onDrawFrame");
        }
    }


    /**
     * 销毁特效和美颜
     */
    public void destroy() {
        mGiftFilter.destroy();
        mAiyaBeautyFilter.destroy();
        mAiyaBigEeyeFilter.destroy();
        mAiyaThinFaceFilter.destroy();
    }
}
