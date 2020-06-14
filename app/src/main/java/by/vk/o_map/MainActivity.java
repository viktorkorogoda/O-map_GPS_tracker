package by.vk.o_map;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.jenetics.jpx.GPX;
import io.jenetics.jpx.Track;
import io.jenetics.jpx.TrackSegment;

import static by.vk.o_map.Constants.CONTINUE;
import static by.vk.o_map.Constants.DATE_TIME_PATTERN;
import static by.vk.o_map.Constants.GPX_EXTENSION;
import static by.vk.o_map.Constants.PAUSE;
import static by.vk.o_map.Constants.SELECTED;
import static by.vk.o_map.Constants.START;
import static java.util.stream.Collectors.toList;

public class MainActivity extends AppCompatActivity {

    private static final String POINT = "point";
    private static final String LINEAR = "linear";
    private static final String ALL_TIME_TRACKS = "allTimeTracks";
    private static final String START_PAUSE_BUTTON_START = "start_pause_button_start";
    private static final String TAG = MainActivity.class.getSimpleName();
    private BtDeviceAdapter btDeviceAdapter;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private MyReceiver myReceiver;
    private GPSTrackerService mService = null;
    private boolean mBound = false;
    private GPX allTimeGpxTrack;
    private GPX allTimeGpxTrackSegment;
    private List<GPX> allTimeGpxSegments = new ArrayList<>();
    private GPX linearTrackGpx;

    private boolean createNewSegment;

    private Handler handler;
    private Runnable runnable;

    private AppBarConfiguration mAppBarConfiguration;
    private String selectedPointSign = "";
    private String selectedLinearSign = "";


    private float mMaxAcceptableAccuracy = 100;
    private Location mPreviousLocation;
    private float mDistance = 0;

    DecimalFormat decimalFormat = new DecimalFormat();

    private String action = "START";

