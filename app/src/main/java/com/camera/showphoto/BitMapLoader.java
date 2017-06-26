package com.camera.showphoto;

import android.graphics.Bitmap;
import android.widget.ImageView;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by cheng.xianxiong on 2017/5/3.
 */

public class BitMapLoader {
    private ThreadPoolExecutor executor;
    private LinkedBlockingQueue sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>();

    public BitMapLoader(){
        executor = new ThreadPoolExecutor(5, 5, 1, TimeUnit.SECONDS,sPoolWorkQueue);
    }

public void stopTask(){
    executor.shutdown();
}
public void loadBigBitmap(final File path , final ImageView v,final int w , final int h ,final int position){
    {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                int position2 = v.getId();
                if(position2 != position)
                    return;

                final Bitmap b = ImageUtils.getGoodBitmap(path , w , h);
                if(b == null)
                    return;

                v.post(new Runnable() {
                    @Override
                    public void run() {
                        int position2 = (int)v.getId();
                        if(position2 != position)
                            return;
                        v.setImageBitmap(b);
                        v.setTag(b);
                    }
                });
            }
        });
    }
}

    /**
     * @param path 图片对应的File对象
     * @param v   加载图片的View
     * @param w   期望图片的宽度
     * @param h   期望图片的高度
     * @param position 此次加载的图片所处的position
     */
    public void loadBitmap(final File path , final ImageView v , final int w , final int h , final int position) {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                int position2 = (int)v.getId();   //若view的id变化过，则表示该view处于被下一次复用的状态，此次不加载图片。
                if(position2 != position)
                    return;

                Bitmap b = ImageUtils.getBitmapFromFile(path,0.3f); //最大拿到0.3兆的图片
                if(b == null)
                    return;
                final Bitmap smallBitmap = ImageUtils.getSmallBitmap(b,w,h);//获取与指定大小的图片

                if(b != null && !b.isRecycled()){
                    b.recycle();
                }

                v.post(new Runnable() {
                    @Override
                    public void run() {
                        int position2 = (int)v.getId();
                        if(position2 != position)
                            return;
                        v.setImageBitmap(smallBitmap);
                        v.setTag(smallBitmap);
                    }
                });
            }
        });
    }

}
