package com.example.danceapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
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
import ca.uol.aig.fftpack.RealDoubleFFT;


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
    private ArrayList<Double[]> list_sound = new ArrayList<Double[]>();
    private Double sound = 0.0;

    //sound stuff
    int frequency = 8000;
    int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private RealDoubleFFT transformer;
    int blockSize = 256;

    RecordAudio recordTask;

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
        transformer = new RealDoubleFFT(blockSize);


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
                    recordTask = new RecordAudio();
                    recordTask.execute();
                }
                else
                {
                    timer.cancel();
                    recordTask.cancel(true);
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
    }

    public void plotGraph() {
        GraphView.GraphViewData[] dataX = new GraphView.GraphViewData[list.size()];
        GraphView.GraphViewData[] dataY = new GraphView.GraphViewData[list.size()];
        GraphView.GraphViewData[] dataZ = new GraphView.GraphViewData[list.size()];
        GraphView.GraphViewData[] dataTotal = new GraphView.GraphViewData[list.size()];
        GraphView.GraphViewData[] dataSound = new GraphView.GraphViewData[list_sound.size()];

        for (int i=0; i<list.size(); i++)
        {
            dataX[i] = new GraphView.GraphViewData(i, list.get(i)[0]);
            dataY[i] = new GraphView.GraphViewData(i, list.get(i)[1]);
            dataZ[i] = new GraphView.GraphViewData(i, list.get(i)[2]);
            dataTotal[i] = new GraphView.GraphViewData(i, list.get(i)[0] + list.get(i)[1] + list.get(i)[2]);
            dataSound[i] = new GraphView.GraphViewData(i, list_sound.get(i)[0]);
        }

        GraphViewSeries graphX = new GraphViewSeries("X-as", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(255, 0, 255), 3), dataX);
        GraphViewSeries graphY = new GraphViewSeries("Y-as", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(51, 51, 255), 3), dataY);
        GraphViewSeries graphZ = new GraphViewSeries("Z-as", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(0, 204, 204), 3), dataZ);
        GraphViewSeries graphTotal = new GraphViewSeries("dataTotal", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(0, 0, 0), 3), dataTotal);
        GraphViewSeries graphSound = new GraphViewSeries("sound", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(0, 0, 225), 3), dataSound);

        GraphView graphView = new LineGraphView(this, "Dance movement");
        graphView.addSeries(graphX);
        graphView.addSeries(graphY);
        graphView.addSeries(graphZ);
        graphView.addSeries(graphTotal);
        graphView.addSeries(graphSound);

        // optional - legend
        graphView.setShowLegend(true);
//        graphView.setViewPort(2, 40);
//        graphView.setScrollable(true);
//        optional - activate scaling / zooming
        graphView.setScalable(true);

        LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
        layout.addView(graphView);
    }

    public void saveCsv()
    {
        CSVWriter writer;
        List<String[]> database = new ArrayList<String[]>();
        SimpleDateFormat s = new SimpleDateFormat("ddMMyyyy");
        String format = s.format(new Date());



        String outputFile = getFilesDir()+"/"+ format + "dance.csv";

        for(int i = 0; i < list.size(); i++)
        {
            database.add(new String[]{String.valueOf(i), list.get(i)[0].toString(), list.get(i)[1].toString(), list.get(i)[2].toString()});
        }

        try
        {
            writer = new CSVWriter(new FileWriter(outputFile));
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
                        list_sound.add(new Double[]{sound});

                    }
                    catch (Exception e) {
                        Log.d("error", e.toString());
                    }
                }
            });
        }
    };

    public class RecordAudio extends AsyncTask<Void, double[], Void>
    {
        @Override
        protected Void doInBackground(Void... arg0)
        {
            try
            {
                // int bufferSize = AudioRecord.getMinBufferSize(frequency,
                // AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);

                AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency, channelConfiguration, audioEncoding, bufferSize);

                short[] buffer = new short[blockSize];
                double[] toTransform = new double[blockSize];

                audioRecord.startRecording();

                // started = true; hopes this should true before calling
                // following while loop

                while (state)
                {
                    int bufferReadResult = audioRecord.read(buffer, 0, blockSize);

                    for (int i = 0; i < blockSize && i < bufferReadResult; i++)
                    {
                        toTransform[i] = (double) buffer[i] / 32768.0;
                    }
                    transformer.ft(toTransform);
                    publishProgress(toTransform);
                }

                audioRecord.stop();

            } catch (Throwable t) {
                t.printStackTrace();
                Log.e("AudioRecord", "Recording Failed");
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(double[]... toTransform) {

            for (int i = 0; i < toTransform[0].length; i++) {
                sound = toTransform[0][i];
            }
        }
    }
}
