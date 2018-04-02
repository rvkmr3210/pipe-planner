package edu.uark.pipeplanparser.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class IconUtil {

    private static Bitmap mMarkerBitmap;

    public static BitmapDescriptor getPipeMarkerDescriptor() {
        return BitmapDescriptorFactory.fromBitmap(getPipeMarkerBitmap());
    }

    private static Bitmap getPipeMarkerBitmap() {
        if (mMarkerBitmap != null) {
            return mMarkerBitmap;
        } else {
            Bitmap.Config config = Bitmap.Config.ARGB_8888;
            mMarkerBitmap = Bitmap.createBitmap(64, 64, config);
            Canvas canvas = new Canvas(mMarkerBitmap);

            Paint middlePaint = new Paint();
            middlePaint.setColor(Color.RED);

            Paint strokePaint = new Paint();
            strokePaint.setColor(Color.WHITE);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(8f);

            canvas.drawCircle(32, 32, 32, middlePaint);
            canvas.drawCircle(32, 32, 28, strokePaint);

            return mMarkerBitmap;
        }
    }
}
