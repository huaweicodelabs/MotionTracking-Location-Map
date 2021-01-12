package com.huawei.hmsdemo.lbsad.java;

import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.TextView;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.common.ResolvableApiException;
import com.huawei.hms.location.FusedLocationProviderClient;
import com.huawei.hms.location.LocationAvailability;
import com.huawei.hms.location.LocationCallback;
import com.huawei.hms.location.LocationRequest;
import com.huawei.hms.location.LocationResult;
import com.huawei.hms.location.LocationServices;
import com.huawei.hms.location.LocationSettingsRequest;
import com.huawei.hms.location.LocationSettingsResponse;
import com.huawei.hms.location.LocationSettingsStatusCodes;
import com.huawei.hms.location.SettingsClient;
import com.huawei.hms.maps.CameraUpdate;
import com.huawei.hms.maps.CameraUpdateFactory;
import com.huawei.hms.maps.HuaweiMap;
import com.huawei.hms.maps.LocationSource;
import com.huawei.hms.maps.MapView;
import com.huawei.hms.maps.OnMapReadyCallback;
import com.huawei.hms.maps.model.BitmapDescriptorFactory;
import com.huawei.hms.maps.model.LatLng;
import com.huawei.hms.maps.model.Marker;
import com.huawei.hms.maps.model.MarkerOptions;
import com.huawei.hms.maps.model.PolylineOptions;
import com.huawei.hmsdemo.lbsad.java.bean.PathBean;
import com.huawei.hmsdemo.lbsad.R;

import java.io.File;
import java.text.DecimalFormat;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private MapView mMapView;
    private HuaweiMap mHwMap;
    private Marker mMarkerStart, mMarkerEnd;
    private LocationRequest mLocationRequest;
    private TextView mTvStart, mTvSpeed, mTvDistance;
    private Chronometer mTime;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private PathBean mPath = new PathBean();
    private long mSeconds = 0;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private DecimalFormat mDecimalFormat = new DecimalFormat("0.00");
    private PolylineOptions mPolylineOptions;
    private boolean mIsRunning = false;
    private LocationSource.OnLocationChangedListener mListener;
    private Runnable mTimeRunnable = new Runnable() {
        @Override
        public void run() {
            mTime.setText(formatSeconds());
            mHandler.postDelayed(this, 1000);
        }
    };
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    private static final String TAG = "MapActivity";
    private LocationCallback mLocationCallback;

    /**
     * GPS数据.
     */
    private HandlerThread mGpsDataThread;
    private Handler mGpsDataHandler;
    private File mGpsDataFile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_map_activity);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // check location settings
        checkLocationSettings();

        // init MapView
        mMapView = findViewById(R.id.hw_mapview);
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mMapView.onCreate(mapViewBundle);
        mMapView.getMapAsync(this);

