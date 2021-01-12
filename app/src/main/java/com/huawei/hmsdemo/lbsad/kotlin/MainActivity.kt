package com.huawei.hmsdemo.lbsad.kotlin

import android.Manifest
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.huawei.hmf.tasks.OnSuccessListener
import com.huawei.hmf.tasks.Task
import com.huawei.hms.common.ApiException
import com.huawei.hms.common.ResolvableApiException
import com.huawei.hms.location.FusedLocationProviderClient
import com.huawei.hms.location.LocationRequest
import com.huawei.hms.location.LocationServices
import com.huawei.hms.location.LocationSettingsRequest
import com.huawei.hms.location.LocationSettingsStatusCodes
import com.huawei.hms.location.SettingsClient
import com.huawei.hmsdemo.lbsad.R

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val mHandler = Handler(Looper.getMainLooper())
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.btn_start_running).setOnClickListener(this)
        findViewById<View>(R.id.btn_mock_location).setOnClickListener(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // SDK ≤ 28 Dynamically Applying for Required Permissions
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            Log.i(TAG, "sdk <= 28 Q")
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                val strings = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                ActivityCompat.requestPermissions(this, strings, 1)
            }
        } else {
            // //Dynamicly apply for permissions required for SDK > 28. Add the android.permission.ACCESS_BACKGROUND_LOCATION permission.
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                val strings = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
                ActivityCompat.requestPermissions(this, strings, 2)
            }
        }
        val settingsClient: SettingsClient = LocationServices.getSettingsClient(this)
        val builder: LocationSettingsRequest.Builder = LocationSettingsRequest.Builder()
        val mLocationRequest = LocationRequest()
        builder.addLocationRequest(mLocationRequest)
        val locationSettingsRequest: LocationSettingsRequest = builder.build()
        //检查设备定位设置
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener {
                //设置满足定位条件，再发起位置请求
                Log.d(TAG, "location setting success")
            }
            .addOnFailureListener { e ->
                //设置不满足定位条件
                val statusCode: Int = (e as ApiException).getStatusCode()
                Log.d(TAG, "location setting status code $statusCode")
                when (statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        val rae: ResolvableApiException = e as ResolvableApiException
                        //调用startResolutionForResult可以弹窗提示用户打开相应权限
                        rae.startResolutionForResult(this@MainActivity, 0)
                    } catch (sie: SendIntentException) {
                        //…
                    }
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
        fusedLocationProviderClient!!.setMockMode(false)
    }

    override fun onClick(view: View) {
        if (view.id == R.id.btn_start_running) {
            val intent = Intent(this@MainActivity, MapActivity::class.java)
            startActivity(intent)
        } else if (view.id == R.id.btn_mock_location) {
            val voidTask: Task<Void> = fusedLocationProviderClient!!.setMockMode(true)
            voidTask.addOnSuccessListener(object : OnSuccessListener<Void?> {
                override fun onSuccess(aVoid: Void?) {
                    Toast.makeText(this@MainActivity, "setMockMode onSuccess", Toast.LENGTH_SHORT)
                        .show()
                    mHandler.post(object : Runnable {
                        var point = 0
                        override fun run() {
                            if (point + 1 >= MockGpsData.POINTS.size) {
                                return
                            }
                            val latitude: Double = MockGpsData.POINTS[point++]
                            val longitude: Double = MockGpsData.POINTS[point++]
                            val mockLocation = Location(LocationManager.GPS_PROVIDER)
                            mockLocation.longitude = longitude
                            mockLocation.latitude = latitude
                            val voidTask: Task<Void> =
                                fusedLocationProviderClient!!.setMockLocation(mockLocation)
                            voidTask.addOnSuccessListener { }.addOnFailureListener { }

                            mHandler.postDelayed(this, 100)
                        }
                    })
                }
            }).addOnFailureListener { e ->
                if (e != null) {
                    Toast.makeText(
                        this@MainActivity,
                        "setMockMode onFailure:" + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}