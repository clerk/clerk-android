# JNI resolves only these transport methods by name.
-keep class com.clerk.api.QuickJSWorker {
    native <methods>;
    private void emitBytes(byte[]);
    private byte[] randomBase64(int);
}
