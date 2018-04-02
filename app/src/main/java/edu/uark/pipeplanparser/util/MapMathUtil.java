package edu.uark.pipeplanparser.util;

import android.graphics.PointF;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class MapMathUtil {

    public static final int NO_DIRECTION = -1;
    public static final int LEFT = 0;
    public static final int RIGHT = 1;

    /**
     * Due to the nature of polylines, if the point is on one side of the first segment, then it is
     * also on the same side for all other line segments.
     * @param testPoint
     * @param polyline
     * @return
     */
    public static int getSide(LatLng testPoint, List<LatLng> polyline) throws IllegalArgumentException {
        if (polyline == null || polyline.size() < 2) {
            throw new IllegalArgumentException("You must supply a LatLng List with at least two items.");
        }

        if (testPoint == null) {
            throw new IllegalArgumentException("You must supply a non-null LatLng value.");
        }

        LatLng llA = polyline.get(0);
        LatLng llB = polyline.get(1);
        LatLng llC = testPoint;

        PointF pointA = new PointF((float)llA.latitude, (float)llA.longitude);
        PointF pointB = new PointF((float)llB.latitude, (float)llB.longitude);
        PointF pointC = new PointF((float)llC.latitude, (float)llC.longitude);
        return isLeft(pointA, pointB, pointC) ? LEFT : RIGHT;
    }

    public static boolean isLeft(PointF a, PointF b, PointF c){
        return ((b.x - a.x)*(c.y - a.y) - (b.y - a.y)*(c.x - a.x)) > 0;
    }

    public static float getLength(List<LatLng> points) {

        float distance = 0f;

        for (int i = 1; i < points.size(); i++) {
            LatLng point1 = points.get(i - 1);
            LatLng point2 = points.get(i);

            distance += getDistance(point1, point2);
        }

        return distance;
    }

    private static float getDistance(LatLng a, LatLng b) {
        double xDifference = a.latitude - b.latitude;
        double yDifference = a.longitude - b.longitude;
        return (float) Math.sqrt(xDifference * xDifference + yDifference * yDifference);
    }

    public static LatLng getPointWithAngle(LatLng originPoint, float distance, int angle) {
        float radians = (float) Math.toRadians(angle);

        PointF point = new PointF((float) (distance * Math.cos(radians)), (float) (distance * Math.sin(radians)));

        LatLng newPoint = new LatLng(originPoint.latitude + point.x, originPoint.longitude + point.y);
        return newPoint;
    }

    public static LatLng linearInterpolate(LatLng start, LatLng end, float percentage) {
        double xDifference = end.latitude - start.latitude;
        double yDifference = end.longitude - start.longitude;

        return null;
    }

}
