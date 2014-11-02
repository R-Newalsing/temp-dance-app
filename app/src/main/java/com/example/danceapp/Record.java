package com.example.danceapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import au.com.bytecode.opencsv.CSVWriter;


public class Record extends Activity implements SensorEventListener {

    private float mLastX, mLastY, mLastZ;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    final Handler handler = new Handler();
    Timer timer = new Timer();
    Vibrator vibrate;
    Button button;
    EditText text;
    private boolean state = false;
    private ArrayList<Float[]> list = new ArrayList<Float[]>();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer , SensorManager.SENSOR_DELAY_FASTEST);
        // vibrator
        vibrate = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        //sound stuff

        button  = (Button) findViewById(R.id.button);
        text    = (EditText) findViewById(R.id.editText);

        button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                if(!state)
                {   // log every set seconds
                    // timer.schedule(doAsynchronousTask, 0, 10);
                    text.setVisibility(View.GONE);
                    state = true;
                }
                else
                {
                    //timer.cancel();
                    //saveCsv();
                    plotGraph();
                    state = false;
                    button.setVisibility(View.GONE);
                }

            }
        });
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        mLastX = Math.abs(x);
        mLastY = Math.abs(y);
        mLastZ = Math.abs(z);

        list.add(new Float[]{mLastX, mLastY, mLastZ});
    }

    public void plotGraph() {
        GraphView.GraphViewData[] dataTotal = new GraphView.GraphViewData[list.size()];
        double value[] = new double[list.size()];

        for (int i=0; i<list.size(); i++)
        {
            value[i] = Math.sqrt(
                    (list.get(i)[0]*list.get(i)[0]) +
                    (list.get(i)[1]*list.get(i)[1]) +
                    (list.get(i)[2]*list.get(i)[2])
            );
        }


        for (int i=0; i<list.size(); i++)
        {
            dataTotal[i] = new GraphView.GraphViewData(i, value[i]);
        }

        GraphViewSeries graphTotal2 = new GraphViewSeries("dataTotal", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(0, 204, 204), 3), dataTotal);

        GraphView graphView = new LineGraphView(this, "Dance movement");
        graphView.addSeries(graphTotal2);

        // optional - legend
        graphView.setShowLegend(true);
        graphView.setScrollable(true);
        graphView.setScalable(true);

        LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
        layout.addView(graphView);
    }

    public void saveCsv()
    {
        CSVWriter writer;
        List<String[]> database = new ArrayList<String[]>();
        List<String[]> soundDb = new ArrayList<String[]>();
        SimpleDateFormat s = new SimpleDateFormat("ddMMyyyykkmm");
        String format = s.format(new Date());



        String outputFile  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/"+ format + "dance.csv";
        String outputSound  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/"+ format + "Sound.csv";
        String outputFile2 = getFilesDir() +"/"+ format + "dance.csv";

        for(int i = 0; i < list.size(); i++)
        {
            database.add(new String[]{String.valueOf(i), list.get(i)[0].toString(), list.get(i)[1].toString(), list.get(i)[2].toString()});
        }

        try
        {
            writer = new CSVWriter(new FileWriter(outputFile));
            writer.writeAll(database);
            writer.close();

            writer = new CSVWriter(new FileWriter(outputFile2));
            writer.writeAll(database);
            writer.close();

            writer = new CSVWriter(new FileWriter(outputSound));
            writer.writeAll(soundDb);
            writer.close();
        }
        catch (IOException e){Log.d("WRITING", e.toString());}
    }

    TimerTask doAsynchronousTask = new TimerTask() {
        @Override
        public void run() {
            handler.post(new Runnable() {
                @SuppressWarnings("unchecked")
                public void run() {
                    try {
                        // add to array
                        list.add(new Float[]{mLastX, mLastY, mLastZ});
                    }
                    catch (Exception e) {
                        Log.d("error", e.toString());
                    }
                }
            });
        }
    };
}
