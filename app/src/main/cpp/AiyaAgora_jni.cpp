//----------哎吖科技添加 start----------
#include <jni.h>
#include <string>
#include <android/log.h>
#include <sstream>

#include "IAgoraRtcEngine.h"
#include "IAgoraMediaEngine.h"

JavaVM *agoraJVM = NULL;
jobject agoraDataCallback = NULL;

class AgoraVideoFrameObserver : public agora::media::IVideoFrameObserver {
public:
    virtual bool onCaptureVideoFrame(VideoFrame& videoFrame) override {

        JNIEnv *env;

        agoraJVM->AttachCurrentThread(&env, NULL);

        if (agoraDataCallback == NULL) {
            agoraJVM->DetachCurrentThread();
            return true;
        }

        jclass clazz = env->GetObjectClass(agoraDataCallback);
        if (clazz == NULL) {
            agoraJVM->DetachCurrentThread();
            return true;
        }

        jmethodID methodID = env->GetMethodID(clazz, "onResult", "([BII)V");
        if (methodID == NULL) {
            agoraJVM->DetachCurrentThread();
            return true;
        }

        // 组装数据
        jsize len = videoFrame.yStride * videoFrame.height * 3 / 2;
        jbyteArray array = env->NewByteArray(len);
        jbyte* buf = new jbyte[len];
        int yLength = videoFrame.yStride * videoFrame.height;
        memcpy(buf, videoFrame.yBuffer, static_cast<size_t>(yLength));
        int uLength = yLength / 4;
        memcpy(buf + yLength, videoFrame.uBuffer, static_cast<size_t>(uLength));
        memcpy(buf + yLength + uLength, videoFrame.vBuffer, static_cast<size_t>(uLength));
        env->SetByteArrayRegion(array, 0, len, buf);

        //调用该java方法
        env->CallVoidMethod(agoraDataCallback, methodID, array, videoFrame.yStride, videoFrame.height);

        // 数据回写
        env->GetByteArrayRegion(array, 0, len, buf);
        memcpy(videoFrame.yBuffer, buf, static_cast<size_t>(yLength));
        memcpy(videoFrame.uBuffer, buf + yLength, static_cast<size_t>(uLength));
        memcpy(videoFrame.vBuffer, buf + yLength + uLength, static_cast<size_t>(uLength));

        // 释放资源
        delete []buf;
        env->DeleteLocalRef(array);
        agoraJVM->DetachCurrentThread();

        return true;
    }

    virtual bool onRenderVideoFrame(unsigned int uid, VideoFrame& videoFrame) override {
        return true;
    }
};

static AgoraVideoFrameObserver agoraVideoFrameObserver;

extern "C"
JNIEXPORT int JNICALL loadAgoraRtcEnginePlugin(agora::rtc::IRtcEngine* engine) {
    if (engine == NULL){
        return 0;
    }
    agora::util::AutoPtr<agora::media::IMediaEngine> mediaEngine;
    mediaEngine.queryInterface(engine, agora::AGORA_IID_MEDIA_ENGINE);
    if (mediaEngine) {
        mediaEngine->registerVideoFrameObserver(&agoraVideoFrameObserver);
    }

    return 0;
}

extern "C"
JNIEXPORT void JNICALL unloadAgoraRtcEnginePlugin(agora::rtc::IRtcEngine* engine) {
    agora::util::AutoPtr<agora::media::IMediaEngine> mediaEngine;
    mediaEngine.queryInterface(engine, agora::AGORA_IID_MEDIA_ENGINE);
    if (mediaEngine) {
        mediaEngine->registerVideoFrameObserver(NULL);
    }

    if (agoraDataCallback) {
        JNIEnv *env;
        agoraJVM->AttachCurrentThread(&env, NULL);
        env->DeleteGlobalRef(agoraDataCallback);
        agoraDataCallback = NULL;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aiyaapp_aiya_AyAgoraTool_setAgoraDataCallback(JNIEnv *env, jclass type, jobject callback) {

    env->GetJavaVM(&agoraJVM);

    agoraDataCallback = env->NewGlobalRef(callback);
}
//----------哎吖科技添加 end----------
