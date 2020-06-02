package by.vk.o_map;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    private static final String POINT = "point";
    private static final String LINEAR = "linear";
    private BtDeviceAdapter btDeviceAdapter;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private AppBarConfiguration mAppBarConfiguration;
    private String selectedPointSign = "";
    private String selectedLinearSign = "";

    private final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {

                Log.i("myActivity", "started");
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i("myActivity", "device found: " + device.getName() + "__:__ " + device.getAddress());
                // Create a new device item
                BtDevice newDevice = new BtDevice(device, false);
                // Add it to our adapter
                btDeviceAdapter.add(newDevice);
                btDeviceAdapter.notifyDataSetChanged();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){

                Log.i("myActivity", "finished");
            }
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

        ScrollView sView = findViewById(R.id.point_sign_scroll_view);
        sView.setVerticalScrollBarEnabled(false);
        sView.setHorizontalScrollBarEnabled(false);

        ImageButton selectPointSignButton = findViewById(R.id.select_point_sign_button);
        selectPointSignButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                openPointSignSelector();
            }
        });
        
        ImageButton selectLinearButton = findViewById(R.id.select_linear_sign_button);
        selectLinearButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                openLinearSignSelector();
            }
        });

        Button openBluetoothSearchButton = findViewById(R.id.bluetooth_search_btn);
        openBluetoothSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkForBluetoothAvailable()) {
                    openBluetoothSearchDialog();
                }
            }
        });

         Button savePointButton = findViewById(R.id.save_point_button);
         savePointButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {

             }
         });
    }

    private void initGridLayout(GridLayout pointSignsView, String prefix) {
        List<Integer> pointSetNames = getPicNames(prefix);
        int total = pointSetNames.size();
        int column = 8;
        GridLayout gridLayout = createGridLayout(pointSetNames, total, column);
        for(int i =0, gridColumn = 0, gridRow = 0; i < total; i++, gridColumn++) {
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
        oImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, String> signName = getSingName(v.getResources().getResourceEntryName(imagesResource));
                if (signName.get(POINT) != null) {
                    selectedPointSign = signName.get(POINT);
                    showToast(selectedPointSign + " selected");
                    ImageButton button = findViewById(R.id.select_point_sign_button);
                    button.setImageResource(imagesResource);
                }
                if (signName.get(LINEAR) != null) {
                    selectedLinearSign = signName.get(LINEAR);
                    showToast(selectedLinearSign + " selected");
                    ImageButton button = findViewById(R.id.select_linear_sign_button);
                    button.setImageResource(imagesResource);
                }
            }
        });
        oImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                TextView signTitle = ((ConstraintLayout)v.getParent().getParent().getParent()).findViewById(R.id.point_scroll_container_sign_title);
                String signName = v.getResources().getResourceEntryName(imagesResource);
                if (signName.startsWith(POINT)) {
                    setTextViewTitle(signTitle, signName);
                }
                if (signName.startsWith(LINEAR)) {
                    setTextViewTitle(signTitle, signName);
                }
                return true;
            }
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
        // Inflate the menu; this adds items to the action bar if it is present.
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
        initGridLayout((GridLayout) pointSignsView.findViewById(R.id.point_scroll_view), POINT);
    }

    public void openLinearSignSelector() {
        LayoutInflater inflater = LayoutInflater.from(this);
        ConstraintLayout linearSignsView = (ConstraintLayout) inflater.inflate(R.layout.linear_grid_layout, null);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setView(linearSignsView);
        AlertDialog alert = alertDialog.create();
        alert.show();
        initGridLayout((GridLayout) linearSignsView.findViewById(R.id.linear_scroll_view), LINEAR);
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
}
