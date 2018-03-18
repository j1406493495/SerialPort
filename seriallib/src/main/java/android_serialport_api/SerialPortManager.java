package android_serialport_api;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * @author woong
 * Created by woong on 2018/3/14
 */

public class SerialPortManager {
    private final String TAG = "SerialPortManager";

    private String mDevice = "/dev/ttyO3";
    private int mBaudrate = 9600;

    /**
     * 是否打开串口标志
     */
    private boolean mSerialPortStatus = false;
    /**
     * 线程状态，为了安全终止线程
     */
    private boolean mThreadStatus;

    /**
     * 数据发送后是否有返回
     */
    private volatile boolean mHasRecv;
    private byte[] mRecvBuf = new byte[5];
    private int mRecvSize = 0;


    public SerialPort mSerialPort = null;
    public InputStream mInputStream = null;
    public OutputStream mOutputStream = null;

    private ReadThread mReadThread;
    private HandlerThread mSendHandlerThread;
    private Handler mSendHandler;

    private SerialPortManager() {}

    private static volatile SerialPortManager instance = null;
    public static SerialPortManager getInstance() {
        if (instance == null) {
            synchronized (SerialPortManager.class) {
                if (instance == null){
                    instance = new SerialPortManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 打开串口
     *
     * @return serialPort串口对象
     */
    public SerialPort openSerialPort() {
        try {
            File deviceFile = new File(mDevice);
            if (!deviceFile.exists()) {
                Log.e(TAG, "device is null === ");
                return null;
            }
            mSerialPort = new SerialPort(new File(mDevice), mBaudrate, 0);

            mSerialPortStatus = true;
            mThreadStatus = false;

            mInputStream = mSerialPort.getInputStream();
            mOutputStream = mSerialPort.getOutputStream();

            //数据发送线程
            startSendThread();
            //开始线程监控是否有数据要接收
            mReadThread = new ReadThread();
            mReadThread.setName("Recv Thread");
            mReadThread.start();
        } catch (IOException e) {
            Log.e(TAG, "openSerialPort: 打开串口异常：" + e.toString());
            return mSerialPort;
        }
        Log.e(TAG, "openSerialPort: 打开串口");
        return mSerialPort;
    }
    
    /**
     * 关闭串口
     */
    public void closeSerialPort() {
        try {
            if (mSerialPortStatus) {
                mSerialPortStatus = false;
                mThreadStatus = true;

                mInputStream.close();
                mOutputStream.close();

                mSerialPort.close();
                Log.e(TAG, "closeSerialPort: 关闭串口成功");
            }
        } catch (IOException e) {
            Log.e(TAG, "closeSerialPort: 关闭串口异常：" + e.toString());
            return;
        }
    }
    
    /**
     * 发送串口指令
     *
     * @param sendData 数据指令
     */
    public void sendSerialPort(byte[] sendData) {
        Log.e(TAG, "sendSerialPort: 发送数据");
        sendBytes(sendData);
    }

    private synchronized void sendBytes(byte[] sendBytes) {
        if (mSendHandler != null) {
            Message message = mSendHandler.obtainMessage();
            message.obj = sendBytes;
            message.sendToTarget();
        }
    }

    private void startSendThread() {
        mSendHandlerThread = new HandlerThread("send handler thread");
        mSendHandlerThread.start();
        mSendHandler = new Handler(mSendHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                mHasRecv = false;
                byte[] sendData = (byte[]) msg.obj;

                try {
                    if (sendData.length > 0 && mSerialPortStatus) {
                        mOutputStream.write(sendData);
                        mOutputStream.write('\n');
                        mOutputStream.flush();
                        Log.e(TAG, "sendSerialPort: 串口数据发送成功");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "sendSerialPort: 串口数据发送失败：" + e.toString());
                }

                try {
                    //1s后，若无返回数据，再次发送
                    Thread.sleep(1000);

                    if (!mHasRecv) {
                        sendBytes(sendData);
                        onDataReceiveListener.onDataRecvError();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    }
    
    /**
     * 读数据线程
     */
    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            Log.e(TAG, "进入线程run == ");
            //判断进程是否在运行，更安全的结束进程
            while (!mThreadStatus && !isInterrupted()) {
                Log.e(TAG, "进入线程while == ");
                //读取数据的大小
                int size = 0;
                try {
                    Log.e(TAG, "startRead == ");

                    //64   1024
                    byte[] buffer = new byte[64];
                    if (mInputStream == null) {
                        Log.e(TAG, "mInputStream == null");
                        return;
                    }

                    size = mInputStream.read(buffer);
                    if (size > 0) {
                        byte[] readBytes = new byte[size];
                        System.arraycopy(buffer, 0, readBytes, 0, size);

                        for (int i = 0; i < size && mRecvSize < 5; i++) {
                            mRecvBuf[mRecvSize++] = readBytes[i];
                            if (mRecvBuf[0] != (byte) 0xAA) {
                                mRecvSize = 0;
                            }
                        }

                        if (mRecvSize >= 5 && mRecvBuf[0] == (byte) 0xAA) {
                            mRecvSize = 0;
                            mHasRecv = true;
                            onDataReceiveListener.onDataReceive(mRecvBuf, size);
                        }
                    }

                    Log.e(TAG, "endRead === " );
                } catch (IOException e) {
                    Log.e(TAG, "run: 数据读取异常：" + e.toString());
                }
            }
        }
    }


    /**
     * 监听数据接收
     */
    public OnDataReceiveListener onDataReceiveListener = null;

    public interface OnDataReceiveListener {
        void onDataReceive(byte[] buffer, int size);
        void onDataRecvError();
    }
    
    public void setOnDataReceiveListener(OnDataReceiveListener dataReceiveListener) {
        onDataReceiveListener = dataReceiveListener;
    }
    
}
