package cn.com.woong.serialdemo;

import android.app.Application;
import android.content.Context;

import com.blankj.utilcode.util.Utils;

import butterknife.ButterKnife;

/**
 * @author woong
 * Created by wong on 2018/3/7.
 */

public class App extends Application {
    private static App mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        Utils.init(mInstance);
        ButterKnife.setDebug(true);
    }

    public static Context getAppContext() {
        return mInstance.getApplicationContext();
    }

    public static App getInstance() {
        return mInstance;
    }
}
