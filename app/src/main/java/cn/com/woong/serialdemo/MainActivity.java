package cn.com.woong.serialdemo;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.blankj.utilcode.util.ConvertUtils;

import butterknife.BindView;
import butterknife.OnClick;
import cn.com.woong.serialdemo.base.BaseActivity;
import android_serialport_api.SerialPortManager;
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

    SerialPortManager mSerialPortManager;

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
            public void onDataReceive(byte[] buffer, int size) {

            }
        });
    }

    @OnClick({R.id.btn_serial_open, R.id.btn_serial_close, R.id.btn_serial_send})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_serial_open:
                mSerialPortManager.openSerialPort();
                break;
            case R.id.btn_serial_close:
                mSerialPortManager.closeSerialPort();
                break;
            case R.id.btn_serial_send:
                String sendStr = "AA53323100000050AC";
                mSerialPortManager.sendSerialPort(ConvertUtils.hexString2Bytes(sendStr));
                break;
            default:
                break;
        }
    }

}
