package edu.uark.pipeplanparser.util;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import edu.uark.pipeplanparser.R;
import edu.uark.pipeplanparser.model.Pipe;
import edu.uark.pipeplanparser.model.PipeSegment;

public class TrailerCommunicatorUtil {

    public interface TrailerCommunicatorUtilCallback {
        void connectionResult(boolean result);

        void openingBluetoothSocket(BluetoothDevice device);

        void locationUpdated(LatLng latlng);

        void stringSent(String sentString);

        void distanceUpdated(double distance, PipeSegment inPipe);
    }

    public static final String TAG = TrailerCommunicatorUtil.class.getSimpleName();

    public static final int BLUETOOTH = 0;
    public static final int USB = 1;

    private static final ScheduledExecutorService mWorker = Executors.newSingleThreadScheduledExecutor();

    private final int REQUEST_ENABLE_BT = 10;
    private final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private final UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private int mConnectionType = USB;

    private Context mContext;

    private BluetoothSocket mBluetoothSocket;
    private UsbSerialDevice mUsbSerial;

    private LatLng mCurrentLocation;
    private LatLng mLastLocation;
    private PipeSegment mCurrentSegment;

    private double mDistanceTraveled = 0.0;

    private boolean mPipeUnrollStarted = false;

    private boolean mShouldPollLocations = false;
    private Thread mLocationPoller;

    private Pipe mPipe;

