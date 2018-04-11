package cn.com.woong.serialdemo.packets;

import android_serialport_api.SerialPacket;
import cn.com.woong.serialdemo.utils.TestUtil;

/**
 * Created by wong on 2018/3/21.
 */
public class TestPacket extends SerialPacket {
    private static int increaseId = 0;
    private int mPacketId;
    public int retryCount;

    public TestPacket() {
        deviceType = 0x53;
        deviceNum = 0x00;
    }

    public void setPacketId() {
        increaseId++;
        if (increaseId  == Integer.MAX_VALUE) {
            increaseId = 0;
        }

        mPacketId = increaseId;
    }

    public int getPacketId() {
        return mPacketId;
    }

    @Override
    public byte[] encodeSendPacket() {
        byte[] msg = serialMsg.encodeSendMsg();
        byte[] sendBytes = new byte[msg.length + 2];

        //packet公用数据
        sendBytes[0] = deviceType;
        sendBytes[1] = deviceNum;
        //message数据
        System.arraycopy(msg, 0, sendBytes, 2, msg.length);

        return sendBytes;
    }

    @Override
    public void decodeRecvPacket(Object recvObject) {
        byte[] recvBytes = (byte[]) recvObject;

        deviceType = recvBytes[0];
        deviceNum = recvBytes[1];
        byte[] msg = new byte[recvBytes.length - 2];
        System.arraycopy(recvBytes, 2, msg, 0, msg.length);
        serialMsg = TestUtil.getInstance().parseMsg(msg);
    }
}
