package com.example.maciej.imageprocessing

import android.Manifest
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG: String = "MainActivity"
        const val PERMISSION_REQUEST_CAMERA: Int = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    }
}
