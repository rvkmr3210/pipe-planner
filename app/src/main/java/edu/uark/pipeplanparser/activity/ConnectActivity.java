package edu.uark.pipeplanparser.activity;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import edu.uark.pipeplanparser.R;
import edu.uark.pipeplanparser.model.Pipe;
import edu.uark.pipeplanparser.model.PipeSegment;
import edu.uark.pipeplanparser.util.TrailerCommunicatorUtil;

public class ConnectActivity extends AppCompatActivity implements TrailerCommunicatorUtil.TrailerCommunicatorUtilCallback, OnMapReadyCallback {

    private final String TAG = ConnectActivity.class.getSimpleName();

    public static final String EXTRA_PIPE = "EXTRA_PIPE";

    Button mConnectButton;
    ImageView mTractorImage;
    TextView mInfoText;
    Button mFinishButton;
    ViewFlipper mFlipper;
    TextView mDistanceText;

    ProgressBar mProgress;
    TextView mProgressText;
    Button mCancelButton;

    GoogleMap mMap;

    Pipe mPipe;

    TrailerCommunicatorUtil mTrailerCommunicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        } else if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mPipe = getIntent().getParcelableExtra(EXTRA_PIPE);

        mConnectButton = (Button) findViewById(R.id.action_connect);

        mFinishButton = (Button) findViewById(R.id.action_finish);
        mFinishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mInfoText = (TextView) findViewById(R.id.connect_info_text);
        mTractorImage = (ImageView) findViewById(R.id.tractor_image);

        mDistanceText = (TextView) findViewById(R.id.distance_label);
        mDistanceText.setText(R.string.distance_text_template_before);

        mProgress = (ProgressBar) findViewById(R.id.connect_progress);
        mProgressText = (TextView) findViewById(R.id.connect_progress_text);
        mCancelButton = (Button) findViewById(R.id.action_cancel);

        mFlipper = (ViewFlipper) findViewById(R.id.connect_flipper);

        mTrailerCommunicator = TrailerCommunicatorUtil.getCommunicator(this, mPipe);
        mTrailerCommunicator.setCallback(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTrailerCommunicator.register();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mTrailerCommunicator.unregister();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTrailerCommunicator.stop();
        mTrailerCommunicator.unregister();
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.action_connect:
                displayProgressBar(true);
                mTrailerCommunicator.connect();
                break;
            case R.id.action_cancel:
                displayProgressBar(false);
                mTrailerCommunicator.cancelDiscovery();
                break;
            case R.id.action_finish:
                finish();
                break;
        }
    }

    @Override
    public void connectionResult(boolean result) {
        if (result) {
            setUpMap();
        } else {
            displayProgressBar(false);
            displayError();
        }
    }

    @Override
    public void openingBluetoothSocket(BluetoothDevice device) {
        if (device == null) {
            mProgressText.setText("Looking for MIRI Trailers...");
        } else {
            mProgressText.setText(getString(R.string.connecting_to_printer, device.getName()));
        }
    }

    Marker mLocationMarker;

    @Override
    public void locationUpdated(LatLng latlng) {
        Log.d(TAG, "Location updated: " + latlng);

        if (mLocationMarker == null) {
            MarkerOptions options = new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.user_location))
                    .position(latlng)
                    .anchor(0.5f, 0.5f)
                    .draggable(false);
            mLocationMarker = mMap.addMarker(options);
        }

        mLocationMarker.setPosition(latlng);
    }

    @Override
    public void stringSent(String sentString) {
        Snackbar.make(mConnectButton, "Sent string: " + sentString, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void distanceUpdated(double distanceTraveled, PipeSegment inSegment) {
        mDistanceText.setText(String.format(getString(R.string.distance_text_template),
                inSegment.getHoleSize(),
                Math.max((15.24 - distanceTraveled) * 3.28084, 0.0))); // distance measured in meters, display in feet
    }

    public void setUpMap() {
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mFlipper.setDisplayedChild(1);
        mapFragment.getMapAsync(this);
    }

    private void displayProgressBar(boolean show) {
        if (show) {
            mProgress.setVisibility(View.VISIBLE);
            mProgressText.setVisibility(View.VISIBLE);
            mCancelButton.setVisibility(View.VISIBLE);
            mInfoText.setVisibility(View.INVISIBLE);
            mConnectButton.setVisibility(View.INVISIBLE);
            mTractorImage.setVisibility(View.INVISIBLE);
        } else {
            mProgress.setVisibility(View.INVISIBLE);
            mProgressText.setVisibility(View.INVISIBLE);
            mCancelButton.setVisibility(View.INVISIBLE);
            mInfoText.setVisibility(View.VISIBLE);
            mConnectButton.setVisibility(View.VISIBLE);
            mTractorImage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mTrailerCommunicator.result(requestCode, resultCode);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setAllGesturesEnabled(true);

        // sets the padding on the map to make room for the finish button underneath the google logo
        int mapPaddingDp = 8;  // 8 dps
        final float scale = getResources().getDisplayMetrics().density;
        int mapPaddingPx = (int) (mapPaddingDp * scale + 0.5f);
        mMap.setPadding(mapPaddingPx, mapPaddingPx, mapPaddingPx, mapPaddingPx * 7);

        mPipe.addToMap(mMap);
        LatLngBounds mapBounds = mPipe.getBounds();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mapBounds, 150));
    }
    public void displayError() {
        Snackbar.make(mConnectButton, getString(R.string.error_socket_opening), Snackbar.LENGTH_SHORT).show();
    }
}
