package com.camera.showphoto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.File;

/**
 * Created by cheng.xianxiong on 2017/5/3.
 */

public class ImageUtils {
    private static String TAG = "phototest";

    /**
     *
     * @param oldBitmap bitmap对象
     * @param w 目标宽度
     * @param h 目标高度
     * @return  拥有目标宽度与目标高度的图片，图片可能会变形
     */
    public static Bitmap getSmallBitmap(Bitmap oldBitmap , int w , int h){
            Bitmap newBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(newBitmap);
            RectF r1 = new RectF(0,0 ,w ,h);
            Rect r2 = new Rect(0,0, oldBitmap.getWidth(),oldBitmap.getHeight());
            canvas.drawBitmap(oldBitmap,r2,r1,new Paint());
            return newBitmap;
    }

    /**
     *
     * @param f 图片对应的文件
     * @param w 目标图片的最大宽度
     * @param h  目标图片的最大高度
     * @return   返回的图片在保证不变形的情况下，高度不大于@param h，宽度不大于@param w
     */
    public static Bitmap getGoodBitmap(File f , int w , int h){
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(),option);
        int wSzie = option.outWidth / w;
        int hSize = option.outHeight / h;

        int inSampleSize = wSzie > hSize? wSzie:hSize;
        if(inSampleSize < 1){
            inSampleSize = 1;
        }
        option.inSampleSize = inSampleSize;
        option.inJustDecodeBounds = false;

        Bitmap bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(),option);

        return bitmap;
    }

    /**
     *
     * @param path 文件路径
     * @param maxSize 返回bitmap最大的尺寸  单位兆
     * @return bitmap 返回的图片被缩小了，但是图片没有变形
     */
    public static Bitmap getBitmapFromFile(File path , float maxSize){
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path.getAbsolutePath(),option);

        int size = option.outHeight * option.outWidth;
        Log.e(TAG,"bitmapsize is " + size);
        int inSampleSize = (int) (size / (maxSize * 1024 * 1024));
        if(inSampleSize < 1){
            inSampleSize = 1;
        }
        option.inSampleSize = inSampleSize;//为长宽缩小的倍数，图片实际上应该是缩小了inSampleSize*inSampleSize倍
        option.inJustDecodeBounds = false;

        Bitmap bitmap = BitmapFactory.decodeFile(path.getAbsolutePath(),option);

        return bitmap;
    }

    /**
     *
     * @return 屏幕尺寸,第一个为宽度，第二个为高度
     */
    public static int[] getScreenSize(Context context){
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        int width = wm.getDefaultDisplay().getWidth();
        int height = wm.getDefaultDisplay().getHeight();
        int[] size = new int[2];
        size[0] = width;
        size[1] = height;
        return size;
    }


}
