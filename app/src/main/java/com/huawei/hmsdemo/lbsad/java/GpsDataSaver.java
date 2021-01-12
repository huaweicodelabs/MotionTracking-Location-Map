package com.huawei.hmsdemo.lbsad.java;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class GpsDataSaver implements Runnable {
    private final String dotData;

    private final File dotFile;

    public GpsDataSaver(File file, String dotData)
    {
        this.dotData = dotData;
        this.dotFile = file;
    }

    @Override
    public void run() {
        FileWriter fw = null;
        try {
            fw = new FileWriter(dotFile, true);
            fw.write(dotData);
            Log.d("GpsDataSaver", "Location data save success, data is " + dotData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (null != fw)
            {
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
