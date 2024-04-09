package com.example.TrafficSignRecognition;

import android.graphics.Bitmap;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity{
    public int maxRes = 500; // We scale our image to the resolution of maximum 1000 pixels
    public int maxNoObjects = 86; // Maximum number of objects
    public double [][] greyCdown1 = new double[maxRes][maxRes]; // Downsampled (4 times) and filtered grey image
    public int nObj, nk; // maxNoKeyPoints - maximum number of keypoints
    public int [][][] ICdifObj = new int [maxNoObjects][291][183]; // Different objects: Array with number of the point(s); differences are in the following array
    public double [][][] ICdifDoubleObj = new double [maxNoObjects][291][183]; // Different objects: Array with number of the point(s); differences are in the following array

    public double [][] octave1000First = new double[maxRes][maxRes]; // Here, the figure will have the white border
    public double [][] octave1000Second = new double[maxRes][maxRes]; // Here, the figure will have the white border
    public double [][] octave1000Third = new double[maxRes][maxRes]; // Here, the figure will have the white border
    public double [][] octave1000Fourth = new double[maxRes][maxRes]; // Here, the figure will have the white border
    public double [][] octave1000Fifth = new double[maxRes][maxRes]; // Here, the figure will have the white border



    Thread t1, t2, t3, t4, t5, t6, t7, t8; // Threads

    public int radius0, radius1, radius2, radius3, radius4, MatrixBorder, k, maxNoKeyPoints=400; // maxNoKeyPoints - maximum number of keypoints in one part, i.e. maximum 400 keypoints
    public double minFirst, minSecond, sigma0, sigma1, sigma2, sigma3, sigma4, max, min, trace, det, threshold = 7.65; // 7.65 = 255 * 0.03;
    public double [][] maskS0 = new double[11][11]; // Mask with the Gaussian blur function's values
    public double [][] maskS1 = new double[13][13]; // Mask with the Gaussian blur function's values
    public double [][] maskS2 = new double[19][19]; // Mask with the Gaussian blur function's values
    public double [][] maskS3 = new double[25][25]; // Mask with the Gaussian blur function's values
    public double [][] maskS4 = new double[35][35]; // Mask with the Gaussian blur function's values

    public double [][] DoG1000First = new double[maxRes][maxRes];
    public double [][] DoG1000Second = new double[maxRes][maxRes];
    public double [][] DoG1000Third = new double[maxRes][maxRes];
    public double [][] DoG1000Fourth = new double[maxRes][maxRes];

    public double [][] Hessian = new double [2][2]; // 2x2 Hessian matrix

    public int [][] keypoints1000 = new int [450][2]; // Info about keypoints

    public double [] xk = new double [291]; // Coordinates of keypoints' net: 25 keypoints (1st level) + 58 keypoints (2nd level; 4 points, border, is included on the 1st level)
    public double [] yk = new double [291]; // Coordinates of keypoints' net
    public double [] IC = new double [291]; // Average intensities of in the circles around keypoints in the descriptor

}