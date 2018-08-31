package com.wbkj.faceidentify;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.FaceRequest;
import com.iflytek.cloud.IdentityListener;
import com.iflytek.cloud.IdentityResult;
import com.iflytek.cloud.IdentityVerifier;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RequestListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechUtility;
import com.wbkj.faceidentify.picture.HeadTrim;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    private String TAG = getClass().getCanonicalName();
    private final int REQUEST_PICTURE_CHOOSE = 1;
    private final int REQUEST_CAMERA_IMAGES = 2;
    private FaceRequest faceRequest;
    private Toast toast;
    private byte[] imageData = null;
    private Bitmap image = null;
    private EditText editTextUserPassword;
    private String UserPassword;
    private ProgressDialog progressDialog;
    private File pictureFile;
    private IdentityVerifier mIdVerifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // android 7.0系统解决拍照的问题
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();
        requestPermissions();
//        init();
    }

    private void init() {
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        SpeechUtility.createUtility(this, "appid="+"讯飞申请的APPID");//初始化SDK 千万不能忘记写appid后边的等号  id是自己其官网上申请的
        faceRequest = new FaceRequest(this);

        mIdVerifier = IdentityVerifier.createVerifier(this, new InitListener() {
            @Override
            public void onInit(int i) {

            }
        });

        initView();
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(true);
        progressDialog.setTitle("请稍后...");
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (null != faceRequest) {
                    faceRequest.cancel();
                }
            }
        });
    }

    private void initView() {
        editTextUserPassword = (EditText)findViewById(R.id.editTextUserPassword);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonPicture:
                pictureFile = new File(Environment.getExternalStorageDirectory(),
                        "picture" + System.currentTimeMillis() / 1000 + ".jpg");
                Intent picIntent = new Intent();
                picIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

                picIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(pictureFile));
                picIntent.putExtra(MediaStore.Images.Media.ORIENTATION, 0);
                startActivityForResult(picIntent, REQUEST_CAMERA_IMAGES);
                break;
            case R.id.buttonRegister:
                UserPassword = ((EditText) findViewById(R.id.editTextUserPassword)).getText().toString();
                if (TextUtils.isEmpty(UserPassword)) {
                    showToast("请输入您的密钥");
                    return;
                } else if (null != imageData) {
                    progressDialog.setMessage("注册中,请稍后...");
                    progressDialog.show();
                    faceRequest.setParameter(SpeechConstant.AUTH_ID, UserPassword);//将授权标识和密码上传服务器记录
                    faceRequest.setParameter(SpeechConstant.WFR_SST, "reg");//业务类型train Or verify
                    faceRequest.sendRequest(imageData, requestListener);
                } else showToast("请先进行图像拍摄...");
                break;
            case R.id.buttonVerification:
                UserPassword = ((EditText) findViewById(R.id.editTextUserPassword)).getText().toString();
                if (TextUtils.isEmpty(UserPassword)) {
                    showToast("密钥不能为空哦...");
                } else if (imageData != null) {
                    progressDialog.setMessage("验证中,请稍等...");
                    progressDialog.show();//6--12字符 不能以数字开头
                    faceRequest.setParameter(SpeechConstant.AUTH_ID, UserPassword);
                    faceRequest.setParameter(SpeechConstant.WFR_SST, "verify");
                    faceRequest.sendRequest(imageData, requestListener);
                } else showToast("请先捕捉头像...");
                break;
            case R.id.buttonDetect:
                UserPassword = ((EditText) findViewById(R.id.editTextUserPassword)).getText().toString();
                if (TextUtils.isEmpty(UserPassword)) {
                    showToast("密钥不能为空哦...");
                } else if (imageData != null) {
                    progressDialog.setMessage("验证中,请稍等...");
                    progressDialog.show();//6--12字符 不能以数字开头
                    faceRequest.setParameter(SpeechConstant.AUTH_ID, UserPassword);
                    faceRequest.setParameter(SpeechConstant.WFR_SST,"detect");
                    faceRequest.sendRequest(imageData,requestListener);
                    /*faceRequest.setParameter(SpeechConstant.WFR_SST, "delete");
                    faceRequest.sendRequest(imageData, requestListener);*/
                } else showToast("请先捕捉头像...");
                break;
            case R.id.buttonPicture1:
                pictureFile = new File(Environment.getExternalStorageDirectory(),
                        "picture" + System.currentTimeMillis() / 1000 + ".jpg");
                Intent picIntent1 = new Intent();
                picIntent1.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

                picIntent1.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(pictureFile));
                picIntent1.putExtra(MediaStore.Images.Media.ORIENTATION, 0);
                startActivityForResult(picIntent1, REQUEST_CAMERA_IMAGES);
                break;
            case R.id.buttonRegister1:
                UserPassword = ((EditText) findViewById(R.id.editTextUserPassword)).getText().toString();
                if (TextUtils.isEmpty(UserPassword)) {
                    showToast("请输入您的密钥");
                    return;
                } else if (null != imageData) {
                    progressDialog.setMessage("注册中,请稍后...");
                    progressDialog.show();
                    mIdVerifier.setParameter(SpeechConstant.MFV_SCENES,"ifr");
                    mIdVerifier.setParameter(SpeechConstant.MFV_SST,"enroll");
                    mIdVerifier.setParameter(SpeechConstant.AUTH_ID,UserPassword);
                    mIdVerifier.startWorking(identityListener);
                    // 子业务执行参数，若无可以传空字符传
                    StringBuffer params = new StringBuffer();
                    mIdVerifier.writeData("ifr",params.toString(),imageData,0,imageData.length);
                    mIdVerifier.stopWrite("ifr");
                } else showToast("请先进行图像拍摄...");
                break;
            case R.id.buttonVerification1:
                UserPassword = ((EditText) findViewById(R.id.editTextUserPassword)).getText().toString();
                if (TextUtils.isEmpty(UserPassword)) {
                    showToast("密钥不能为空哦...");
                } else if (imageData != null) {
                    progressDialog.setMessage("验证中,请稍等...");
                    mIdVerifier.setParameter(SpeechConstant.MFV_SCENES,"ifr");
                    mIdVerifier.setParameter(SpeechConstant.MFV_SST,"verify");
                    mIdVerifier.setParameter(SpeechConstant.MFV_VCM,"sin");
                    mIdVerifier.setParameter(SpeechConstant.AUTH_ID,UserPassword);
                    mIdVerifier.startWorking(identityListener);
                    StringBuffer params = new StringBuffer();
                    mIdVerifier.writeData("ifr",params.toString(),imageData,0,imageData.length);
                    mIdVerifier.stopWrite("ifr");
                } else showToast("请先捕捉头像...");
                break;
            case R.id.buttonDetect1:
                UserPassword = ((EditText) findViewById(R.id.editTextUserPassword)).getText().toString();
                if (TextUtils.isEmpty(UserPassword)) {
                    showToast("密钥不能为空哦...");
                } else{
                    progressDialog.setMessage("删除中,请稍等...");
                    progressDialog.show();//6--12字符 不能以数字开头
                    mIdVerifier.setParameter(SpeechConstant.MFV_SCENES,"ifr");
                    mIdVerifier.setParameter(SpeechConstant.AUTH_ID,UserPassword);
                    StringBuffer params = new StringBuffer();
                    mIdVerifier.execute("ifr","delete",params.toString(),identityListener);
                }
                break;
        }
    }

    /**
     * 处理拍完照后，跳转到裁剪界面
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "运行到了onActivityResult");
        if(resultCode != RESULT_OK){
            Log.i(TAG,"requestCode未成功");
            return;
        }
        String fileSrc = null;
        if(requestCode == REQUEST_PICTURE_CHOOSE){
            if("file".equals(data.getData().getScheme())){
                fileSrc = data.getData().getPath();
                Log.i(TAG,"file "+fileSrc);
            }else {
                String[] proj = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(data.getData(), proj, null, null, null);
                cursor.moveToFirst();
                int idx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                fileSrc = cursor.getString(idx);
                cursor.close();
            }
            HeadTrim.corpPicture(this, Uri.fromFile(new File(fileSrc)));
        }else if(requestCode == REQUEST_CAMERA_IMAGES){
            if(null == pictureFile){
                showToast("拍照失败，请重试...");
                return;
            }
            fileSrc = pictureFile.getAbsolutePath();
            updataGallery(fileSrc);
            HeadTrim.corpPicture(this,Uri.fromFile(new File(fileSrc)));
        }else if(requestCode == HeadTrim.REQUEST_CROP_IMAGE){
            Bitmap bitmap = data.getParcelableExtra("data");
            Log.i(TAG,"bitmp是否为空");
            if(null != bitmap){
                HeadTrim.saveBitmapToFile(MainActivity.this,bitmap);
            }
            fileSrc = HeadTrim.getImagePath(MainActivity.this);//获取图片保存路径
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            image = BitmapFactory.decodeFile(fileSrc,options);
            options.inSampleSize = Math.max(1, (int) Math.ceil(Math.max(
                    (double) options.outWidth / 1024f,
                    (double) options.outHeight / 1024f
            )));
            options.inJustDecodeBounds = false;
            image = BitmapFactory.decodeFile(fileSrc,options);
            //如果imageBitmap 为空图片不能正常获取
            if(null == image){
                showToast("图片信息无法正常获取");
                return;
            }
            int degree = HeadTrim.readPictureDegree(fileSrc);
            if(degree != 0){
                image = HeadTrim.rotateImage(degree,image);
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG,80,byteArrayOutputStream);
            imageData = byteArrayOutputStream.toByteArray();
            ((ImageView)findViewById(R.id.imageViewHead)).setImageBitmap(image);
        }
    }

    @Override
    public void finish() {
        if(null != progressDialog){
            progressDialog.dismiss();
        }
        super.finish();
    }

    private void updataGallery(String fileName) {
        MediaScannerConnection.scanFile(this, new String[]{fileName}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                    }
                });
    }

    private void showToast(String s) {
        Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
    }

    /**
     * 请求对象监听  ， 对服务器返回来的数据进行解析  JSON格式
     */

    private RequestListener requestListener = new RequestListener() {
        @Override
        public void onEvent(int i, Bundle bundle) {
        }
        @Override
        public void onBufferReceived(byte[] bytes) {
            if (null != progressDialog) {
                progressDialog.dismiss();
            }
            try {
                String result = new String(bytes, "utf-8");
                Log.i(TAG, result);
                JSONObject object = new JSONObject(result);
                String type = object.optString("sst");//获取业务类型
                if ("reg".equals(type)) {//注册
                    register(object);
                } else if ("verify".equals(type)) {//校验
                    verify(object);
                } else if ("detect".equals(type)) {
                    detect(object);
                } else if ("aligm".equals(type)) {
                    //align(object);
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCompleted(SpeechError speechError) {//完成后
            if(null != progressDialog){
                progressDialog.dismiss();
            }
            if(speechError != null ){
                switch (speechError.getErrorCode()){
                    case ErrorCode.MSP_ERROR_ALREADY_EXIST:
                        showToast("密钥已被注册，请更换后再试...");
                        break;
                    default:showToast(speechError.getPlainDescription(true));
                        break;
                }
            }
        }
    };

    private IdentityListener identityListener = new IdentityListener() {
        @Override
        public void onResult(IdentityResult identityResult, boolean b) {
            if (null != progressDialog && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            try {
                JSONObject object = new JSONObject(identityResult.getResultString());
                if (object.getString("ret").equals("0")){
                    if (object.getString("sst").equals("enroll")){
                        showToast("注册成功");
                    }else if (object.getString("sst").equals("verify")){
                        if (object.getString("decision").equals("accepted")){
                            showToast("验证通过");
                        }else {
                            showToast("验证失败");
                        }
                    }else if (object.getString("sst").equals("delete")){
                        showToast("用户删除成功");
                    }
                }else {
                    showToast("数据获取失败");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onError(SpeechError speechError) {
            if (null != progressDialog && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            showToast(speechError.toString());
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {
            /*if (null != progressDialog && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }*/
        }
    };

    //检测
    private void detect(JSONObject object) throws JSONException{
        int ret = object.getInt("ret");
        if(ret != 0){
            showToast("检测失败");
        }else if("success".equals(object.get("rst"))){
            JSONArray jsonArray = object.getJSONArray("face");
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStrokeWidth(Math.max(image.getWidth(), image.getHeight()) / 100f);
            Bitmap bitmap = Bitmap.createBitmap(image.getWidth(),image.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawBitmap(image,new Matrix(),null);
            for(int i = 0;i<jsonArray.length();i++){
                float x1 = (float) jsonArray.getJSONObject(i).getJSONObject("position").getDouble("left");
                float y1 = (float) jsonArray.getJSONObject(i).getJSONObject("position").getDouble("top");
                float x2 = (float) jsonArray.getJSONObject(i).getJSONObject("position").getDouble("right");
                float y2 = (float) jsonArray.getJSONObject(i).getJSONObject("position").getDouble("bottom");
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(new Rect((int)x1,(int)x2,(int)x2,(int)y2),paint);
            }
            image = bitmap;
            ((ImageView)findViewById(R.id.imageViewHead)).setImageBitmap(image);
        }else {
            showToast("检测失败");
        }
    }
    /**
     * 校验
     * @param object
     */
    private void verify(JSONObject object) throws JSONException{
        int ret = object.getInt("ret");
        Log.i(TAG,"ret校验"+ret);
        if(ret != 0){
            showToast("校验失败..."+ret);
        }else if("success".equals(object.get("rst"))){
            if(object.getBoolean("verf")){
                showToast("验证通过");
                editTextUserPassword.setText(null);
//                startActivity(new Intent(MainActivity.this,JumpActivity.class));
            }else if(!object.getBoolean("verf")){
                showToast("验证不通过");
            }else showToast("验证失败");
        }
    }
    /**
     * 如果收回的数据类型是注册 进行一下处理
     * @param object
     */
    private void register(JSONObject object) throws JSONException{
        int ret = object.getInt("ret");//解析ret返回值  0代表成功 -1失败  或者其他的错误异常代码
        if(ret != 0){
            showToast("注册失败");
            return;
        }else if("success".equals(object.get("rst"))){
            showToast("注册成功");
            editTextUserPassword.setText(null);
        }else showToast("注册失败，错误");
    }


    /**
     * 6.0请求危险权限
     */
    private void requestPermissions() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.CAMERA)) {
                init();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.CAMERA,
                                Manifest.permission.INTERNET
                        }, 1);
            }
        }else {
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode==1){
            if (grantResults[2] == PackageManager.PERMISSION_GRANTED){
                init();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
