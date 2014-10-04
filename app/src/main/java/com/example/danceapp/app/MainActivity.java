package com.example.danceapp.app;

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
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import au.com.bytecode.opencsv.CSVWriter;

public class MainActivity extends Activity implements SensorEventListener {

    private float mLastX, mLastY, mLastZ;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    int BPM;
    final Handler handler = new Handler();
    Timer timer = new Timer();
    private float force;
    Vibrator vibrate;
    double counter = 0;
    Button button;
    EditText text;
    private boolean state = false;
    private ArrayList<Float[]> list = new ArrayList<Float[]>();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer , SensorManager.SENSOR_DELAY_FASTEST);
        // vibrator
        vibrate = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);


        button  = (Button) findViewById(R.id.button);
        text    = (EditText) findViewById(R.id.editText);

        button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                if(!state)
                {   // log every set seconds
                    timer.schedule(doAsynchronousTask, 0, 100);
                    text.setVisibility(View.GONE);
                    state = true;
                }
                else
                {
                    timer.cancel();
                    for(int i=0;i<list.size();i++)
                    {
                        Log.d("LOG", "X: " + list.get(i)[0].toString() + " - Y: " + list.get(i)[1].toString() + " - Z: " + list.get(i)[2].toString());
                    }
                    saveCsv();
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

        force = mLastX + mLastY + mLastZ;

    }

    public void plotGraph() {
        GraphView.GraphViewData[] dataX = new GraphView.GraphViewData[list.size()];
        GraphView.GraphViewData[] dataY = new GraphView.GraphViewData[list.size()];
        GraphView.GraphViewData[] dataZ = new GraphView.GraphViewData[list.size()];
        GraphView.GraphViewData[] dataTotal = new GraphView.GraphViewData[list.size()];

        for (int i=0; i<list.size(); i++)
        {

            dataX[i] = new GraphView.GraphViewData(i, list.get(i)[0]);
            dataY[i] = new GraphView.GraphViewData(i, list.get(i)[1]);
            dataZ[i] = new GraphView.GraphViewData(i, list.get(i)[2]);
            dataTotal[i] = new GraphView.GraphViewData(i, list.get(i)[0] + list.get(i)[1] + list.get(i)[2]);
        }

        GraphViewSeries graphX = new GraphViewSeries("X-as", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(255, 00, 255), 3), dataX);
        GraphViewSeries graphY = new GraphViewSeries("Y-as", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(51, 51, 255), 3), dataY);
        GraphViewSeries graphZ = new GraphViewSeries("Z-as", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(00, 204, 204), 3), dataZ);
        GraphViewSeries graphTotal = new GraphViewSeries("dataTotal", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(0, 0, 0), 3), dataTotal);

        GraphView graphView = new LineGraphView(this, "Dance movement");
        graphView.addSeries(graphX);
        graphView.addSeries(graphY);
        graphView.addSeries(graphZ);
        graphView.addSeries(graphTotal);
        // optional - legend
        graphView.setShowLegend(true);

        LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
        layout.addView(graphView);
    }

    public void saveCsv()
    {
        CSVWriter writer;
        List<String[]> database = new ArrayList<String[]>();
        SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss");
        String format = s.format(new Date());

        String outputFile = Environment.getExternalStorageDirectory().getPath()+"/"+ format + "dance.csv";

        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput("config.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write("hai");
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }


        for(int i = 0; i < list.size(); i++)
        {
            database.add(new String[]{String.valueOf(i), list.get(i).toString()});
        }

        try
        {
            writer = new CSVWriter(new FileWriter(outputFile));
            writer.writeAll(database);
            writer.close();
        }
        catch (IOException e){e.printStackTrace();}
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
//
//                        if(counter%3 == 0)
//                        {
//                            vibrate.vibrate(150);
//                        }
//                        else
//                        {
//                            vibrate.vibrate(100);
//                        }
//
//                        counter++;
                    }
                    catch (Exception e) {
                        Log.d("error", e.toString());
                    }
                }
            });
        }
    };
}