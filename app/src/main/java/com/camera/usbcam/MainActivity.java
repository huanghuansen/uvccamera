package com.camera.usbcam;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2016 saki t_saki@serenegiant.com
 *
 * File name: MainActivity.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
 * Files in the jni/libjpeg, jni/libusb, jin/libuvc, jni/rapidjson folder may have a different license, see the respective files.
 * see the respective files.
*/

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.camera.encoder.MediaAudioEncoder;
import com.camera.encoder.MediaEncoder;
import com.camera.encoder.MediaMuxerWrapper;
import com.camera.encoder.MediaVideoEncoder;
import com.camera.usb.IFrameCallback;
import com.camera.usb.USBMonitor;
import com.camera.usb.UVCCamera;
import com.camera.utils.PreferencesUtils;
import com.camera.videotest2.VideotestActivity;
import com.camera.widget.BaseActivity;
import com.camera.widget.SimpleUVCCameraTextureView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public final class MainActivity extends BaseActivity {
    private static final boolean DEBUG = true;    // set false when releasing
    private static final String TAG = "huansen";

    private static final int CAPTURE_STOP = 0;
    private static final int CAPTURE_PREPARE = 1;
    private static final int CAPTURE_RUNNING = 2;
    private boolean mIsRecordMode = true;
    private final Object mSync = new Object();
    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera;
    private SimpleUVCCameraTextureView mUVCCameraView;
    // for open&start / stop&close camera preview
    private ToggleButton mCameraButton;
    // for start & stop movie capture
    private ImageButton mCaptureButton, mSwitchModeButton;
    private ImageView mOtherSettingButton, mReviewImage;
    private TextView mvideotimetext;
    private int mCaptureState = CAPTURE_STOP;//huansen
    private Surface mPreviewSurface;
    private boolean isTakePicture = false;
    protected static String imagePath = "";
    public static String External_Storage = "";
    private String timeStr = "";
    private long video_timer = 0;
    private boolean isStopCount = true;
    private long mRecordingStartTime;
    private boolean allowedvideo = false;//是否允许进入拍照和录像
    private Toast mToast;
    private SoundPool mSoundPool;
    private int mRefocusSound;
    private SoundPool mVideoStartSoundPool;
    private int mVideoStartRefocusSound;
    private SoundPool mVideoStopSoundPool;
    private int mVideoStopRefocusSound;
    // Handler messages
    private final int UVC_TAKE_PICTURE = 1;
    private final int UVC_START_CAPTURE = 2;
    private final int UVC_STOP_CAPTURE = 3;
    private PowerManager.WakeLock wakeLock = null;
    ShutdownBroadcastReceiver mShutdownBroadcastReceiver;
    private boolean isshowMemory = false;
    private Timer mCheckTimer;
    private int mExtensionCustomerValue = 0;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        initView();
        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        //startLogcatManager();
        countTimer();
        External_Storage = getStoragePath(MainActivity.this, true);
        mSoundPool = new SoundPool(1, AudioManager.STREAM_RING, 0);
        mRefocusSound = mSoundPool.load(this, R.raw.camera_click2, 1);
        mVideoStartSoundPool = new SoundPool(1, AudioManager.STREAM_RING, 0);
        mVideoStartRefocusSound = mVideoStartSoundPool.load(this, R.raw.video_record, 1);
        mVideoStopSoundPool = new SoundPool(1, AudioManager.STREAM_RING, 0);
        mVideoStopRefocusSound = mVideoStopSoundPool.load(this, R.raw.focus_complete, 1);
        mShutdownBroadcastReceiver = new ShutdownBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
        registerReceiver(mShutdownBroadcastReceiver, intentFilter);
        Thread.setDefaultUncaughtExceptionHandler(new MyCrashHandler());
    }

    private void initView() {
        mCameraButton = (ToggleButton) findViewById(R.id.camera_button);
        mCameraButton.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mvideotimetext = (TextView) findViewById(R.id.vidiotimeText);
        mCaptureButton = (ImageButton) findViewById(R.id.capture_button);
        mCaptureButton.setOnClickListener(mOnClickListener);
        mSwitchModeButton = (ImageButton) findViewById(R.id.btn_switch_mode);
        mSwitchModeButton.setOnClickListener(mSwitchOnClickListener);
        mOtherSettingButton = (ImageView) findViewById(R.id.btn_other_setting);
        mOtherSettingButton.setOnClickListener(mSettingOnClickListener);
        mReviewImage = (ImageView) findViewById(R.id.review_image);
        mReviewImage.setOnClickListener(mreviewOnClickListener);
        mUVCCameraView = (SimpleUVCCameraTextureView) findViewById(R.id.UVCCameraTextureView1);
        mUVCCameraView.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        mUVCCameraView.setSurfaceTextureListener(mSurfaceTextureListener);
        mCheckTimer = new Timer();
        mCheckTimer.schedule(new TimerTask() {
            private int flag;
            @Override
            public void run() {
                runOnUiThread(new Runnable() {      // UI thread
                    @Override
                    public void run() {
                        if (mUVCCamera != null) {
                            switch (mUVCCamera.getExtensionCustomerValue()) {
                                case 1:
                                    if (flag != 1) {
                                        flag = 1;
                                        if (mCaptureState == CAPTURE_STOP) {
                                            mHandler.sendEmptyMessage(UVC_TAKE_PICTURE);
                                        }
                                    }
                                    break;
                                case 2:
                                    if (flag != 2) {
                                        flag = 2;
                                        if (allowedvideo == true) {
                                            if (mCaptureState == CAPTURE_STOP) {
                                                mHandler.sendEmptyMessage(UVC_START_CAPTURE);
                                            } else {
                                                mHandler.sendEmptyMessage(UVC_STOP_CAPTURE);
                                            }
                                        }
                                    }
                                    break;
                                default:
                                    if (flag != 0) {
                                        flag = 0;
                                    }
                                    break;
                            }
                        }

                    }
                });
            }
        }, 1000, 1000);
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "onStart()");
        super.onStart();
        getStorgeIsSDcard();
        getFaultcameradata();
        mUVCCameraView.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        synchronized (mSync) {
            if (mUSBMonitor != null) {
                mUSBMonitor.register();
            }
            if (!mCameraButton.isChecked()) {
                //CameraDialog.showDialog(MainActivity.this);
                final List<UsbDevice> Device = mUSBMonitor.getDeviceList();
                if (!Device.isEmpty()) {
                    allowedvideo = true;
                    mUSBMonitor.requestPermission(Device.get(0));
                }
            } else {
                allowedvideo = false;
            }
        }

          /*  if (mUVCCamera != null) {
                mUVCCamera.startPreview();
				Log.d(tag,"onStart2");
                mUVCCamera.setFrameCallback(mIFrameCallback, mUVCCamera.PIXEL_FORMAT_RGBX);
                mUVCCamera.setAudioCallback(mIAudioCallback);
            }*/

        //	setCameraButton(false);huansen
        //	updateItems();
        //  mReviewImage.setImageBitmap(getImageThumbnail(imagePath, 60, 60));
    }


    @Override
    protected void onStop() {
        Log.v(TAG, "onStop()");
        if (mCaptureState != CAPTURE_STOP) {
            mHandler.sendEmptyMessage(UVC_STOP_CAPTURE);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        synchronized (mSync) {
            if (mUVCCamera != null) {
                //  getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//释放屏幕常亮
                mUVCCamera.stopPreview();
            }
            mUSBMonitor.unregister();
        }
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (mCaptureState != CAPTURE_STOP) {
            mHandler.sendEmptyMessage(UVC_STOP_CAPTURE);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onPause() {
        Log.v(TAG, "onPause()");
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.v(TAG, "onResume()");
        super.onResume();
    }

    @Override
    public void onDestroy() {
        try {
            synchronized (mSync) {
                if (mUVCCamera != null) {
                    mUVCCamera.destroy();
                    mUVCCamera = null;
                }
                if (mUSBMonitor != null) {
                    mUSBMonitor.destroy();
                    mUSBMonitor = null;
                }
            }
            if (mToast != null) {
                mToast.cancel();
            }
            if (mCheckTimer != null) {
                mCheckTimer.cancel();
            }
            // disableCameraOTG();
            unregisterReceiver(mShutdownBroadcastReceiver);
            mCameraButton = null;
            mCaptureButton = null;
            mvideotimetext = null;
            mUVCCameraView = null;
            mOtherSettingButton = null;
            mReviewImage = null;
            mSwitchModeButton = null;
            releaseWakeLock();
            stopLogcatManager();
            super.onDestroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
     */
    private void acquireWakeLock() {
        if (null == wakeLock) {
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "PostLocationService");
            if (null != wakeLock) {
                wakeLock.acquire();
            }
        }
    }

    //释放设备电源锁
    private void releaseWakeLock() {
        if (null != wakeLock) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    private void enableCameraOTG() {
        File file = new File("/sys/devices/soc.0/78d9000.usb/backboard_otg_power_enable");
        if (file.exists()) {
            try {
                BufferedWriter out = new BufferedWriter(new FileWriter(file));
                Log.v(TAG, "enableCameraOTG().........");
                out.write("enable");
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "enable failed", e);
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void disableCameraOTG() {
        File file = new File("/sys/devices/soc.0/78d9000.usb/backboard_otg_power_enable");
        if (file.exists()) {
            try {
                BufferedWriter out = new BufferedWriter(new FileWriter("/sys/devices/soc.0/78d9000.usb/backboard_otg_power_enable"));
                Log.v(TAG, "disableCameraOTG().......");
                out.write("disable");
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "disable failed", e);
            }
        }
    }

    private void startLogcatManager() {
        String folderPath = null;

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {// save in SD card first
            folderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Download";

        } else {// If the SD card does not exist, save in the directory of application.
            folderPath = this.getFilesDir().getAbsolutePath() + File.separator + "Download";

        }
        LogcatFileManager.getInstance().start(folderPath);
    }

    private void stopLogcatManager() {
        //LogcatFileManager.getInstance().stop();
    }

    private final OnCheckedChangeListener mOnCheckedChangeListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
            synchronized (mSync) {
                if (!isChecked && mUVCCamera == null) {
                    //CameraDialog.showDialog(MainActivity.this);
                    //enableCameraOTG();
                    final List<UsbDevice> Device = mUSBMonitor.getDeviceList();
                    if (!Device.isEmpty()) {
                        allowedvideo = true;
                        mUSBMonitor.requestPermission(Device.get(0));
                    }
                } else if (mUVCCamera != null) {
                    //disableCameraOTG();
                    allowedvideo = false;
                    mUVCCamera.destroy();
                    mUVCCamera = null;
                }
            }
            //	updateItems();
        }
    };

    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            if (allowedvideo == true) {
                if (mIsRecordMode == true)
                    if (mCaptureState == CAPTURE_STOP) {
                        mHandler.sendEmptyMessage(UVC_START_CAPTURE);
                    } else {
                        mHandler.sendEmptyMessage(UVC_STOP_CAPTURE);
                    }
                else {
                    mHandler.sendEmptyMessage(UVC_TAKE_PICTURE);
                }
            } else {
                showToast("请确认是否连接和打开摄像头");
            }
        }
    };
    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UVC_TAKE_PICTURE: {
                    if (allowedvideo == true) {
                        isTakePicture = true;
                        showToast("拍照成功");
                    }
                    break;
                }
                case UVC_START_CAPTURE: {
                    if ((allowedvideo == true) && (mCaptureState == CAPTURE_STOP)) {
                        if (((MediaMuxerWrapper.isSaveSDCard == false)) && (getAvailableInternalMemorySize(MainActivity.this) < 110080)) {
                            showToast("空间不足，无法录像");
                            break;
                        } else if (((MediaMuxerWrapper.isSaveSDCard == true)) && (getAvailableExternalMemorySize(MainActivity.this) < 450887680)) {
                            showToast("SD卡空间不足，无法录像");
                            break;
                        }
                        mVideoStartSoundPool.play(mVideoStartRefocusSound, 1.0f, 1.0f, 0, 0, 1.0f);
                        mRecordingStartTime = SystemClock.uptimeMillis();
                        mCaptureButton.setImageResource(R.drawable.shutter_button_video_stop);
                        mSwitchModeButton.setVisibility(View.GONE);
                        mCameraButton.setVisibility(View.GONE);
                        mOtherSettingButton.setVisibility(View.GONE);
                        mReviewImage.setVisibility(View.GONE);
                        mvideotimetext.setVisibility(View.VISIBLE);
                        mvideotimetext.setText("00:00:00");
                        mRecordingStartTime = SystemClock.uptimeMillis();
                        isStopCount = false;
                        mIsRecordMode = true;
                        acquireWakeLock();
                        //  mRecordingStartTime = SystemClock.uptimeMillis();
                        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//保持屏幕常亮
                        startCapture();
                    }
                    break;
                }
                case UVC_STOP_CAPTURE: {
                    if (mCaptureState != CAPTURE_STOP) {
                        if (isshowMemory == true) {
                            isshowMemory = false;//显示内存不足
                            showToast("空间不足，无法录像，文件已保存");
                        }
                        mCaptureButton.setImageResource(R.drawable.shutter_button_video);
                        mSwitchModeButton.setVisibility(View.VISIBLE);
                        mCameraButton.setVisibility(View.VISIBLE);
                        mOtherSettingButton.setVisibility(View.VISIBLE);
                        mReviewImage.setVisibility(View.VISIBLE);
                        mvideotimetext.setVisibility(View.GONE);
                        // showToast("录像结束");
                        mIsRecordMode = true;
                        releaseWakeLock();
                        mVideoStopSoundPool.play(mVideoStopRefocusSound, 1.0f, 1.0f, 0, 0, 1.0f);
                        // getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//释放屏幕常亮
                        isStopCount = true;
                        stopCapture();
                    }
                    break;
                }
                default:
                    break;
            }
        }
    };

    private String millisecondToTimeString(long milliSeconds) {
        long seconds = milliSeconds / 1000; // round down to compute seconds
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long remainderMinutes = minutes - (hours * 60);
        long remainderSeconds = seconds - (minutes * 60);
        StringBuilder timeStringBuilder = new StringBuilder();
        // Hours
        if (hours < 10) {
            timeStringBuilder.append('0');
        }
        timeStringBuilder.append(hours);
        timeStringBuilder.append(':');
        // Minutes
        if (remainderMinutes < 10) {
            timeStringBuilder.append('0');
        }
        timeStringBuilder.append(remainderMinutes);
        timeStringBuilder.append(':');
        // Seconds
        if (remainderSeconds < 10) {
            timeStringBuilder.append('0');
        }
        timeStringBuilder.append(remainderSeconds);

        return timeStringBuilder.toString();
    }

    private Runnable TimerRunnable = new Runnable() {

        @Override
        public void run() {
            if (!isStopCount) {
                // video_timer += 1000;
                //  timeStr = TimeUtil.getFormatTime(video_timer);
                //  mvideotimetext.setText(timeStr);
                long now = SystemClock.uptimeMillis();
                long delta = now - mRecordingStartTime;
                timeStr = millisecondToTimeString(delta);
                String mStr = timeStr.substring(2, 8);
                if ((mStr.equals(":30:00")) || (mStr.equals(":59:59"))) {
                    new StartNewFileThread().start();
                }
                mvideotimetext.setText(timeStr);
            }
            countTimer();
        }
    };

    private class StartNewFileThread extends Thread {
        @Override
        public void run() {
            super.run();
            if (mCaptureState != CAPTURE_STOP) {
                if (((MediaMuxerWrapper.isSaveSDCard == false)) && (getAvailableInternalMemorySize(MainActivity.this) < 110080)) {
                    isshowMemory = true;
                    mHandler.sendEmptyMessage(UVC_STOP_CAPTURE);
                    return;
                } else if (((MediaMuxerWrapper.isSaveSDCard == true)) && (getAvailableExternalMemorySize(MainActivity.this) < 450887680)) { //小于430M,返回显示内存不足，停止录像
                    isshowMemory = true;
                    mHandler.sendEmptyMessage(UVC_STOP_CAPTURE);
                    return;
                } else
                    stopCapture();
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (mCaptureState == CAPTURE_STOP) {
                startCapture();
            }
        }
    }

    private void countTimer() {
        mHandler.postDelayed(TimerRunnable, 1000);
    }

    private final OnClickListener mSwitchOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            if (mIsRecordMode) {
                mSwitchModeButton.setImageResource(R.drawable.ic_switch_camera);
                mCaptureButton.setImageResource(R.drawable.btn_shutter_record);
                mIsRecordMode = false;
            } else {
                mSwitchModeButton.setImageResource(R.drawable.ic_switch_video);
                mCaptureButton.setImageResource(R.drawable.shutter_button_video);
                mIsRecordMode = true;
            }
        }
    };
    private final OnClickListener mreviewOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
         /*   File file = new File(imagePath);
            Intent it = new Intent(Intent.ACTION_VIEW);
            Uri mUri = Uri.parse("file://" + file.getPath());
            it.setDataAndType(mUri, "image/*");
            startActivity(it);*/
            if (mCaptureState != CAPTURE_STOP) {
                stopCapture();
            }
            Intent intent = new Intent(MainActivity.this, VideotestActivity.class);
            startActivity(intent);
            //Intent intent = new Intent(MainActivity.this, ShowphotoActivity.class);
            // startActivity(intent);
        }
    };
    private final OnClickListener mSettingOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            if (mCaptureState != CAPTURE_STOP) {
                stopCapture();
            }
            Intent intent = new Intent(MainActivity.this, SettingActivity.class);
            startActivity(intent);
        }
    };
    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            //  Toast.makeText(MainActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
            synchronized (mSync) {
                if (mUVCCamera != null) {
                    mUVCCamera.destroy();
                    mUVCCamera = null;
                }
            }
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    if (!mCameraButton.isChecked()) {
                        final UVCCamera camera = new UVCCamera();
                        camera.open(ctrlBlock);
                        if (DEBUG) Log.i(TAG, "supportedSize:" + camera.getSupportedSize());
                        if (mPreviewSurface != null) {
                            mPreviewSurface.release();
                            mPreviewSurface = null;
                        }
                    /*
                    {
						int numCodec = MediaCodecList.getCodecCount();
						Log.i(TAG, "We got " + numCodec + " Codecs");

						for (int i = 0; i < numCodec; i++) {
							MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

							if (codecInfo.isEncoder())
								continue;

							String[] type = codecInfo.getSupportedTypes();
							for (int j = 0; j < type.length; j++) {
								Log.i(TAG, "We got type " + type[j]);
							}
						}
					}
					*/
                        try {
                            getFaultcameradata();
                            camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG);
                        } catch (final IllegalArgumentException e) {
                            if (DEBUG) Log.i(TAG, "setPreviewSize Fail");
                            camera.destroy();
                            return;
                        }
                        final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
                        if (st != null) {
                            mPreviewSurface = new Surface(st);
                            camera.setPreviewDisplay(mPreviewSurface);
                            camera.startPreview();
                            camera.setFrameCallback(mIFrameCallback, camera.PIXEL_FORMAT_RGBX);
                            camera.setAudioCallback(mIAudioCallback);
                        }
                        synchronized (mSync) {
                            mUVCCamera = camera;
                        }
                    }
                }
            }, 0);
        }

        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
            // XXX you should check whether the comming device equal to camera device that currently using
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    synchronized (mSync) {
                        if (mUVCCamera != null) {
                            mUVCCamera.close();
                        }
                    }
                    if (mPreviewSurface != null) {
                        mPreviewSurface.release();
                        mPreviewSurface = null;
                    }
                }
            }, 0);
            //	setCameraButton(false);huansen
        }

        @Override
        public void onDettach(final UsbDevice device) {
            allowedvideo = false;
            if (mCaptureState != CAPTURE_STOP) {
                mHandler.sendEmptyMessage(UVC_STOP_CAPTURE);
                showToast("摄像头已异常断开");
            }
            // Toast.makeText(MainActivity.this, "摄像头已移除", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(final UsbDevice device) {
            //	setCameraButton(false);huansen
        }
    };

    private void getFaultcameradata() {
        SharedPreferences sharedata = getSharedPreferences("data", 0);
        String data = PreferencesUtils.getString(this, SettingActivity.FBL_KEY, "480x640");// sharedata.getString("usbcamerafenbianlv", null);
        if (data.equalsIgnoreCase("480x640")) {
            setUvcCameraResolution(1);
        } else if (data.equalsIgnoreCase("768x1024")) {
            setUvcCameraResolution(2);
        } else if (data.equalsIgnoreCase("720x1280")) {
            setUvcCameraResolution(3);
        } else if (data.equalsIgnoreCase("1080x1920")) {
            setUvcCameraResolution(4);
        } else if (data.equalsIgnoreCase("1200x1600")) {
            setUvcCameraResolution(5);
        }
    }

    private void getStorgeIsSDcard() {
        SharedPreferences sharedata1 = getSharedPreferences("data", 0);
        String data1 = PreferencesUtils.getString(this, SettingActivity.SDstorge_KEY, "话机");
        if (data1.equalsIgnoreCase("话机")) {
            MediaMuxerWrapper.isSaveSDCard = false;
        }
        if (data1.equalsIgnoreCase("SD卡")) {
            if (existSDCard() == true)
                MediaMuxerWrapper.isSaveSDCard = true;
            else
                MediaMuxerWrapper.isSaveSDCard = false;
        }
    }

    private boolean existSDCard() {//判断SD是否移除
        if (getSdCardWriteableState(this, External_Storage).equals(
                android.os.Environment.MEDIA_MOUNTED)) {
            return true;
        } else
            return false;
    }

    private void setUvcCameraResolution(int preview_resolution) {
        switch (preview_resolution) {
            case 1: {
                UVCCamera.DEFAULT_PREVIEW_WIDTH = 640;
                UVCCamera.DEFAULT_PREVIEW_HEIGHT = 480;
                break;
            }
            case 2: {
                UVCCamera.DEFAULT_PREVIEW_WIDTH = 1024;
                UVCCamera.DEFAULT_PREVIEW_HEIGHT = 768;
                break;
            }
            case 3: {
                UVCCamera.DEFAULT_PREVIEW_WIDTH = 1280;
                UVCCamera.DEFAULT_PREVIEW_HEIGHT = 720;
                break;
            }
            case 4: {
                UVCCamera.DEFAULT_PREVIEW_WIDTH = 1920;
                UVCCamera.DEFAULT_PREVIEW_HEIGHT = 1080;
                break;
            }
            case 5: {
                UVCCamera.DEFAULT_PREVIEW_WIDTH = 1600;
                UVCCamera.DEFAULT_PREVIEW_HEIGHT = 1200;
                break;
            }
            default: {
                break;
            }
            //END
        }

    }

    private final IFrameCallback mIFrameCallback = new IFrameCallback() {
        private int flag;

        @Override
        public void onFrame(final ByteBuffer frame) {
          /*  if (mUVCCamera.checkSupportFlag(UVCCamera.PU_BACKLIGHT)) {
                 switch (mUVCCamera.getBacklightComp()) {
                    case 1:
                        Log.e(TAG, "mIFrameCallback2...... ");
                        if (flag != 1) {
                            flag = 1;
                            mUVCCamera.resetBacklightComp();
                            frame.clear();
                            if (mCaptureState == CAPTURE_STOP) {
                                mHandler.sendEmptyMessage(UVC_TAKE_PICTURE);
                            }
                        }
                        break;
                    case 2:
                        Log.e(TAG, "mIFrameCallback3...... ");
                        if (flag != 2) {
                            flag = 2;
                            mUVCCamera.resetBacklightComp();
                            if (allowedvideo == true) {
                                if (mCaptureState == CAPTURE_STOP) {
                                    mHandler.sendEmptyMessage(UVC_START_CAPTURE);
                                } else {
                                    mHandler.sendEmptyMessage(UVC_STOP_CAPTURE);
                                }
                            }
                        }
                        break;
                    default:
                        Log.e(TAG, "mIFrameCallback4...... ");
                        if (flag != 0) {
                            flag = 0;
                        }
                        break;
                }
            }*/
            if (isTakePicture) {
                isTakePicture = false;
                mSoundPool.play(mRefocusSound, 1.0f, 1.0f, 0, 0, 1.0f);
                takePicture(frame);
            }
            /*
            int r = mUVCCamera.getExtensionCustomerValue();
			if (r == 1)
				Log.i(TAG, "ExtensionCustomerValue " + r);
			*/
        }
    };

    protected void showToast(String msg) {
        if (mToast == null) {
            mToast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(msg);
        }
        mToast.show();
    }

    private final IFrameCallback mIAudioCallback = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer pcmData) {
            /*
            if (out == null) {
				String folderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Download";
				try {
					out = new FileOutputStream(new File(folderPath, "usbaudio.pcm"), true);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}

			}
			if (out != null) {
				byte[] pcm = new byte[pcmData.remaining()];
				pcmData.get(pcm);
				try {
					out.write(pcm);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			*/
            //Log.i(TAG, "get pcm bytes " + pcmData.capacity());
        }

        public FileOutputStream out = null;
    };

    private void setCameraButton(final boolean isOn) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mCameraButton != null) {
                    try {
                        mCameraButton.setOnCheckedChangeListener(null);
                        mCameraButton.setChecked(isOn);
                    } finally {
                        mCameraButton.setOnCheckedChangeListener(mOnCheckedChangeListener);
                    }
                }
                if (!isOn && (mCaptureButton != null)) {
                    mCaptureButton.setVisibility(View.INVISIBLE);
                }
            }
        }, 0);
    }

    //**********************************************************************
    private final SurfaceTextureListener mSurfaceTextureListener = new SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(final SurfaceTexture surface, final int width, final int height) {
        }

        @Override
        public void onSurfaceTextureSizeChanged(final SurfaceTexture surface, final int width, final int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface) {
            if (mPreviewSurface != null) {
                mPreviewSurface.release();
                mPreviewSurface = null;
            }
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(final SurfaceTexture surface) {
            if (mVideoEncoder != null && mCaptureState == CAPTURE_RUNNING) {
                mVideoEncoder.frameAvailableSoon();
            }
            /*
            if (mEncoder != null && mCaptureState == CAPTURE_RUNNING) {
				mEncoder.frameAvailable();
			}
			*/
        }
    };

    private Bitmap getImageThumbnail(String imagePath, int width, int height) {
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        options.inJustDecodeBounds = false;

        int h = options.outHeight;
        int w = options.outWidth;
        int beWidth = w / width;
        int beHeight = h / height;
        int be = 1;
        if (beWidth < beHeight) {
            be = beWidth;
        } else {
            be = beHeight;
        }
        if (be <= 0) {
            be = 1;
        }
        options.inSampleSize = be;
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }


    private Bitmap getVideoThumbnail(String videoPath, int width, int height,
                                     int kind) {
        Bitmap bitmap = null;
        bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, kind);
        System.out.println("w" + bitmap.getWidth());
        System.out.println("h" + bitmap.getHeight());
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

    private final void takePicture(final ByteBuffer frame) {
        /*
            ByteBuffer raw = ByteBuffer.allocate(frame.capacity());
			frame.rewind();
			raw.put(frame);
			mRawFrameData = raw;
		*/
        if (frame != null && frame.remaining() > 0) {
            try {
                final String path = getCaptureFile(Environment.DIRECTORY_DCIM, ".jpeg");
                byte[] rawData = new byte[frame.remaining()];
                frame.get(rawData);
                Log.i(TAG, "get picture frame: " + rawData.length);
                byte[] jpgData = mUVCCamera.extensionCompressRgbx(rawData, mUVCCamera.DEFAULT_PREVIEW_WIDTH, mUVCCamera.DEFAULT_PREVIEW_HEIGHT, 90);
                Log.i(TAG, "compress picture " + jpgData.length);
                FileOutputStream out = null;
                out = new FileOutputStream(new File(path), true);
                imagePath = path;
                out.write(jpgData);
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private MediaMuxerWrapper mMuxer = null;
    private MediaVideoEncoder mVideoEncoder = null;
    private MediaAudioEncoder mAudioEncoder = null;

    private final void startCapture() {
        if (DEBUG) Log.v(TAG, "startCapture:");
        if (mMuxer == null && (mCaptureState == CAPTURE_STOP)) {
            mCaptureState = CAPTURE_PREPARE;
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    try {
                        //showToast("录像开始");
                        mMuxer = new MediaMuxerWrapper(".mp4");    // if you record audio only, ".m4a" is also OK.
                        if (true) {
                            mVideoEncoder = new MediaVideoEncoder(mMuxer, mMediaEncoderListener, mUVCCamera.DEFAULT_PREVIEW_WIDTH, mUVCCamera.DEFAULT_PREVIEW_HEIGHT);
                        }
                        if (true) {
                            mAudioEncoder = new MediaAudioEncoder(mMuxer, mMediaEncoderListener);
                        }
                        mMuxer.prepare();
                        mMuxer.startRecording();
                        //isTakePicture = true;
                    } catch (final IOException e) {
                        mCaptureState = CAPTURE_STOP;
                    }
                }
            }, 0);
            //updateItems();
        }
    }

    private final void stopCapture() {
        if (DEBUG) Log.v(TAG, "stopCapture:");
        queueEvent(new Runnable() {
            @Override
            public void run() {
                synchronized (mSync) {
                    if (mUVCCamera != null) {
                        mUVCCamera.stopCapture();
                    }
                }
                if (mMuxer != null) {
                    mMuxer.stopRecording();
                    mMuxer = null;
                }
            }
        }, 0);
    }
    /*
    private Encoder mEncoder;
	private final void startCapture() {
		if (DEBUG) Log.v(TAG, "startCapture:");
		if (mEncoder == null && (mCaptureState == CAPTURE_STOP)) {
			mCaptureState = CAPTURE_PREPARE;
			queueEvent(new Runnable() {
				@Override
				public void run() {
					final String path = getCaptureFile(Environment.DIRECTORY_DCIM, ".mp4");
					if (!TextUtils.isEmpty(path)) {
						mEncoder = new SurfaceEncoder(path);
						mEncoder.setEncodeListener(mEncodeListener);
						try {
							mEncoder.prepare();
							mEncoder.startRecording();
						} catch (final IOException e) {
							mCaptureState = CAPTURE_STOP;
						}
					} else
						throw new RuntimeException("Failed to start capture.");
				}
			}, 0);
			updateItems();
		}
	}
	private final void stopCapture() {
		if (DEBUG) Log.v(TAG, "stopCapture:");
		queueEvent(new Runnable() {
			@Override
			public void run() {
				synchronized (mSync) {
					if (mUVCCamera != null) {
						mUVCCamera.stopCapture();
					}
				}
				if (mEncoder != null) {
					mEncoder.stopRecording();
					mEncoder = null;
				}
			}
		}, 0);
	}
	*/
    /**
     * callbackds from Encoder
     */
    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onPreapared:");
            if (encoder instanceof MediaVideoEncoder) {
                synchronized (mSync) {
                    if (mUVCCamera != null) {
                        mUVCCamera.startCapture(mVideoEncoder.getInputSurface());
                    }
                }
                mCaptureState = CAPTURE_RUNNING;
            }
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onRelease:");
            if (encoder instanceof MediaVideoEncoder) {
                synchronized (mSync) {
                    if (mUVCCamera != null) {
                        mUVCCamera.stopCapture();
                    }
                }
                mCaptureState = CAPTURE_STOP;
                //	updateItems();
            }
        }
    };

    /*
        private final EncodeListener mEncodeListener = new EncodeListener() {
            @Override
            public void onPreapared(final Encoder encoder) {
                if (DEBUG) Log.v(TAG, "onPreapared:");
                synchronized (mSync) {
                    if (mUVCCamera != null) {
                        mUVCCamera.startCapture(((SurfaceEncoder)encoder).getInputSurface());
                    }
                }
                mCaptureState = CAPTURE_RUNNING;
            }

            @Override
            public void onRelease(final Encoder encoder) {
                if (DEBUG) Log.v(TAG, "onRelease:");
                synchronized (mSync) {
                    if (mUVCCamera != null) {
                        mUVCCamera.stopCapture();
                    }
                }
                mCaptureState = CAPTURE_STOP;
                updateItems();
            }
        };
    */
    private void updateItems() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCaptureButton.setVisibility(mCameraButton.isChecked() ? View.VISIBLE : View.INVISIBLE);
                mCaptureButton.setColorFilter(mCaptureState == CAPTURE_STOP ? 0 : 0xffff0000);
            }
        });
    }

    private final String getCaptureFile(final String type, final String ext) {
        if (MediaMuxerWrapper.isSaveSDCard == false) {
            final File dir = new File(Environment.getExternalStoragePublicDirectory(type), "USBCamera");
            dir.mkdirs();    // create directories if they do not exist
            if (dir.canWrite()) {
                return (new File(dir, getDateTimeString() + ext)).toString();
            }
            return null;
        } else {
            final File dir = new File(External_Storage, "/DCIM/USBCamera");
            dir.mkdirs();      // create directories if they do not exist
            if (dir.canWrite()) {
                return (new File(dir, getDateTimeString() + ext)).toString();
            }
            return null;
        }
    }

    /******
     * 获取SD卡挂载的根目录
     *
     * @param mContext
     * @param is_removale
     * @return
     */
    public static String getStoragePath(Context mContext, boolean is_removale) {

        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                if (is_removale == removable) {
                    Log.v(TAG, "SD路径" + path);
                    return path;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * get current date and time as String
     *
     * @return
     */
    private static final SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);

    private static final String getDateTimeString() {
        final GregorianCalendar now = new GregorianCalendar();
        return mDateTimeFormat.format(now.getTime());
    }

    /*********************************
     * 判断SD卡的状态
     *
     * @param context
     * @param path
     * @return
     */
    public static String getSdCardWriteableState(Context context, String path) {
        Class<?> storageVolumeClazz = null;
        try {
            StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeStateMethod = StorageManager.class.getMethod("getVolumeState", new Class[]{String.class});
            String state = (String) getVolumeStateMethod.invoke(sm, path);
            return state;
        } catch (Exception e) {
            Log.e(TAG, "getSdCardWriteableState() failed", e);
        }
        return null;
    }

    /**
     * 接收关机广播类
     * 如果正在录像，就停止录像
     */
    public class ShutdownBroadcastReceiver extends BroadcastReceiver {

        private static final String ACTION_SHUTDOWN = "android.intent.action.ACTION_SHUTDOWN";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Shut down this system, ShutdownBroadcastReceiver onReceive()");

            if (intent.getAction().equals(ACTION_SHUTDOWN)) {
                Log.i(TAG, "ShutdownBroadcastReceiver onReceive(), Do thing!");
                if (mCaptureState != CAPTURE_STOP) {
                    stopCapture();
                }
            }
        }
    }

    /**
     * 获取可用内存
     * getAvailableInternalMemorySize
     *
     * @param context
     * @return long  返回可用块大小
     * /*package
     */
    public Long getAvailableInternalMemorySize(Context context) {
        File file = Environment.getDataDirectory();
        StatFs statFs = new StatFs(file.getPath());
        long availableBlocksLong = statFs.getAvailableBlocksLong();//3107289  //110080=430M
        long blockSizeLong = statFs.getBlockSizeLong();//4K 块大小
        //return Formatter.formatFileSize(context, availableBlocksLong
        //      * blockSizeLong);
        return availableBlocksLong;
    }

    /**
     * 获取SD卡可用内存
     * getAvailableInternalMemorySize
     *
     * @param context
     * @return long  返回可用块大小
     * /*package
     */
    public Long getAvailableExternalMemorySize(Context context) {
        long ret = 0;
        if (MediaMuxerWrapper.isSaveSDCard == true) {
            StatFs statFs = new StatFs(getStoragePath(context, true));
            long availableBlocksLong = statFs.getAvailableBlocksLong();
            long blockSizeLong = statFs.getBlockSizeLong();
            //return Formatter.formatFileSize(context, availableBlocksLong
            //      * blockSizeLong);  //已G为单位
            return availableBlocksLong * blockSizeLong; //450887680  430M
        } else
            return ret;
    }

    class MyCrashHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, final Throwable throwable) {
            // Deal this exception
            // if (mCaptureState != CAPTURE_STOP) {
            //     stopCapture();
            //  }
            Log.e("huansen", "崩溃了)");

        }
    }
}