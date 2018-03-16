package cn.com.woong.serialdemo.constant;

/**
 * Created by wong on 2018/3/7.
 */

public class CmdCode {
    /**
     * 起始标志/结束码
     */
    public static final String CMD_START = "AA";
    public static final String CMD_END = "AC";
    public static final byte CMD_START_BYTE = (byte) 0xAA;
    public static final byte CMD_END_BYTE = (byte) 0xAC;

    /**
     * 命令字
     */
    public static final String CMD_CW_START = "53";
    public static final String CMD_CW_QUERY = "52";
    public static final String CMD_CW_HEARTBEAT = "48";
    public static final String CMD_CW_CLOSE = "43";
    public static final byte CMD_CW_START_BYTE = (byte) 0x53;
    public static final byte CMD_CW_QUERY_BYTE = (byte) 0x52;
    public static final byte CMD_CW_HEARTBEAT_BYTE = (byte) 0x48;
    public static final byte CMD_CW_CLOSE_BYTE = (byte) 0x43;

    /**
     * 数据包
     */
    public static final String CMD_DATA_FILLING = "00";
    public static final byte CMD_DATA_FILLING_BYTE = (byte) 0x00;
}
