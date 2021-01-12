package com.huawei.hmsdemo.lbsad.java;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.common.ResolvableApiException;
import com.huawei.hms.location.FusedLocationProviderClient;
import com.huawei.hms.location.LocationRequest;
import com.huawei.hms.location.LocationServices;
import com.huawei.hms.location.LocationSettingsRequest;
import com.huawei.hms.location.LocationSettingsResponse;
import com.huawei.hms.location.LocationSettingsStatusCodes;
import com.huawei.hms.location.SettingsClient;
import com.huawei.hmsdemo.lbsad.R;
import com.huawei.hmsdemo.lbsad.java.MapActivity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Handler mHandler = new Handler();
    private FusedLocationProviderClient fusedLocationProviderClient;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_start_running).setOnClickListener(this);
        findViewById(R.id.btn_mock_location).setOnClickListener(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // SDK<=28 所需权限动态申请
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            Log.i(TAG, "sdk <= 28 Q");
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                String[] strings =
                        {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
                ActivityCompat.requestPermissions(this, strings, 1);
            }
        } else {
            // SDK>28 所需权限动态申请，需添加“android.permission.ACCESS_BACKGROUND_LOCATION”权限
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this,
                    "android.permission.ACCESS_BACKGROUND_LOCATION") != PackageManager.PERMISSION_GRANTED) {
                String[] strings = {android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        "android.permission.ACCESS_BACKGROUND_LOCATION"};
                ActivityCompat.requestPermissions(this, strings, 2);
            }
        }


        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        LocationRequest mLocationRequest = new LocationRequest();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();
        //检查设备定位设置
        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        //设置满足定位条件，再发起位置请求
                        Log.d(TAG, "location setting success");

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        //设置不满足定位条件
                        int statusCode = ((ApiException) e).getStatusCode();
                        Log.d(TAG, "location setting status code " + statusCode);
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                try {
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    //调用startResolutionForResult可以弹窗提示用户打开相应权限
                                    rae.startResolutionForResult(MainActivity.this, 0);
                                } catch (IntentSender.SendIntentException sie) {
                                    //…
                                }
                                break;
                            default:
                                break;
                        }
                    }
                });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        fusedLocationProviderClient.setMockMode(false);

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_start_running) {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            startActivity(intent);
        } else if (view.getId() == R.id.btn_mock_location) {
            Task<Void> voidTask = fusedLocationProviderClient.setMockMode(true);
            voidTask.addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(MainActivity.this, "setMockMode onSuccess", Toast.LENGTH_SHORT).show();
                    mHandler.post(new Runnable() {

                        int point = 0;

                        @Override
                        public void run() {
                            if (point + 1 >= MockGpsData.POINTS.length) {
                                return;
                            }
                            double latitude = MockGpsData.POINTS[point++];
                            double longitude = MockGpsData.POINTS[point++];
                            final Location mockLocation = new Location(LocationManager.GPS_PROVIDER);
                            mockLocation.setLongitude(longitude);
                            mockLocation.setLatitude(latitude);
                            Task<Void> voidTask = fusedLocationProviderClient.setMockLocation(mockLocation);
                            voidTask.addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(Exception e) {
                                }
                            });
                            mHandler.postDelayed(this, 100);
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(MainActivity.this, "setMockMode onFailure:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onRequestPermissionsResult:PERMISSION_GRANTED");
        }
    }


}
