package com.huawei.hmsdemo.lbsad.kotlin

import android.content.IntentSender.SendIntentException
import android.location.Location
import android.os.*
import android.util.Log
import android.widget.Chronometer
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.huawei.hmf.tasks.Task
import com.huawei.hms.common.ApiException
import com.huawei.hms.common.ResolvableApiException
import com.huawei.hms.location.FusedLocationProviderClient
import com.huawei.hms.location.LocationAvailability
import com.huawei.hms.location.LocationCallback
import com.huawei.hms.location.LocationRequest
import com.huawei.hms.location.LocationResult
import com.huawei.hms.location.LocationServices
import com.huawei.hms.location.LocationSettingsRequest
import com.huawei.hms.location.LocationSettingsStatusCodes
import com.huawei.hms.location.SettingsClient
import com.huawei.hms.maps.CameraUpdate
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.LocationSource
import com.huawei.hms.maps.MapView
import com.huawei.hms.maps.OnMapReadyCallback
import com.huawei.hms.maps.model.BitmapDescriptorFactory
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions
import com.huawei.hms.maps.model.PolylineOptions
import com.huawei.hmsdemo.lbsad.kotlin.bean.PathBean
import com.huawei.hmsdemo.lbsad.R
import java.io.File
import java.text.DecimalFormat

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMapView: MapView
    private var mHwMap: HuaweiMap? = null
    private var mPolylineOptions: PolylineOptions? = null
    private var mListener: LocationSource.OnLocationChangedListener? = null

    private var mMarkerStart: Marker? = null
    private var mMarkerEnd: Marker? = null
    private var mLocationRequest: LocationRequest? = null
    private var mTvStart: TextView? = null
    private var mTvSpeed: TextView? = null
    private var mTvDistance: TextView? = null
    private var mTime: Chronometer? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private val mPath: PathBean = PathBean()
    private var mSeconds: Long = 0
    private val mHandler = Handler(Looper.getMainLooper())
    private val mDecimalFormat = DecimalFormat("0.00")
    private var mIsRunning = false
    private val mTimeRunnable: Runnable = object : Runnable {
        override fun run() {
            mTime!!.text = formatSeconds()
            mHandler.postDelayed(this, 1000)
        }
    }
    private var mLocationCallback: LocationCallback? = null

    /**
     * GPS Data.
     */
    private var mGpsDataThread: HandlerThread? = null
    private var mGpsDataHandler: Handler? = null
    private var mGpsDataFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_map_activity)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // check location settings
        checkLocationSettings()

        // init MapView
        mMapView = findViewById(R.id.hw_mapview)
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY)
        }
        mMapView.onCreate(mapViewBundle)
        mMapView.getMapAsync(this)

        /* Add the exercise data display UI. */
        mTvSpeed = findViewById(R.id.tv_speed)
        mTvDistance = findViewById(R.id.tv_distance)
        mTime = findViewById(R.id.cm_time)
        mTvStart = findViewById(R.id.tv_start)
        mTvStart!!.setOnClickListener({ processStartClick() })


        /* Initializing Map Kit Polyline */
        mPolylineOptions = PolylineOptions()
        mPolylineOptions!!.color(resources.getColor(R.color.colorAccent))
        mPolylineOptions!!.width(5f)

        /* Recording GPS Data */
        mGpsDataFile = File(getExternalFilesDir(null), "GpsData.txt")
    }

    override fun onMapReady(huaweiMap: HuaweiMap?) {
        Log.d(TAG, "onMapReady: ")
        mHwMap = huaweiMap
        mHwMap?.isMyLocationEnabled = true
        mHwMap?.uiSettings?.isZoomControlsEnabled = false

        //Add Location Source
        mHwMap?.setLocationSource(object : LocationSource {
            override fun activate(onLocationChangedListener: LocationSource.OnLocationChangedListener?) {
                mListener = onLocationChangedListener
            }

            override fun deactivate() {}
        })
        //Obtains the current position and updates the map camera.
        try {
            val lastLocation: Task<Location> = fusedLocationProviderClient!!.lastLocation
            lastLocation.addOnSuccessListener { location ->
                mListener!!.onLocationChanged(location)
                if (mListener != null) {
                    mListener!!.onLocationChanged(location)
                    val cameraUpdate: CameraUpdate = CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude), 15f
                    )
                    mHwMap!!.animateCamera(cameraUpdate)
                }
            }.addOnFailureListener {
                Log.d(TAG, "onMapReady: Obtains the current position failure")

            }
        } catch (e: Exception) {

        }
    }

    companion object {
        private const val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"
        private const val TAG = "MapActivity"
    }

    override fun onStart() {
        super.onStart()
        mMapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mMapView.onStop()
    }

    override fun onDestroy() {
        removeLocationUpdatesWithCallback()
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
        mMapView.onDestroy()
    }

    override fun onPause() {
        mMapView.onPause()
        super.onPause()
        mGpsDataThread!!.quitSafely()
        try {
            mGpsDataThread!!.join()
            mGpsDataThread = null
            mGpsDataHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        mMapView.onResume()
        mGpsDataThread = HandlerThread("DotThread")
        mGpsDataThread!!.start()
        mGpsDataHandler = Handler(mGpsDataThread!!.looper)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mMapView.onLowMemory()
    }

    private fun checkLocationSettings() {
        val builder: LocationSettingsRequest.Builder = LocationSettingsRequest.Builder()
        val locationSettingsRequest: LocationSettingsRequest = builder.build()
        val settingsClient: SettingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener { requestLocationUpdate() }.addOnFailureListener { e ->
                when ((e as ApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        val rae: ResolvableApiException = e as ResolvableApiException
                        rae.startResolutionForResult(this@MapActivity, 0)
                    } catch (sie: SendIntentException) {
                    }
                }
            }
    }

    private fun requestLocationUpdate() {
        mLocationRequest = LocationRequest()
        mLocationRequest!!.interval = 5000
        mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                writeGpsData2Sdcard(locationResult.lastLocation)
                if (mIsRunning) {
                    processLocationChange(locationResult.lastLocation)
                }
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability?) {
                super.onLocationAvailability(locationAvailability)
            }
        }
        fusedLocationProviderClient
            ?.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper())
            ?.addOnSuccessListener { Log.i(TAG, "request location updates success") }
            ?.addOnFailureListener { e ->
                Log.e(
                    TAG,
                    "request location updates failed, error: " + e.message
                )
            }
    }

    /**
     * Removed when the location update is no longer required.
     */
    private fun removeLocationUpdatesWithCallback() {
        try {
            val voidTask: Task<Void> =
                fusedLocationProviderClient!!.removeLocationUpdates(mLocationCallback)
            voidTask.addOnSuccessListener { }.addOnFailureListener { }
        } catch (e: Exception) {
            Log.e(TAG, "removeLocationUpdatesWithCallback Exception : $e", )
        }
    }

    private fun processStartClick() {
        if (mIsRunning) {
            mIsRunning = false
            mPath.endTime = (System.currentTimeMillis())
            mTvStart!!.text = "Start"
            mHandler.removeCallbacks(mTimeRunnable)
            if (mPath.mPathLinePoints!!.size > 0) {
                mPath.endPoint = (mPath.mPathLinePoints!!.get(mPath.mPathLinePoints!!.size - 1))
                if (null != mMarkerStart && null != mMarkerEnd) {
                    mMarkerStart!!.remove()
                    mMarkerEnd!!.remove()
                }
                val startPointOptions: MarkerOptions = MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.map_marker_bubble_azure_small))
                    .position(mPath.mStartPoint)
                startPointOptions.title("Start Point")
                startPointOptions.snippet("Start Point")
                mMarkerStart = mHwMap!!.addMarker(startPointOptions)
                val endPointOptions: MarkerOptions = MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.map_marker_bubble_azure_small))
                    .position(mPath.mEndPoint)
                endPointOptions.title("End Point")
                endPointOptions.snippet("End Point")
                mMarkerEnd = mHwMap!!.addMarker(endPointOptions)
            }
        } else {
            mIsRunning = true
            mPath.reset()
            mPath.startTime = System.currentTimeMillis()
            mHandler.post(mTimeRunnable)
            mTvStart!!.text = "Stop"
        }
    }

    private fun processLocationChange(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        if (mPath.mStartPoint == null) {
            mPath.mStartPoint =  latLng
        }
        mPath.addPoint(latLng)
        val distance: Float = mPath.updateDistance()
        val sportMile = distance / 1000.0
        if (mSeconds > 0) {
            val distribution = mSeconds.toDouble() / 60.0 / sportMile
            mPath.setDistribution(distribution)
            mTvSpeed!!.text = mDecimalFormat.format(distribution)
            mTvDistance!!.text = mDecimalFormat.format(sportMile)
        } else {
            mPath.setDistribution(0.0)
            mTvSpeed!!.text = "0.00"
            mTvDistance!!.text = "0.00"
        }
        //在地图上绘制运动轨迹
        mPolylineOptions!!.add(latLng)
        mHwMap!!.addPolyline(mPolylineOptions)
        //更新Map镜头
        if (mListener != null) {
            mListener!!.onLocationChanged(location)
            val cameraUpdate: CameraUpdate = CameraUpdateFactory.newLatLngZoom(
                LatLng(location.latitude, location.longitude), 15f
            )
            mHwMap!!.animateCamera(cameraUpdate)
        }
    }

    fun formatSeconds(): String {
        val hh = if (mSeconds / 3600 > 9) mSeconds / 3600   else mSeconds / 3600
        val mm =
            if (mSeconds % 3600 / 60 > 9) mSeconds % 3600 / 60 else  mSeconds % 3600 / 60
//        val ss =
//            if (mSeconds % 3600 % 60 > 9) mSeconds % 3600 % 60  else  mSeconds % 3600 % 60
        mSeconds++
        return "${hh.toString()}:${mm.toString()}"
    }

    private fun writeGpsData2Sdcard(location: Location) {
        Log.d(
            TAG,
            "write latitude and longitude, latitude: " + location.latitude + ", longitude: " + location.longitude
        )
        mGpsDataHandler!!.post(
            GpsDataSaver(
                mGpsDataFile, """
     ${location.latitude}, ${location.longitude}
     
     """.trimIndent()
            )
        )
    }



}