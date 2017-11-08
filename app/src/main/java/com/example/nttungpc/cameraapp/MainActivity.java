package com.example.nttungpc.cameraapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener {
    private static final String TAG = MainActivity.class.toString();
    private SurfaceView svCamera;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private ImageView ivSwitch;
    private boolean isFrontCamera;
    private String imageUri;
    private ImageView ivCapture;
    private Camera.PictureCallback pictureCallback;
    private File imageFile;
    private ImageView ivbackground;

    private ImageView ivPreview;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // only API>=23
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) ||
                    (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) ||
                    (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED)) {

                ActivityCompat.requestPermissions
                        (this, new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }

        svCamera = (SurfaceView) findViewById(R.id.sv_camera);
        ivSwitch = (ImageView) findViewById(R.id.iv_switch);
        ivCapture = (ImageView) findViewById(R.id.iv_capture);
        ivPreview = (ImageView) findViewById(R.id.iv_preview);
        ivbackground = (ImageView) findViewById(R.id.iv_background);

        ivPreview.setOnClickListener(this);
        ivSwitch.setOnClickListener(this);
        ivCapture.setOnClickListener(this);

        surfaceHolder = svCamera.getHolder();
        surfaceHolder.addCallback(this);

        pictureCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                // get image
                Bitmap rawCaptureImage = BitmapFactory.decodeByteArray(
                        data, 0, data.length);
                Matrix matrix = new Matrix();
                if (isFrontCamera){
                    matrix.preScale(-1.0f,1.0f);
                }

                matrix.postRotate(90);

                Bitmap rotatedBitmap = Bitmap.createBitmap(rawCaptureImage,
                        0,0,rawCaptureImage.getWidth(),rawCaptureImage.getHeight(),
                        matrix,true);
                Bitmap scaleBitmap = Bitmap.createScaledBitmap(rotatedBitmap,ivbackground.getWidth(),ivbackground.getHeight(),true);
                Bitmap background = Bitmap.createBitmap(ivbackground.getWidth(),ivbackground.getHeight(),
                        Bitmap.Config.ARGB_8888);
                Canvas canvasBackground = new Canvas(background);
                ivbackground.draw(canvasBackground);
                // Match
                Bitmap finalBitmap = Bitmap.createBitmap(background.getWidth(),background.getHeight(),background.getConfig());
                Canvas canvasFinal = new Canvas(finalBitmap);
                canvasFinal.drawBitmap(scaleBitmap,new Matrix(),null);
                canvasFinal.drawBitmap(background,0,0,null);
                //set image
                ivPreview.setImageBitmap(finalBitmap);

                // save to galleryy
                String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                File cameraAppFolder = new File(root);
                cameraAppFolder.mkdirs();
                imageFile = new File(cameraAppFolder,getCurrentTime() + ".jpeg");
                try {
                    FileOutputStream fout = new FileOutputStream(imageFile);
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG,100,fout);
                    fout.flush();
                    fout.close();
                    MediaScannerConnection.scanFile(MainActivity.this,new String[]{imageFile.getAbsolutePath()},null,null);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // refresh
                refreshCamera();
            }
        };
    }

    public String getCurrentTime(){
        Date date = Calendar.getInstance().getTime();
        return  date.toString();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        try {
            setUpCamera(-1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refreshCamera() {
        camera.stopPreview();
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setUpCamera(int camID) throws IOException {
        if (camID == -1){
            camera = Camera.open();
        }
        else{
            camera = Camera.open(camID);
        }

        camera.setDisplayOrientation(90);
        int maxWidth = 0;
        int maxHeight = 0;
        Camera.Parameters parameters = camera.getParameters();
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width*9-size.height*16 == 0){
                if (maxWidth < size.width){
                    maxWidth = size.width;
                    maxHeight = size.height;
                    parameters.setPreviewSize(maxWidth,maxHeight);
                    parameters.setPictureSize(maxWidth,maxHeight);
                }
            }
        }
        parameters.setJpegQuality(100);
        camera.setParameters(parameters);
        camera.setPreviewDisplay(surfaceHolder);
        camera.startPreview();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
       try {
            setUpCamera(-1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (camera!=null){
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.iv_switch:{
                if (isFrontCamera){
                    switchCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
                }else{
                    switchCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
                }
                isFrontCamera = !isFrontCamera;
                break;
            }
            case R.id.iv_capture: {
                camera.takePicture(null,null,pictureCallback);
                break;
            }
            case R.id.iv_preview: {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_VIEW);
                startActivity(intent);
                break;
            }
        }
    }

    private void switchCamera(int cameraType) {
        // Kiem tra xem camera co ton tai k
        int camID = checkCamera(cameraType);

        // Switch
        if (camID != -1) {
            camera.stopPreview();
            camera.release();
            try {
                setUpCamera(camID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int checkCamera(int cameraType) {
        int camID = -1;
        for (int i=0;i<Camera.getNumberOfCameras();i++){
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(i,cameraInfo);
            if (cameraInfo.facing == cameraType){
                camID = i;
                break;
            }
        }
        return camID;
    }
}
