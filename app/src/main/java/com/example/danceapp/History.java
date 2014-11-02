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
        GraphView.GraphViewData[] dataTotal = new GraphView.GraphViewData[list.size()];
        GraphView.GraphViewData[] delta = new GraphView.GraphViewData[list.size()];
        GraphView.GraphViewData[] beepTotal = new GraphView.GraphViewData[list.size()];

        double beep[]   = new double[list.size()];
        for (int i=0; i<list.size(); i++)
        {
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

        for (int i=0; i<list.size(); i++)
        {

            dataTotal[i] = new GraphView.GraphViewData(i, Math.sqrt(((list.get(i)[0]*list.get(i)[0])*2 + (list.get(i)[1]*list.get(i)[1])*2 + (list.get(i)[2]*list.get(i)[2])*2)-10));
            delta[i] = new GraphView.GraphViewData(i, list.get(i)[3]);
            beepTotal[i] = new GraphView.GraphViewData(i, beep[i]);
        }

        GraphViewSeries beepTotal2 = new GraphViewSeries("Beep", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(0, 0, 255), 3), beepTotal);
        GraphViewSeries graphTotal = new GraphViewSeries("dataTotal", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(0, 0, 0), 3), dataTotal);
        GraphViewSeries graphSound = new GraphViewSeries("delta", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(0, 255, 0), 3), delta);

        GraphView graphView = new LineGraphView(this, "Dance movement");

        graphView.addSeries(graphTotal);
        graphView.addSeries(graphSound);
        graphView.addSeries(beepTotal2);
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
