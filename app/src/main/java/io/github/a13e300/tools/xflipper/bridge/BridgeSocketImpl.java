package io.github.a13e300.tools.xflipper.bridge;

import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;

import de.robv.android.xposed.XposedHelpers;
import io.github.a13e300.tools.xflipper.Logger;
import io.github.a13e300.tools.xflipper.XposedEntry;

public class BridgeSocketImpl extends SocketImpl {
    private final Socket mSocket;
    private final SocketImpl mImpl;

    public BridgeSocketImpl(Socket socket, SocketImpl delegate) {
        mSocket = socket;
        mImpl = delegate;
    }

    private Object invokeDelegateIOE(String method, Object... args) throws IOException {
        try {
            return XposedHelpers.callMethod(mImpl, method, args);
        } catch (XposedHelpers.InvocationTargetError e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            } else {
                throw new RuntimeException(cause);
            }
        }
    }

    private Object invokeDelegateSOE(String method, Object... args) throws SocketException {
        try {
            return XposedHelpers.callMethod(mImpl, method, args);
        } catch (XposedHelpers.InvocationTargetError e) {
            Throwable cause = e.getCause();
            if (cause instanceof SocketException) {
                throw (SocketException) cause;
            } else {
                throw new RuntimeException(cause);
            }
        }
    }

    @Override
    protected void create(boolean stream) throws IOException {
        // https://cs.android.com/android/platform/superproject/main/+/main:libcore/ojluni/src/main/java/java/net/AbstractPlainSocketImpl.java;l=128;drc=955031fd40b9bed1f383c373a7b6650122f88557
        // https://cs.android.com/android/platform/superproject/main/+/main:libcore/ojluni/src/main/java/java/net/PlainSocketImpl.java;l=128;drc=955031fd40b9bed1f383c373a7b6650122f88557
        try {
            if (!stream) throw new IllegalArgumentException("non-stream is not supported");
            // create ...
            FileDescriptor fd = (FileDescriptor) XposedHelpers.getObjectField(mImpl, "fd");
            Bundle b = XposedEntry.Companion.getAppContext()
                    .getContentResolver()
                    .call(Uri.parse("content://io.github.a13e300.tools.xflipper.socket_provider"), "create", "", null);
            if (b == null) {
                throw new IllegalStateException("please allow module to run and connect to network");
            }
            ParcelFileDescriptor pfd = b.getParcelable("fd");
            XposedHelpers.callMethod(fd, "setInt$", pfd.detachFd());
            XposedHelpers.callStaticMethod(Class.forName("libcore.io.IoUtils"), "setFdOwner", fd, mImpl);
            if (mSocket != null)
                XposedHelpers.callMethod(mSocket, "setCreated");
            if (XposedHelpers.getObjectField(mImpl, "serverSocket") != null) {
                throw new IllegalArgumentException("i dont know how to handle server socket");
            }
        } catch (Throwable e) {
            Logger.e("failed to create:", e);
            throw new IOException(e);
        } finally {
            // restore orig impl
            XposedHelpers.setObjectField(mSocket, "impl", mImpl);
        }
    }

    @Override
    protected void connect(String s, int i) throws IOException {
        invokeDelegateIOE("connect", s, i);
    }

    @Override
    protected void connect(InetAddress inetAddress, int i) throws IOException {
        invokeDelegateIOE("connect", inetAddress, i);
    }

    @Override
    protected void connect(SocketAddress socketAddress, int i) throws IOException {
        invokeDelegateIOE("connect", socketAddress, i);
    }

    @Override
    protected void bind(InetAddress inetAddress, int i) throws IOException {
        invokeDelegateIOE("bind", inetAddress, i);
    }

    @Override
    protected void listen(int i) throws IOException {
        invokeDelegateIOE("listen", i);
    }

    @Override
    protected void accept(SocketImpl socket) throws IOException {
        invokeDelegateIOE("accept", socket);
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        return (InputStream) invokeDelegateIOE("getInputStream");
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        return (OutputStream) invokeDelegateIOE("getOutputStream");
    }

    @Override
    protected int available() throws IOException {
        return (int) invokeDelegateIOE("available");
    }

    @Override
    protected void close() throws IOException {
        invokeDelegateIOE("close");
    }

    @Override
    protected void sendUrgentData(int i) throws IOException {
        invokeDelegateIOE("sendUrgentData", i);
    }

    @Override
    public void setOption(int i, Object o) throws SocketException {
        invokeDelegateSOE("setOption", i, o);
    }

    @Override
    public Object getOption(int i) throws SocketException {
        return invokeDelegateSOE("getOption", i);
    }
}
