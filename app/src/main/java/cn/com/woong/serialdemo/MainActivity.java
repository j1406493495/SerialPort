package cn.com.woong.serialdemo;

import android.view.View;
import android.widget.Button;
import butterknife.BindView;
import butterknife.OnClick;
import cn.com.woong.serialdemo.base.BaseActivity;
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

    private static final int LINE_START = 31;
    private static final int COLUMN_START = 30;

//    private SerialPortManager mSerialPortManager;
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

//        mSerialPortManager = SerialPortManager.getInstance();
//        mSerialPortManager.setOnDataReceiveListener(new SerialPortManager.OnDataReceiveListener() {
//            @Override
//            public void onDataReceive(final byte[] buffer, final int size) {
//                LogUtils.d("read buffer === " + ConvertUtils.bytes2HexString(buffer) + ", size == " + size);
//                parseRecvData(buffer);
//            }
//
//            @Override
//            public void onDataRecvError() {
//                sendData(PacketUtils.writePacket(CmdCode.CMD_CW_QUERY,
//                        String.valueOf(mDataLine), String.valueOf(mDataColumn)));
//            }
//        });
    }

    @OnClick({R.id.btn_serial_open, R.id.btn_serial_close})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_serial_open:
//                mSerialPortManager.openSerialPort();
                break;
            case R.id.btn_serial_close:
//                mSerialPortManager.closeSerialPort();
                break;
            default:
                break;
        }
    }
}
