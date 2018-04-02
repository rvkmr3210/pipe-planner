package edu.uark.pipeplanparser.activity;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.dropbox.chooser.android.DbxChooser;
import com.google.android.gms.maps.model.LatLng;

import edu.uark.pipeplanparser.R;
import edu.uark.pipeplanparser.model.Pipe;
import edu.uark.pipeplanparser.model.PipeSegment;
import edu.uark.pipeplanparser.util.SnackbarUtil;
import edu.uark.pipeplanparser.util.TrailerCommunicatorUtil;

public class HelpActivity extends AppCompatActivity implements TrailerCommunicatorUtil.TrailerCommunicatorUtilCallback {

    TrailerCommunicatorUtil mTrailerCommunicator; // used for communicating to the trailer
    Pipe mPipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        mPipe = getIntent().getParcelableExtra("EXTRA_PIPE");

        mTrailerCommunicator = TrailerCommunicatorUtil.getCommunicator(this, mPipe);
        mTrailerCommunicator.setCallback(this);
        mTrailerCommunicator.connect();
    }

    @Override
    public void connectionResult(boolean result) {

    }

    @Override
    public void openingBluetoothSocket(BluetoothDevice device) {

    }

    @Override
    public void locationUpdated(LatLng latlng) {
        TextView locationTextView = (TextView) findViewById(R.id.location_textview);
        locationTextView.setText("Location (lat, lon): " + latlng.latitude + ", " + latlng.longitude);
    }

    @Override
    public void stringSent(String sentString) {

    }

    @Override
    public void distanceUpdated(double distance, PipeSegment inLevee) {

    }

}
