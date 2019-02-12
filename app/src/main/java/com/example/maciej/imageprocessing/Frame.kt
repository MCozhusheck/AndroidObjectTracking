package com.example.maciej.imageprocessing

import org.opencv.core.Scalar

interface Frame {
    companion object {
        val RED = Scalar(255.0, .0, .0, 255.0)
        val GREEN = Scalar(.0, 255.0, .0, 255.0)
        val BLUE = Scalar(.0, .0, 255.0, 255.0)
    }
    var thickness: Int
    var color: Scalar
    var isSelected: Boolean
    fun copy(frame: Frame)
}