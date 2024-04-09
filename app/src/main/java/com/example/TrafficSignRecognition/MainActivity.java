package com.example.TrafficSignRecognition;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;


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

    }

    private boolean allPermissionsGranted(){

        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }


}
