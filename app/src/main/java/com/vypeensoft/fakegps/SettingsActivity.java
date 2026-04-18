package com.vypeensoft.fakegps;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        RadioGroup rgInterval = findViewById(R.id.rg_interval);
        int currentInterval = getSharedPreferences("prefs", MODE_PRIVATE).getInt("interval", 5);

        if (currentInterval == 5) ((RadioButton) findViewById(R.id.rb_5s)).setChecked(true);
        else if (currentInterval == 10) ((RadioButton) findViewById(R.id.rb_10s)).setChecked(true);
        else if (currentInterval == 15) ((RadioButton) findViewById(R.id.rb_15s)).setChecked(true);

        rgInterval.setOnCheckedChangeListener((group, checkedId) -> {
            int interval = 5;
            if (checkedId == R.id.rb_5s) interval = 5;
            else if (checkedId == R.id.rb_10s) interval = 10;
            else if (checkedId == R.id.rb_15s) interval = 15;
            
            getSharedPreferences("prefs", MODE_PRIVATE).edit().putInt("interval", interval).apply();
        });
    }
}
