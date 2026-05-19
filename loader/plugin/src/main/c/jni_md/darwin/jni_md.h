/*
 * Minimal platform-specific JNI header for macOS cross-compilation.
 * Mirrors the public OpenJDK jni_md.h surface, scoped to what TotemGuard's
 * native bridge needs (jint, jlong, jbyte, JNIEXPORT/IMPORT/CALL).
 */
#ifndef _JAVASOFT_JNI_MD_H_
#define _JAVASOFT_JNI_MD_H_

#ifndef __has_attribute
  #define __has_attribute(x) 0
#endif

#if (defined(__GNUC__) && ((__GNUC__ > 4) || (__GNUC__ == 4) && (__GNUC_MINOR__ > 2))) || __has_attribute(visibility)
  #define JNIEXPORT     __attribute__((visibility("default")))
  #define JNIIMPORT     __attribute__((visibility("default")))
#else
  #define JNIEXPORT
  #define JNIIMPORT
#endif

#define JNICALL

typedef int             jint;
typedef long long       jlong;
typedef signed char     jbyte;

#endif /* !_JAVASOFT_JNI_MD_H_ */
