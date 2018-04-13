package edu.uark.pipeplanparser.model;

import android.os.Parcel;
import android.os.Parcelable;

public class PipePlanner implements Parcelable {

    private String comment;
    private String pipeFunction;
    private String pipeSize;
    private String holeSize;
    private String stationStart;
    private String stationEnd;
    private String furrowCount;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getPipeFunction() {
        return pipeFunction;
    }

    public void setPipeFunction(String pipeFunction) {
        this.pipeFunction = pipeFunction;
    }

    public String getPipeSize() {
        return pipeSize;
    }

    public void setPipeSize(String pipeSize) {
        this.pipeSize = pipeSize;
    }

    public String getHoleSize() {
        return holeSize;
    }

    public void setHoleSize(String holeSize) {
        this.holeSize = holeSize;
    }

    public String getStationStart() {
        return stationStart;
    }

    public void setStationStart(String stationStart) {
        this.stationStart = stationStart;
    }

    public String getStationEnd() {
        return stationEnd;
    }

    public void setStationEnd(String stationEnd) {
        this.stationEnd = stationEnd;
    }

    public String getFurrowCount() {
        return furrowCount;
    }

    public void setFurrowCount(String furrowCount) {
        this.furrowCount = furrowCount;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.comment);
        dest.writeString(this.pipeFunction);
        dest.writeString(this.pipeSize);
        dest.writeString(this.holeSize);
        dest.writeString(this.stationStart);
        dest.writeString(this.stationEnd);
        dest.writeString(this.furrowCount);
    }

    public PipePlanner() {
    }

    protected PipePlanner(Parcel in) {
        this.comment = in.readString();
        this.pipeFunction = in.readString();
        this.pipeSize = in.readString();
        this.holeSize = in.readString();
        this.stationStart = in.readString();
        this.stationEnd = in.readString();
        this.furrowCount = in.readString();
    }

    public static final Parcelable.Creator<PipePlanner> CREATOR = new Parcelable.Creator<PipePlanner>() {
        @Override
        public PipePlanner createFromParcel(Parcel source) {
            return new PipePlanner(source);
        }

        @Override
        public PipePlanner[] newArray(int size) {
            return new PipePlanner[size];
        }
    };
}
