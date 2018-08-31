package com.wbkj.faceidentify.picture;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 图片裁剪类
 * 调用方法 HeadTrim.corpPicture(this, Uri.fromFile(new File(fileSrc)));
 */

public class HeadTrim {
    public final static int REQUEST_PICTURE_CHOOSE = 1;
    public final static int REQUEST_CAMERA_IMAGE = 2;
    public final static int REQUEST_CROP_IMAGE = 3;

    public static void corpPicture(Activity activity, Uri uri){
        Intent innerIntent = new Intent("com.android.camera.action.CROP");//启用android自带的裁剪
        innerIntent.setDataAndType(uri,"image/*");
        innerIntent.putExtra("crop","true");//裁剪小正方形，不然没有裁剪功能，只能选取
        innerIntent.putExtra("aspectX",1);//放大缩小缩放比例
        innerIntent.putExtra("aspectY",1);//缩放比例
        innerIntent.putExtra("outputX",320);
        innerIntent.putExtra("outputY",320);//图片大小
        innerIntent.putExtra("return-data",true);

        //切图大小不足 320 会出现黑框，防止出现黑框并输出
        innerIntent.putExtra("scale",true);
        innerIntent.putExtra("scaleUpIfNeeded",true);
        File imageFile = new File(getImagePath(activity.getApplicationContext()));//从这个文件路径创建文件
        innerIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());//带上裁剪好的图片传递出去
        activity.startActivityForResult(innerIntent,REQUEST_CROP_IMAGE);
    }

    public static String getImagePath(Context context){
        String path;
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            path = context.getFilesDir().getAbsolutePath();
        }else {
            path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/msc/";
        }
        if (!path.endsWith("/")){
            path+="/";
        }
        File folder = new File(path);
        if (folder != null && !folder.exists()){
            folder.mkdirs();
        }
        path += "ifd.jpg";
        return path;
    }

    public static int readPictureDegree(String path){
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);//阅读和写作在JPEG文件的Exif标记类
            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);//方向常规
            switch (orientation){
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return degree;
    }

    /**
     * 旋转图片
     */
    public static Bitmap rotateImage(int angle,Bitmap bitmap){
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
        /**
         * createBitmap  返回一个不可变的位图从源位图的子集
         * @param source   The bitmap we are subsetting
         * @param x        The x coordinate of the first pixel in source
         * @param y        The y coordinate of the first pixel in source
         * @param width    The number of pixels in each row
         * @param height   The number of rows
         * @param m        Optional matrix to be applied to the pixels   可选的矩阵应用到像素
         * @param filter   true if the source should be filtered.        如果源应筛选，true
         *                   Only applies if the matrix contains more than just
         *                   translation.
         */
        return resizedBitmap;
    }

    /**
     * 保存图片
     */
    public static void saveBitmapToFile(Context context,Bitmap bitmap){
        String file_path = getImagePath(context);
        File file = new File(file_path);
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
