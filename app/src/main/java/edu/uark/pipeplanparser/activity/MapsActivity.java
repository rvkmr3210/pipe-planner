package edu.uark.pipeplanparser.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.ui.IconGenerator;

import java.util.ArrayList;

import edu.uark.pipeplanparser.R;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int PERMISSION_REQUEST_CODE = 200;
    private GoogleMap mMap;
    ArrayList<LatLng> points;
    ArrayList<LatLng> referencePoints;
    double[] distances = {0.0, 41.0, 250.0, 426.0, 551.0, 644.0, 732.0, 770.0};
    private GPSTracker gps;
    private double distance = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        if (!checkLocationPermission()) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        points = new ArrayList<LatLng>();
        referencePoints = new ArrayList<>();
    }

    private boolean checkLocationPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION);
        return result == PackageManager.PERMISSION_GRANTED;
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        gps = new GPSTracker(MapsActivity.this);

        // check if GPS enabled
        if (gps.canGetLocation()) {
            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();
            LatLng sydney = new LatLng(gps.getLatitude(), gps.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(sydney)
                    .title("Current Location"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(gps.getLatitude(), gps.getLongitude()), 15.0f));

            // \n is for new line
            Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
        }

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {
                referencePoints.add(point);

                if (referencePoints.size() > 1) {
                    if (referencePoints.size() <= distances.length) {
                        LatLng destinationPoint = getDestinationPoint(points.get(points.size() - 1),
                                calculateBearing(points.get(points.size() - 1), point),
                                distances[referencePoints.size() - 1] - distances[referencePoints.size() - 2]);
                        points.add(destinationPoint);

                    } else {
                        Toast.makeText(MapsActivity.this, "No More Stations", Toast.LENGTH_SHORT).show();
                    }

                } else points.add(point);
                if (points.size() > 1) {
                    distance = distance + distance(points.get(points.size() - 2),points.get(points.size() - 1));
                }

                // Instantiating the class MarkerOptions to plot marker on the map
                MarkerOptions markerOptions = new MarkerOptions();

                // Setting latitude and longitude of the marker position
                markerOptions.position(points.get(points.size() - 1));

                // Setting titile of the infowindow of the marker
                markerOptions.title("Station : "+points.size());

                // Setting the content of the infowindow of the marker
                markerOptions.snippet("distance :" + distance);

                // Setting custom marker icon
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.user_location));

                //Specifies the anchor to be at a particular point in the marker image.
                markerOptions.anchor(0.5f, 0.5f);

                // Instantiating the class PolylineOptions to plot polyline in the map
                PolylineOptions polylineOptions = new PolylineOptions();

                // Setting the color of the polyline
                polylineOptions.color(Color.BLUE);

                // Setting the width of the polyline
                polylineOptions.width(15);

                // Setting points of polyline
                polylineOptions.addAll(points);

                // Adding the polyline to the map
                mMap.addPolyline(polylineOptions);

                // Adding the marker to the map
                mMap.addMarker(markerOptions);

              /*  ArrayList<String> arrayForPoliline = new ArrayList<String>();
                arrayForPoliline.add("41");
                arrayForPoliline.add("250");
                arrayForPoliline.add("426");
                arrayForPoliline.add("551");
                arrayForPoliline.add("644");
                arrayForPoliline.add("732");
                arrayForPoliline.add("770");

                if (points.size() > 0) {

                    for (LatLng latLng : points) {


                        String s[] = latLng.toString().replaceAll("lat/lng:", "").split(",");


                        for (String s1 : arrayForPoliline) {
                            if (Double.parseDouble(s1) == distance(Double.parseDouble(s[0].replaceAll("[()]", "")),
                                    Double.parseDouble(s[1].replaceAll("[()]", "")),
                                    Float.parseFloat(String.valueOf(point.latitude)), Float.parseFloat(String.valueOf(point.longitude)))) {
                                Toast.makeText(getApplicationContext(), "Your  distance  - \nLat: " + distance(Double.parseDouble(s[0].replaceAll("[()]", "")), Double.parseDouble(s[1].replaceAll("[()]", "")),
                                        Float.parseFloat(String.valueOf(point.latitude)), Float.parseFloat(String.valueOf(point.longitude))), Toast.LENGTH_LONG).show();
                                // Setting points of polyline
                                polylineOptions.addAll(points);

                                // Adding the polyline to the map
                                mMap.addPolyline(polylineOptions);

                                // Adding the marker to the map
                                mMap.addMarker(markerOptions);
                            }
                        }


                    }
                }
*/

            }
        });

        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {

            @Override
            public void onMapLongClick(LatLng point) {
                // Clearing the markers and polylines in the google map
                mMap.clear();
                points.clear();
                referencePoints.removeAll(referencePoints);
                distance = 0;
            }
        });
    }


    private double distance(LatLng from,LatLng to) {
        double distance= SphericalUtil.computeDistanceBetween(from,to);
        return distance*3.28084;

    }

    public double calculateBearing(LatLng src, LatLng dest) {
        Log.d("bearing",""+SphericalUtil.computeHeading(src, dest));
        return SphericalUtil.computeHeading(src, dest);
    }

    private LatLng getDestinationPoint(LatLng source, double brng, double dist) {
        LatLng dest=SphericalUtil.computeOffset(source, (dist * 0.3048), brng);
        Log.d("destLocation", "" + dest);
        return dest ;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {

                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (locationAccepted) {
                        Toast.makeText(MapsActivity.this, "Permission Granted, Now you can access location data ", Toast.LENGTH_LONG).show();
                        gps = new GPSTracker(MapsActivity.this);

                        // check if GPS enabled
                        if (gps.canGetLocation()) {


                            double latitude = gps.getLatitude();
                            double longitude = gps.getLongitude();
                            LatLng sydney = new LatLng(gps.getLatitude(), gps.getLongitude());
                            mMap.addMarker(new MarkerOptions()
                                    .position(sydney)
                                    .title("Current Location"));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(gps.getLatitude(), gps.getLongitude()), 12.0f));

                            // \n is for new line
                            Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
                        } else {
                            // can't get location
                            // GPS or Network is not enabled
                            // Ask user to enable GPS/network in settings
                            gps.showSettingsAlert();
                        }
                    } else {

                        Toast.makeText(MapsActivity.this, "Permission Denied, You cannot access location data ", Toast.LENGTH_LONG).show();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)) {
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            requestPermissions(new String[]{ACCESS_FINE_LOCATION},
                                                    PERMISSION_REQUEST_CODE);
                                        }
                                    }
                                };
                                return;
                            }
                        }

                    }
                }
                break;
        }
    }


}
