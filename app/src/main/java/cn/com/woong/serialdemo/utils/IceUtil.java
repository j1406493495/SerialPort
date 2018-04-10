package cn.com.woong.serialdemo.utils;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;

import java.util.HashMap;
import java.util.LinkedList;

import android_serialport_api.SerialMsg;
import android_serialport_api.SerialPortManager;

/**
 * Created by wong on 2018/3/20.
 */

public class IceUtil implements SerialPortManager.OnDataReceiveListener {

    private static final byte DEVICE_TYPE = (byte) 0x53;
    private static final byte ICE_READ = (byte) 0xA2;
    private static final byte DEVICE_NUM = (byte) 0x00;
    private static final int MAX_RETRY_COUNT = 3;
    /**
     * 单例
     */
    private static volatile IceUtil instance = null;
    private SerialPortManager mSerialPortManager;
    private SendThread mSendThread;
    private LinkedList<IcePacket> mSendPacketQueue = new LinkedList<>();
    private HashMap<Integer, OnPacketListener> mPacketListenerHashMap = new HashMap<>();
    private volatile int mCurrentPacketId;
    private String mDevice;
    private int mBaudrate;
    private byte[] mPacketHead = new byte[3];
    private int mPacketHeadIndex;
    private byte[] mRecvData = null;
    private int mRecvDataIndex = 0;
    private int mRecvDataSize;

