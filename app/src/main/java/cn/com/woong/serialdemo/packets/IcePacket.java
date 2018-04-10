package cn.com.woong.serialdemo.packets;

import android_serialport_api.SerialPacket;
import cn.com.woong.vmcoffee.utils.IceUtil;

/**
 * Created by wong on 2018/3/21.
 */

public class IcePacket extends SerialPacket {
    private byte[] checkCode = new byte[2];

    private static int increaseId = 0;
    private int mPacketId;
    public int retryCount;

    public IcePacket() {
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
        byte[] sendBytes = new byte[msg.length + 4];
        sendBytes[0] = deviceType;
        sendBytes[1] = deviceNum;

        System.arraycopy(msg, 0, sendBytes, 2, msg.length);

        sendBytes[sendBytes.length - 2] = checkCode(sendBytes)[0];
        sendBytes[sendBytes.length - 1] = checkCode(sendBytes)[1];
        return sendBytes;
    }

    @Override
    public void decodeRecvPacket(byte[] recvBytes) {
        deviceType = recvBytes[0];
        deviceNum = recvBytes[1];
        byte[] msg = new byte[recvBytes.length - 4];
        System.arraycopy(recvBytes, 2, msg, 0, msg.length);
        serialMsg = IceUtil.getInstance().parseMsg(msg);
        checkCode[0] = recvBytes[recvBytes.length - 2];
        checkCode[1] = recvBytes[recvBytes.length - 1];
    }

    private byte[] checkCode(byte[] bytes) {
        int code = 0;
        for (int i = 0; i < bytes.length - 2; i++) {
            code += bytes[i] & 0xFF;
        }

        byte[] checkCodeBytes = new byte[2];
        checkCodeBytes[0] = (byte) ((code >> 8) & 0xFF);
        checkCodeBytes[1] = (byte) (code & 0xFF);
        return checkCodeBytes;
    }
}
