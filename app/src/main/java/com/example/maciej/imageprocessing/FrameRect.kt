package com.example.maciej.imageprocessing

import org.opencv.core.MatOfPoint
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc

class FrameRect: Rect {
    var rect: Rect
    constructor(points: MatOfPoint) {
        rect = Imgproc.boundingRect(points)
    }
}