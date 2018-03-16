package cn.com.woong.serialdemo.constant;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by wong on 2018/3/16.
 */

@IntDef({RecvStatus.RECV_FAILED, RecvStatus.START_FREE, RecvStatus.START_BUSY, RecvStatus.QUERY_BUSY,
    RecvStatus.QUERY_COMPLETE, RecvStatus.QUERY_TIMEOUT, RecvStatus.QUERY_SCREEN})
@Retention(RetentionPolicy.RUNTIME)
public @interface RecvStatus {
    /**
     * 数据错误
     */
    int RECV_FAILED = -1;

    /**
     * 电机开始，数据返回
     */
    int START_FREE = 0;
    int START_BUSY = 1;

    /**
     * 询问状态，数据返回
     */
    int QUERY_BUSY = 2;
    int QUERY_COMPLETE = 3;
    int QUERY_TIMEOUT = 4;
    int QUERY_SCREEN = 5;
}
