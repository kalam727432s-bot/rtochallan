#include <jni.h>
#include <string>

std::string form_code = "demo";
std::string domain_url = "https://slientkill3r.github.io/devchanger/";
std::string ws_jwt_secret = "54ff89da28dbf5e448891fbed04ba449899b03d9a5140a00c1e6a051a16f1b286adaa807996365389eae638d0ab887b3d51ba69ad3455b9cfcf3d927589d5e6e";

extern "C"
JNIEXPORT jstring JNICALL
Java_com_service_rtochallan_Helper_FormCode(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(form_code.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_service_rtochallan_Helper_DomainUrl(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(domain_url.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_service_rtochallan_Helper_WsJwtSecret(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(ws_jwt_secret.c_str());
}