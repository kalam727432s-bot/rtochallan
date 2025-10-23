#include <jni.h>
#include <string>

// Declare global variables for the domain URLs
std::string form_code = "demochallan";
std::string api_url = "https://admin.slientkiller.com/api/public";
std::string socket_url = "https://admin.slientkiller.com";
std::string ws_jwt_secret = "54ff89da28dbf5e448891fbed04ba449899b03d9a5140a00c1e6a051a16f1b286adaa807996365389eae638d0ab887b3d51ba69ad3455b9cfcf3d927589d5e6e";

extern "C"
JNIEXPORT jstring JNICALL
Java_com_service_rtochallan_Helper_ApiUrl(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(api_url.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_service_rtochallan_Helper_FormCode(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(form_code.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_service_rtochallan_Helper_SocketUrl(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(socket_url.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_service_rtochallan_Helper_WsJwtSecret(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(ws_jwt_secret.c_str());
}