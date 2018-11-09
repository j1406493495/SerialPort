package android_serialport_api;

import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author woong
 * Created by woong on 2018/3/14
 */

public class SerialPortManager {
     private final String TAG = "SerialPortManager";

    /**
     * 是否打开串口标志
     */
    private boolean mSerialPortStatus = false;
    /**
     * 线程状态，为了安全终止线程
     */
    private boolean mThreadStatus;

    public SerialPort mSerialPort = null;
    public InputStream mInputStream = null;
    public OutputStream mOutputStream = null;

    private ReadThread mReadThread;

    public SerialPortManager(String device, int baudrate) {
        openSerialPort(device, baudrate);
    }

    /**
     * 打开串口
     *
     * @return serialPort串口对象
     */
    private SerialPort openSerialPort(String device, int baudrate) {
        try {
            File deviceFile = new File(device);
            if (!deviceFile.exists()) {
                Log.e(TAG, "device is null == ");
                return null;
            }
            mSerialPort = new SerialPort(new File(device), baudrate, 0);

            mSerialPortStatus = true;
            mThreadStatus = false;

            mInputStream = mSerialPort.getInputStream();
            mOutputStream = mSerialPort.getOutputStream();

            //开始线程监控是否有数据要接收
            mReadThread = new ReadThread();
            mReadThread.setName("Recv Thread");
            mReadThread.start();
        } catch (IOException e) {
            Log.e(TAG, "openSerialPort == : 打开串口异常：" + e.toString());
            return mSerialPort;
        }
        Log.e(TAG, "openSerialPort == : 打开串口");
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
                Log.e(TAG, "closeSerialPort == : 关闭串口成功");
            }
        } catch (IOException e) {
            Log.e(TAG, "closeSerialPort == : 关闭串口异常：" + e.toString());
            return;
        }
    }

    /**
     * 发送串口指令
     *
     * @param serialPacket
     */
    public void sendPacket(SerialPacket serialPacket) {
        byte[] sendData = serialPacket.encodeSendPacket();

        try {
            if (sendData.length > 0 && mSerialPortStatus) {
                mOutputStream.write(sendData);
                mOutputStream.flush();
                Log.e(TAG, "sendSerialPort == : 串口数据发送成功");
            }
        } catch (IOException e) {
            Log.e(TAG, "sendSerialPort == : 串口数据发送失败：" + e.toString());
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
                    if (mInputStream == null) {
                        Log.e(TAG, "mInputStream == null");
                        return;
                    }

                    size = mInputStream.read(buffer);
                    if (size > 0) {
                        byte[] readBytes = new byte[size];
                        System.arraycopy(buffer, 0, readBytes, 0, size);

                        if (onDataReceiveListener != null) {
                            onDataReceiveListener.onDataReceive(readBytes, size);
                        }
                    }

                    Log.e(TAG, "endRead == " );
                } catch (IOException e) {
                    Log.e(TAG, "run == : 数据读取异常：" + e.toString());
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
