package com.camera.videotest2;

import android.app.Activity;
import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.camera.encoder.MediaMuxerWrapper;
import com.camera.usbcam.MainActivity;
import com.camera.usbcam.R;
import com.camera.utils.PreferencesUtils;

import android.widget.AdapterView.OnItemClickListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VideotestActivity extends Activity {
    private String cur_path = "/storage/emulated/0/DCIM/USBCamera";
    private List<Picture> listPictures;
    ListView listView;
    String TAG = "huansen";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videotest);
        loadVaule();
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            if (msg.what == 0) {
                List<Picture> listPictures = (List<Picture>) msg.obj;
//				Toast.makeText(getApplicationContext(), "handle"+listPictures.size(), 1000).show();
                MyAdapter adapter = new MyAdapter(listPictures);
                listView.setAdapter(adapter);
            }
        }

    };

    private File getfilePath() {
        if (MediaMuxerWrapper.isSaveSDCard == true) {
            return new File(MainActivity.External_Storage, "/DCIM/USBCamera");
        } else {
            return new File(cur_path);
        }
    }

    private void loadVaule() {
        File file = getfilePath();
        File[] files = null;
        files = file.listFiles();
        listPictures = new ArrayList<Picture>();
        for (int i = 0; i < files.length; i++) {

            Picture picture = new Picture();
            if (files[i].getPath().toLowerCase().endsWith(".jpeg")) {
                picture.setBitmap(getImageThumbnail(files[i].getPath(), 200, 200));
                picture.setPath(files[i].getPath());
                picture.setName(files[i].getName());
            } else {
                picture.setBitmap(getVideoThumbnail(files[i].getPath(), 200, 200, MediaStore.Images.Thumbnails.MICRO_KIND));
                picture.setPath(files[i].getPath());
                picture.setName(files[i].getName());
            }
            listPictures.add(picture);

        }
        listView = (ListView) findViewById(R.id.lv_show);
        Message msg = new Message();
        msg.what = 0;
        msg.obj = listPictures;
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,

                                    long arg3) {

//int position =Integer.parseInt(arg1.getTag().toString());
                playVideo(listPictures.get(arg2).getPath());
                Log.e("path", listPictures.get(arg2).getPath());
            }
        });
        handler.sendMessage(msg);

    }


    //获取视频的缩略图
    private Bitmap getVideoThumbnail(String videoPath, int width, int height, int kind) {
        Bitmap bitmap = null;
        // 获取视频的缩略图
        bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, kind);
//		        System.out.println("w"+bitmap.getWidth());
//		        System.out.println("h"+bitmap.getHeight());
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

    //获取图片的缩略图
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

    public class MyAdapter extends BaseAdapter {
        private List<Picture> listPictures;

        public MyAdapter(List<Picture> listPictures) {
            super();
            this.listPictures = listPictures;

        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return listPictures.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return listPictures.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public View getView(int position, View v, ViewGroup arg2) {
            // TODO Auto-generated method stu
            View view = getLayoutInflater().inflate(R.layout.item, null);
            ImageView imageView = (ImageView) view.findViewById(R.id.iv_show);
            TextView textView = (TextView) view.findViewById(R.id.tv_show);

            imageView.setImageBitmap(listPictures.get(position).getBitmap());
            textView.setText(listPictures.get(position).getName());
            return view;

        }
    }

    //调用系统播放器   播放视频
    private void playVideo(String videoPath) {
//					   Intent intent = new Intent(Intent.ACTION_VIEW);
//					   String strend="";
//					   if(videoPath.toLowerCase().endsWith(".mp4")){
//						   strend="mp4";
//					   }
//					   else if(videoPath.toLowerCase().endsWith(".3gp")){
//						   strend="3gp";
//					   }
//					   else if(videoPath.toLowerCase().endsWith(".mov")){
//						   strend="mov";
//					   }
//					   else if(videoPath.toLowerCase().endsWith(".avi")){
//						   strend="avi";
//					   }
//					   intent.setDataAndType(Uri.parse(videoPath), "video/*");
//					   startActivity(intent);
        if (videoPath.toLowerCase().endsWith(".mp4")) {
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            File file = new File(videoPath);
            intent.setDataAndType(Uri.fromFile(file), "video/*");
            startActivity(intent);
        } else if (videoPath.toLowerCase().endsWith(".jpeg")) {
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            File file = new File(videoPath);
            intent.setDataAndType(Uri.fromFile(file), "image/*");
            startActivity(intent);
        }
    }

}
