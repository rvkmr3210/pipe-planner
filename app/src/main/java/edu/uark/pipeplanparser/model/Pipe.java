package edu.uark.pipeplanparser.model;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import edu.uark.pipeplanparser.util.IconUtil;
import edu.uark.pipeplanparser.util.MapMathUtil;

public class Pipe implements Mappable, Parcelable {

    public static final String TAG = Pipe.class.getSimpleName();

    private List<PipeSegment> mPipeSegments = new ArrayList<>();

    private Polyline mPolyline;
    private List<Polyline> mFurrowLines = new ArrayList<>();
    private List<Marker> mPipeMarkers = new ArrayList<>();

    private int mFurrowAngle = 0;

    protected Pipe(Parcel in) {
        mFurrowAngle = in.readInt();
        in.readTypedList(mPipeSegments, PipeSegment.CREATOR);
    }

    public static final Creator<Pipe> CREATOR = new Creator<Pipe>() {
        @Override
        public Pipe createFromParcel(Parcel in) {
            return new Pipe(in);
        }

        @Override
        public Pipe[] newArray(int size) {
            return new Pipe[size];
        }
    };

    @NonNull
    public static LatLngBounds getBoundsFromPipes(@NonNull List<Pipe> pipes) {
        LatLngBounds.Builder latLngBoundsBuilder = new LatLngBounds.Builder();

        for (Pipe pipe : pipes) {
            List<LatLng> pipePoints = pipe.getPoints();
            for (LatLng ll : pipePoints) {
                latLngBoundsBuilder.include(ll);
            }
        }

        return latLngBoundsBuilder.build();
    }

    public LatLngBounds getBounds() {
        LatLngBounds.Builder latLngBoundsBuilder = new LatLngBounds.Builder();

        for (LatLng l : getPoints()) {
            latLngBoundsBuilder.include(l);
        }

        return latLngBoundsBuilder.build();
    }

    @NonNull
    public static List<Pipe> getPipesFromSegments(@NonNull List<PipeSegment> segments) {
        ArrayList<Pipe> pipes = new ArrayList<>();

        if (segments.size() == 0) {
            return pipes;
        }

        Pipe currentPipe = new Pipe();
        PipeSegment previousSegment = segments.get(0);
        currentPipe.addSegment(previousSegment);

        for (int i = 1; i < segments.size(); i++) {
            PipeSegment currentSegment = segments.get(i);

            if (!currentSegment.getStartPoint().equals(previousSegment.getEndPoint())) {
                pipes.add(currentPipe);
                currentPipe = new Pipe();
            }

            currentPipe.addSegment(currentSegment);
            previousSegment = currentSegment;
        }

        pipes.add(currentPipe);

        return pipes;
    }

    public Pipe() {

    }

    public Pipe(List<PipeSegment> segments) {
        mPipeSegments = segments;
    }

    public void addSegment(PipeSegment segment) {
        mPipeSegments.add(segment);
    }

    public List<LatLng> getPoints() {
        ArrayList<LatLng> points = new ArrayList<>();

        if (mPipeSegments.size() == 0) {
            return points;
        }

        for (PipeSegment pipeSegment : mPipeSegments) {
            points.add(pipeSegment.getStartPoint());
        }

        points.add(mPipeSegments.get(mPipeSegments.size() - 1).getEndPoint());

        return points;
    }

    @Override
    public void addToMap(@NonNull GoogleMap googleMap) {
        List<LatLng> points = getPoints();

        PolylineOptions polylineOptions = new PolylineOptions()
                .color(Color.RED)
                .width(5f)
                .addAll(points)
                .zIndex(-1);

        mPolyline = googleMap.addPolyline(polylineOptions);

        for (int i = 0; i < points.size(); i++) {
            LatLng point = points.get(i);
            Marker marker = googleMap.addMarker(createMarkerOptions(point, i));
            marker.hideInfoWindow();
            mPipeMarkers.add(marker);
        }
    }

    @Override
    public void removeFromMap() {
        if (mPolyline != null) {
            mPolyline.remove();
            mPolyline = null;
        }

        if (mPipeMarkers.size() > 0) {
            for (Marker marker : mPipeMarkers) {
                marker.remove();
            }
            mPipeMarkers.clear();
        }
    }

    public void setAngle(int angleDegrees) {
        mFurrowAngle = angleDegrees;
    }

    public void drawFurrows(GoogleMap mMap) {
        Log.d(TAG, "Drawing furrows");
        List<LatLng> points = getPoints();
        float furrowLineLength = MapMathUtil.getLength(points) * 2f;

        if (mFurrowLines.size() > 0) {
            for (Polyline polyline : mFurrowLines) {
                polyline.remove();
            }
            mFurrowLines.clear();
        }

        for (LatLng point : points) {
            LatLng otherPoint = MapMathUtil.getPointWithAngle(point, furrowLineLength, mFurrowAngle);
            PolylineOptions options = new PolylineOptions();
            options.add(point, otherPoint)
                   .color(Color.YELLOW)
                   .width(20f);
            mFurrowLines.add(mMap.addPolyline(options));
        }
    }

    public PipeSegment segmentForMarker(Marker marker) {
        int index = Integer.parseInt(marker.getTitle()) - 1;
        if (index < 0) index = 0;

        return mPipeSegments.get(index);
    }

    private MarkerOptions createMarkerOptions(LatLng point, int index) {
        MarkerOptions markerOptions = new MarkerOptions()
                .draggable(false)
                .title("" + index)
                .zIndex(1)
                .position(point)
                .icon(IconUtil.getPipeMarkerDescriptor())
                .anchor(0.5f, 0.5f);

        return markerOptions;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mFurrowAngle);
        dest.writeTypedList(mPipeSegments);
    }

    public PipeSegment getSegmentForLocation(LatLng mCurrentLocation) {
        // TODO Implement
        return mPipeSegments.get(0);
    }

    public void hideMarkerInfoWindows() {
        for (Marker marker : mPipeMarkers) {
            marker.hideInfoWindow();
        }
    }
}
