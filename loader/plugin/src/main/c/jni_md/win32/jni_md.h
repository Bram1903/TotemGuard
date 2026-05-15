/*
 * Minimal platform-specific JNI header for Windows cross-compilation.
 * Mirrors the public OpenJDK jni_md.h surface, scoped to what TotemGuard's
 * native bridge needs (jint, jlong, jbyte, JNIEXPORT/IMPORT/CALL).
 */
#ifndef _JAVASOFT_JNI_MD_H_
#define _JAVASOFT_JNI_MD_H_

#if defined(__GNUC__)
  #define JNIEXPORT     __attribute__((dllexport))
  #define JNIIMPORT     __attribute__((dllimport))
#else
  #define JNIEXPORT     __declspec(dllexport)
  #define JNIIMPORT     __declspec(dllimport)
#endif

#if defined(_M_IX86) && !defined(_M_AMD64) && !defined(_M_ARM64)
  #define JNICALL       __stdcall
#else
  #define JNICALL
#endif

typedef long            jint;
typedef long long       jlong;
typedef signed char     jbyte;

#endif /* !_JAVASOFT_JNI_MD_H_ */
