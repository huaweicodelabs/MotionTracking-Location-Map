package com.huawei.hmsdemo.lbsad.kotlin.bean

import android.os.Parcel
import android.os.Parcelable
import com.huawei.hms.maps.common.util.DistanceCalculator
import com.huawei.hms.maps.model.LatLng
import java.util.*

/**
 * Store a run path
 */
class PathBean : Parcelable {
    var mStartPoint: LatLng? = null
    var mEndPoint: LatLng? = null
    var mPathLinePoints: MutableList<LatLng>? = ArrayList<LatLng>()
    var distance = 0f
    var duration: Long? = null
    var startTime: Long? = null
    var endTime: Long? = null
    var speed: Double? = null
    var distribution: Double? = null

    constructor() {}

    fun reset() {
        distance = 0f
        duration = 0L
        startTime = 0L
        endTime = 0L
        speed = 0.0
        speed = 0.0
        mPathLinePoints!!.clear()
        mStartPoint = null
        mEndPoint = null
    }

    var startPoint: LatLng?
        get() = mStartPoint
        set(startpoint) {
            mStartPoint = startpoint
        }
    var endPoint: LatLng?
        get() = mEndPoint
        set(endpoint) {
            mEndPoint = endpoint
        }
    val pathLine: List<Any>?
        get() = mPathLinePoints

    fun setPathLine(pathLine: MutableList<LatLng>?) {
        mPathLinePoints = pathLine
    }

    fun addPoint(point: LatLng) {
        mPathLinePoints!!.add(point)
    }

    fun setDistribution(distribution: Double) {
        this.distribution = distribution
    }

    fun updateDistance(): Float {
        var distance = 0f
        if (mPathLinePoints == null || mPathLinePoints!!.size == 0) {
            return this.distance
        }
        for (i in 0 until mPathLinePoints!!.size - 1) {
            val firstLatLng: LatLng = mPathLinePoints!![i]
            val secondLatLng: LatLng = mPathLinePoints!![i + 1]
            val betweenDis: Double = DistanceCalculator.computeDistanceBetween(
                firstLatLng,
                secondLatLng
            )
            distance = (distance + betweenDis).toFloat()
        }
        this.distance = distance
        return distance
    }

    override fun toString(): String {
        val record = StringBuilder()
        record.append("recordSize:" + pathLine!!.size + ", ")
        record.append("distance:" + distance + "m, ")
        record.append("duration:" + duration + "s")
        return record.toString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(mStartPoint, flags)
        dest.writeParcelable(mEndPoint, flags)
        dest.writeTypedList(mPathLinePoints)
        dest.writeValue(distance)
        dest.writeValue(duration)
        dest.writeValue(startTime)
        dest.writeValue(endTime)
        dest.writeValue(speed)
    }

    protected constructor(`in`: Parcel) {
        mStartPoint = `in`.readParcelable(LatLng::class.java.classLoader)
        mEndPoint = `in`.readParcelable(LatLng::class.java.classLoader)
        mPathLinePoints = `in`.createTypedArrayList<LatLng>(LatLng.CREATOR)
        distance = (`in`.readValue(Float::class.java.classLoader) as Float?)!!
        duration = `in`.readValue(Long::class.java.classLoader) as Long?
        startTime = `in`.readValue(Long::class.java.classLoader) as Long?
        endTime = `in`.readValue(Long::class.java.classLoader) as Long?
        speed = `in`.readValue(Double::class.java.classLoader) as Double?
    }

    companion object {
        val CREATOR: Parcelable.Creator<PathBean?> = object : Parcelable.Creator<PathBean?> {
            override fun createFromParcel(source: Parcel): PathBean? {
                return PathBean(source)
            }

            override fun newArray(size: Int): Array<PathBean?> {
                return arrayOfNulls(size)
            }
        }
    }
}