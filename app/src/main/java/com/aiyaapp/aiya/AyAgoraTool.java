// ----------哎吖科技添加----------
package com.aiyaapp.aiya;

import java.nio.ByteBuffer;

public class AyAgoraTool {
    static {
        System.loadLibrary("apm-video-process");
    }

    // 声网数据回调
    public static native void setAgoraDataCallback(AgoraDataCallback callback);

    public interface AgoraDataCallback {
        void onResult(final byte[] buffer, final int width, final int height);
    }
}

// ----------哎吖科技添加----------