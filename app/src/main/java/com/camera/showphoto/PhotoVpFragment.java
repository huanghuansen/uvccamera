package com.camera.showphoto;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

import com.camera.usbcam.R;

import java.util.ArrayList;

/**
 * Created by cheng.xianxiong on 2017/5/4.
 */

public class PhotoVpFragment extends Fragment implements View.OnClickListener{
    private String TAG = "PhotoVpFragment";

    private View rootView;
    private ViewPager vp;

    private ArrayList<View> viewCache = new ArrayList<View>();

    private View bgView;
    private int[] screenSize;
    private BitMapLoader loader;
    private MyAdapter adapter = new MyAdapter();
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        rootView = inflater.inflate(R.layout.fragment_photo_pager, null);
        initData();
        initView();
        return rootView;
    }

    private void initView() {
        bgView = rootView.findViewById(R.id.img_bg);
        vp = (ViewPager) rootView.findViewById(R.id.vp);
       vp.setAdapter(adapter);
    }

    private void initData() {
        screenSize = ImageUtils.getScreenSize(getActivity());
        loader = new BitMapLoader();
    }

    /**
     * 背景颜色的渐变。。
     * @param position
     */
    public void onShow(int position) {
        vp.setCurrentItem(position);
        //getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ScaleAnimation animation = new ScaleAnimation(0.3f, 1.0f, 0.3f, 1.0f,
                500f, 500f);
        animation.setDuration(800);//设置动画持续时间

        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
        alphaAnimation.setDuration(800);
        /*ImageView img = (ImageView) vp.getChildAt(vp.getCurrentItem()).getTag();
        img.startAnimation(animation);*/
        bgView.startAnimation(alphaAnimation);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void onHide(){
        ((ShowphotoActivity)getActivity()).hidePhotoVp();
        /*ScaleAnimation animation = new ScaleAnimation(1.0f,0.3f, 1.0f, 0.3f,
                500f, 500f);
        animation.setDuration(800);//设置动画持续时间
        AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
        alphaAnimation.setDuration(800);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                        ((MainActivity)getActivity()).hidePhotoVp();

               *//*for (int i = 0; i < rootViews.length; i++) {
                    vp.removeView(rootViews[i]);
                }*//*
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        if(vp.getCurrentItem() > 0){

        }
        quitImageView.startAnimation(animation);
        bgView.startAnimation(alphaAnimation);*/
    }


    @Override
    public void onClick(View v) {
        onHide();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        /**
         * 手动回收Bitmap对象
         */
        for(int i = 0; i<vp.getChildCount(); i++){
            ImageView img = (ImageView) vp.getChildAt(i).getTag();
            Bitmap bitmap = (Bitmap) img.getTag();
            if(bitmap != null && !bitmap.isRecycled())
                bitmap.recycle();
        }
    }

    private class MyAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return ShowphotoActivity.photos.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Log.e(TAG, "instantiateItem :" + position);
            View convertView;
            ImageView imgView = null;
            if(viewCache.size() == 0){
                convertView = LayoutInflater.from(getActivity()).inflate(R.layout.item_vp, null);
                imgView = (ImageView) convertView.findViewById(R.id.img);
                imgView.setOnClickListener(PhotoVpFragment.this);
                convertView.setTag(imgView);//复用convertView，且下次不需再次findViewById去拿ImageView；
            }else{
                convertView = viewCache.remove(0);
                imgView = (ImageView) convertView.getTag();
            }
            imgView.setId(position);
            loader.loadBigBitmap(ShowphotoActivity.photos[position] ,imgView , screenSize[0] , screenSize[1] , position);
            container.addView(convertView);

            return convertView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View convertView = (View) object;
            ImageView img = (ImageView) convertView.getTag();
            img.setImageBitmap(null);
            Bitmap a = (Bitmap) img.getTag();
            if(a!= null && !a.isRecycled()){
                a.recycle();//将该view上的图片及时回收掉
            }
            container.removeView(convertView);
            viewCache.add(convertView);
        }
    }
}
