package cn.com.woong.serialdemo.utils;

import android.os.SystemClock;
import android.util.SparseArray;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.LogUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingDeque;

import android_serialport_api.SerialMsg;
import android_serialport_api.SerialPortManager;
import cn.com.woong.serialdemo.packets.TestMsg;
import cn.com.woong.serialdemo.packets.TestPacket;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by wong on 2018/3/20.
 */
public class TestUtil implements SerialPortManager.OnDataReceiveListener {
    private static final int MAX_RETRY_COUNT = 3;

    private SerialPortManager mSerialPortManager;
    private SendThread mSendThread;
    private SparseArray<PublishSubject<TestPacket>> mPacketSubjects = new SparseArray<>();
    private SparseArray<TestPacket> mTestPackets = new SparseArray<>();
    private LinkedBlockingDeque<TestPacket> mLinkedBlockingDeque = new LinkedBlockingDeque<>();
    private volatile int mCurrentPacketId = -1;

    private boolean mCanParse = true;
    private int mRecvHeadIndex = 0;
    private int mRealDataIndex = 0;
    private byte[] mRecvRealData = new byte[6];


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
            String device = "/dev/ttyO3";
            int baudrate = 19200;
            mSerialPortManager = new SerialPortManager(device, baudrate);
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
    public void sendPacketNoResult(TestPacket testPacket) {
        testPacket.serialMsg.needAck = false;

        try {
            mLinkedBlockingDeque.put(testPacket);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Observable<TestPacket> sendPacket(final TestPacket testPacket) {
        final PublishSubject<TestPacket> subject = PublishSubject.create();

        return subject.doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable disposable) throws Exception {
                testPacket.serialMsg.needAck = true;
                mTestPackets.put(testPacket.getPacketId(), testPacket);
                mPacketSubjects.put(testPacket.getPacketId(), subject);
                mLinkedBlockingDeque.put(testPacket);
            }
        });
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
        if (!mCanParse) {
            return;
        }

        //todo: 根据自定义协议，获得完整的返回数据
        for (int i = 0; i < size; i++) {
            if (mRecvHeadIndex < 2) {
                if (mRealDataIndex == 0) {
                    if (buffer[i] == (byte) 0xDD) {
                        mRecvHeadIndex = 1;
                    }
                } else if (mRealDataIndex == 1) {
                    if (buffer[i] == (byte) 0x02) {
                        mRecvHeadIndex = 2;
                    }
                }
            } else {
                if (mRealDataIndex < 6) {
                    mRecvRealData[mRealDataIndex++] = buffer[i];
                    continue;
                }

                if (buffer[i] == (byte)0xCC && mRealDataIndex == 6) {
                    parseRealData();
                    mRealDataIndex = 0;
                    mRecvHeadIndex = 0;
                }
            }
        }
    }

    /**
     * 串口返回数据解析
     */
    private void parseRealData() {
        mCanParse = false;

        TestPacket recvTestPacket = mTestPackets.get(mCurrentPacketId);
        if (recvTestPacket != null) {
            recvTestPacket.decodeRecvPacket(mRecvRealData);
            recvTestPacket.serialMsg.hasAck = true;

            PublishSubject<TestPacket> recvSubject = mPacketSubjects.get(mCurrentPacketId);
            recvSubject.onNext(recvTestPacket);
            recvSubject.onComplete();
            mPacketSubjects.remove(mCurrentPacketId);
            mTestPackets.remove(mCurrentPacketId);
        }

        mCurrentPacketId = -1;
        mCanParse = true;
    }

    /**
     * send thread, 读取队列，发送数据到串口
     */
    private class SendThread extends Thread {
        @Override
        public void run() {
            while (mSerialPortManager != null) {
                TestPacket testPacket = mLinkedBlockingDeque.peek();
                if (testPacket != null) {
                    mCurrentPacketId = testPacket.getPacketId();

                    if (testPacket.serialMsg.hasAck) {
                        mLinkedBlockingDeque.poll();
                        SystemClock.sleep(100);
                        continue;
                    }

                    if (testPacket.serialMsg.needAck) {
                        testPacket.retryCount++;

                        if (testPacket.retryCount > MAX_RETRY_COUNT) {
                            PublishSubject<TestPacket> recvSubject = mPacketSubjects.get(mCurrentPacketId);
                            recvSubject.onError(new Throwable("retry timeout ==== "));
                            mLinkedBlockingDeque.poll();
                            mTestPackets.remove(mCurrentPacketId);
                            mPacketSubjects.remove(mCurrentPacketId);
                            SystemClock.sleep(100);
                            continue;
                        }
                    } else {
                        mLinkedBlockingDeque.poll();
                    }

                    mSerialPortManager.sendPacket(testPacket);
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
