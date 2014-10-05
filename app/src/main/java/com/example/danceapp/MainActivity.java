package com.example.danceapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class MainActivity extends Activity {

    private Button recordButton;
    private Button historyButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recordButton    = (Button) findViewById(R.id.record_button);
        historyButton   = (Button) findViewById(R.id.history_button);

        // add clicks
        recordButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                Intent intent = new Intent(getApplicationContext(), Record.class);
                startActivity(intent);
            }
        });

        historyButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                Intent intent = new Intent(getApplicationContext(), History.class);
                startActivity(intent);
            }
        });
    }
}
