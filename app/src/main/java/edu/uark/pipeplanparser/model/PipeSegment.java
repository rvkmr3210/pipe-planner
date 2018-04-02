package edu.uark.pipeplanparser.model;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

public class PipeSegment implements Mappable, Parcelable {

    private float mDiameter;
    private int mLengthFrom;
    private int mLengthTo;
    private String mFurrowsIrrigated;
    private int mHolesPerFurrow;
    private String mHoleSize;
    private int mFurrowCount;

    private LatLng mStart;
    private LatLng mEnd;

    private Polyline mPolyline;
    private Circle[] mCircles;

    protected PipeSegment(Parcel in) {
        mDiameter = in.readFloat();
        mLengthFrom = in.readInt();
        mLengthTo = in.readInt();
        mFurrowsIrrigated = in.readString();
        mHolesPerFurrow = in.readInt();
        mHoleSize = in.readString();
        mFurrowCount = in.readInt();
        mStart = in.readParcelable(LatLng.class.getClassLoader());
        mEnd = in.readParcelable(LatLng.class.getClassLoader());
    }

    public static final Creator<PipeSegment> CREATOR = new Creator<PipeSegment>() {
        @Override
        public PipeSegment createFromParcel(Parcel in) {
            return new PipeSegment(in);
        }

        @Override
        public PipeSegment[] newArray(int size) {
            return new PipeSegment[size];
        }
    };

    public void increaseDiameter() {
        mDiameter += 1;
    }

    public PipeSegment(float diameter, int lengthFrom, int lengthTo, String furrowsIrrigated, int holesPerFurrow, String holeSize, int furrowCount, LatLng start, LatLng end) {
        mDiameter = diameter;
        mLengthFrom = lengthFrom;
        mLengthTo = lengthTo;
        mFurrowsIrrigated = furrowsIrrigated;
        mHolesPerFurrow = holesPerFurrow;
        mHoleSize = holeSize;
        mFurrowCount = furrowCount;
        mStart = start;
        mEnd = end;
    }

    public PipeSegment(@NonNull List<String> toParse) {
        parseInformation(toParse);
    }

    private void parseInformation(List<String> toParse) {

        int index = 0;

        if (toParse == null || toParse.size() == 0) {
            return;
        }

        setDiameter(Float.parseFloat(toParse.get(index)));
        index++;

        while(index+1 < toParse.size() && !toParse.get(index+1).equals("-")) {
            index++;
        }

        setLengthFrom(Integer.parseInt(toParse.get(index)));
        index += 2;
        setLengthTo(Integer.parseInt(toParse.get(index)));
        index += 2;

        setHolesPerFurrow(Integer.parseInt(toParse.get(index)));
        index += 1;

        setHoleSize(toParse.get(index));
        index += 1;

        setFurrowCount(Integer.parseInt(toParse.get(index)));
        index += 1;

        setStartPoint(new LatLng(Double.parseDouble(toParse.get(index)), Double.parseDouble(toParse.get(index+1))));
        index += 2;

        setEndPoint(new LatLng(Double.parseDouble(toParse.get(index)), Double.parseDouble(toParse.get(index+1))));
    }

    @Override
    public void addToMap(@NonNull GoogleMap googleMap) {
        PolylineOptions polylineOptions = new PolylineOptions()
                .add(getStartPoint())
                .add(getEndPoint())
                .color(Color.RED)
                .width(4f)
                .zIndex(-1);

        mPolyline = googleMap.addPolyline(polylineOptions);

        mCircles = new Circle[2];

        CircleOptions circleOptions = new CircleOptions()
                .center(getStartPoint())
                .fillColor(Color.RED)
                .radius(3);

        mCircles[0] = googleMap.addCircle(circleOptions);

        circleOptions.center(getEndPoint());

        mCircles[1] = googleMap.addCircle(circleOptions);
    }

    @Override
    public void removeFromMap() {
        if (mPolyline != null) {
            mPolyline.remove();
            mPolyline = null;
        }

        if (mCircles != null && mCircles.length == 2) {
            for (Circle circle : mCircles) {
                if (circle != null) {
                    circle.remove();
                }
            }
            mCircles = null;
        }
    }

    public void setDiameter(float diameter) {
        mDiameter = diameter;
    }

    public float getDiameter() {
        return mDiameter;
    }

    public void setLengthFrom(int lengthFrom) {
        mLengthFrom = lengthFrom;
    }

    public int getLengthFrom() {
        return mLengthFrom;
    }

    public void setLengthTo(int lengthTo) {
        mLengthTo = lengthTo;
    }

    public int getLengthTo() {
        return mLengthTo;
    }

    public void setFurrowsIrrigated(String furrowsIrrigated) {
        mFurrowsIrrigated = furrowsIrrigated;
    }

    public String getFurrowsIrrigated() {
        return mFurrowsIrrigated;
    }

    public void setHolesPerFurrow(int holesPerFurrow) {
        mHolesPerFurrow = holesPerFurrow;
    }

    public int getHolesPerFurrow() {
        return mHolesPerFurrow;
    }

    public void setHoleSize(String holeSize) {
        mHoleSize = holeSize;
    }

    public String getHoleSize() {
        return mHoleSize;
    }

    public void setFurrowCount(int furrowCount) {
        mFurrowCount = furrowCount;
    }

    public int getFurrowCount() {
        return mFurrowCount;
    }

    public void setStartPoint(LatLng start) {
        mStart = start;
    }

    public LatLng getStartPoint() {
        return mStart;
    }

    public void setEndPoint(LatLng end) {
        mEnd = end;
    }

    public LatLng getEndPoint() {
        return mEnd;
    }

    public String toString() {
        return "" + mDiameter + ", " + mLengthFrom + "-" + mLengthTo + ", " + mFurrowsIrrigated +
                ", " + mHolesPerFurrow + ", " + mHoleSize + ", " + mFurrowCount + ", " + mStart.toString() + "; " + mEnd.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(mDiameter);
        dest.writeInt(mLengthFrom);
        dest.writeInt(mLengthTo);
        dest.writeString(mFurrowsIrrigated);
        dest.writeInt(mHolesPerFurrow);
        dest.writeString(mHoleSize);
        dest.writeInt(mFurrowCount);
        dest.writeParcelable(mStart, flags);
        dest.writeParcelable(mEnd, flags);
    }
}