    private final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {

                Log.i(TAG, "started");
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(TAG, "device found: " + device.getName() + "__:__ " + device.getAddress());
                // Create a new device item
                BtDevice newDevice = new BtDevice(device, false);
                // Add it to our adapter
                btDeviceAdapter.add(newDevice);
                btDeviceAdapter.notifyDataSetChanged();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

                Log.i(TAG, "finished");
            }
        }
    };


    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            GPSTrackerService.LocalBinder binder = (GPSTrackerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.point_signs_view, R.id.nav_slideshow)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(navigationView, navController);

        decimalFormat.setMaximumFractionDigits(2);
        ScrollView sView = findViewById(R.id.point_sign_scroll_view);
        sView.setVerticalScrollBarEnabled(false);
        sView.setHorizontalScrollBarEnabled(false);
        handler = new Handler();
        myReceiver = new MyReceiver();

        loadStartPauseButtonState();
        ImageButton selectPointSignButton = findViewById(R.id.select_point_sign_button);
        selectPointSignButton.setOnClickListener(view -> openPointSignSelector());

        ImageButton selectLinearButton = findViewById(R.id.select_linear_sign_button);
        selectLinearButton.setOnClickListener(view -> openLinearSignSelector());

        Button openBluetoothSearchButton = findViewById(R.id.bluetooth_search_btn);
        openBluetoothSearchButton.setOnClickListener(v -> {
            if (checkForBluetoothAvailable()) {
                openBluetoothSearchDialog();
            }
        });

        Button savePointButton = findViewById(R.id.save_point_button);
        savePointButton.setOnClickListener(v -> {

        });
        final ImageButton startPauseAllTimeTrackButton = findViewById(R.id.button_start_pause_all_time_track);
        startPauseAllTimeTrackButton.setTooltipText(START);
        final ImageButton stopAllTimeTrackButton = findViewById(R.id.button_stop_all_time_track);
        stopAllTimeTrackButton.setEnabled(false);

        startPauseAllTimeTrackButton.setOnClickListener(v -> {

            switch (startPauseAllTimeTrackButton.getTooltipText().toString()) {
                case START:
                    action = START;
                    break;
                case PAUSE:
                    action = PAUSE;
                    break;
                case CONTINUE:
                    action = CONTINUE;
                    break;
            }

            if (action.equals(START)) {
                mDistance = 0;
                if (allTimeGpxTrack == null) {
                    allTimeGpxTrack = GPX.builder().version(GPX.Version.V11).build();
                }
                allTimeGpxTrackSegment = GPX.builder().version(GPX.Version.V11).build();
                mService.requestLocationUpdates();
                createNewSegment = true;
                startPauseAllTimeTrackButton.setTooltipText(PAUSE);
                startPauseAllTimeTracking(false);
                setPausePic(startPauseAllTimeTrackButton);
            }
            if (action.equals(PAUSE)) {
                allTimeGpxSegments.add(allTimeGpxTrackSegment);
                startPauseAllTimeTrackButton.setTooltipText(CONTINUE);
                startPauseAllTimeTracking(true);
                mService.removeLocationUpdates();
                setStartPic(startPauseAllTimeTrackButton);
                allTimeGpxTrackSegment = null;
                mPreviousLocation = null;
            }
            if (action.equals(CONTINUE)) {
                allTimeGpxTrackSegment = GPX.builder().version(GPX.Version.V11).build();
                mService.requestLocationUpdates();
                startPauseAllTimeTrackButton.setTooltipText(PAUSE);
                createNewSegment = true;
                startPauseAllTimeTracking(false);
                setPausePic(startPauseAllTimeTrackButton);
            }
            stopAllTimeTrackButton.setEnabled(true);
        });

        stopAllTimeTrackButton.setOnClickListener(v -> {
            startPauseAllTimeTrackButton.setTooltipText(START);
            setStartPic(startPauseAllTimeTrackButton);
            stopAllTimeTrackButton.setEnabled(false);
            mService.removeLocationUpdates();
            if (allTimeGpxTrackSegment != null) {
                allTimeGpxSegments.add(allTimeGpxTrackSegment);
            }
            buildAllTimeGpxTrackFromSegments();
            stopAllTimeTracking();
            saveAllTimeTrack();
            allTimeGpxSegments.clear();
            allTimeGpxTrack = null;
            allTimeGpxTrackSegment = null;
            createNewSegment = true;
            mPreviousLocation = null;
            ((TextView) findViewById(R.id.distance)).setText("");
        });
        checkPermissions();
    }

    private void buildAllTimeGpxTrackFromSegments() {
        for (Track track : allTimeGpxTrack.getTracks()) {
            track.getSegments().clear();
            for (GPX allTimeGpxSegment : allTimeGpxSegments) {
                List<TrackSegment> trackSegments = allTimeGpxSegment.getTracks().stream()
                        .map(Track::getSegments)
                        .flatMap(Collection::stream)
                        .collect(toList());
                track.getSegments().addAll(trackSegments);
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mService.removeLocationUpdates();
        stopAllTimeTracking();
        saveAllTimeTrack();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, GPSTrackerService.class), mServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    private void loadStartPauseButtonState() {
        setStartPic(findViewById(R.id.button_start_pause_all_time_track));
    }

    private void setStartPic(ImageButton imageButton) {
        imageButton.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
    }

    private void setPausePic(ImageButton imageButton) {
        imageButton.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
    }

    private void initGridLayout(GridLayout pointSignsView, String prefix) {
        List<Integer> pointSetNames = getPicNames(prefix);
        int total = pointSetNames.size();
        int column = 8;
        GridLayout gridLayout = createGridLayout(pointSetNames, total, column);
        for (int i = 0, gridColumn = 0, gridRow = 0; i < total; i++, gridColumn++) {
            if (gridColumn == column) {
                gridColumn = 0;
                gridRow++;
            }
            gridLayout.addView(createImageView(pointSetNames.get(i), gridColumn, gridRow));
        }
        pointSignsView.addView(gridLayout);
    }

    private ImageView createImageView(final int imagesResource, int column, int row) {
        ImageView oImageView = new ImageView(this);
        oImageView.setImageResource(imagesResource);
        GridLayout.LayoutParams param = new GridLayout.LayoutParams();
        param.height = 100;
        param.width = 100;
        param.setGravity(Gravity.CENTER);
        param.columnSpec = GridLayout.spec(column);
        param.rowSpec = GridLayout.spec(row);
        oImageView.setLayoutParams(param);
        oImageView.setOnClickListener(v -> {
            Map<String, String> signName = getSingName(v.getResources().getResourceEntryName(imagesResource));
            if (signName.get(POINT) != null) {
                selectedPointSign = signName.get(POINT);
                showToast(selectedPointSign + SELECTED);
                ImageButton button = findViewById(R.id.select_point_sign_button);
                button.setImageResource(imagesResource);
            }
            if (signName.get(LINEAR) != null) {
                selectedLinearSign = signName.get(LINEAR);
                showToast(selectedLinearSign + SELECTED);
                ImageButton button = findViewById(R.id.select_linear_sign_button);
                button.setImageResource(imagesResource);
            }
        });
        oImageView.setOnLongClickListener(v -> {
            TextView signTitle = ((ConstraintLayout) v.getParent().getParent().getParent()).findViewById(R.id.point_scroll_container_sign_title);
            String signName = v.getResources().getResourceEntryName(imagesResource);
            if (signName.startsWith(POINT)) {
                setTextViewTitle(signTitle, signName);
            }
            if (signName.startsWith(LINEAR)) {
                setTextViewTitle(signTitle, signName);
            }
            return true;
        });
        return oImageView;
    }

    private void setTextViewTitle(TextView textView, String signName) {
        int stringId = getResources().getIdentifier(signName, "string", getPackageName());
        if (stringId != 0) {
            textView.setText(getResources().getString(stringId));
        } else {
            textView.setText("");
        }
    }

    private Map<String, String> getSingName(String resourceEntryName) {
        Map<String, String> signName = new HashMap<>();
        if (resourceEntryName != null) {
            if (resourceEntryName.startsWith(POINT)) {
                String[] strings = resourceEntryName.split(POINT + "_");
                if (strings.length > 0) {
                    signName.put(POINT, strings[1].replace("_", "."));
                }
            } else {
                String[] strings = resourceEntryName.split(LINEAR + "_");
                if (strings.length > 0) {
                    signName.put(LINEAR, strings[1].replace("_", "."));
                }
            }
        }
        return signName;
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT)
                .show();
    }

    private GridLayout createGridLayout(List<Integer> pointSetNames, int total, int column) {
        int row = total / column;
        GridLayout gridLayout = new GridLayout(this);
        gridLayout.setColumnCount(column);
        gridLayout.setRowCount(row + 1);

        GridLayout.LayoutParams gridParams = new GridLayout.LayoutParams();
        gridParams.height = GridLayout.LayoutParams.MATCH_PARENT;
        gridParams.width = GridLayout.LayoutParams.MATCH_PARENT;
        gridLayout.setLayoutParams(gridParams);
        return gridLayout;
    }

    private List<Integer> getPicNames(String prefix) {
        Field[] drawables = R.drawable.class.getDeclaredFields();
        List<Integer> picNames = new ArrayList<>();
        for (Field f : drawables) {
            try {
                if (f.getName().startsWith(prefix)) {
                    picNames.add(f.getInt(null));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return picNames;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public void openPointSignSelector() {
        LayoutInflater inflater = LayoutInflater.from(this);
        ConstraintLayout pointSignsView = (ConstraintLayout) inflater.inflate(R.layout.point_scroll_container, null);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setView(pointSignsView);
        AlertDialog alert = alertDialog.create();
        alert.show();
        initGridLayout(pointSignsView.findViewById(R.id.point_scroll_view), POINT);
    }

    public void openLinearSignSelector() {
        LayoutInflater inflater = LayoutInflater.from(this);
        ConstraintLayout linearSignsView = (ConstraintLayout) inflater.inflate(R.layout.linear_grid_layout, null);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setView(linearSignsView);
        AlertDialog alert = alertDialog.create();
        alert.show();
        initGridLayout(linearSignsView.findViewById(R.id.linear_scroll_view), LINEAR);
    }

    public void openBluetoothSearchDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        ConstraintLayout btDevicesLayout = (ConstraintLayout) inflater.inflate(R.layout.bt_search_layout, null);
        fillViewWithBtDevices(btDevicesLayout);
//        registerBtReceiver();
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setView(btDevicesLayout);
        alertDialog.setPositiveButton("OK", null);
        AlertDialog alert = alertDialog.create();
        alert.show();

//        Button searchBtButton = btDevicesLayout.findViewById(R.id.new_bt_search_button);
//        searchBtButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                btDeviceAdapter.getBtDevices().clear();
//                btDeviceAdapter.notifyDataSetChanged();
//                bluetoothAdapter.startDiscovery();
//            }
//        });
    }

    private void registerBtReceiver() {
        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(bReceiver, filter);

    }

    public void fillViewWithBtDevices(ConstraintLayout seatBtDevicesLayout) {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        List<BtDevice> btDeviceList = new ArrayList<>();
        for (BluetoothDevice bluetoothDevice : pairedDevices) {
            btDeviceList.add(new BtDevice(bluetoothDevice, false));
        }

        RecyclerView rvContacts = seatBtDevicesLayout.findViewById(R.id.bt_devices_recycler_view);

        btDeviceAdapter = new BtDeviceAdapter(btDeviceList);
        rvContacts.setAdapter(btDeviceAdapter);
        rvContacts.setLayoutManager(new LinearLayoutManager(this));
    }

    private boolean checkForBluetoothAvailable() {
        if (bluetoothAdapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.bt_not_available_title)
                    .setMessage(R.string.bt_not_available_text)
                    .setPositiveButton(R.string.bt_not_available_exit, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return false;
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBT, 1);
            }
            return true;
        }
    }

    @Override
    protected void onStop() {
        if (mBound) {
            unbindService(mServiceConnection);
            mBound = false;
        }
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                mService.requestLocationUpdates();
            } else {
                Snackbar.make(
                        findViewById(R.id.nav_host_fragment),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        })
                        .show();
            }
        }
    }

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra(GPSTrackerService.EXTRA_LOCATION);
            if (location != null) {
                Toast.makeText(MainActivity.this, Utils.getLocationText(location),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startPauseAllTimeTracking(boolean pause) {
        int delay = 1000;
        if (!pause) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    handler.postDelayed(this, delay);
                    Location location = mService.getmLocation();
                    if (isValidLocation(location)) {
                        buildGpx(location);
                        TextView distanceTextView = findViewById(R.id.distance);
                        if (mDistance > 0) {
                            distanceTextView.setText(decimalFormat.format(mDistance));
                            showToast("altitude: " + decimalFormat.format(location.getAltitude()));
                        }
                    }
                }
            };
            handler.postDelayed(runnable, delay);
        } else {
            if (runnable != null) {
                handler.removeCallbacks(runnable);
                Log.i(TAG, allTimeGpxTrack.toString());
            }
        }
    }

    private void plusDistance(Location location) {
        if (mPreviousLocation != null) {
            mDistance += mPreviousLocation.distanceTo(location);
        }
    }

    private boolean isValidLocation(Location location) {
        return location.getAltitude() > 0 && location.getSpeed() > 0;
    }

    private boolean isNotFarAwayFromPreviousLocation(Location location) {
        float distance = mPreviousLocation.distanceTo(location);
        boolean notFarAway = distance < mMaxAcceptableAccuracy;
        if (notFarAway) {
            return true;
        } else {
            mPreviousLocation = null;
            return false;
        }
    }

    private boolean isEqualLocation(Location location) {
        return mPreviousLocation == null || mPreviousLocation.getLatitude() == location.getLatitude() && mPreviousLocation.getLongitude() == location.getLongitude() && mPreviousLocation.getAltitude() == location.getAltitude();
    }

    private void stopAllTimeTracking() {
        if (runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    private void buildGpx(Location location) {
        if (allTimeGpxTrack != null && location != null) {
            if (action.equals(CONTINUE)) {
                addPointToLastSegment(location);
                Log.i(TAG, allTimeGpxTrack.getTracks().toString());
            }
            if (action.equals(START)) {
                if (createNewSegment) {
                    allTimeGpxTrack = allTimeGpxTrack
                            .toBuilder()
                            .addTrack(track -> track
                                    .addSegment(segment -> segment
                                            .addPoint(point -> point
                                                    .lat(location.getLatitude())
                                                    .lon(location.getLongitude())
                                                    .ele(location.getAltitude())
                                                    .speed(location.getSpeed())
                                                    .course(location.getBearing())
                                                    .time(location.getTime())
                                                    .build())))
                            .build();
                    createNewSegment = false;
                }
                Log.i(TAG, allTimeGpxTrack.getTracks().toString());
                addPointToLastSegment(location);
            }
            plusDistance(location);
            mPreviousLocation = location;
        }
    }

    private void addPointToLastSegment(Location location) {
        if (allTimeGpxTrackSegment.getTracks().isEmpty()) {
            allTimeGpxTrackSegment = allTimeGpxTrackSegment
                    .toBuilder()
                    .addTrack(track -> track
                            .addSegment(segment -> segment
                                    .addPoint(point -> point
                                            .lat(location.getLatitude())
                                            .lon(location.getLongitude())
                                            .ele(location.getAltitude())
                                            .speed(location.getSpeed())
                                            .course(location.getBearing())
                                            .time(location.getTime())
                                            .build())))
                    .build();
        } else {
            allTimeGpxTrackSegment = allTimeGpxTrackSegment.toBuilder()
                    .trackFilter()
                    .map(track -> track.toBuilder()
                            .map(segment -> segment.toBuilder()
                                    .addPoint(point -> point
                                            .lat(location.getLatitude())
                                            .lon(location.getLongitude())
                                            .ele(location.getAltitude())
                                            .speed(location.getSpeed())
                                            .course(location.getBearing())
                                            .time(location.getTime())
                                            .build())
                                    .build())
                            .build())
                    .build()
                    .build();
        }
        createNewSegment = false;
    }

    private void saveAllTimeTrack() {
        try {
            if (allTimeGpxTrack != null) {
                String path = getPackageFolderName();
                checkAndCreatePackageFolder(path);
                path = path + File.separator + ALL_TIME_TRACKS;
                checkFolderAndCreate(path);
                String time = new SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault()).format(Calendar.getInstance().getTime());
                GPX.write(allTimeGpxTrack, path + File.separator + time + GPX_EXTENSION);
                showToast("gpx " + path + File.separator + time + GPX_EXTENSION + " was saved");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkAndCreatePackageFolder(String path) {
        checkFolderAndCreate(path);
    }

    private String getPackageFolderName() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + getPackageName();
    }

    private void checkFolderAndCreate(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<>();
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            final String[] permissions = missingPermissions.toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS, grantResults);
        }
    }
}
