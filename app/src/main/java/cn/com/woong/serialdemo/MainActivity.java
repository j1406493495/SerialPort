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
import cn.com.woong.serialdemo.constant.Constant;
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
    @BindView(R.id.btn_serial_send)
    Button serialSend;
    @BindView(R.id.et_send_data)
    EditText etSendData;
    @BindView(R.id.tv_read_data)
    TextView tvReadData;
    @BindView(R.id.et_send_line)
    EditText etSendLine;
    @BindView(R.id.et_send_column)
    EditText etSendColumn;
    @BindView(R.id.btn_query)
    Button btnQuery;

    private byte mLineBegin = 0x31;
    private byte mColumnBegin = 0x30;
    private String mDataLine;
    private String mDataColumn;
    private String mCheckCode;
    private SerialPortManager mSerialPortManager;

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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LogUtils.e("read buffer === " + ConvertUtils.bytes2HexString(buffer) + ", size == " + size);
                        tvReadData.setText(ConvertUtils.bytes2HexString(buffer));
                    }
                });
            }
        });
    }

    @OnClick({R.id.btn_serial_open, R.id.btn_serial_close, R.id.btn_serial_send, R.id.btn_query})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_serial_open:
                mSerialPortManager.openSerialPort();
                break;
            case R.id.btn_serial_close:
                mSerialPortManager.closeSerialPort();
                break;
            case R.id.btn_serial_send:
                getDataCode(Constant.CMD_CW_START);

                String sendStr = Constant.CMD_START + Constant.CMD_CW_START + mDataLine + mDataColumn
                        + Constant.CMD_DATA_FILLING + Constant.CMD_DATA_FILLING + Constant.CMD_DATA_FILLING
                        + mCheckCode + Constant.CMD_END;
                LogUtils.e("sendStr === " + sendStr);

                byte[] sendBytes = ConvertUtils.hexString2Bytes(sendStr);
                for (int i = 0, count = sendBytes.length; i < count; i++) {
                    LogUtils.e("byte " + i + " == " + sendBytes[i]);
                }
                mSerialPortManager.sendSerialPort(ConvertUtils.hexString2Bytes(sendStr));
                break;
            case R.id.btn_query:
                getDataCode(Constant.CMD_CW_QUERY);

                String queryStr = Constant.CMD_START + Constant.CMD_CW_QUERY+ mDataLine + mDataColumn
                        + Constant.CMD_DATA_FILLING + Constant.CMD_DATA_FILLING + Constant.CMD_DATA_FILLING
                        + mCheckCode + Constant.CMD_END;
                LogUtils.e("sendStr === " + queryStr);

                byte[] queryBytes = ConvertUtils.hexString2Bytes(queryStr);
                for (int i = 0, count = queryBytes.length; i < count; i++) {
                    LogUtils.e("byte " + i + " == " + queryBytes[i]);
                }
                mSerialPortManager.sendSerialPort(ConvertUtils.hexString2Bytes(queryStr));
                break;
            default:
                break;
        }
    }

    private void getDataCode(String cmdCw) {
        mDataLine = etSendLine.getText().toString().trim();
        mDataColumn = etSendColumn.getText().toString().trim();

        byte cmdCwStart = ConvertUtils.hexString2Bytes(cmdCw)[0];
        byte dataLine = ConvertUtils.hexString2Bytes(mDataLine)[0];
        byte dataColumn = ConvertUtils.hexString2Bytes(mDataColumn)[0];
        byte[] checkCode = new byte[1];
        checkCode[0] = (byte) (cmdCwStart ^ dataLine ^ dataColumn);
        mCheckCode = ConvertUtils.bytes2HexString(checkCode);
    }
}
