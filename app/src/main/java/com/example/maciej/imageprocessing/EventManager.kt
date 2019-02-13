package com.example.maciej.imageprocessing

class EventManager {
    private var subscribers: ArrayList<FrameRect> = ArrayList()

    fun add(rect: FrameRect) {
        subscribers.add(rect)
    }
    fun add(rects: ArrayList<FrameRect>) {
        subscribers = rects
    }
    fun remove(rect: FrameRect) {
        subscribers.remove(rect)
    }

    fun update(command: ScreenTouchedCommand) {
        val frameSelected = command.execute()
        frameSelected ?: return
        frameSelected.isSelected = true
        for (sub in subscribers){
            if (sub != frameSelected)
                sub.isVisible = false
        }
    }
}