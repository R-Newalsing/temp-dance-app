package com.example.danceapp;

import android.app.Activity;
import android.app.ListActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Created by rewi on 4-10-2014.
 */
public class History extends Activity {

    public List<Map<String, String>> danceList = new ArrayList<Map<String,String>>();
    public ListView listview;
    public String path;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history);

        listview = (ListView) findViewById(R.id.list);
        path = getFilesDir().toString();

        //get data
        initList();

        SimpleAdapter simpleAdpt = new SimpleAdapter(this, danceList, android.R.layout.simple_list_item_1, new String[] {"dance"}, new int[] {android.R.id.text1});
        listview.setAdapter(simpleAdpt);

        // set click events
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,int position, long id) {
                int new_id = (int) id;
                plotGraph(String.valueOf(danceList.get(new_id).get("dance")));
            }
        });
    }

    public void plotGraph(String file) {
        ArrayList<Float[]> list = new ArrayList<Float[]>();

        try
        {
            CSVReader reader = new CSVReader(new FileReader( getFilesDir()+"/"+file));

            String [] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                // nextLine[] is an array of values from the line
                Log.d("CSV", nextLine[0]);
                list.add(new Float[]{Float.valueOf(nextLine[1]), Float.valueOf(nextLine[2]), Float.valueOf(nextLine[3]), Float.valueOf(nextLine[4])});
            }
        }
        catch (IOException e){Log.d("FILEREADER", e.toString());}

        GraphView.GraphViewData[] dataX = new GraphView.GraphViewData[list.size()];
        GraphView.GraphViewData[] dataY = new GraphView.GraphViewData[list.size()];
        GraphView.GraphViewData[] dataZ = new GraphView.GraphViewData[list.size()];
        GraphView.GraphViewData[] dataTotal = new GraphView.GraphViewData[list.size()];
        GraphView.GraphViewData[] dataSound = new GraphView.GraphViewData[list.size()];

        for (int i=0; i<list.size(); i++)
        {

            dataX[i] = new GraphView.GraphViewData(i, list.get(i)[0]);
            dataY[i] = new GraphView.GraphViewData(i, list.get(i)[1]);
            dataZ[i] = new GraphView.GraphViewData(i, list.get(i)[2]);
            dataTotal[i] = new GraphView.GraphViewData(i, list.get(i)[0] + list.get(i)[1] + list.get(i)[2]);
            dataSound[i] = new GraphView.GraphViewData(i, list.get(i)[3]);
        }

        GraphViewSeries graphX = new GraphViewSeries("X-as", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(255, 00, 255), 3), dataX);
        GraphViewSeries graphY = new GraphViewSeries("Y-as", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(51, 51, 255), 3), dataY);
        GraphViewSeries graphZ = new GraphViewSeries("Z-as", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(00, 204, 204), 3), dataZ);
        GraphViewSeries graphTotal = new GraphViewSeries("dataTotal", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(0, 0, 0), 3), dataTotal);
        GraphViewSeries graphSound = new GraphViewSeries("sound", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(0, 255, 0), 3), dataSound);

        GraphView graphView = new LineGraphView(this, "Dance movement");
        graphView.addSeries(graphX);
        graphView.addSeries(graphY);
        graphView.addSeries(graphZ);
        graphView.addSeries(graphTotal);
        graphView.addSeries(graphSound);
        // optional - legend
        graphView.setShowLegend(true);
//        graphView.setViewPort(2, 40);
        graphView.setScrollable(true);
        // optional - activate scaling / zooming
        graphView.setScalable(false);

        listview.setVisibility(View.GONE);
        LinearLayout layout = (LinearLayout) findViewById(R.id.list_wrap);
        layout.addView(graphView);
    }

    private void initList() {
        File f = new File(path);
        File file[] = f.listFiles();

        for (int i=0; i < file.length; i++)
        {
            danceList.add(createDance("dance", file[i].getName()));
        }
    }

    /**
     * @param key
     * @param name
     * @return HashMap
     */
    private HashMap<String, String> createDance(String key, String name) {
        HashMap<String, String> dance = new HashMap<String, String>();
        dance.put(key, name);

        return dance;
    }
}
