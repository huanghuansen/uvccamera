package com.camera.showvedio;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.camera.usbcam.R;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

import java.util.ArrayList;
import java.util.List;
@SuppressLint("ShowToast")
public class ShowvideoActivity extends Activity {
    private ListView mListView;
    private VideodetailListviewAdapter adapter;
    private List<BitmapEntity> bit = new ArrayList<BitmapEntity>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_showvideo);
        mListView = (ListView) this.findViewById(R.id.listview);
        initImageLoader(getApplicationContext());
        new Search_photo().start();
    }
    public static void initImageLoader(Context context) {
        // This configuration tuning is custom. You can tune every option, you may tune some of them,
        // or you can create default configuration by
        //  ImageLoaderConfiguration.createDefault(this);
        // method.
        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(context);
        config.threadPriority(Thread.NORM_PRIORITY - 2);
        config.denyCacheImageMultipleSizesInMemory();
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        config.diskCacheSize(50 * 1024 * 1024); // 50 MiB
        config.tasksProcessingOrder(QueueProcessingType.LIFO);
        config.writeDebugLogs(); // Remove for release app
        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config.build());

    }
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @SuppressLint("ShowToast")
        public void handleMessage(android.os.Message msg) {
            if (msg.what == 1&&bit!=null) {
                adapter = new VideodetailListviewAdapter(ShowvideoActivity.this, bit);
                //Toast.makeText(ShowvideoActivity.this, "视频总数:"+bit.size(),1);
                mListView.setAdapter(adapter);
                mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        if(bit.size()!=0){
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            String type = "video/mp4";
                            Uri uri = Uri.parse("file://"+bit.get(arg2).getUri());
                            intent.setDataAndType(uri, type);
                            startActivity(intent);
                        }
                    }
                });
            }
        };
    };
    /**
     *
     * 遍历系统数据库找出相应的是视频的信息，每找出一条视频信息的同时利用与之关联的找出对应缩略图的uri
     * 再异步加载缩略图，
     * 由于查询速度非常快，全部查找完成在设置，等待时间不会太长
     * @author Administrator
     *
     */
    class Search_photo extends Thread {
        @Override
        public void run() {
            // 如果有sd卡（外部存储卡）
            if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
                Uri originalUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = ShowvideoActivity.this.getApplicationContext().getContentResolver();
                Cursor cursor = cr.query(originalUri, null, null, null, null);
                if (cursor == null) {
                    return;
                }
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));
                    String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                    long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));
                    long duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
                    //获取当前Video对应的Id，然后根据该ID获取其缩略图的uri
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
                    String[] selectionArgs = new String[] { id + "" };
                    String[] thumbColumns = new String[] { MediaStore.Video.Thumbnails.DATA,
                            MediaStore.Video.Thumbnails.VIDEO_ID };
                    String selection = MediaStore.Video.Thumbnails.VIDEO_ID + "=?";

                    String uri_thumb = "";
                    Cursor thumbCursor = (ShowvideoActivity.this.getApplicationContext().getContentResolver()).query(
                            MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, thumbColumns, selection, selectionArgs,
                            null);
                    if (thumbCursor != null && thumbCursor.moveToFirst()) {
                        uri_thumb = thumbCursor
                                .getString(thumbCursor.getColumnIndexOrThrow(MediaStore.Video.Thumbnails.DATA));

                    }

                    BitmapEntity bitmapEntity = new BitmapEntity(title, path, size, uri_thumb, duration);

                    bit.add(bitmapEntity);

                }
                if (cursor != null) {
                    cursor.close();
                    mHandler.sendEmptyMessage(1);
                }
            }
        }
    }
}
