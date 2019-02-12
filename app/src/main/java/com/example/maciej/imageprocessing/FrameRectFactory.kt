package com.example.maciej.imageprocessing

import org.opencv.core.CvType
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.imgproc.Imgproc

class FrameRectFactory {
    public fun createRectFrameArrayList(contours: ArrayList<MatOfPoint>): ArrayList<FrameRect>{
        val minContourWidth = 35
        val minContourHeight = 35
        val rectangles: ArrayList<FrameRect> = ArrayList()

        val approxCurve = MatOfPoint2f()
        val contour2f = MatOfPoint2f()
        for (contour in contours) {
            contour.convertTo(contour2f, CvType.CV_32FC2)
            val approxDistance = Imgproc.arcLength(contour2f, true) * 0.02
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true)
            val points = MatOfPoint()
            approxCurve.convertTo(points, CvType.CV_32SC2)
            val rect = FrameRect(points)
            if(rect.width < minContourWidth || rect.height < minContourHeight)
                continue
            rectangles.add(rect)
        }

        return  rectangles
    }
}