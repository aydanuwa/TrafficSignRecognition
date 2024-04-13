package com.example.TrafficSignRecognition;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity {
    public int maxRes = 500; // We scale our image to the resolution of maximum 1000 pixels
    public int maxNoObjects = 86; // Maximum number of objects
    public double[][] greyCdown1 = new double[maxRes][maxRes]; // Downsampled (4 times) and filtered grey image
    public int nObj, nk; // maxNoKeyPoints - maximum number of keypoints
    public int[][][] ICdifObj = new int[maxNoObjects][291][183]; // Different objects: Array with number of the point(s); differences are in the following array
    public double[][][] ICdifDoubleObj = new double[maxNoObjects][291][183]; // Different objects: Array with number of the point(s); differences are in the following array

    public double[][] octave1000First = new double[maxRes][maxRes]; // Here, the figure will have the white border
    public double[][] octave1000Second = new double[maxRes][maxRes]; // Here, the figure will have the white border
    public double[][] octave1000Third = new double[maxRes][maxRes]; // Here, the figure will have the white border
    public double[][] octave1000Fourth = new double[maxRes][maxRes]; // Here, the figure will have the white border
    public double[][] octave1000Fifth = new double[maxRes][maxRes]; // Here, the figure will have the white border
    public int x, y, i, j, width, height, flagMax, flagMin, kBest, i1, i2, pixel, i3, k0, k1, k2, k3, nObjBest, kBestThreshold = 19600; // kBestThreshold=19317; // kBestThreshold - minimum value to select the object


    Thread t1, t2, t3, t4, t5, t6, t7, t8; // Threads

    public int radius0, radius1, radius2, radius3, radius4, MatrixBorder, k, maxNoKeyPoints = 400; // maxNoKeyPoints - maximum number of keypoints in one part, i.e. maximum 400 keypoints
    public double minFirst, minSecond, sigma0, sigma1, sigma2, sigma3, sigma4, max, min, trace, det, threshold = 7.65; // 7.65 = 255 * 0.03;
    public double[][] maskS0 = new double[11][11]; // Mask with the Gaussian blur function's values
    public double[][] maskS1 = new double[13][13]; // Mask with the Gaussian blur function's values
    public double[][] maskS2 = new double[19][19]; // Mask with the Gaussian blur function's values
    public double[][] maskS3 = new double[25][25]; // Mask with the Gaussian blur function's values
    public double[][] maskS4 = new double[35][35]; // Mask with the Gaussian blur function's values

    public double[][] DoG1000First = new double[maxRes][maxRes];
    public double[][] DoG1000Second = new double[maxRes][maxRes];
    public double[][] DoG1000Third = new double[maxRes][maxRes];
    public double[][] DoG1000Fourth = new double[maxRes][maxRes];

    public double[][] Hessian = new double[2][2]; // 2x2 Hessian matrix

    public int[][] keypoints1000 = new int[450][2]; // Info about keypoints

    public double[] xk = new double[291]; // Coordinates of keypoints' net: 25 keypoints (1st level) + 58 keypoints (2nd level; 4 points, border, is included on the 1st level)
    public double[] yk = new double[291]; // Coordinates of keypoints' net
    public double[] IC = new double[291]; // Average intensities of in the circles around keypoints in the descriptor
    TextureView textureView;
    MediaPlayer mp = new MediaPlayer(); // MediaPlayer
    public String fileSeparator = System.getProperty("file.separator");
    File file;
    FileInputStream is;
    BufferedReader reader;
    public String line;
    private static final String TAG = MainActivity.class.getSimpleName();
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"};
    private int REQUEST_CODE_PERMISSIONS = 101;
    public ByteBuffer bb;
    public byte[] buf;
    Bitmap bmOut;
    OutputStream out;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.view_finder);

        try {
            mp.setDataSource(getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + fileSeparator + "DeviceIsReady.mp3");//Writing location
            mp.prepare();
            mp.start();
            // Here, we read number of objects from root.txt
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + fileSeparator + "KnowledgeBase" + fileSeparator + "root.txt");
            if (file.exists()) {
                is = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(is));
                line = reader.readLine();
                is.close();
                nObj = Integer.parseInt(line);

            } else Log.i(TAG, "File root.txt does not exist");

            for (height = 0; height < nObj; height++) {
                file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + fileSeparator + "KnowledgeBase" + fileSeparator + "d" + height + ".txt");
                if (file.exists()) {
                    is = new FileInputStream(file);
                    reader = new BufferedReader(new InputStreamReader(is));
                    for (i = 0; i < 25; i++) { // First 25 keypoints
                        line = reader.readLine(); // this is the number of keypoint, i.e. i; we can skip it
                        for (j = 0; j < 12; j++) {
                            line = reader.readLine();
                            ICdifObj[height][i][j] = Integer.parseInt(line); // Number of keypoint j related to the keypoint i
                            line = reader.readLine();
                            ICdifDoubleObj[height][i][j] = Double.parseDouble(line); // Difference in intencities between the target keypoint and another one
                        }
                        line = reader.readLine(); // We read empty line here
                    }
                    for (i = 25; i < 83; i++) { // The rest of keypoints
                        line = reader.readLine(); // this is the number of keypoint, i.e. i; we can skip it
                        for (j = 25; j < 57; j++) {
                            line = reader.readLine();
                            ICdifObj[height][i][j] = Integer.parseInt(line); // Number of keypoint j related to the keypoint i
                            line = reader.readLine();
                            ICdifDoubleObj[height][i][j] = Double.parseDouble(line); // Difference in intencities between the target keypoint and another one
                        }
                        line = reader.readLine(); // We read empty line here
                    }
                    for (i = 83; i < 291; i++) { // The rest of keypoints
                        line = reader.readLine(); // this is the number of keypoint, i.e. i; we can skip it
                        for (j = 83; j < 183; j++) {
                            line = reader.readLine();
                            ICdifObj[height][i][j] = Integer.parseInt(line); // Number of keypoint j related to the keypoint i
                            line = reader.readLine();
                            ICdifDoubleObj[height][i][j] = Double.parseDouble(line); // Difference in intencities between the target keypoint and another one
                        }
                        line = reader.readLine(); // We read empty line here
                    }
                    is.close();
                    Log.i(TAG, "Knowledge base was read successfully: " + height);

                } else {
                    Log.i(TAG, "File does not exist");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "Knowledgebase was not read");
        }
        if (allPermissionsGranted()) {
            Log.i(TAG, "All permissions were granted");
            startCamera(); //start camera if permission has been granted by user
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

    }

    private void startCamera(){
        CameraX.unbindAll();

        Rational aspectRatio = new Rational (textureView.getWidth(), textureView.getHeight());
        Size screen = new Size(textureView.getWidth(), textureView.getHeight()); //size of the screen
        PreviewConfig pConfig = new PreviewConfig.Builder()
                .setTargetAspectRatio(aspectRatio)
                .setTargetResolution(screen)

                .build();
        Preview preview = new Preview(pConfig);
        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput output){
                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView, 0);

                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform();
                    }
                });

        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);

        findViewById(R.id.imgCapture).setOnClickListener(v -> {
            imgCap.takePicture(new ImageCapture.OnImageCapturedListener() {
                @Override
                public void onCaptureSuccess(ImageProxy image, int rotationDegrees) {
                    try {
                        // Lines for analysis by taking the picture
                        bb = image.getPlanes()[0].getBuffer();
                        buf = new byte[bb.remaining()];
                        bb.get(buf);
                        bmOut = BitmapFactory.decodeByteArray(buf, 0, buf.length, null);

                        // Lines for analysis of an image object OInput
//                        file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"OInput.jpg");
//                        bmOut = BitmapFactory.decodeFile(file.getPath());
//
//                        file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + fileSeparator + "Temp" + fileSeparator + "CameraImage_Original_" + Calendar.getInstance().getTime() + ".jpg");
//                        out = new FileOutputStream(file);
//                        bmOut.compress(Bitmap.CompressFormat.JPEG, 100, out);
//                        out.flush(); out.close();

                        //Starting the analysis of new image or file with SIFT
                        //maxRes 180
                        Log.i(TAG, "We have a bitmap!");
                        //Saving the bitmap onto disk to see what we recieved from camera
                        width = bmOut.getWidth();
                        height = bmOut.getHeight();
                        Log.i(TAG, "Width =  " + width + "   Height = " + height);
                        // Scaling the image to the maxRes
                        if (height > width) { // if height > width
                            width = Math.round(maxRes * width / height);
                            height = maxRes;
                        } else {
                            height = Math.round(maxRes * height / width);
                            width = maxRes;
                        }
                        bmOut = Bitmap.createScaledBitmap(bmOut, width, height, true); // Here, we scale bitmap to maxRes pixels; true means that we use bilinear filtering for better image

                        //Converting to greyscale
                        t1 = new Thread(new ThreadGrey(0, 62), "t1");
                        t2 = new Thread(new ThreadGrey(62, 125), "t2");
                        t3 = new Thread(new ThreadGrey(125, 187), "t3");
                        t4 = new Thread(new ThreadGrey(187, 250), "t4");
                        t5 = new Thread(new ThreadGrey(250, 312), "t5");
                        t6 = new Thread(new ThreadGrey(312, 375), "t6");
                        t7 = new Thread(new ThreadGrey(375, 437), "t7");
                        t8 = new Thread(new ThreadGrey(437, 500), "t1");
                        t1.start();
                        t2.start();
                        t3.start();
                        t4.start();
                        t5.start();
                        t6.start();
                        t7.start();
                        t8.start();

                        Log.i(TAG, "New width =  " + width + "   New height = " + height);
                        t1.join();
                        t2.join();
                        t3.join();
                        t4.join();
                        t5.join();
                        t6.join();
                        t7.join();
                        t8.join();
                        file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + fileSeparator + "Temp" + fileSeparator + "CameraImage_Grey_" + maxRes + " " + Calendar.getInstance().getTime() + ".jpg");
                        out = new FileOutputStream(file);
                        bmOut.compress(Bitmap.CompressFormat.JPEG, 100, out);
                        out.flush();
                        out.close();
                        Log.i(TAG, "We have got greyscale bitmap :)");


                        sigma0 = 1.0;
                        radius0 = 3; // radius0 is the radius of the matrix for the Gaussian blur for the current scale
                        sigma1 = 1.414214;
                        radius1 = 5; // radius1 is the radius of the matrix for the Gaussian blur for the current scale
                        sigma2 = 2.0;
                        radius2 = 6; // radius2 is the radius of the matrix for the Gaussian blur for the current scale
                        sigma3 = 2.828427;
                        radius3 = 9; // radius3 is the radius of the matrix for the Gaussian blur for the maximum scale
                        sigma4 = 4.0;
                        radius4 = 12; // radius4 is the radius of the matrix for the Gaussian blur for the current scale

                        MainMethodSIFT(); //trying to find new object
                        k0 = 0; // No objects found
                        for (i3 = 0; i3 < nObj; i3++) {
                            if (octave1000First[0][i3] == 1) {
                                k0 = 1;
                                break;
                            }
                        }

                        if (k0 == 0) {
                            sigma0 = 1.414214;
                            radius0 = 5; // radius0 is the radius of the matrix for the Gaussian blur for the current scale
                            sigma1 = 2.0;
                            radius1 = 6; // radius1 is the radius of the matrix for the Gaussian blur for the current scale
                            sigma2 = 2.828427;
                            radius2 = 9; // radius2 is the radius of the matrix for the Gaussian blur for the current scale
                            sigma3 = 4.0;
                            radius3 = 12; // radius3 is the radius of the matrix for the Gaussian blur for the maximum scale
                            sigma4 = 5.656854;
                            radius4 = 17; // radius4 is the radius of the matrix for the Gaussian blur for the current scale
                            MainMethodSift(); //trying to find object with different sigma val
                            k0 = 0; // No objects found
                            for (i3 = 0; i3 < nObj; i3++) {
                                if (octave1000First[0][i3] == 1) {
                                    k0 = 2;
                                    break;
                                }
                            }
                            if (k0 == 0) {
                                sigma0 = 0.5;
                                radius0 = 2; // radius0 is the radius of the matrix for the Gaussian blur for the current scale
                                sigma3 = Math.sqrt(2.0);
                                radius3 = 5; // radius3 is the radius of the matrix for the Gaussian blur for the current scale
                                sigma1 = 0.5 * sigma3;
                                radius1 = 3; // radius1 is the radius of the matrix for the Gaussian blur for the current scale
                                sigma2 = 1.0;
                                radius2 = 4; // radius2 is the radius of the matrix for the Gaussian blur for the current scale
                                sigma4 = 2.0;
                                radius4 = 6; // radius4 is the radius of the matrix for the Gaussian blur for the maximum scale
                                MainMethodSIFT(); // trying to find object with different sigma val
                                k0 = 0; // No objects found
                                for (i3 = 0; i3 < nObj; i3++) {
                                    if (octave1000First[0][i3] == 1) {
                                        k0 = 3;
                                        break;
                                    }
                                }
                                if(k0 == 0){
                                    mp.reset();
                                    mp.setDataSource(getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + fileSeparator + "NoObjectFound.mp3");
                                    mp.prepare();
                                    mp.start();
                                    Log.i(TAG, "Object was not found");
                                    Thread.sleep(2000);
                                }
                            }
                        }
                        if (k0!=0){
                            if (octave1000First[0][10]==0 && octave1000First[0][29]==1){
                                mp.reset(); //after it it is like the object is just created
                                mp.setDataSource(getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + fileSeparator + "KnowledgeBase" + fileSeparator + "Name19.mp3"); //No entry for pedestrians
                                mp.prepare();
                                mp.start();
                                Thread.sleep(1500);
                            }
                            mp.reset();
                            mp.setDataSource(getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + fileSeparator + "AnalysisFinished.mp3");
                            mp.prepare();
                            mp.start();
                            Log.i(TAG, "Object found with sigma # " + k0);
                            Thread.sleep(2000);
                        }
                        finish();
                        startActivity(getIntent());

                } catch(Exception e){
                    Log.i(TAG, "Exception  " + e);
                }
            }








            });
        });
    }



    private void updateTransform() {
        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cX = w * 0.5f;
        float cY = h * 0.5f;

        int rotationDgr;
        int rotation = (int) textureView.getRotation();

        switch (rotation) {
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float) rotationDgr, cX, cY);
        textureView.setTransform(mx);
    }

    private boolean allPermissionsGranted() {

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    class ThreadGrey implements Runnable{
        private int xStart,xEnd,x, y,pixel;
        public ThreadGrey(int xStart, int xEnd) {
            this.xStart = xStart;
            this.xEnd = xEnd;
        }
        @Override
        public void run() {
            if (width>height) for (x = xStart; x < xEnd; x++){ //width
                for (y = 0; y < height; y++){ //height
                    pixel = bmOut.getPixel(x,y);
                    greyCdown1[x][y] = 0.21 * Color.red(pixel) + 0.72 * Color.green(pixel) + 0.07 * Color.blue(pixel);
                    pixel = (int) greyCdown1[x][y];
                    bmOut.setPixel(x, y, Color.argb(255, pixel, pixel, pixel));
                }
            }
            else for(x = 0; x < width; x++){ //width
                for (y = xStart; y < xEnd; y++){ //height
                    pixel = bmOut.getPixel(x,y);
                    greyCdown1[x][y] = 0.21 * Color.red(pixel) + 0.72 * Color.green(pixel) + 0.07 * Color.blue(pixel);
                    pixel = (int) greyCdown1[x][y];
                    bmOut.setPixel(x, y, Color.argb(255, pixel, pixel, pixel));
                }
            }
        }
    }
}