package cn.com.woong.serialdemo;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.com.woong.serialdemo.base.BaseActivity;
import cn.com.woong.serialdemo.packets.TestMsg;
import cn.com.woong.serialdemo.packets.TestPacket;
import cn.com.woong.serialdemo.utils.TestUtil;
import cn.com.woong.serialdemo.widget.TitleBarLayout;

/**
 * @author Created by wong on 2018/3/14.
 */

public class MainActivity extends BaseActivity {
    @BindView(R.id.title_bar)
    TitleBarLayout titleBar;
    @BindView(R.id.btn_serial_open)
    Button serialOpen;
    @BindView(R.id.btn_serial_close)
    Button serialClose;
    @BindView(R.id.btn_serial_send)
    Button btnSerialSend;

    TestUtil mTestUtil;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        titleBar.setTitle(getString(R.string.serial_port_debug));

        mTestUtil = TestUtil.getInstance();
    }

    @OnClick({R.id.btn_serial_open, R.id.btn_serial_close, R.id.btn_serial_send})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_serial_open:
                mTestUtil.init();
                break;
            case R.id.btn_serial_close:
                mTestUtil.destory();
                break;
            case R.id.btn_serial_send:
                TestPacket testPacket = new TestPacket();
                TestMsg testMsg = new TestMsg();
                testPacket.serialMsg = testMsg;
                mTestUtil.sendPacket(testPacket);
            default:
                break;
        }
    }
}
