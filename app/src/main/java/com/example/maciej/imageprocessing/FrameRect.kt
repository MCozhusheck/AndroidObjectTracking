package com.example.maciej.imageprocessing

import android.util.Log
import org.opencv.core.MatOfPoint
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

class FrameRect (val X:Int, val Y:Int, val szer: Int, val wys: Int): Rect(X,Y,szer,wys), Frame {
    override var thickness = 3
    override var color = Frame.RED
    override var isSelected = false
    constructor(points: MatOfPoint) : this(Imgproc.boundingRect(points).x, Imgproc.boundingRect(points).y,
                                          Imgproc.boundingRect(points).width, Imgproc.boundingRect(points).width)

    override fun copy(frame: Frame) {
        this.thickness = frame.thickness
        this.color = frame.color
        this.isSelected = frame.isSelected
    }
}