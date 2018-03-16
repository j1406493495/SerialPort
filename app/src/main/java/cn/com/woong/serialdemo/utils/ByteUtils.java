package cn.com.woong.serialdemo.utils;

/**
 * Created by wong on 2018/3/16.
 */

public class ByteUtils {

    private static final char hexDigits[] =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String byteToHexString(byte srcByte) {
        char[] des = new char[2];
        des[0] = hexDigits[srcByte >>> 4 & 0x0f];
        des[1] = hexDigits[srcByte & 0x0f];

        return new String(des);
    }
}
