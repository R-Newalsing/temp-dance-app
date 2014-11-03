package com.example.danceapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
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

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
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
    //Wakelock for running in the background
    private PowerManager.WakeLock wakeLock;
    private int currentTime = 0;
    ToneGenerator toneG;
    double delta[];
    double averageArray[];
    double beep[];
    double value[];

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
        toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        //sound stuff

        button  = (Button) findViewById(R.id.button);
        text    = (EditText) findViewById(R.id.editText);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                if(!state)
                {   // log every set seconds
                    timer.schedule(doAsynchronousTask, 0, (1000/15)/10);
                    text.setVisibility(View.GONE);
                    state = true;
                    wakeLock.acquire();
                }
                else
                {
                    timer.cancel();
                    toneG.stopTone();
                    toneG.release();
                    plotGraph();
                    saveCsv();
                    state = false;
                    button.setVisibility(View.GONE);
                    wakeLock.release();
                    calculate();
                }

            }
        });
    }

    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
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
    }

    public static double getMaxValue(double[] array){
        double maxValue = array[0];
        for(int i=1;i < array.length;i++){
            if(array[i] > maxValue){
                maxValue = array[i];
            }
        }
        return maxValue;
    }

    public static double getMinValue(double[] array){
        double minValue = array[0];
        for(int i=1;i<array.length;i++){
            if(array[i] < minValue){
                minValue = array[i];
            }
        }
        return minValue;
    }

    private void calculate()
    {
        int totalPeaks  = 0;
        int good        = 0;
        int bad         = 0;
        int score       = 0;


        for(int i = 0; i < delta.length; i++) {
            if (i % 300 == 0 && i != 0)
            {
                int count       = 0;

                for(int j = 0; j < 30; j++)
                {
                    if(delta[i + j] < averageArray[i])
                    {
                        count++;
                    }
                }

                if(count == 30)
                {
                    good++;
                }
                else
                {
                    bad++;
                }

                totalPeaks++;
            }
        }
        score = (int)((double)good / (double)(totalPeaks/2) * 100.0);
        Log.d("TEMP", String.valueOf(totalPeaks/2 + " -- " + good / (totalPeaks/2) + "Good = " + good + " - Bad = " + bad + " - total = " + totalPeaks + " - score = " + score));
        Toast toast = Toast.makeText(this, "Good = " + good + " - Bad = " + bad + " - total = " + totalPeaks + " - score = " + score, Toast.LENGTH_LONG);
        toast.show();
    }

    public void plotGraph() {
        GraphView.GraphViewData[] dataTotal = new GraphView.GraphViewData[list.size()];
        GraphView.GraphViewData[] beepTotal = new GraphView.GraphViewData[list.size()];
        GraphView.GraphViewData[] deltaTotal = new GraphView.GraphViewData[list.size()];
        GraphView.GraphViewData[] averageTotal = new GraphView.GraphViewData[list.size()];

        value           = new double[list.size()];
        beep            = new double[list.size()];
        delta           = new double[list.size()];
        double temp[]   = new double[40];
        averageArray    = new double[list.size()];
        double high     = 0.0;
        double low      = 0.0;

        for (int i=0; i<list.size(); i++)
        {
            value[i] = Math.abs(Math.sqrt((
                    (
                        (list.get(i)[0]*list.get(i)[0])*2 +
                        (list.get(i)[1]*list.get(i)[1])*2 +
                        (list.get(i)[2]*list.get(i)[2])*2)
                    )-10)
            );

            if(i%100 == 0)
            {
                if(i%300 == 0)
                {
                    beep[i] = 40;
                }
                else
                {
                    beep[i] = 30;
                }
            }
            else
            {
                beep[i] = 0;
            }
        }

        for (int i=0; i<value.length; i++)
        {
            if(i >= 20 && i < (value.length-20))
            {
                for(int j=0; j < temp.length; j++)
                {
                    temp[j] = value[(i-20)+j];

                    high    = getMaxValue(temp);
                    low     = getMinValue(temp);
                }
                delta[i] = high - low;
            }
            else
            {
                delta[i] = 0;
            }
        }

        double sum      = 0.0;
        double average  = 0.0;

        for(int i = 0; i < averageArray.length; i++)
        {
            if(i >= 600 && i < (averageArray.length - 600))
            {
                if(i%300 == 0)
                {
                    for(int j = 0; j < 600; j++)
                    {
                        sum += delta[(i-300) + j];
                    }

                    average = sum / 600;
                }

                averageArray[i] = average * 1.5;
                sum     = 0.0;
            }
            else
            {
                averageArray[i] = 0.0;
            }
        }

        for (int i=0; i<list.size(); i++)
        {
            dataTotal[i]    = new GraphView.GraphViewData(i, value[i]);
            beepTotal[i]    = new GraphView.GraphViewData(i, beep[i]);
            deltaTotal[i]   = new GraphView.GraphViewData(i, delta[i]);
            averageTotal[i] = new GraphView.GraphViewData(i, averageArray[i]);
        }

        GraphViewSeries TotalAb = new GraphViewSeries("Total", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(0, 0, 0), 3), dataTotal);
        GraphViewSeries beepTotal2 = new GraphViewSeries("Beep", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(0, 0, 255), 3), beepTotal);
        GraphViewSeries deltaTotal2 = new GraphViewSeries("delta", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(255, 0, 0), 3), deltaTotal);
        GraphViewSeries averageTotal2 = new GraphViewSeries("delta", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(0, 200, 200), 3), averageTotal);

        GraphView graphView = new LineGraphView(this, "Dance movement");
        graphView.addSeries(TotalAb);
        graphView.addSeries(deltaTotal2);
        graphView.addSeries(beepTotal2);
        graphView.addSeries(averageTotal2);

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
        SimpleDateFormat s = new SimpleDateFormat("ddMMyyyykkmm");
        String format = s.format(new Date());

        String outputFile  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/"+ format + "dance.csv";
        String outputFile2 = getFilesDir() +"/"+ format + "dance.csv";

        for(int i = 0; i < list.size(); i++)
        {
            database.add(new String[]{String.valueOf(i), list.get(i)[0].toString(), list.get(i)[1].toString(), list.get(i)[2].toString(), String.valueOf(value[i]), String.valueOf(delta[i]), String.valueOf(averageArray[i]), String.valueOf(beep[i])});
        }

        try
        {
            writer = new CSVWriter(new FileWriter(outputFile));
            writer.writeAll(database);
            writer.close();

            writer = new CSVWriter(new FileWriter(outputFile2));
            writer.writeAll(database);
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
                       if(currentTime%100 == 0)
                        {
                           if(currentTime%300 == 0) {
                                toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                            }else {
                                toneG.startTone(ToneGenerator.TONE_CDMA_DIAL_TONE_LITE, 150);
                            }
                        }

                        currentTime++;
                    }
                    catch (Exception e) {
                        Log.d("error", e.toString());
                    }
                }
            });
        }
    };
}
