package com.example.maciej.imageprocessing

import android.Manifest
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.video.BackgroundSubtractorMOG2
import org.opencv.video.Video;

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    companion object {
        const val TAG: String = "MainActivity"
        const val PERMISSION_REQUEST_CAMERA: Int = 1
        const val REQUEST_IMAGE_CAPTURE = 2
    }

    private var mOpenCvCameraView: CameraBridgeViewBase? = null
    private val mIsJavaCamera = true
    var frame: Mat? = null
    var fgMask: Mat? = null
    var backSub: BackgroundSubtractorMOG2? = null

    private val mLoaderCallback = object: BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")
                    mOpenCvCameraView?.enableView()
                    backSub = Video.createBackgroundSubtractorMOG2()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            Log.i(TAG, "permission not granted")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                PERMISSION_REQUEST_CAMERA
            )
        } else {
            Log.i(TAG, "permission granted")
        }

        mOpenCvCameraView = findViewById(R.id.camera)
        mOpenCvCameraView?.setVisibility(SurfaceView.VISIBLE)
        mOpenCvCameraView?.setCvCameraViewListener(this)

    }

    public override fun onPause() {
        super.onPause()
        if (mOpenCvCameraView != null)
            mOpenCvCameraView?.disableView()
    }
    public override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }
    public override fun onDestroy() {
        super.onDestroy()
        if (mOpenCvCameraView != null)
            mOpenCvCameraView?.disableView()

    }
    override fun onCameraViewStarted(width: Int, height: Int) {
        frame = Mat(height, width, CvType.CV_8UC4)
        fgMask = Mat(height, width, CvType.CV_8UC4)
    }
    override fun onCameraViewStopped() {
        frame?.release()
        fgMask?.release()
    }
    override fun onCameraFrame(inputFrame: CvCameraViewFrame?): Mat? {
        frame = inputFrame?.rgba()
        backSub?.apply(frame, fgMask)

        return fgMask
    }
}
