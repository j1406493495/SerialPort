package cn.com.woong.serialdemo;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.LogUtils;

import android_serialport_api.SerialPortManager;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.com.woong.serialdemo.base.BaseActivity;
import cn.com.woong.serialdemo.constant.CmdCode;
import cn.com.woong.serialdemo.constant.RecvStatus;
import cn.com.woong.serialdemo.utils.PacketUtils;
import cn.com.woong.serialdemo.widget.TitleBarLayout;

/**
 * Created by wong on 2018/3/14.
 */

public class MainActivity extends BaseActivity {
    @BindView(R.id.title_bar)
    TitleBarLayout titleBar;
    @BindView(R.id.btn_serial_open)
    Button serialOpen;
    @BindView(R.id.btn_serial_close)
    Button serialClose;
    @BindView(R.id.et_send_line)
    EditText etSendLine;
    @BindView(R.id.et_send_column)
    EditText etSendColumn;
    @BindView(R.id.btn_serial_send)
    Button serialSend;
    @BindView(R.id.btn_query)
    Button btnQuery;
    @BindView(R.id.btn_rotation)
    Button btnRotation;

    private static final int LINE_START = 31;
    private static final int COLUMN_START = 30;

    private SerialPortManager mSerialPortManager;
    private int mDataLine;
    private int mDataColumn;
    private byte[] mRecvBytes = new byte[5];
    private int mRecvSize;
    private boolean mRotationFlag = false;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        titleBar.setTitle(getString(R.string.serial_port_debug));

        mSerialPortManager = SerialPortManager.getInstance();
        mSerialPortManager.setOnDataReceiveListener(new SerialPortManager.OnDataReceiveListener() {
            @Override
            public void onDataReceive(final byte[] buffer, final int size) {
                LogUtils.d("read buffer === " + ConvertUtils.bytes2HexString(buffer) + ", size == " + size);
                for (int i = 0; i < size && mRecvSize < 5; i++) {
                    mRecvBytes[mRecvSize++] = buffer[i];
                }

                if (mRecvSize >= 5) {
                    mRecvSize = 0;
                    int recvStatus = PacketUtils.parsePacket(mRecvBytes);
                    if (mRotationFlag) {
                        rotation(recvStatus);
                    }
                }
            }
        });
    }

    @OnClick({R.id.btn_serial_open, R.id.btn_serial_close, R.id.btn_serial_send, R.id.btn_query, R.id.btn_rotation})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_serial_open:
                mSerialPortManager.openSerialPort();
                break;
            case R.id.btn_serial_close:
                mSerialPortManager.closeSerialPort();
                break;
            case R.id.btn_serial_send:
                mSerialPortManager.sendSerialPort(PacketUtils.writePacket(CmdCode.CMD_CW_START,
                        etSendLine.getText().toString().trim(), etSendColumn.getText().toString().trim()));
                break;
            case R.id.btn_query:
                mSerialPortManager.sendSerialPort(PacketUtils.writePacket(CmdCode.CMD_CW_QUERY,
                        etSendLine.getText().toString().trim(), etSendColumn.getText().toString().trim()));
                break;
            case R.id.btn_rotation:
                mRotationFlag = true;
                mDataLine = LINE_START;
                mDataColumn = COLUMN_START;
                mSerialPortManager.sendSerialPort(PacketUtils.writePacket(CmdCode.CMD_CW_START,
                        String.valueOf(mDataLine), String.valueOf(mDataColumn)));
                break;
            default:
                break;
        }
    }

    private void rotation(int rotationFlag) {
        if (rotationFlag == RecvStatus.START_FREE) {
            mSerialPortManager.sendSerialPort(PacketUtils.writePacket(CmdCode.CMD_CW_QUERY,
                    String.valueOf(mDataLine), String.valueOf(mDataColumn)));
        } else if (rotationFlag == RecvStatus.QUERY_COMPLETE) {
            mDataLine += 1;
            mDataColumn += 1;
            mSerialPortManager.sendSerialPort(PacketUtils.writePacket(CmdCode.CMD_CW_START,
                    String.valueOf(mDataLine), String.valueOf(mDataColumn)));
        } else if (rotationFlag == RecvStatus.QUERY_BUSY) {
            mSerialPortManager.sendSerialPort(PacketUtils.writePacket(CmdCode.CMD_CW_QUERY,
                    String.valueOf(mDataLine), String.valueOf(mDataColumn)));
        } else if (rotationFlag == RecvStatus.QUERY_TIMEOUT) {
            mSerialPortManager.sendSerialPort(PacketUtils.writePacket(CmdCode.CMD_CW_QUERY,
                    String.valueOf(mDataLine), String.valueOf(mDataColumn)));
        }
    }
}
