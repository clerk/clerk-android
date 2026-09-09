#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include "quickjs/quickjs.h"

typedef struct {
  JSRuntime *runtime;
  JSContext *context;
  JSValue receive;
  jobject peer;
  jmethodID emit;
  jmethodID random;
  JNIEnv *env;
  uint64_t deadline;
} ClerkEngine;

static uint64_t monotonic_ms(void) {
  struct timespec now;
  clock_gettime(CLOCK_MONOTONIC, &now);
  return (uint64_t)now.tv_sec * 1000 + now.tv_nsec / 1000000;
}

static int interrupt(JSRuntime *runtime, void *opaque) {
  (void)runtime;
  ClerkEngine *engine = opaque;
  return monotonic_ms() > engine->deadline;
}

static void throw_engine_error(JNIEnv *env) {
  jclass exception = (*env)->FindClass(env, "java/lang/IllegalStateException");
  if (exception) (*env)->ThrowNew(env, exception, "engine_execution_failed");
}

static JSValue emit_message(JSContext *context, JSValueConst self, int argc, JSValueConst *argv) {
  (void)self;
  if (argc != 1) return JS_ThrowTypeError(context, "Invalid host message");
  ClerkEngine *engine = JS_GetContextOpaque(context);
  size_t length;
  const char *text = JS_ToCStringLen(context, &length, argv[0]);
  if (!text) return JS_EXCEPTION;
  if (length > 16 * 1024 * 1024) { JS_FreeCString(context, text); return JS_ThrowRangeError(context, "Host message too large"); }
  JNIEnv *env = engine->env;
  jbyteArray bytes = (*env)->NewByteArray(env, (jsize)length);
  if (!bytes) { JS_FreeCString(context, text); return JS_ThrowInternalError(context, "Host allocation failed"); }
  (*env)->SetByteArrayRegion(env, bytes, 0, (jsize)length, (const jbyte *)text);
  JS_FreeCString(context, text);
  (*env)->CallVoidMethod(env, engine->peer, engine->emit, bytes);
  (*env)->DeleteLocalRef(env, bytes);
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); return JS_ThrowInternalError(context, "Host delivery failed"); }
  return JS_UNDEFINED;
}

static JSValue random_bytes(JSContext *context, JSValueConst self, int argc, JSValueConst *argv) {
  (void)self;
  int32_t count;
  if (argc != 1 || JS_ToInt32(context, &count, argv[0]) || count < 0 || count > 65536) return JS_ThrowRangeError(context, "Invalid random size");
  ClerkEngine *engine = JS_GetContextOpaque(context);
  JNIEnv *env = engine->env;
  jbyteArray data = (*env)->CallObjectMethod(env, engine->peer, engine->random, count);
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); return JS_ThrowInternalError(context, "Native randomness failed"); }
  if (!data) return JS_ThrowInternalError(context, "Native randomness failed");
  jsize length = (*env)->GetArrayLength(env, data);
  if (length == 0) { (*env)->DeleteLocalRef(env, data); return JS_NewStringLen(context, "", 0); }
  jbyte *bytes = (*env)->GetByteArrayElements(env, data, NULL);
  if (!bytes) { (*env)->DeleteLocalRef(env, data); return JS_ThrowInternalError(context, "Host allocation failed"); }
  JSValue result = JS_NewStringLen(context, (const char *)bytes, length);
  (*env)->ReleaseByteArrayElements(env, data, bytes, JNI_ABORT);
  (*env)->DeleteLocalRef(env, data);
  return result;
}

static int drain(ClerkEngine *engine) {
  JSContext *context;
  int result;
  while ((result = JS_ExecutePendingJob(engine->runtime, &context)) > 0) {}
  if (result < 0) {
    JSValue exception = JS_GetException(context);
    JS_FreeValue(context, exception);
  }
  return result;
}

