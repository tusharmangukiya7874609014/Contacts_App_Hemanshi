#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_contactshandlers_contactinfoall_helper_Constants_getKeyValue(JNIEnv *env, jclass clazz) {
    std::string key = "rr3f8ka9gc";
    return env->NewStringUTF(key.c_str());
}