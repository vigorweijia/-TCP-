package com.example.androidwifttest;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class TaskCenter {
    private static TaskCenter instance;

    private TaskCenter() {super();}

    public static TaskCenter sharedCenter() {
        if(instance == null) {
            synchronized (TaskCenter.class) {
                if(instance == null) instance = new TaskCenter();
            }
        }
        return instance;
    }

    private static final String TAG = "TaskCenter";

    private Socket my_socket;

    private String ipAddress;

    private int port;

    private Thread my_thread;

    private OutputStream outputStream;

    private InputStream inputStream;

    private OnServerConnectedCallbackBlock connectedCallback;

    private OnServerDisconnectedCallbackBlock disconnectedCallback;

    private OnReceiveCallbackBlock receiveCallback;

    /*
    * socket format: IP + port
    * @param ipAddress
    * @param port number
    */
    public void connect(final String ipAddress, final int port) {
        my_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG,"ip: "+ipAddress+" port: "+ port);
                    Log.d(TAG,"Creating Socket...");
                    my_socket = new Socket("192.168.137.1", 54321);
//                    socket.setSoTimeout ( 2 * 1000 );//设置超时时间
                    Log.d(TAG, "Create Socket Succeed. Connecting...");
                    if (isConnected()) {
                        TaskCenter.sharedCenter().ipAddress = ipAddress;
                        TaskCenter.sharedCenter().port = port;
                        if (connectedCallback != null) {
                            connectedCallback.callback();
                        }
                        outputStream = my_socket.getOutputStream();
                        inputStream = my_socket.getInputStream();
                        receive();
                        Log.i(TAG,"连接成功");
                    }else {
                        Log.i(TAG,"连接失败");
                        if (disconnectedCallback != null) {
                            disconnectedCallback.callback(new IOException("连接失败"));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG,"连接异常");
                    if (disconnectedCallback != null) {
                        disconnectedCallback.callback(e);
                    }
                }
            }
        });
        my_thread.start();
    }

    public boolean isConnected() {
        return my_socket.isConnected();
    }

    public void connect() {
        connect(ipAddress, port);
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (isConnected()) {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                my_socket.close();
                if (my_socket.isClosed()) {
                    if (disconnectedCallback != null) {
                        disconnectedCallback.callback(new IOException("断开连接"));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 接收数据
     */
    public void receive() {
        while (isConnected()) {
            try {
                /**得到的是16进制数，需要进行解析*/
                byte[] bt = new byte[2048];
//                获取接收到的字节和字节数
                int length = inputStream.read(bt);
//                获取正确的字节
                if(length <= 0) continue;
                byte[] bs = new byte[length];
                System.arraycopy(bt, 0, bs, 0, length);

                String str = new String(bs, "UTF-8");
                if (str != null) {
                    if (receiveCallback != null) {
                        receiveCallback.callback(str);
                    }
                }
                Log.i(TAG,"接收成功");
            } catch (IOException e) {
                Log.i(TAG,"接收失败");
            }
        }
    }

    /**
     * 发送数据
     *
     * @param data  数据
     */
    public void send(final byte[] data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (my_socket != null) {
                    try {
                        byte []newdata = new byte[data.length+1];
                        //手动添加一个终止符'\n',服务器端接收到后的readLine操作可以持续进行
                        int len = data.length;
                        for(int i = 0; i < len; i++) newdata[i] = data[i];
                        newdata[newdata.length-1] = '\n';
                        outputStream.write(newdata);
                        outputStream.flush();
                        Log.i(TAG,"发送成功");
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.i(TAG,"发送失败");
                    }
                } else {
                    connect();
                }
            }
        }).start();

    }

    public interface OnServerConnectedCallbackBlock {
        void callback();
    }

    public interface OnServerDisconnectedCallbackBlock {
        void callback(IOException e);
    }

    public interface OnReceiveCallbackBlock {
        void callback(String rcvMsg);
    }

    public void setConnectedCallback(OnServerConnectedCallbackBlock connectedCallback) {
        this.connectedCallback = connectedCallback;
    }

    public void setDisconnectedCallback(OnServerDisconnectedCallbackBlock disconnectedCallback) {
        this.disconnectedCallback = disconnectedCallback;
    }

    public void setReceivedCallback(OnReceiveCallbackBlock receivedCallback) {
        this.receiveCallback = receivedCallback;
    }
    /**
     * 移除回调
     */
    private void removeCallback() {
        connectedCallback = null;
        disconnectedCallback = null;
        receiveCallback = null;
    }
}
