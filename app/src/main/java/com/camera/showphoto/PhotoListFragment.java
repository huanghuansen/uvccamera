package com.camera.showphoto;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.camera.usbcam.R;

import java.io.File;


public class PhotoListFragment extends Fragment {
    private String TAG = "PhotoListFragment";

    private View rootView;
    private ShowphotoActivity  showphotoActivity;
    private GridView gridView;
    private int w;
    private BitMapLoader loader;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
         super.onCreateView(inflater, container, savedInstanceState);
         rootView = inflater.inflate(R.layout.fragment_photo_list,null);
        initData();
        initView();
        return rootView;
    }

    private void initView(){
        gridView = (GridView) rootView.findViewById(R.id.gridView);
        gridView.setAdapter(new MyAdapter());
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showphotoActivity.showPhotoVp(position);
            }
        });
    }

    private void initData(){
        showphotoActivity = (ShowphotoActivity) getActivity();
        w = ImageUtils.getScreenSize(getActivity())[0] / 4;
        loader = new BitMapLoader();

    }

    private class MyAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return ShowphotoActivity.photos.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.e(TAG,"this position is "+position);
            if(convertView == null){
                convertView = new ImageView(getActivity());
                convertView.setMinimumWidth(w);
                convertView.setMinimumHeight(w);//给ImageView设置一个最小的尺寸，避免gridView一次性加载所有的item
            }else{
                ((ImageView)convertView).setImageBitmap(null);
                Bitmap a = (Bitmap) convertView.getTag();
                if(a!= null && !a.isRecycled()){
                    a.recycle();//将该view上的图片及时回收掉
                }
            }
            convertView.setId(position);//加载图片为异步操作，若convertView因为复用导致id不一样，将不加载图片。
            loader.loadBitmap(ShowphotoActivity.photos[position],(ImageView)convertView,w,w,position);
            return convertView;
        }
    }
}
