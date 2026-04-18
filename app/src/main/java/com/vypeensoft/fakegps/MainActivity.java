package com.vypeensoft.fakegps;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.vypeensoft.fakegps.model.LocationPoint;
import com.vypeensoft.fakegps.service.MockLocationService;
import android.os.Environment;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;

import com.vypeensoft.fakegps.utils.GpxParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawer;
    private TextView tvStatus, tvLat, tvLon;
    private Spinner spinnerGpx;
    private ImageButton btnRefresh;
    private Button btnStart, btnStop;

    private List<File> gpxFiles = new ArrayList<>();
    private final String TARGET_FOLDER = Environment.getExternalStorageDirectory() + "/Fake_GPS_Locations";

    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double lat = intent.getDoubleExtra(MockLocationService.EXTRA_LAT, 0);
            double lon = intent.getDoubleExtra(MockLocationService.EXTRA_LON, 0);
            tvLat.setText(getString(R.string.lat_label, String.valueOf(lat)));
            tvLon.setText(getString(R.string.lon_label, String.valueOf(lon)));
            updateUiState(true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.app_name, R.string.app_name);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        tvStatus = findViewById(R.id.tv_status);
        tvLat = findViewById(R.id.tv_lat);
        tvLon = findViewById(R.id.tv_lon);

        spinnerGpx = findViewById(R.id.spinner_gpx);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);

        btnRefresh.setOnClickListener(v -> scanGpxFolder());
        btnStart.setOnClickListener(v -> startSimulation());
        btnStop.setOnClickListener(v -> stopSimulation());

        checkPermissions();
        scanGpxFolder();
        checkMockLocationEnabled();
    }

    private void scanGpxFolder() {
        if (!checkStoragePermission()) return;

        File folder = new File(TARGET_FOLDER);
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                Toast.makeText(this, "Created folder: " + TARGET_FOLDER, Toast.LENGTH_SHORT).show();
            }
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".gpx"));
        gpxFiles.clear();
        List<String> fileNames = new ArrayList<>();

        if (files != null) {
            for (File f : files) {
                gpxFiles.add(f);
                fileNames.add(f.getName());
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, fileNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGpx.setAdapter(adapter);

        if (fileNames.isEmpty()) {
            Toast.makeText(this, "No GPX files found in " + TARGET_FOLDER, Toast.LENGTH_LONG).show();
        }
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(this, "All Files Access required to scan folder", Toast.LENGTH_LONG).show();
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 103);
                return false;
            }
        }
        return true;
    }

    private void checkMockLocationEnabled() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                lm.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, false, false, false, 0, 5);
                lm.removeTestProvider(LocationManager.GPS_PROVIDER);
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Please set this app as 'Mock Location App' in Developer Options", Toast.LENGTH_LONG).show();
            showMockLocationWarning();
        } catch (Exception e) {
            // Log other errors but don't crash the app
            e.printStackTrace();
        }
    }

    private void showMockLocationWarning() {
        btnStart.setText("Setup Mock Location");
        btnStart.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, "Could not open Developer Options. Please open manually.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 102);
            }
        }
    }

    private void startSimulation() {
        int selectedIndex = spinnerGpx.getSelectedItemPosition();
        if (selectedIndex < 0 || gpxFiles.isEmpty()) {
            Toast.makeText(this, "Please select a GPX file", Toast.LENGTH_SHORT).show();
            return;
        }

        File selectedFile = gpxFiles.get(selectedIndex);
        List<LocationPoint> points;
        try {
            points = GpxParser.parse(new FileInputStream(selectedFile));
        } catch (Exception e) {
            Toast.makeText(this, "Error reading file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        int interval = getSharedPreferences("prefs", MODE_PRIVATE).getInt("interval", 5);

        Intent intent = new Intent(this, MockLocationService.class);
        intent.setAction(MockLocationService.ACTION_START);
        intent.putExtra(MockLocationService.EXTRA_POINTS, (Serializable) points);
        intent.putExtra(MockLocationService.EXTRA_INTERVAL, interval);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        updateUiState(true);
    }

    private void stopSimulation() {
        Intent intent = new Intent(this, MockLocationService.class);
        intent.setAction(MockLocationService.ACTION_STOP);
        startService(intent);
        updateUiState(false);
    }

    private void updateUiState(boolean running) {
        tvStatus.setText(running ? R.string.status_running : R.string.status_stopped);
        tvStatus.setTextColor(running ? 
                ContextCompat.getColor(this, android.R.color.holo_green_dark) : 
                ContextCompat.getColor(this, android.R.color.holo_red_dark));
        btnStart.setEnabled(!running);
        btnStop.setEnabled(running);
        spinnerGpx.setEnabled(!running);
        btnRefresh.setEnabled(!running);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(locationReceiver, new IntentFilter(MockLocationService.BROADCAST_UPDATE), Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(locationReceiver, new IntentFilter(MockLocationService.BROADCAST_UPDATE));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(locationReceiver);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_help) {
            startActivity(new Intent(this, HelpActivity.class));
        } else if (id == R.id.nav_about) {
            startActivity(new Intent(this, AboutActivity.class));
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
