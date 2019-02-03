package com.example.admindeveloper.seismometer;

import android.graphics.Color;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Calendar;

public class DisplayGraph {

    GraphView dataGraph;

    private int counter=0;
    LineGraphSeries<DataPoint> lineX,lineY,lineZ;

    private void initializeDataGraph()
    {
        if(lineX==null)
        {
            lineX=new LineGraphSeries<>();
            lineY=new LineGraphSeries<>();
            lineZ=new LineGraphSeries<>();
            lineX.setThickness(5);
            lineX.setColor(Color.MAGENTA);
            lineX.setDrawDataPoints(false);
            lineX.setTitle("X");
            lineY.setThickness(5);
            lineY.setColor(Color.BLACK);
            lineY.setDrawDataPoints(false);
            lineY.setTitle("Y");
            lineZ.setThickness(5);
            lineZ.setColor(Color.BLUE);
            lineZ.setDrawDataPoints(false);
            lineZ.setTitle("Z");
            dataGraph.addSeries(lineX);
            dataGraph.addSeries(lineY);
            dataGraph.addSeries(lineZ);
            dataGraph.getViewport().setYAxisBoundsManual(true);
            dataGraph.getViewport().setMinY(17000);
            dataGraph.getViewport().setMaxY(24000);
            dataGraph.getViewport().setXAxisBoundsManual(true);
            dataGraph.getViewport().setMinX(0);
            dataGraph.getViewport().setMaxX(500);
        }
    }

    private void setCustomLabel()
    {
        dataGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(){
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    // show normal x values
                    return Calendar.getInstance().getTime().getSeconds()+" s";
                } else {
                    // show currency for y values
                    return super.formatLabel(value, isValueX);
                }
            }
        });
    }

    public void displayRawDataGraph(int x , int y , int z) {
        lineX.appendData(new DataPoint(counter,x),true,500);
        lineY.appendData(new DataPoint(counter,y),true,500);
        lineZ.appendData(new DataPoint(counter,z),true,500);
        counter++;
    }

    public DisplayGraph(GraphView dataGraph)
    {
        this.dataGraph=dataGraph;
        initializeDataGraph();
        setCustomLabel();
    }
}
