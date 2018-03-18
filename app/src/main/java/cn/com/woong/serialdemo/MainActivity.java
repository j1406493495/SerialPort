package cn.com.woong.serialdemo;

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;

import java.util.Arrays;

import android_serialport_api.SerialPortManager;
import butterknife.BindView;
import butterknife.OnClick;
import cn.com.woong.serialdemo.base.BaseActivity;
import cn.com.woong.serialdemo.constant.CmdCode;
import cn.com.woong.serialdemo.constant.RecvStatus;
import cn.com.woong.serialdemo.utils.PacketUtils;
import cn.com.woong.serialdemo.widget.TitleBarLayout;

/**
 * @author
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
    @BindView(R.id.btn_pause_rotation)
    Button btnPauseRotation;
    @BindView(R.id.tv_log)
    TextView tvLog;

    private static final int LINE_START = 31;
    private static final int COLUMN_START = 30;

    private SerialPortManager mSerialPortManager;
    private int mDataLine;
    private int mDataColumn;
    private boolean mRotationFlag = false;
    private boolean mModeRotation = false;
    private String mLogStr = "";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        titleBar.setTitle(getString(R.string.serial_port_debug));
        mDataLine = LINE_START;
        mDataColumn = COLUMN_START;

        mSerialPortManager = SerialPortManager.getInstance();
        mSerialPortManager.setOnDataReceiveListener(new SerialPortManager.OnDataReceiveListener() {
            @Override
            public void onDataReceive(final byte[] buffer, final int size) {
                LogUtils.d("read buffer === " + ConvertUtils.bytes2HexString(buffer) + ", size == " + size);
                parseRecvData(buffer);
            }

            @Override
            public void onDataRecvError() {
                sendData(PacketUtils.writePacket(CmdCode.CMD_CW_QUERY,
                        String.valueOf(mDataLine), String.valueOf(mDataColumn)));
            }
        });
    }

    private void parseRecvData(final byte[] recvBytes) {
        final int recvStatus = PacketUtils.parsePacket(recvBytes);
        if (mRotationFlag) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLogStr = "接收数据 " + ConvertUtils.bytes2HexString(recvBytes) + "\n" + mLogStr;
                    tvLog.setText(mLogStr);
                    rotation(recvStatus);
                }
            });
        }
    }

    @OnClick({R.id.btn_serial_open, R.id.btn_serial_close, R.id.btn_serial_send, R.id.btn_query,
            R.id.btn_rotation, R.id.btn_pause_rotation})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_serial_open:
                mSerialPortManager.openSerialPort();
                break;
            case R.id.btn_serial_close:
                mSerialPortManager.closeSerialPort();
                break;
            case R.id.btn_serial_send:
                String dataLine = etSendLine.getText().toString().trim();
                String dataColumn = etSendColumn.getText().toString().trim();
                if (!TextUtils.isEmpty(dataLine) && !TextUtils.isEmpty(dataColumn)) {
                    sendData(PacketUtils.writePacket(CmdCode.CMD_CW_START,
                            dataLine, dataColumn));
                } else {
                    ToastUtils.showShort("行/列数据不能为空");
                }
                break;
            case R.id.btn_query:
                String dataQueryLine = etSendLine.getText().toString().trim();
                String dataQueryColumn = etSendColumn.getText().toString().trim();
                if (!TextUtils.isEmpty(dataQueryLine) && !TextUtils.isEmpty(dataQueryColumn)) {
                    sendData(PacketUtils.writePacket(CmdCode.CMD_CW_QUERY,
                            dataQueryLine, dataQueryColumn));
                } else {
                    ToastUtils.showShort("行/列数据不能为空");
                }
                break;
            case R.id.btn_rotation:
                mModeRotation = true;
                mRotationFlag = true;
                btnRotation.setEnabled(false);
                btnPauseRotation.setEnabled(true);

                sendData(PacketUtils.writePacket(CmdCode.CMD_CW_START,
                        String.valueOf(mDataLine), String.valueOf(mDataColumn)));
                break;
            case R.id.btn_pause_rotation:
                mRotationFlag = false;
                btnRotation.setEnabled(true);
                btnPauseRotation.setEnabled(false);
                break;
            default:
                break;
        }
    }

    private void rotation(int rotationFlag) {
        LogUtils.d("rotationFlag === " + rotationFlag);
        if (rotationFlag == RecvStatus.START_FREE) {
            mLogStr = "开始 空闲 \n" + mLogStr;
            tvLog.setText(mLogStr);
            sendData(PacketUtils.writePacket(CmdCode.CMD_CW_QUERY,
                    String.valueOf(mDataLine), String.valueOf(mDataColumn)));
        } else if (rotationFlag == RecvStatus.QUERY_COMPLETE) {
            if (mDataColumn < 39) {
                mDataColumn += 1;
            } else {
                mDataColumn = 30;
                if (mDataLine < 33) {
                    mDataLine += 1;
                } else {
                    mDataLine = 31;
                }
            }
            mLogStr = "询问 完成 \n" + mLogStr;
            tvLog.setText(mLogStr);
            sendData(PacketUtils.writePacket(CmdCode.CMD_CW_START,
                    String.valueOf(mDataLine), String.valueOf(mDataColumn)));
        } else if (rotationFlag == RecvStatus.QUERY_BUSY) {
            mLogStr = "询问 忙 \n" + mLogStr;
            tvLog.setText(mLogStr);
            sendData(PacketUtils.writePacket(CmdCode.CMD_CW_QUERY,
                    String.valueOf(mDataLine), String.valueOf(mDataColumn)));
        } else if (rotationFlag == RecvStatus.QUERY_TIMEOUT) {
            mLogStr = "询问 超时 \n" + mLogStr;
            tvLog.setText(mLogStr);
            sendData(PacketUtils.writePacket(CmdCode.CMD_CW_QUERY,
                    String.valueOf(mDataLine), String.valueOf(mDataColumn)));
        } else {
            mLogStr = "返回数据错误 \n" + mLogStr;
            tvLog.setText(mLogStr);
            sendData(PacketUtils.writePacket(CmdCode.CMD_CW_QUERY,
                    String.valueOf(mDataLine), String.valueOf(mDataColumn)));
        }
    }

    private void sendData(byte[] sendData) {
        mLogStr = "发送 " + ConvertUtils.bytes2HexString(sendData) + "\n" + mLogStr;
        tvLog.setText(mLogStr);
        mSerialPortManager.sendSerialPort(sendData);
    }
}
