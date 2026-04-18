package com.vypeensoft.fakegps;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import com.vypeensoft.fakegps.utils.GpxParser;

import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawer;
    private TextView tvStatus, tvLat, tvLon, tvTime, tvFileInfo;
    private Button btnStart, btnStop, btnLoad;

    private List<LocationPoint> loadedPoints;
    private String loadedFileName;

    private final ActivityResultLauncher<String[]> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    loadGpxFile(uri);
                }
            }
    );

    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double lat = intent.getDoubleExtra(MockLocationService.EXTRA_LAT, 0);
            double lon = intent.getDoubleExtra(MockLocationService.EXTRA_LON, 0);
            String time = intent.getStringExtra(MockLocationService.EXTRA_TIME);

            tvLat.setText(getString(R.string.lat_label, String.valueOf(lat)));
            tvLon.setText(getString(R.string.lon_label, String.valueOf(lon)));
            tvTime.setText(getString(R.string.time_label, time != null ? time : "--"));
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
        tvTime = findViewById(R.id.tv_time);
        tvFileInfo = findViewById(R.id.tv_file_info);

        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnLoad = findViewById(R.id.btn_load);

        btnLoad.setOnClickListener(v -> filePickerLauncher.launch(new String[]{"*/*"}));
        btnStart.setOnClickListener(v -> startSimulation());
        btnStop.setOnClickListener(v -> stopSimulation());

        checkPermissions();
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

    private void loadGpxFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            loadedPoints = GpxParser.parse(inputStream);
            loadedFileName = uri.getLastPathSegment();
            tvFileInfo.setText("Loaded: " + loadedFileName + " (" + loadedPoints.size() + " points)");
            Toast.makeText(this, "GPX Loaded successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error parsing GPX: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void startSimulation() {
        if (loadedPoints == null || loadedPoints.isEmpty()) {
            Toast.makeText(this, "Please load a GPX file first", Toast.LENGTH_SHORT).show();
            return;
        }

        int interval = getSharedPreferences("prefs", MODE_PRIVATE).getInt("interval", 5);

        Intent intent = new Intent(this, MockLocationService.class);
        intent.setAction(MockLocationService.ACTION_START);
        intent.putExtra(MockLocationService.EXTRA_POINTS, (Serializable) loadedPoints);
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
        btnLoad.setEnabled(!running);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(locationReceiver, new IntentFilter(MockLocationService.BROADCAST_UPDATE), Context.RECEIVER_EXPORTED);
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
