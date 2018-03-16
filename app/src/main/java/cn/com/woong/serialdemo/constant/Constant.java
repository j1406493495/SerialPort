package cn.com.woong.serialdemo.constant;

/**
 * Created by wong on 2018/3/7.
 */

public class Constant {
    /**
     * 起始标志/结束码
     */
    public static final String CMD_START = "AA";
    public static final String CMD_END = "AC";

    /**
     * 命令字
     */
    public static final String CMD_CW_START = "53";
    public static final String CMD_CW_QUERY = "52";
    public static final String CMD_CW_HEARTBEAT = "48";
    public static final String CMD_CW_CLOSE = "43";

    /**
     * 数据包
     */
    public static final String CMD_DATA_FILLING = "00";
}
