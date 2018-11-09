# SerialPort

## 简介

SerialPort是Android串口通信的封装库。

## Gradle依赖

> **Step 1.** Add the JitPack repository to your build file
>
> Add it in your root build.gradle at the end of repositories:
>
> ```groovy
> 	allprojects {
> 		repositories {
> 			...
> 			maven { url 'https://jitpack.io' }
> 		}
> 	}
> ```
>
> **Step 2.** Add the dependency
>
> ```groovy
> 	dependencies {
> 	        implementation 'com.github.j1406493495:SerialPort:1.0.0'
> 	}
> ```
>
>

## 使用方式

根据实际项目中的串口号和波特率，创建一个串口管理类SerialPortManager，并设置串口数据回调监听。

```java
   public void init() {
        if (mSerialPortManager == null) {
            String device = "/dev/ttyO3";
            int baudrate = 38400;
            mSerialPortManager = new SerialPortManager(device, baudrate);
            mSerialPortManager.setOnDataReceiveListener(this);
        }
    }
```

```java
    public interface OnDataReceiveListener {
        void onDataReceive(byte[] buffer, int size);
    }
```

**发送数据：**

```java
mSerialPortManager.sendPacket(testPacket);
```

其中testPacket继承自SerialPacket，为串口数据的封装类，详见示例代码。



---

本文由 [Woong](http://woong.com.cn/) 创作，采用 [知识共享署名4.0](https://creativecommons.org/licenses/by/4.0/) 国际许可协议进行许可
本站文章除注明转载/出处外，均为本站原创或翻译，转载前请务必署名
最后编辑时间为:2018-11-09