package com.example.maciej.imageprocessing

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Camera
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.IMPORTANT_FOR_ACCESSIBILITY_AUTO
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.video.BackgroundSubtractorMOG2
import org.opencv.video.Video;
import org.opencv.core.Scalar





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
    var mCamera: Camera? = null

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
            val mCamera = getCameraInstance()
            mCamera?.cancelAutoFocus()

            Log.i(TAG, "permission granted")
        }

        mOpenCvCameraView = findViewById(R.id.camera)
        mOpenCvCameraView?.setVisibility(SurfaceView.VISIBLE)
        mOpenCvCameraView?.setCvCameraViewListener(this)

    }
    private fun getCameraInstance(): Camera? {
        return try {
            Camera.open() // attempt to get a Camera instance
        } catch (e: Exception) {
            Log.i(TAG, "failed to initialize camera")
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
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
        fgMask = filterFgMask(fgMask)

        val contours = getContours(fgMask)
        val rectangles = getRectanglesFromContours(contours)
        displayRectangles(frame, rectangles)

        return frame
    }
    private fun filterFgMask(fgMask: Mat?) : Mat? {
        val kernelErode = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(2.0, 2.0))
        val kernelDilate = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(8.0, 8.0))
        Imgproc.erode(fgMask, fgMask, kernelErode)
        Imgproc.dilate(fgMask, fgMask, kernelDilate)
        return fgMask
    }
    private fun getContours(fgMask: Mat?): ArrayList<MatOfPoint> {
        val contours = ArrayList<MatOfPoint>()
        val threshold = 100.0
        val cannyOutput = Mat()
        Imgproc.Canny(fgMask, cannyOutput, threshold, threshold * 2)
        val hierarchy = Mat()
        Imgproc.findContours(cannyOutput, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_TC89_L1)
        hierarchy.release()
        return contours
    }
    private fun getRectanglesFromContours(contours: ArrayList<MatOfPoint>): ArrayList<Rect> {
        val minContourWidth = 35
        val minContourHeight = 35
        val rectangles: ArrayList<Rect> = ArrayList()

        val approxCurve = MatOfPoint2f()
        val contour2f = MatOfPoint2f()
        for (contour in contours) {
            contour.convertTo(contour2f, CvType.CV_32FC2)
            val approxDistance = Imgproc.arcLength(contour2f, true) * 0.02
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true)
            val points = MatOfPoint()
            approxCurve.convertTo(points, CvType.CV_32SC2)
            //val rect = FrameRect(points)
            val rect = Imgproc.boundingRect(points)
            if(rect.width < minContourWidth || rect.height < minContourHeight)
                continue
            rectangles.add(rect)
        }

        return  rectangles
    }
    private fun displayRectangles(frame: Mat? ,rectangles: ArrayList<Rect>): Mat? {
        for (rect in rectangles) {
            Imgproc.rectangle(frame, Point(rect.x.toDouble(), rect.y.toDouble()),
                Point((rect.x + rect.width).toDouble(), (rect.y + rect.height).toDouble()), Scalar(255.0, 0.0, 0.0, 255.0), 3)
        }
        return frame
    }
}
