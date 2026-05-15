/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2026 Bram and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Native bridge that calls ClassLoader#defineClass through JNI. JNI method
 * invocation skips Java's module access checks, so the loader does not need
 * --add-opens java.base/java.lang=ALL-UNNAMED to inject the API classes into
 * the host plugin classloader.
 */

#include <jni.h>

JNIEXPORT jclass JNICALL
Java_com_deathmotion_totemguard_loader_classloader_NativeClassLoader_nativeDefineClass(
    JNIEnv *env, jclass self,
    jobject classLoader, jstring name, jbyteArray bytes
) {
    if (classLoader == NULL || bytes == NULL) {
        jclass npe = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (npe != NULL) {
            (*env)->ThrowNew(env, npe, "classLoader or bytes is null");
        }
        return NULL;
    }

    jclass classLoaderClass = (*env)->FindClass(env, "java/lang/ClassLoader");
    if (classLoaderClass == NULL) {
        return NULL;
    }

    jmethodID defineClassMethod = (*env)->GetMethodID(
        env, classLoaderClass, "defineClass",
        "(Ljava/lang/String;[BII)Ljava/lang/Class;"
    );
    if (defineClassMethod == NULL) {
        return NULL;
    }

    jsize length = (*env)->GetArrayLength(env, bytes);
    jobject result = (*env)->CallObjectMethod(
        env, classLoader, defineClassMethod,
        name, bytes, (jint)0, length
    );
    return (jclass)result;
}