    TrailerCommunicatorUtilCallback mCallback;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {

                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication
                            mConnectionType = USB;
                            setUpUsb(device);
                            mCallback.connectionResult(true);
                            finishConnection();
                        }
                    } else {
                        Log.d("ConnectActivity", "Permission denied for device " + device);
                        beginBluetoothDiscovery();
                    }
                }
            }
        }
    };

    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mBluetoothSocket != null) {
                return;
            }

            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                if (isValidMacAddress(device.getAddress())) {
                    Log.d(TAG, "onReceive: Found a bluetooth device called " + device.getName() + " on the waves. Opening socket");
                    if (BluetoothAdapter.getDefaultAdapter() != null) {
                        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    }

                    new ConnectTask(device).execute();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (mCallback != null) {
                    mCallback.connectionResult(false);
                    displaySnackbar(R.string.no_bluetooth_devices_found);
                }
            }
        }
    };

    private static TrailerCommunicatorUtil mCommunicator;

    private TrailerCommunicatorUtil(Context c, Pipe pipe) {
        mContext = c;
        mPipe = pipe;
        createLocationPoller();
    }

    public static TrailerCommunicatorUtil getCommunicator(Context c, Pipe pipe) {
        if (mCommunicator == null) {
            mCommunicator = new TrailerCommunicatorUtil(c, pipe);
        }

        mCommunicator.resetThread();

        return mCommunicator;
    }

    private void resetThread() {
        if (mLocationPoller.isAlive()) {
            mLocationPoller.interrupt();
        }
        createLocationPoller();
    }

    private void createLocationPoller() {
        mLocationPoller = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mShouldPollLocations) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        return;
                    }
                    String transmit = getString();
                    if (transmit != null) {
                        Log.d(TAG, "locationPoller: Received String: " + transmit);
                        String[] split = transmit.split(",");
                        if (split.length < 2) continue;

                        double latitude, longitude;

                        try {
                            latitude = Double.parseDouble(split[0]);
                            longitude = Double.parseDouble(split[1]);
                            if (longitude > 0) {
                                longitude *= -1;
                            }
                        } catch (Exception e) {
                            continue;
                        }

                        mLastLocation = mCurrentLocation;
                        mCurrentLocation = new LatLng(latitude, longitude);
                        if (mLastLocation == null) {
                            mLastLocation = mCurrentLocation;
                        }
                        evaluateLocation();
                        if (mCallback != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mCallback != null) {
                                        mCallback.locationUpdated(mCurrentLocation);
                                    }
                                }
                            });
                        }
                    }
                }
            }
        });
    }

    public void setCallback(TrailerCommunicatorUtilCallback callback) {
        mCallback = callback;
    }

    public PendingIntent getPermissionIntent() {
        return PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
    }

    public void register() {
        mContext.registerReceiver(mUsbReceiver, new IntentFilter(ACTION_USB_PERMISSION));
    }

    public void stop() {
        mShouldPollLocations = false;
        mLocationPoller.interrupt();
        if (mBluetoothSocket != null) {
            try {
                mBluetoothSocket.close();
            } catch (Exception e) {

            }
        }
        disconnect();
    }

    public void disconnect() {
        if (mConnectionType == USB && mUsbSerial != null) {
            mUsbSerial.close();
        } else if (mConnectionType == BLUETOOTH && mBluetoothSocket != null) {
            try {
                mBluetoothSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void registerBluetooth() {
        mShouldPollLocations = true;
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mContext.registerReceiver(mBluetoothReceiver, filter);
    }

    public void unregister() {
        mShouldPollLocations = false;
        mLocationPoller.interrupt();

        try {
            mContext.unregisterReceiver(mUsbReceiver);
        } catch (Exception e) {

        }
        try {
            mContext.unregisterReceiver(mBluetoothReceiver);
        } catch (Exception e) {

        }
    }

    private void setUpUsb(UsbDevice device) {
        if (device == null) return;
        Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.usb_successful, Snackbar.LENGTH_SHORT).show();
        UsbManager usbManager = getUsbManager();
        UsbDeviceConnection mConnection = usbManager.openDevice(device);
        mUsbSerial = UsbSerialDevice.createUsbSerialDevice(device, mConnection);
        mUsbSerial.open();
        mUsbSerial.setBaudRate(115200);
        mUsbSerial.setDataBits(UsbSerialInterface.DATA_BITS_8);
        mUsbSerial.setStopBits(UsbSerialInterface.STOP_BITS_1);
        mUsbSerial.setParity(UsbSerialInterface.PARITY_NONE);
        mUsbSerial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
    }

    private List<Integer> getValidArduinoIds() {
        ArrayList<Integer> arduinoIds = new ArrayList<>();
        arduinoIds.add(0x403);
        arduinoIds.add(0x2341);
        arduinoIds.add(0x2a03);
        return arduinoIds;
    }

    private UsbManager getUsbManager() {
        return (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
    }

    private boolean sendBytes(byte[] bytes) {
        switch (mConnectionType) {
            case BLUETOOTH:
                try {
                    //mBluetoothSocket.connect();
                    mBluetoothSocket.getOutputStream().write(bytes);
                    //mBluetoothSocket.close();

                    Log.d(TAG, "Sending bytes over Bluetooth: " + new String(bytes));
                    return true;
                } catch (Exception e) {
                    return false;
                }

            case USB:
                try {
                    mUsbSerial.write(bytes);
                    return true;
                } catch (Exception e) {
                    return false;
                }
        }

        return false;
    }

    private boolean sendString(final String string) {
        if (mCallback != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCallback.stringSent(string);
                }
            });
        }
        return sendBytes(string.getBytes());
    }

    private String getString() {
        byte[] result = getBytes();

        return (result == null) ? null : new String(result);
    }

    private byte[] getBytes() {
        if (mConnectionType == USB && mUsbSerial != null && mUsbSerial.open()) {
            byte[] buffer = new byte[1024];
            int bytesRead = mUsbSerial.syncRead(buffer, 5000);

            if (bytesRead <= 0) {
                return null;
            }

            return Arrays.copyOf(buffer, bytesRead);

        } else if (mConnectionType == BLUETOOTH && mBluetoothSocket != null && mBluetoothSocket.isConnected()) {
            byte[] buffer = new byte[1024];
            int bytesRead = 0;
            try {
                if (mBluetoothSocket.getInputStream().available() > 0) {
                    Thread.sleep(100);
                    bytesRead = mBluetoothSocket.getInputStream().read(buffer);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

            if (bytesRead <= 0) {
                return null;
            }

            return Arrays.copyOf(buffer, bytesRead);
        }

        return null;
    }

    public boolean isConnected() {
        return (mConnectionType == USB && mUsbSerial != null) || (mConnectionType == BLUETOOTH && mBluetoothSocket != null);
    }

    public void connect() {
        Log.d(TAG, "connect(): Beginning connection...");
        if (mCallback == null) {
            mCallback.openingBluetoothSocket(null);
        }
        UsbManager usbManager = getUsbManager();
        HashMap usbDevices = usbManager.getDeviceList();

        List<Integer> arduinoIds = getValidArduinoIds();

        UsbDevice usbDevice = null;

        if (!usbDevices.isEmpty()) {
            boolean connected = false;
            for (Object device : usbDevices.values()) {
                usbDevice = (UsbDevice) device;

                int deviceVID = usbDevice.getVendorId();
                if (arduinoIds.contains(deviceVID)) {
                    usbManager.requestPermission((UsbDevice) device, getPermissionIntent());
                    connected = true;
                }

                if (connected) break;
            }
            if (!connected) {
                Log.d(TAG, "connect(): No USB device found. Starting Bluetooth discovery...");
                beginBluetoothDiscovery();
            } else if (mCallback != null) {
                Log.d(TAG, "connect(): Usb device already connected. Finishing up...");
            }
        } else {
            Log.d(TAG, "connect(): No USB devices are plugged in. Defaulting to Bluetooth discovery...");
            beginBluetoothDiscovery();
        }
    }

    private void finishConnection() {
        Log.d(TAG, "Finishing connection. Sending \"START\" to trailer...");
        sendString("START");
        mShouldPollLocations = true;
        mLocationPoller.start();
    }

    private void beginBluetoothDiscovery() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Log.d(TAG, "beginBluetoothDiscovery: No bluetooth on this device");
            displaySnackbar(R.string.error_device_missing_bluetooth);
            // Device does not support Bluetooth
            return;
        }

        if (!btAdapter.isEnabled()) {
            Log.d(TAG, "beginBluetoothDiscovery: Bluetooth is disabled, requesting to enable");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            getActivity().startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Log.d(TAG, "beginBluetoothDiscovery: Bluetooth is enabled, checking for devices");
            checkForBluetoothDevices();
        }
    }

    private void evaluateLocation() {
        if (!isConnected()) {
            return;
        }

        if (mPipeUnrollStarted) {
            mDistanceTraveled += distanceBetween(mLastLocation, mCurrentLocation);
            mLastLocation = mCurrentLocation;

            updateCurrentLevee();
            if (mDistanceTraveled >= 15.24) {
                mDistanceTraveled = 0.0;
                // check which levee we're in
                if (mCurrentSegment != null) {
                    sendHoles(mCurrentSegment);
                }
            }

            // update distance label
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCallback.distanceUpdated(mDistanceTraveled, mCurrentSegment);
                }
            });
            // we also don't want to display less than 0
        } else {
            // check entry into one of the levees
            updateCurrentLevee();
            if (mCurrentSegment != null) {
                mPipeUnrollStarted = true;
                Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        if (mCurrentSegment != null)
                            sendHoles(mCurrentSegment);
                    }
                };

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.distanceUpdated(50.0, mCurrentSegment);
                    }
                });
                // We delay sending the initial holes value over USB. The Arduino seems to restart upon
                // initial serial port opening, so we wait for that to finish (5 seconds SHOULD be enough)
                mWorker.schedule(task, 5000, TimeUnit.MILLISECONDS);
            }
        }
    }

    private Activity getActivity() {
        return (Activity) mContext;
    }

    private void updateCurrentLevee() {
        PipeSegment newSegment = mPipe.getSegmentForLocation(mCurrentLocation);

        if (newSegment != mCurrentSegment) {
            mCurrentSegment = newSegment;
            sendString("CHANGE -> -> -> -> -> ->");
            sendHoles(newSegment);
            mDistanceTraveled = 0.0;
        }
    }

    private void sendHoles(PipeSegment segment) {
        if (segment != null) {
            sendString(segment.getHoleSize());
        }
    }

    public void result(int requestCode, int resultCode) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
            checkForBluetoothDevices();
        }
    }

    private float distanceBetween(LatLng first, LatLng second) {
        float[] results = new float[1];
        Location.distanceBetween(first.latitude, first.longitude, second.latitude, second.longitude, results);
        return results[0];
    }

    private List<String> getBluetoothAddresses() {
        return Arrays.asList(mContext.getResources().getStringArray(R.array.bluetooth_mac_addresses));
    }

    private boolean isValidMacAddress(String address) {

        return true;

//        for (String validAddress : getBluetoothAddresses()) {
//            if (address.equalsIgnoreCase(validAddress)) {
//                return true;
//            }
//        }
//        return false;
    }

    private void checkForBluetoothDevices() {
        Set<BluetoothDevice> pairedDevices;
        if (BluetoothAdapter.getDefaultAdapter() != null) {
            pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        } else {
            return;
        }
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            BluetoothDevice foundDevice = null;

            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                Log.d(TAG, "checkForBluetoothDevices: Device: " + device.getName() + " | " + device.getAddress());

                List<String> addresses = getBluetoothAddresses();
                for (String s : addresses) {
                    if (s.equalsIgnoreCase(device.getAddress())) {
                        foundDevice = device;
                        Log.d(TAG, "checkForBluetoothDevices: Valid MIRI printer found and is connected");
                        break;
                    }
                }
                if (foundDevice != null) break;
            }

            if (foundDevice != null) {
                Log.d(TAG, "checkForBluetoothDevices: Opening socket for already connected device: " + foundDevice.getName());
                if (mCallback != null) {
                    mCallback.openingBluetoothSocket(foundDevice);
                }

                new ConnectTask(foundDevice).execute();
//                try {
//                    mBluetoothSocket = foundDevice.createInsecureRfcommSocketToServiceRecord(APP_UUID);
//                    mConnectionType = BLUETOOTH;
//                    mCallback.connectionResult(true);
//                    finishConnection();
//                } catch (IOException e) {
//                    mCallback.connectionResult(false);
//                }
                //new ConnectTask(foundDevice).execute();
                return;
            }
        }

        registerBluetooth();
        // now check the radio waves for devices to connect to
        if (BluetoothAdapter.getDefaultAdapter() != null) {
            BluetoothAdapter.getDefaultAdapter().startDiscovery();
        }
    }

    public void cancelDiscovery() {
        if (BluetoothAdapter.getDefaultAdapter() != null) {
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        }
        if (mCallback != null) {
            mCallback.connectionResult(false);
        }
    }

    private void displaySnackbar(int stringId) {
        View view = getActivity().findViewById(android.R.id.content);
        Snackbar.make(view, stringId, Snackbar.LENGTH_SHORT).show();
    }

    private class ConnectTask extends AsyncTask<Void, Void, Boolean> {
        private final BluetoothSocket mmSocket;

        ConnectTask(BluetoothDevice device) {
            BluetoothSocket tmp = null;


            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createInsecureRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmSocket = tmp;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (BluetoothAdapter.getDefaultAdapter() != null) {
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            }

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                if (!mmSocket.isConnected())
                    mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                connectException.printStackTrace();
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    closeException.printStackTrace();
                }
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mBluetoothSocket = mmSocket;
                if (mCallback != null) {
                    mConnectionType = BLUETOOTH;
                    mCallback.connectionResult(true);
                    finishConnection();
                }
            } else {
                if (mCallback != null) {
                    mCallback.connectionResult(false);
                }
            }
        }
    }
}
