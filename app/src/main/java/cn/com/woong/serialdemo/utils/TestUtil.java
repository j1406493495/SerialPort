package cn.com.woong.serialdemo.utils;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.LogUtils;

import java.util.HashMap;
import java.util.LinkedList;

import android_serialport_api.SerialMsg;
import android_serialport_api.SerialPortManager;
import cn.com.woong.serialdemo.packets.TestMsg;
import cn.com.woong.serialdemo.packets.TestPacket;

/**
 * Created by wong on 2018/3/20.
 */
public class TestUtil implements SerialPortManager.OnDataReceiveListener {
    private static final int MAX_RETRY_COUNT = 3;

    private SerialPortManager mSerialPortManager;
    private SendThread mSendThread;
    private LinkedList<TestPacket> mSendPacketQueue = new LinkedList<>();
    private HashMap<Integer, OnPacketListener> mPacketListenerHashMap = new HashMap<>();
    private volatile int mCurrentPacketId;
    private String mDevice;
    private int mBaudrate;

    /**
     * 单例
     */
    private static volatile TestUtil instance = null;
    public static TestUtil getInstance() {
        if (instance == null) {
            synchronized (TestUtil.class) {
                if (instance == null) {
                    instance = new TestUtil();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化
     */
    public void init() {
        if (mSerialPortManager == null) {
            mDevice = "/dev/ttyO3";
            mBaudrate = 38400;
            mSerialPortManager = new SerialPortManager(mDevice, mBaudrate);
            mSerialPortManager.setOnDataReceiveListener(this);
        }

        mSendThread = new SendThread();
        mSendThread.setName("send thread");
        mSendThread.start();
    }

    public void destory() {
        if (mSerialPortManager != null) {
            mSerialPortManager.closeSerialPort();
            mSerialPortManager = null;
        }
    }

    /**
     * 指令数据存入队列，sendthread读取队列发送到串口
     *
     * @param testPacket
     */
    public void sendPacket(TestPacket testPacket) {
        testPacket.setPacketId();
        testPacket.serialMsg.needAck = false;
        mSendPacketQueue.offer(testPacket);
    }

    public void sendPacket(TestPacket testPacket, OnPacketListener onPacketListener) {
        testPacket.setPacketId();
        testPacket.serialMsg.needAck = true;
        mSendPacketQueue.offer(testPacket);
        mPacketListenerHashMap.put(testPacket.getPacketId(), onPacketListener);
    }

    /**
     * 串口数据返回
     *
     * @param buffer
     * @param size
     */
    @Override
    public void onDataReceive(byte[] buffer, int size) {
        LogUtils.e("data receive buffer === " + ConvertUtils.bytes2HexString(buffer));

        //todo: 根据自定义协议，获得完整的返回数据
        Object recvObject = null;

        parseRecvData(recvObject);
    }

    /**
     * 串口返回数据解析
     */
    private void parseRecvData(Object recvObject) {
        //todo: 根据自定义协议，解析返回数据

        synchronized (mSendPacketQueue) {
            for (int i = 0; i < mSendPacketQueue.size(); i++) {
                TestPacket packet = mSendPacketQueue.get(i);

                if (packet.getPacketId() != mCurrentPacketId) {
                    continue;
                }

                packet.decodeRecvPacket(recvObject);
                if (packet.serialMsg != null) {
                    packet.serialMsg.hasAck = true;
                    if (mPacketListenerHashMap.get(packet.getPacketId()) != null) {
                        mPacketListenerHashMap.get(packet.getPacketId()).onRecvSuccess(packet);
                        mPacketListenerHashMap.remove(packet.getPacketId());
                    }
                }
            }
        }
    }

    public SerialMsg parseMsg(byte[] msg) {
        byte msgType = (byte) (msg[0] - 0xA0);
        SerialMsg serialMsg = null;
        switch (msgType) {
            case 0x00:
                break;
            case 0x01:
                serialMsg = new TestMsg();
                break;
            default:
                break;
        }

        if (serialMsg != null) {
            serialMsg.decodeRecvMsg(msg);
        }

        return serialMsg;
    }


    public interface OnPacketListener {
        void onRecvSuccess(TestPacket testPacket);

        void onRecvFailed();
    }

    /**
     * send thread, 读取队列，发送数据到串口
     */
    private class SendThread extends Thread {
        @Override
        public void run() {
            while (mSerialPortManager != null) {
                TestPacket testPacket;
                while (mSendPacketQueue.peek() != null) {
                    testPacket = mSendPacketQueue.peek();
                    if (testPacket.serialMsg.hasAck) {
                        mSendPacketQueue.poll();
                        break;
                    }

                    if (testPacket.retryCount > MAX_RETRY_COUNT) {
                        mSendPacketQueue.poll();
                        if (mPacketListenerHashMap.get(testPacket.getPacketId()) != null) {
                            mPacketListenerHashMap.get(testPacket.getPacketId()).onRecvFailed();
                            mPacketListenerHashMap.remove(testPacket.getPacketId());
                        }
                        break;
                    }

                    mCurrentPacketId = testPacket.getPacketId();
                    mSerialPortManager.sendPacket(testPacket);
                    if (testPacket.serialMsg.needAck) {
                        testPacket.retryCount++;
                    } else {
                        mSendPacketQueue.poll();
                    }
                    break;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
