package com.github.shiraji.kenkenpasample;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Handler handler = new Handler();
        final Button button = (Button)findViewById(R.id.network_launch_button);
        final Button restartButton = (Button)findViewById(R.id.reset_button);
        final LoadingSM loadingSM = LoadingSM.create();
        loadingSM.init();

        loadingSM.setListener(new LoadingSM.NetworkDoneListener() {
            @Override
            public void done() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        button.setEnabled(false);
                        button.setVisibility(View.GONE);

                        Toast.makeText(MainActivity.this, "Network loading completed!", Toast.LENGTH_LONG).show();

                        restartButton.setEnabled(true);
                        restartButton.setVisibility(View.VISIBLE);

                        loadingSM.close();
                    }
                });
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingSM.load();
            }
        });

        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartButton.setEnabled(false);
                restartButton.setVisibility(View.GONE);

                button.setEnabled(true);
                button.setVisibility(View.VISIBLE);

                loadingSM.reset();
            }
        });
    }
}
