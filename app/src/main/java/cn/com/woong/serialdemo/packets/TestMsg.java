package cn.com.woong.serialdemo.packets;

import android_serialport_api.SerialMsg;

/**
 * Created by wong on 2018/4/11.
 */

public class TestMsg extends SerialMsg {

    @Override
    public byte[] encodeSendMsg() {
        //todo: 封装待发送数据
        return new byte[0];
    }

    @Override
    public void decodeRecvMsg(byte[] recvMsg) {
        //todo: 解析已接收数据
    }
}
