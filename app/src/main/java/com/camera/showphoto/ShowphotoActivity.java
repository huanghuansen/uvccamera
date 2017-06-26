package com.camera.showphoto;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import com.camera.encoder.MediaMuxerWrapper;
import com.camera.usbcam.MainActivity;
import com.camera.usbcam.R;

import java.io.File;

public class ShowphotoActivity extends FragmentActivity {
    public static File[] photos;
    private FragmentTransaction fragAction;
    private PhotoListFragment photoListFragment;
    private PhotoVpFragment photoVpFragment;

    String TAG = " ShowphotoActivity";
    boolean isShowPhotoVp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_showphoto);
        initData();
        initView();
    }

    private void initView() {
        fragAction = getSupportFragmentManager().beginTransaction();
        photoListFragment = new PhotoListFragment();
        photoVpFragment = new PhotoVpFragment();
        fragAction.add(R.id.frame, photoListFragment);
        fragAction.add(R.id.frame, photoVpFragment);
        fragAction.show(photoListFragment);
        fragAction.hide(photoVpFragment);
        fragAction.commit();
    }

    private void initData() {
        if(MediaMuxerWrapper.isSaveSDCard ==false){
            String photoPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "DCIM/USBCamera";
            File f = new File(photoPath);
            photos = f.listFiles();
        }
        else {
            String photoPath = MainActivity.External_Storage + File.separator + "DCIM/USBCamera";
            File f = new File(photoPath);
            photos = f.listFiles();
        }
    }

    /**
     *
     * @param position 需要展示的图片所处的position
     */
    public void showPhotoVp(int position) {
        fragAction = getSupportFragmentManager().beginTransaction();
        fragAction.show(photoVpFragment);
        // fragAction.addToBackStack(frgActionName);
        fragAction.commit();
        photoVpFragment.onShow(position);
        isShowPhotoVp = true;
    }

    /**
     * 退出单独展示照片的Fragment，并与界面解除绑定
     */
    public void hidePhotoVp(){
        fragAction = getSupportFragmentManager().beginTransaction();
        fragAction.hide(photoVpFragment);
        fragAction.remove(photoVpFragment);
        photoVpFragment = new PhotoVpFragment();
        fragAction.add(R.id.frame , photoVpFragment);
        fragAction.hide(photoVpFragment);
        fragAction.commit();
        isShowPhotoVp = false;
    }
    @Override
    public void onBackPressed() {
        if (isShowPhotoVp) {
            photoVpFragment.onHide();
            return;
        }
        super.onBackPressed();
    }
}