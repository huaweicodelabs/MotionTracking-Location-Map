package com.huawei.hmsdemo.lbsad.java.bean;

import android.os.Parcel;
import android.os.Parcelable;

import com.huawei.hms.maps.common.util.DistanceCalculator;
import com.huawei.hms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Store a run path
 */
public class PathBean implements Parcelable {

    private LatLng mStartPoint;

    private LatLng mEndPoint;

    private List<LatLng> mPathLinePoints = new ArrayList<>();

    private float mDistance;
    private Long mDuration;
    private Long mStartTime;
    private Long mEndTime;
    private Double mSpeed;
    private Double distribution;

    public PathBean() {

    }

    public void reset() {
        mDistance = 0f;
        mDuration = 0l;
        mStartTime = 0l;
        mEndTime = 0l;
        mSpeed = 0d;
        mSpeed = 0d;
        mPathLinePoints.clear();
        mStartPoint = null;
        mEndPoint = null;
    }

    public LatLng getStartPoint() {
        return mStartPoint;
    }

    public void setStartPoint(LatLng startpoint) {
        this.mStartPoint = startpoint;
    }

    public LatLng getEndPoint() {
        return mEndPoint;
    }

    public void setEndPoint(LatLng endpoint) {
        this.mEndPoint = endpoint;
    }

    public List<LatLng> getPathLine() {
        return mPathLinePoints;
    }

    public void setPathLine(List<LatLng> pathLine) {
        this.mPathLinePoints = pathLine;
    }

    public float getDistance() {
        return mDistance;
    }

    public void setDistance(float distance) {
        this.mDistance = distance;
    }

    public Long getDuration() {
        return mDuration;
    }

    public void setDuration(Long duration) {
        this.mDuration = duration;
    }

    public Long getStartTime() {
        return mStartTime;
    }

    public void setStartTime(Long mStartTime) {
        this.mStartTime = mStartTime;
    }

    public Long getEndTime() {
        return mEndTime;
    }

    public void setEndTime(Long mEndTime) {
        this.mEndTime = mEndTime;
    }

    public void addPoint(LatLng point) {
        mPathLinePoints.add(point);
    }

    public Double getSpeed() {
        return mSpeed;
    }

    public void setSpeed(Double mSpeed) {
        this.mSpeed = mSpeed;
    }

    public void setDistribution(double distribution) {
        this.distribution = distribution;
    }

    public float updateDistance() {
        float distance = 0;
        if (mPathLinePoints == null || mPathLinePoints.size() == 0) {
            return mDistance;
        }
        for (int i = 0; i < mPathLinePoints.size() - 1; i++) {
            LatLng firstLatLng = mPathLinePoints.get(i);
            LatLng secondLatLng = mPathLinePoints.get(i + 1);
            double betweenDis = DistanceCalculator.computeDistanceBetween(firstLatLng,
                    secondLatLng);
            distance = (float) (distance + betweenDis);
        }
        mDistance = distance;
        return distance;
    }


    @Override
    public String toString() {
        StringBuilder record = new StringBuilder();
        record.append("recordSize:" + getPathLine().size() + ", ");
        record.append("distance:" + getDistance() + "m, ");
        record.append("duration:" + getDuration() + "s");
        return record.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mStartPoint, flags);
        dest.writeParcelable(this.mEndPoint, flags);
        dest.writeTypedList(this.mPathLinePoints);
        dest.writeValue(this.mDistance);
        dest.writeValue(this.mDuration);
        dest.writeValue(this.mStartTime);
        dest.writeValue(this.mEndTime);
        dest.writeValue(this.mSpeed);
    }

    protected PathBean(Parcel in) {
        this.mStartPoint = in.readParcelable(LatLng.class.getClassLoader());
        this.mEndPoint = in.readParcelable(LatLng.class.getClassLoader());
        this.mPathLinePoints = in.createTypedArrayList(LatLng.CREATOR);
        this.mDistance = (Float) in.readValue(Float.class.getClassLoader());
        this.mDuration = (Long) in.readValue(Long.class.getClassLoader());
        this.mStartTime = (Long) in.readValue(Long.class.getClassLoader());
        this.mEndTime = (Long) in.readValue(Long.class.getClassLoader());
        this.mSpeed = (Double) in.readValue(Double.class.getClassLoader());
    }

    public static final Creator<PathBean> CREATOR = new Creator<PathBean>() {
        @Override
        public PathBean createFromParcel(Parcel source) {
            return new PathBean(source);
        }

        @Override
        public PathBean[] newArray(int size) {
            return new PathBean[size];
        }
    };
}