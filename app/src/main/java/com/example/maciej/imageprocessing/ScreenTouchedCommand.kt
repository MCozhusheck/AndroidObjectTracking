package com.example.maciej.imageprocessing

import org.opencv.core.Mat

class ScreenTouchedCommand(private val rectangles: ArrayList<FrameRect>, private val x: Float, private val y: Float): Command {
    override fun execute(): FrameRect? {
        for (rect in rectangles){
            if (withinXAxis(rect,x) && withinYAxis(rect, y)){
                rect.color = Frame.GREEN
                rect.isSelected = true
                return rect
            }
        }
        return null
    }
    private fun withinXAxis(rect: FrameRect, x: Float): Boolean {
        val maxX = (rect.x + rect.width).toFloat()
        return (rect.x.toFloat() <= x && x <= maxX)
    }
    private fun withinYAxis(rect: FrameRect, y: Float): Boolean {
        val maxY = (rect.y + rect.height).toFloat()
        return (rect.y.toFloat() <= y && y <= maxY)
    }
}