    public static IceUtil getInstance() {
        if (instance == null) {
            synchronized (CoffeeUtil.class) {
                if (instance == null) {
                    instance = new IceUtil();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化制冰机
     */
    public void iceInit() {
        if (mSerialPortManager == null) {
            mDevice = "/dev/ttyO3";
            mBaudrate = 38400;
            mSerialPortManager = new SerialPortManager(mDevice, mBaudrate);
            mSerialPortManager.setOnDataReceiveListener(this);
        }

        mSendThread = new SendThread();
        mSendThread.setName("send ice thread");
        mSendThread.start();
    }

    /**
     * 发送出冰指令，先查询制冰机状态
     * @param configIcePacket
     */
    public void sendIcePacket(final IcePacket configIcePacket) {
        final IcePacket readIcePacket = new IcePacket();
        final ReadIceMsg readIceMsg = new ReadIceMsg();
        readIcePacket.serialMsg = readIceMsg;

        sendPacket(readIcePacket, new IceUtil.OnPacketListener() {
            @Override
            public void onRecvSuccess(final IcePacket readIcePacket) {
                sendConfigIce(readIcePacket, configIcePacket);
            }

            @Override
            public void onRecvFailed() {
            }
        });
    }

    private void sendConfigIce(IcePacket readIcePacket, IcePacket configIcePacket) {
        ReadIceMsg readIceResultMsg = (ReadIceMsg) readIcePacket.serialMsg;

        SPUtils.getInstance().put(SPKey.SP_ERROR, "");
        if (readIceResultMsg.recvErrorCode != IceInfo.ERROR_NONE) {
            ToastUtils.showLong(IceInfo.getIceErrorStr(readIceResultMsg.recvErrorCode));
            SPUtils.getInstance().put(SPKey.SP_ERROR, IceInfo.getIceErrorStr(readIceResultMsg.recvErrorCode));
            return;
        }

        sendPacket(configIcePacket);
    }

    /**
     * 指令数据存入队列，sendthread读取队列发送到串口
     * @param icePacket
     */
    public void sendPacket(IcePacket icePacket) {
        icePacket.setPacketId();
        icePacket.serialMsg.needAck = false;
        mSendPacketQueue.offer(icePacket);
    }

    public void sendPacket(IcePacket icePacket, OnPacketListener onPacketListener) {
        icePacket.setPacketId();
        icePacket.serialMsg.needAck = true;
        mSendPacketQueue.offer(icePacket);
        mPacketListenerHashMap.put(icePacket.getPacketId(), onPacketListener);
    }

    public void iceDestory() {
        if (mSerialPortManager != null) {
            mSerialPortManager.closeSerialPort();
            mSerialPortManager = null;
        }
    }

    /**
     * 串口数据返回
     * @param buffer
     * @param size
     */
    @Override
    public void onDataReceive(byte[] buffer, int size) {
        LogUtils.e("data receive buffer === " + ConvertUtils.bytes2HexString(buffer));
        //读取制冰机状态解析
        for (int i = 0; i < size; i++) {
            if (mPacketHeadIndex < 3) {
                mPacketHead[mPacketHeadIndex++] = buffer[i];
                if (mPacketHead[0] != DEVICE_TYPE) {
                    mPacketHeadIndex = 0;
                }

                if (mPacketHeadIndex == 3) {
                    if (mPacketHead[2] != ICE_READ) {
                        mPacketHeadIndex = 0;
                    }
                }
            } else if (mPacketHeadIndex >= 3) {
                if (mRecvData == null) {
                    mRecvDataSize = 4;
                    mRecvData = new byte[mRecvDataSize];
                }

                if (mRecvDataIndex < mRecvDataSize) {
                    mRecvData[mRecvDataIndex++] = buffer[i];
                }
            }
        }

        if (mPacketHeadIndex >= 3 && mRecvDataIndex >= mRecvDataSize) {
            mPacketHeadIndex = 0;
            mRecvDataIndex = 0;

            parseRecvData();
        }
    }

    /**
     * 串口返回数据解析
     */
    private void parseRecvData() {
        if (mPacketHead == null || mRecvData == null) {
            return;
        }
        byte[] totalRecvData = new byte[mPacketHead.length + mRecvData.length];
        System.arraycopy(mPacketHead, 0, totalRecvData, 0, mPacketHead.length);
        System.arraycopy(mRecvData, 0, totalRecvData, mPacketHead.length, mRecvData.length);

        LogUtils.e("totalRecvData === " + ConvertUtils.bytes2HexString(totalRecvData));
        mRecvData = null;
        if (!isRecvValid(totalRecvData)) {
            LogUtils.e("recv data is invalid === ");
            return;
        }

        synchronized (mSendPacketQueue) {
            for (int i = 0; i < mSendPacketQueue.size(); i++) {
                IcePacket packet = mSendPacketQueue.get(i);

                if (packet.getPacketId() != mCurrentPacketId) {
                    continue;
                }

                packet.decodeRecvPacket(totalRecvData);
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

    private boolean isRecvValid(byte[] bytes) {
        int code = 0;
        for (int i = 0; i < bytes.length - 2; i++) {
            code += bytes[i] & 0xFF;
        }

        byte[] checkCodeBytes = new byte[2];
        checkCodeBytes[0] = (byte) ((code >> 8) & 0xFF);
        checkCodeBytes[1] = (byte) (code & 0xFF);

        return checkCodeBytes[0] == bytes[bytes.length -2] && checkCodeBytes[1] == bytes[bytes.length - 1];
    }

    public SerialMsg parseMsg(byte[] msg) {
        byte msgType = (byte) (msg[0] - 0xA0);
        SerialMsg serialMsg = null;
        switch (msgType) {
            case TYPE_MSG_CONFIG:
                serialMsg = new ConfigIceMsg();
                break;
            case TYPE_MSG_READ:
                serialMsg = new ReadIceMsg();
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
        void onRecvSuccess(IcePacket coffeePacket);

        void onRecvFailed();
    }

    /**
     * send ice thread, 读取队列，发送数据到串口
     */
    private class SendThread extends Thread {
        @Override
        public void run() {
            while (mSerialPortManager != null) {
                IcePacket icePacket;
                while (mSendPacketQueue.peek() != null) {
                    icePacket = mSendPacketQueue.peek();
                    if (icePacket.serialMsg.hasAck) {
                        mSendPacketQueue.poll();
                        break;
                    }

                    if (icePacket.retryCount > MAX_RETRY_COUNT) {
                        mSendPacketQueue.poll();
                        if (mPacketListenerHashMap.get(icePacket.getPacketId()) != null) {
                            mPacketListenerHashMap.get(icePacket.getPacketId()).onRecvFailed();
                            mPacketListenerHashMap.remove(icePacket.getPacketId());
                        }
                        break;
                    }

                    mCurrentPacketId = icePacket.getPacketId();
                    LogUtils.e("send ice packet === " + icePacket);
                    mSerialPortManager.sendPacket(icePacket);
                    if (icePacket.serialMsg.needAck) {
                        icePacket.retryCount++;
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
