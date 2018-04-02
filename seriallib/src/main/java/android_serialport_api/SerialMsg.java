package android_serialport_api;

/**
 * Created by wong on 2018/3/21.
 */

public abstract class SerialMsg {
    public byte msgType;
    public byte recvMsgType;
    public byte sendMsgLength;
    public byte recvMsgLength;

    public boolean hasAck;
    public boolean needAck;

    public abstract byte getMsgType();

    public abstract byte[] encodeSendMsg();

    public abstract void decodeRecvMsg(byte[] recvMsg);
}
