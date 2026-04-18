package com.vypeensoft.fakegps.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.vypeensoft.fakegps.MainActivity;
import com.vypeensoft.fakegps.model.LocationPoint;

import java.util.ArrayList;
import java.util.List;

public class MockLocationService extends Service {
    private static final String TAG = "MockLocationService";
    private static final String CHANNEL_ID = "MockLocationChannel";
    private static final int NOTIF_ID = 1;

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String EXTRA_POINTS = "EXTRA_POINTS";
    public static final String EXTRA_INTERVAL = "EXTRA_INTERVAL";
    
    public static final String BROADCAST_UPDATE = "com.vypeensoft.fakegps.LOCATION_UPDATE";
    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LON = "lon";
    public static final String EXTRA_TIME = "time";

    private LocationManager locationManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private List<LocationPoint> points = new ArrayList<>();
    private int currentIndex = 0;
    private int intervalSeconds = 5;
    private boolean isRunning = false;

    private final Runnable simulationRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning || points.isEmpty()) return;

            LocationPoint point = points.get(currentIndex);
            updateMockLocation(point);
            broadcastUpdate(point);

            currentIndex = (currentIndex + 1) % points.size(); // Looping
            handler.postDelayed(this, intervalSeconds * 1000L);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            startSimulation(intent);
        } else if (ACTION_STOP.equals(action)) {
            stopSimulation();
        }

        return START_NOT_STICKY;
    }

    private void startSimulation(Intent intent) {
        if (isRunning) return;

        List<LocationPoint> extraPoints = (List<LocationPoint>) intent.getSerializableExtra(EXTRA_POINTS);
        if (extraPoints != null) {
            this.points = extraPoints;
        }
        this.intervalSeconds = intent.getIntExtra(EXTRA_INTERVAL, 5);
        this.currentIndex = 0;
        this.isRunning = true;

        startForeground(NOTIF_ID, createNotification());
        setupTestProvider();
        handler.post(simulationRunnable);
        Log.d(TAG, "Simulation started");
    }

    private void stopSimulation() {
        isRunning = false;
        handler.removeCallbacks(simulationRunnable);
        removeTestProvider();
        stopForeground(true);
        stopSelf();
        Log.d(TAG, "Simulation stopped");
    }

    private void setupTestProvider() {
        try {
            locationManager.addTestProvider(
                    LocationManager.GPS_PROVIDER,
                    false, false, false, false, true, true, true,
                    android.location.Criteria.POWER_LOW,
                    android.location.Criteria.ACCURACY_FINE);
            locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
        } catch (SecurityException | IllegalArgumentException e) {
            Log.e(TAG, "Error setting up test provider: " + e.getMessage());
        }
    }

    private void removeTestProvider() {
        try {
            locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
        } catch (SecurityException | IllegalArgumentException e) {
            Log.e(TAG, "Error removing test provider: " + e.getMessage());
        }
    }

    private void updateMockLocation(LocationPoint point) {
        Location mockLocation = new Location(LocationManager.GPS_PROVIDER);
        mockLocation.setLatitude(point.getLatitude());
        mockLocation.setLongitude(point.getLongitude());
        mockLocation.setAltitude(0);
        mockLocation.setTime(System.currentTimeMillis());
        mockLocation.setAccuracy(1.0f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }

        try {
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockLocation);
            Log.d(TAG, "Mock location set: " + point.getLatitude() + ", " + point.getLongitude());
        } catch (SecurityException | IllegalArgumentException e) {
            Log.e(TAG, "Error setting mock location: " + e.getMessage());
        }
    }

    private void broadcastUpdate(LocationPoint point) {
        Intent intent = new Intent(BROADCAST_UPDATE);
        intent.putExtra(EXTRA_LAT, point.getLatitude());
        intent.putExtra(EXTRA_LON, point.getLongitude());
        intent.putExtra(EXTRA_TIME, point.getTimestamp());
        sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Fake GPS Simulation Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Fake GPS Running")
                .setContentText("Simulating location movement...")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopSimulation();
        super.onDestroy();
    }
}