JNIEXPORT jlong JNICALL Java_com_clerk_api_QuickJSWorker_nativeCreate(JNIEnv *env, jobject peer) {
  ClerkEngine *engine = calloc(1, sizeof(*engine));
  if (!engine) { throw_engine_error(env); return 0; }
  engine->runtime = JS_NewRuntime();
  if (!engine->runtime) { free(engine); throw_engine_error(env); return 0; }
  JS_SetMemoryLimit(engine->runtime, 128 * 1024 * 1024);
  JS_SetMaxStackSize(engine->runtime, 1024 * 1024);
  engine->deadline = monotonic_ms() + 10000;
  JS_SetInterruptHandler(engine->runtime, interrupt, engine);
  engine->context = JS_NewContext(engine->runtime);
  if (!engine->context) { JS_FreeRuntime(engine->runtime); free(engine); throw_engine_error(env); return 0; }
  engine->receive = JS_UNDEFINED;
  engine->peer = (*env)->NewGlobalRef(env, peer);
  engine->env = env;
  jclass cls = (*env)->GetObjectClass(env, peer);
  engine->emit = (*env)->GetMethodID(env, cls, "emitBytes", "([B)V");
  engine->random = (*env)->GetMethodID(env, cls, "randomBase64", "(I)[B");
  (*env)->DeleteLocalRef(env, cls);
  if (!engine->peer || !engine->emit || !engine->random || (*env)->ExceptionCheck(env)) {
    if (engine->peer) (*env)->DeleteGlobalRef(env, engine->peer);
    JS_FreeContext(engine->context); JS_FreeRuntime(engine->runtime); free(engine);
    return 0;
  }
  JS_SetContextOpaque(engine->context, engine);
  JSValue global = JS_GetGlobalObject(engine->context);
  JS_SetPropertyStr(engine->context, global, "__clerkNativeEmit", JS_NewCFunction(engine->context, emit_message, "emit", 1));
  JS_SetPropertyStr(engine->context, global, "__clerkNativeRandom", JS_NewCFunction(engine->context, random_bytes, "random", 1));
  JS_FreeValue(engine->context, global);
  return (jlong)(intptr_t)engine;
}

JNIEXPORT void JNICALL Java_com_clerk_api_QuickJSWorker_nativeEvaluate(JNIEnv *env, jobject peer, jlong handle, jbyteArray source) {
  (void)peer;
  ClerkEngine *engine = (ClerkEngine *)(intptr_t)handle;
  if (!engine) { throw_engine_error(env); return; }
  engine->env = env;
  engine->deadline = monotonic_ms() + 10000;
  jsize length = (*env)->GetArrayLength(env, source);
  jbyte *bytes = (*env)->GetByteArrayElements(env, source, NULL);
  if (!bytes) return;
  // JS_Eval requires a NUL-terminated source buffer.
  char *text = malloc((size_t)length + 1);
  if (!text) { (*env)->ReleaseByteArrayElements(env, source, bytes, JNI_ABORT); throw_engine_error(env); return; }
  memcpy(text, bytes, length); text[length] = 0;
  (*env)->ReleaseByteArrayElements(env, source, bytes, JNI_ABORT);
  JSValue result = JS_Eval(engine->context, text, length, "clerk-bundled-core.js", JS_EVAL_TYPE_GLOBAL);
  free(text);
  if (JS_IsException(result)) { JS_FreeValue(engine->context, JS_GetException(engine->context)); throw_engine_error(env); return; }
  JS_FreeValue(engine->context, result);
  JSValue global = JS_GetGlobalObject(engine->context);
  JSValue core = JS_GetPropertyStr(engine->context, global, "ClerkCore");
  engine->receive = JS_GetPropertyStr(engine->context, core, "receive");
  JS_FreeValue(engine->context, core); JS_FreeValue(engine->context, global);
  if (!JS_IsFunction(engine->context, engine->receive) || drain(engine) < 0) throw_engine_error(env);
}

JNIEXPORT void JNICALL Java_com_clerk_api_QuickJSWorker_nativeReceive(JNIEnv *env, jobject peer, jlong handle, jbyteArray message) {
  (void)peer;
  ClerkEngine *engine = (ClerkEngine *)(intptr_t)handle;
  if (!engine) { throw_engine_error(env); return; }
  engine->env = env;
  engine->deadline = monotonic_ms() + 10000;
  jsize length = (*env)->GetArrayLength(env, message);
  if (length > 16 * 1024 * 1024) { throw_engine_error(env); return; }
  jbyte *bytes = (*env)->GetByteArrayElements(env, message, NULL);
  if (!bytes) return;
  JSValue argument = JS_NewStringLen(engine->context, (const char *)bytes, length);
  (*env)->ReleaseByteArrayElements(env, message, bytes, JNI_ABORT);
  JSValue result = JS_Call(engine->context, engine->receive, JS_UNDEFINED, 1, &argument);
  JS_FreeValue(engine->context, argument);
  int failed = JS_IsException(result);
  JS_FreeValue(engine->context, result);
  if (failed) JS_FreeValue(engine->context, JS_GetException(engine->context));
  if (failed || drain(engine) < 0) throw_engine_error(env);
}

JNIEXPORT void JNICALL Java_com_clerk_api_QuickJSWorker_nativeClose(JNIEnv *env, jobject peer, jlong handle) {
  (void)peer;
  ClerkEngine *engine = (ClerkEngine *)(intptr_t)handle;
  if (!engine) return;
  JS_FreeValue(engine->context, engine->receive);
  JS_FreeContext(engine->context);
  JS_FreeRuntime(engine->runtime);
  (*env)->DeleteGlobalRef(env, engine->peer);
  free(engine);
}
