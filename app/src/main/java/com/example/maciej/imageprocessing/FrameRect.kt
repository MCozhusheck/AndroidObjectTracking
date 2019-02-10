package com.example.maciej.imageprocessing

import org.opencv.core.MatOfPoint
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc

class FrameRect(val X:Int, val Y:Int, val szer: Int, val wys: Int): Rect(X,Y,szer,wys) {
    constructor(points: MatOfPoint) : this(Imgproc.boundingRect(points).x, Imgproc.boundingRect(points).y,
                                          Imgproc.boundingRect(points).width, Imgproc.boundingRect(points).width)
}