package com.example.maciej.imageprocessing

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Camera
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.video.BackgroundSubtractorMOG2
import org.opencv.video.Video;
import kotlin.math.pow
import kotlin.math.sqrt


class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {

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
    val factory: FrameRectFactory = FrameRectFactory()
    var previousRectangles = ArrayList<FrameRect>()
    var rectangles = ArrayList<FrameRect>()
    val eventManager = EventManager()

    private val mLoaderCallback = object: BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")
                    mOpenCvCameraView?.enableView()
                    backSub = Video.createBackgroundSubtractorMOG2()
                    mOpenCvCameraView?.setOnTouchListener(this@MainActivity)
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

        eventManager.add(rectangles)
        frame = inputFrame?.rgba()
        backSub?.apply(frame, fgMask)
        filterFgMask(fgMask)

        val contours = getContours(fgMask)
        rectangles = factory.createRectFrameArrayList(contours)
        updateRectangles(rectangles, previousRectangles)
        displayRectangles(frame, rectangles)
        previousRectangles = rectangles

        return frame
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        val cols = frame!!.cols()
        val rows = frame!!.rows()

        val xOffset = (mOpenCvCameraView!!.width - cols) / 2
        val yOffset = (mOpenCvCameraView!!.height - rows) / 2

        val x = event!!.x - xOffset
        val y = event!!.y - yOffset

        Log.i(TAG, "Touch image coordinates: ($x, $y)")

        if (x < 0 || y < 0 || x > cols || y > rows)
            return false
        val command = ScreenTouchedCommand(rectangles, x, y)
        eventManager.update(command)
        return false
    }
    private fun filterFgMask(fgMask: Mat?) : Mat? {
        val kernelErode = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0))
        val kernelDilate = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(15.0, 15.0))
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
    private fun displayRectangles(frame: Mat? ,rectangles: ArrayList<FrameRect>): Mat? {
        for (rect in rectangles) {
            if (rect.isVisible)
                Imgproc.rectangle(frame, Point(rect.x.toDouble(), rect.y.toDouble()),
                    Point((rect.x + rect.width).toDouble(), (rect.y + rect.height).toDouble()), rect.color, rect.thickness)
        }
        return frame
    }
    private fun updateRectangles(rectangles: ArrayList<FrameRect>, previousRectangles: ArrayList<FrameRect>): ArrayList<FrameRect> {

        val maxDistance = 150.0
        if (previousRectangles.size < 1 || rectangles.size < 1)
            return rectangles

        val everyDistance: ArrayList<Double> = ArrayList(rectangles.size)
        val everyDistanceIndices: ArrayList<Int> = ArrayList(rectangles.size)

        for (i in 0 until rectangles.size) {
            val localDistance: ArrayList<Double> = ArrayList(previousRectangles.size)
            for (j in 0 until previousRectangles.size) {
                val previous = previousRectangles[j]
                val next = rectangles[i]
                val distance = sqrt((previous.x-next.x).toDouble().pow(2)+(previous.y - next.y).toDouble().pow(2))
                localDistance.add(distance)
            }
            var index = -1
            var minDistance = localDistance.min() ?: Double.MAX_VALUE
            if(minDistance > maxDistance)
                minDistance = Double.MAX_VALUE

            if (minDistance != Double.MAX_VALUE)
                index = localDistance.indexOf(minDistance)

            everyDistanceIndices.add(index)
            everyDistance.add(minDistance)
        }

        for (i in 0 until rectangles.size) {
            if (everyDistance[i].isFinite() && everyDistanceIndices[i] > 0) {
                val index = everyDistanceIndices[i]
                rectangles[i].copy(previousRectangles[index])
                for (j in 0 until everyDistanceIndices.size) {
                    if (everyDistanceIndices[j] == index)
                        everyDistanceIndices[j] = -1
                }
            }
        }

        return  rectangles
    }
}
