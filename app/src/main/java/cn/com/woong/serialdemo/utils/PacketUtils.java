package cn.com.woong.serialdemo.utils;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.LogUtils;

import cn.com.woong.serialdemo.constant.CmdCode;
import cn.com.woong.serialdemo.constant.RecvStatus;


/**
 * Created by wong on 2018/3/16.
 */

public class PacketUtils {

    /**
     * 发送指令包封装
     * @param cmdCw
     * @param lineData
     * @param columnData
     * @return
     */
    public static byte[] writePacket(String cmdCw, String lineData, String columnData) {
        byte[] cmdCwStart = ConvertUtils.hexString2Bytes(cmdCw);
        byte[] dataLine = ConvertUtils.hexString2Bytes(lineData);
        byte[] dataColumn = ConvertUtils.hexString2Bytes(columnData);
        String checkCode = "";

        if (cmdCwStart.length >= 1 && dataLine.length >= 1 && dataColumn.length >= 1) {
            byte checkCodeByte = (byte) (cmdCwStart[0] ^ dataLine[0] ^ dataColumn[0]);
            checkCode = ByteUtils.byteToHexString(checkCodeByte);
        }

        String packetStr = CmdCode.CMD_START + cmdCw + lineData + columnData
                + CmdCode.CMD_DATA_FILLING + CmdCode.CMD_DATA_FILLING + CmdCode.CMD_DATA_FILLING
                + checkCode + CmdCode.CMD_END;

        LogUtils.d("packetStr === " + packetStr);

        return ConvertUtils.hexString2Bytes(packetStr);
    }

    public static @RecvStatus int parsePacket(byte[] recvBytes) {
        if (recvBytes[0] == CmdCode.CMD_START_BYTE) {
            switch (recvBytes[1]) {
                case CmdCode.CMD_CW_START_BYTE:
                    if (recvBytes[2] == (byte) 0x30 && recvBytes[3] == (byte) 0x63
                            && recvBytes[4] == CmdCode.CMD_END_BYTE) {
                        //空闲
                        return RecvStatus.START_FREE;
                    } else if (recvBytes[2] == (byte) 0x31 && recvBytes[3] == (byte) 0x62
                            && recvBytes[4] == CmdCode.CMD_END_BYTE) {
                        //忙
                        return RecvStatus.START_BUSY;
                    }
                    break;
                case CmdCode.CMD_CW_QUERY_BYTE:
                    if (recvBytes[2] == (byte) 0x30 && recvBytes[3] == (byte) 0x62
                            && recvBytes[4] == CmdCode.CMD_END_BYTE) {
                        //询问 忙
                        return RecvStatus.QUERY_BUSY;
                    } else if (recvBytes[2] == (byte) 0x31 && recvBytes[3] == (byte) 0x63
                            && recvBytes[4] == CmdCode.CMD_END_BYTE) {
                        //询问 完成
                        return RecvStatus.QUERY_COMPLETE;
                    } else if (recvBytes[2] == (byte) 0x32 && recvBytes[3] == (byte) 0x60
                            && recvBytes[4] == CmdCode.CMD_END_BYTE) {
                        //询问 超时
                        return RecvStatus.QUERY_TIMEOUT;
                    } else if (recvBytes[2] == (byte) 0x34 && recvBytes[3] == (byte) 0x66
                            && recvBytes[4] == CmdCode.CMD_END_BYTE) {
                        //询问 光幕
                        return RecvStatus.QUERY_SCREEN;
                    }
                    break;
                default:
                    break;
            }
        }

        //失败
        return RecvStatus.RECV_FAILED;
    }
}
