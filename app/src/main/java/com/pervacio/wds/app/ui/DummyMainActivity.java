package com.pervacio.wds.app.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.pervacio.wds.R;

public class DummyMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dummy_main);

        Button dataTransferButton = (Button) findViewById(R.id.dataTransferButton);
        dataTransferButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(DummyMainActivity.this, EasyMigrateActivity.class);
                startActivity(myIntent);
            }
        });

        /*
        Intent intent = new Intent(this, EasyMigrateService.class);
        startService(intent);
        */

        ServiceConnection connection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                int test = 0;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        Intent intent = new Intent(this, EasyMigrateService.class);
        getApplicationContext().bindService(intent, connection, this.BIND_AUTO_CREATE);
    }

}