//        添加运动数据展示UI
        mTvSpeed = findViewById(R.id.tv_speed);
        mTvDistance = findViewById(R.id.tv_distance);
        mTime = findViewById(R.id.cm_time);
        mTvStart = findViewById(R.id.tv_start);
        mTvStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processStartClick();
            }
        });


        //初始化Map Kit折线
        mPolylineOptions = new PolylineOptions();
        mPolylineOptions.color(getResources().getColor(R.color.colorAccent));
        mPolylineOptions.width(5f);

        // 记录GPS数据
        mGpsDataFile = new File(getExternalFilesDir(null), "GpsData.txt");
    }

    private void processStartClick() {
        if (mIsRunning) {
            mIsRunning = false;
            mPath.setEndTime(System.currentTimeMillis());
            mTvStart.setText("Start");
            mHandler.removeCallbacks(mTimeRunnable);

            if (mPath.getPathLine().size() > 0) {
                mPath.setEndPoint(mPath.getPathLine().get(mPath.getPathLine().size() - 1));
                if (null != mMarkerStart && null != mMarkerEnd) {
                    mMarkerStart.remove();
                    mMarkerEnd.remove();
                }
                MarkerOptions StartPointOptions = new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.map_marker_bubble_azure_small))
                        .position(mPath.getStartPoint());
                StartPointOptions.title("Start Point");
                StartPointOptions.snippet("Start Point");
                mMarkerStart = mHwMap.addMarker(StartPointOptions);
                MarkerOptions EndPointOptions = new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.map_marker_bubble_azure_small))
                        .position(mPath.getEndPoint());
                EndPointOptions.title("End Point");
                EndPointOptions.snippet("End Point");
                mMarkerEnd = mHwMap.addMarker(EndPointOptions);
            }
        } else {
            mIsRunning = true;
            mPath.reset();
            mPath.setStartTime(System.currentTimeMillis());
            mHandler.post(mTimeRunnable);
            mTvStart.setText("Stop");
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    protected void onDestroy() {
        removeLocationUpdatesWithCallback();
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        mMapView.onDestroy();
    }

    /**
     * Removed when the location update is no longer required.
     */
    private void removeLocationUpdatesWithCallback() {
        try {
            Task<Void> voidTask = fusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
            voidTask.addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "removeLocationUpdates Success");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "removeLocationUpdates Failure: " + e);
                }
            });
        } catch (Exception e) {

        }
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();


        mGpsDataThread.quitSafely();
        try {
            mGpsDataThread.join();
            mGpsDataThread = null;
            mGpsDataHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();

        mGpsDataThread = new HandlerThread("DotThread");
        mGpsDataThread.start();
        mGpsDataHandler = new Handler(mGpsDataThread.getLooper());
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    private void checkLocationSettings() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        LocationSettingsRequest locationSettingsRequest = builder.build();
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        requestLocationUpdate();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            ResolvableApiException rae = (ResolvableApiException) e;
                            rae.startResolutionForResult(MapActivity.this, 0);
                        } catch (IntentSender.SendIntentException sie) {

                        }
                        break;
                }
            }
        });
    }

    private void requestLocationUpdate() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                writeGpsData2Sdcard(locationResult.getLastLocation());

                if (mIsRunning) {
                    processLocationChange(locationResult.getLastLocation());
                }
            }

            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);
            }
        };
        fusedLocationProviderClient
                .requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "request location updates success");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "request location updates failed, error: " + e.getMessage());
                    }
                });
    }

    private void processLocationChange(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (mPath.getStartPoint() == null) {
            mPath.setStartPoint(latLng);
        }
        mPath.addPoint(latLng);
        float distance = mPath.updateDistance();
        double sportMile = distance / 1000d;
        if (mSeconds > 0) {
            double distribution = (double) mSeconds / 60d / sportMile;
            mPath.setDistribution(distribution);
            mTvSpeed.setText(mDecimalFormat.format(distribution));
            mTvDistance.setText(mDecimalFormat.format(sportMile));
        } else {
            mPath.setDistribution(0d);
            mTvSpeed.setText("0.00");
            mTvDistance.setText("0.00");
        }
        //在地图上绘制运动轨迹
        mPolylineOptions.add(latLng);
        mHwMap.addPolyline(mPolylineOptions);
        //更新Map镜头
        if (mListener != null) {
            mListener.onLocationChanged(location);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                    new LatLng(location.getLatitude(), location.getLongitude()), 15f);
            mHwMap.animateCamera(cameraUpdate);
        }
    }


    public String formatSeconds() {
        String hh = mSeconds / 3600 > 9 ? mSeconds / 3600 + "" : "0" + mSeconds
                / 3600;
        String mm = (mSeconds % 3600) / 60 > 9 ? (mSeconds % 3600) / 60 + ""
                : "0" + (mSeconds % 3600) / 60;
        String ss = (mSeconds % 3600) % 60 > 9 ? (mSeconds % 3600) % 60 + ""
                : "0" + (mSeconds % 3600) % 60;
        mSeconds++;
        return hh + ":" + mm;
    }

    @Override
    public void onMapReady(HuaweiMap huaweiMap) {
        mHwMap = huaweiMap;
        mHwMap.setMyLocationEnabled(true);
        mHwMap.getUiSettings().setZoomControlsEnabled(false);

       //添加位置源
        mHwMap.setLocationSource(new LocationSource() {
            @Override
            public void activate(OnLocationChangedListener onLocationChangedListener) {
                mListener = onLocationChangedListener;
            }

            @Override
            public void deactivate() {

            }
        });
        //获取当前定位，更新地图Camera
        try {
            Task<Location> lastLocation = fusedLocationProviderClient.getLastLocation();
            lastLocation.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    mListener.onLocationChanged(location);
                    if (mListener != null) {
                        mListener.onLocationChanged(location);
                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                                new LatLng(location.getLatitude(), location.getLongitude()), 15f);
                        mHwMap.animateCamera(cameraUpdate);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                }
            });
        } catch (Exception e) {

        }
    }

    private void writeGpsData2Sdcard(Location location) {
        Log.d(TAG, "write latitude and longitude, latitude: " + location.getLatitude() + ", longitude: " + location.getLongitude());
        mGpsDataHandler.post(new GpsDataSaver(mGpsDataFile, location.getLatitude() + ", " + location.getLongitude() + "\r\n"));
    }

}
