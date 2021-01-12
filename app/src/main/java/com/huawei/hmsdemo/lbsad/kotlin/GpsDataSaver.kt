package com.huawei.hmsdemo.lbsad.kotlin

import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException

class GpsDataSaver(private val dotFile: File?, private val dotData: String) : Runnable {
    override fun run() {
        var fw: FileWriter? = null
        try {
            fw = FileWriter(dotFile, true)
            fw.write(dotData)
            Log.d("GpsDataSaver", "Location data save success, data is $dotData")
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (null != fw) {
                try {
                    fw.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}