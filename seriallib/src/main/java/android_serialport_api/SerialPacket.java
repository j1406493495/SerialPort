package android_serialport_api;

/**
 * Created by wong on 2018/3/20.
 */

public abstract class SerialPacket {
    public byte deviceType;
    public byte deviceNum;
    public SerialMsg serialMsg;
    public byte checkCode;

    /**
     * 封装串口发送数据
     * @return
     */
    public abstract byte[] encodeSendPacket();

    /**
     * 解析串口返回数据
     */
    public abstract void decodeRecvPacket(Object recvObject);
}
