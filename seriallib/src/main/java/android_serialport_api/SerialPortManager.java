package android_serialport_api;

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

    public SerialPort serialPort = null;
    public InputStream inputStream = null;
    public OutputStream outputStream = null;
    private ReadThread readThread;

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
            serialPort = new SerialPort(new File(mDevice), mBaudrate, 0);

            mSerialPortStatus = true;
            mThreadStatus = false;

            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();

            //开始线程监控是否有数据要接收
            readThread = new ReadThread();
            readThread.start();
        } catch (IOException e) {
            Log.e(TAG, "openSerialPort: 打开串口异常：" + e.toString());
            return serialPort;
        }
        Log.e(TAG, "openSerialPort: 打开串口");
        return serialPort;
    }
    
    /**
     * 关闭串口
     */
    public void closeSerialPort() {
        try {
            if (mSerialPortStatus) {
                mSerialPortStatus = false;
                mThreadStatus = true;

                inputStream.close();
                outputStream.close();

                serialPort.close();
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
        
        try {
            if (sendData.length > 0 && mSerialPortStatus) {
                outputStream.write(sendData);
                outputStream.write('\n');
                outputStream.flush();
                Log.e(TAG, "sendSerialPort: 串口数据发送成功");
            }
        } catch (IOException e) {
            Log.e(TAG, "sendSerialPort: 串口数据发送失败：" + e.toString());
        }
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
                    if (inputStream == null) {
                        Log.e(TAG, "inputStream == null");
                        return;
                    }

                    size = inputStream.read(buffer);
                    Log.e(TAG,"size ==== " + size + "buffer === " + Arrays.toString(buffer));
                    if (size > 0) {
                        byte[] readBytes = new byte[size];
                        System.arraycopy(buffer, 0, readBytes, 0, size);

                        onDataReceiveListener.onDataReceive(readBytes, size);
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
    }
    
    public void setOnDataReceiveListener(OnDataReceiveListener dataReceiveListener) {
        onDataReceiveListener = dataReceiveListener;
    }
    
}
