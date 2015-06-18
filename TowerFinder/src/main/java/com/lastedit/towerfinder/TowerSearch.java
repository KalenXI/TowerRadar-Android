package com.lastedit.towerfinder;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * This was created by kevinvinck on 6/14/13.
 */
public class TowerSearch extends ListActivity {

    String searchIntent;
    String towerSearch;
    int searchType;
    float lastLongitude,lastLatitude;
    SharedPreferences mSharedPreferences;
    PreferenceSerializer mPreferenceSerializer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_towersearch);

        Intent intent = getIntent();

        searchIntent = intent.getAction();

        mSharedPreferences = getSharedPreferences("com.lastedit.tower",Context.MODE_PRIVATE);
        mPreferenceSerializer = new PreferenceSerializer();

        lastLongitude = mSharedPreferences.getFloat("lastLongitude", -1);
        lastLatitude = mSharedPreferences.getFloat("lastLatitude", -1);

        Log.d("tower", String.format("Last Location: %f / %f", lastLongitude, lastLatitude));

        assert searchIntent != null;
        if (searchIntent.equals(".TowerSearch.TV_Stations")) {
            setTitle("TV Stations");
            Toast toast = Toast.makeText(this,"Reloading data.",Toast.LENGTH_LONG);
            toast.show();
            searchType = 0;
            new Thread(new Runnable() {
                public void run() {
                    try {
                        towerSearch = loadTowerList();
                        parseFCCData(towerSearch);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else if (searchIntent.equals(".TowerSearch.FM_Stations")) {
            setTitle("FM Stations");
            searchType = 1;
            new Thread(new Runnable() {
                public void run() {
                    try {
                        towerSearch = loadTowerList();
                        parseFCCData(towerSearch);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else if (searchIntent.equals(".TowerSearch.AM_Stations")) {
            setTitle("AM Stations");
            searchType = 2;
            new Thread(new Runnable() {
                public void run() {
                    try {
                        towerSearch = loadTowerList();
                        parseFCCData(towerSearch);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        Log.d("Tower",intent.getAction());

        if (mPreferenceSerializer.getObject(mSharedPreferences,"lastTowerSearch") != null) {
            new Thread(new Runnable() {
                public void run() {
                    reloadData();
                }
            }).start();
        } else {
            final String[] listArray = getResources().getStringArray(R.array.TowerSearchList);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,listArray);
            setListAdapter(adapter);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Do something when a list item is clicked
        ArrayList<Map> lastTowerSearch = (ArrayList<Map>) mPreferenceSerializer.getObject(mSharedPreferences,"lastTowerSearch");
        HashMap<String, Object> station = (HashMap<String, Object>) lastTowerSearch.get(position);
        mPreferenceSerializer.setObject(mSharedPreferences,"selectedTower",station);
        finish();
        Log.d("tower", "List item was clicked!");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tower_search, menu);
        return true;
    }

    private void reloadData() {

        ArrayList<Map> lastTowerSearch = (ArrayList<Map>) mPreferenceSerializer.getObject(mSharedPreferences,"lastTowerSearch");

        String[] listArray = new String[lastTowerSearch.size()];

        for (int x = 0; x < lastTowerSearch.size(); x++) {
            listArray[x] = (String)lastTowerSearch.get(x).get("stationName");
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,listArray);

        getListView().post(new Runnable() {
            @Override
            public void run() {
                setListAdapter(adapter);
            }
        });

        //setListAdapter(adapter);
    }

    private String loadTowerList() throws Exception {

        float signLat,signLong;
        int latDeg,latMin;
        double latSec;
        int longDeg,longMin;
        double longSec;

        if (lastLatitude < 0)
            signLat = -1.0f;
        else
            signLat = 1.0f;

        if (lastLongitude < 0)
            signLong = -1.0f;
        else
            signLong = 1.0f;

        latDeg = (int) (Math.floor(lastLatitude * signLat) * signLat);
        latMin = (int) (Math.floor(((lastLatitude * signLat) - (latDeg * signLat)) * 60));
        latSec = ((((lastLatitude * signLat) - (latDeg * signLat)) * 60) - latMin) * 60;

        longDeg = (int) (Math.floor(lastLongitude * signLong) * signLong);
        longMin = (int) (Math.floor(((lastLongitude * signLong) - (longDeg * signLong)) * 60));
        longSec = ((((lastLongitude * signLong) - (longDeg * signLong)) * 60) - longMin) * 60;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int searchRadius = Integer.parseInt(preferences.getString("search_radius", "100"));

        String urlString;

        if (searchType == 0) {
            urlString = String.format("http://transition.fcc.gov/fcc-bin/tvq?type=3&list=4&dist=%d&dlat2=%d&mlat2=%d&slat2=%f&dlon2=%d&mlon2=%d&slon2=%f",searchRadius,latDeg,latMin,latSec,longDeg,longMin,longSec);
        } else if (searchType == 1) {
            urlString = String.format("http://transition.fcc.gov/fcc-bin/fmq?&serv=FM&vac=3&list=4&dist=%d&dlat2=%d&mlat2=%d&slat2=%f&dlon2=%d&mlon2=%d&slon2=%f",searchRadius,latDeg,latMin,latSec,longDeg,longMin,longSec);
        } else if (searchType == 2) {
            urlString = String.format("http://transition.fcc.gov/fcc-bin/amq?type=2&list=4&dist=%d&dlat2=%d&mlat2=%d&slat2=%f&dlon2=%d&mlon2=%d&slon2=%f",searchRadius,latDeg,latMin,latSec,longDeg,longMin,longSec);
        } else {
            urlString = null;
        }

        URL oracle = new URL(urlString);
        URLConnection yc = oracle.openConnection();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        yc.getInputStream()));
        String inputLine;

        StringBuilder sb = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            sb.append(inputLine).append("\n");

        }

        Log.d("Tower",sb.toString());
        return sb.toString();
    }

    private void parseFCCData(String data) {
        String[] rowArray = Pattern.compile("|", Pattern.LITERAL).split(data);
        for (int x = 0; x < rowArray.length; x++) {
            rowArray[x] = rowArray[x].trim();
        }

        //Log.d("Tower",String.format("data: %s",data));
        //Log.d("Tower",String.format("rowArray: %s",rowArray));

        ArrayList<Map> dataArray = new ArrayList<Map>();

        SharedPreferences prefs = getSharedPreferences("com.lastedit.tower",Context.MODE_PRIVATE);
        if (searchType == 0) {
            int numStations = (rowArray.length - 1) / 39;

            for (int x = 0; x < numStations; x++) {
                double latSign, lonSign;
                double lat,lon;

                int y = x * 39;

                Map<String,Object> station = new HashMap<String,Object>();

                String stationName = rowArray[(x*39)+1].trim();
                station.put("stationName",stationName);

                //Log.d("Tower",String.format("Location: %s / %s / %s / %s / %s / %s / %s / %s",rowArray[y+19].trim(),rowArray[y+23].trim(),rowArray[y+20].trim(),rowArray[y+21].trim(),rowArray[y+22].trim(),rowArray[y+24].trim(),rowArray[y+25].trim(),rowArray[y+26].trim()));

                String latSignS = rowArray[y+19];
                String lonSignS = rowArray[y+23];
                float latDegS  = Float.parseFloat(rowArray[y+20]);
                float latMinS  = Float.parseFloat(rowArray[y+21]);
                float latSecS  = Float.parseFloat(rowArray[y+22]);
                float lonDegS  = Float.parseFloat(rowArray[y+24]);
                float lonMinS  = Float.parseFloat(rowArray[y+25]);
                float lonSecS  = Float.parseFloat(rowArray[y+26]);

                if (latSignS.equals("S"))
                    latSign = -1.0;
                else
                    latSign = 1.0;
                if (lonSignS.equals("W"))
                    lonSign = -1.0;
                else lonSign = 1.0;

                lat = (latDegS + (latMinS / 60.0) + ((latSecS / 60.0) / 60.0)) * latSign;
                lon = (lonDegS + (lonMinS / 60.0) + ((lonSecS / 60.0) / 60.0)) * lonSign;

                station.put("latitude",lat);
                station.put("longitude",lon);

                String channel = rowArray[y+4].trim();
                String power   = rowArray[y+14].trim();
                String psip    = rowArray[y+38].trim();

                station.put("channel",channel);
                station.put("power",power);
                station.put("psip",psip);

                dataArray.add(station);
            }


            PreferenceSerializer preferenceSerializer = new PreferenceSerializer();
            preferenceSerializer.setObject(prefs,"lastTowerSearch",dataArray);
            reloadData();
        } else if (searchType == 1) {
            int numStations = (rowArray.length - 1) / 38;

            for (int x = 0; x < numStations; x++) {
                double latSign, lonSign;
                double lat,lon;

                int y = x * 38;

                Map<String,Object> station = new HashMap<String,Object>();

                String stationName = rowArray[y+1].trim();
                station.put("stationName",stationName);

                //Log.d("Tower",String.format("Location: %s / %s / %s / %s / %s / %s / %s / %s",rowArray[y+19].trim(),rowArray[y+23].trim(),rowArray[y+20].trim(),rowArray[y+21].trim(),rowArray[y+22].trim(),rowArray[y+24].trim(),rowArray[y+25].trim(),rowArray[y+26].trim()));

                String latSignS = rowArray[y+19];
                String lonSignS = rowArray[y+23];
                float latDegS  = Float.parseFloat(rowArray[y+20]);
                float latMinS  = Float.parseFloat(rowArray[y+21]);
                float latSecS  = Float.parseFloat(rowArray[y+22]);
                float lonDegS  = Float.parseFloat(rowArray[y+24]);
                float lonMinS  = Float.parseFloat(rowArray[y+25]);
                float lonSecS  = Float.parseFloat(rowArray[y+26]);

                if (latSignS.equals("S"))
                    latSign = -1.0;
                else
                    latSign = 1.0;
                if (lonSignS.equals("W"))
                    lonSign = -1.0;
                else lonSign = 1.0;

                lat = (latDegS + (latMinS / 60.0) + ((latSecS / 60.0) / 60.0)) * latSign;
                lon = (lonDegS + (lonMinS / 60.0) + ((lonSecS / 60.0) / 60.0)) * lonSign;

                station.put("latitude",lat);
                station.put("longitude",lon);

                String frequency = rowArray[y+2].trim();
                String power   = rowArray[y+14].trim();

                station.put("frequency",frequency);
                station.put("power",power);

                dataArray.add(station);
            }


            PreferenceSerializer preferenceSerializer = new PreferenceSerializer();
            preferenceSerializer.setObject(prefs,"lastTowerSearch",dataArray);
            reloadData();
        } else if (searchType == 2) {
            int numStations = (rowArray.length - 8) / 36;

            for (int x = 0; x < numStations; x++) {
                double latSign, lonSign;
                double lat,lon;

                int y = x * 32;

                Map<String,Object> station = new HashMap<String,Object>();

                String stationName = rowArray[y+1].trim();
                station.put("stationName",stationName);

                //Log.d("Tower",String.format("Location: %s / %s / %s / %s / %s / %s / %s / %s",rowArray[y+19].trim(),rowArray[y+23].trim(),rowArray[y+20].trim(),rowArray[y+21].trim(),rowArray[y+22].trim(),rowArray[y+24].trim(),rowArray[y+25].trim(),rowArray[y+26].trim()));

                String latSignS = rowArray[y+19];
                String lonSignS = rowArray[y+23];
                float latDegS  = Float.parseFloat(rowArray[y+20]);
                float latMinS  = Float.parseFloat(rowArray[y+21]);
                float latSecS  = Float.parseFloat(rowArray[y+22]);
                float lonDegS  = Float.parseFloat(rowArray[y+24]);
                float lonMinS  = Float.parseFloat(rowArray[y+25]);
                float lonSecS  = Float.parseFloat(rowArray[y+26]);

                if (latSignS.equals("S"))
                    latSign = -1.0;
                else
                    latSign = 1.0;
                if (lonSignS.equals("W"))
                    lonSign = -1.0;
                else lonSign = 1.0;

                lat = (latDegS + (latMinS / 60.0) + ((latSecS / 60.0) / 60.0)) * latSign;
                lon = (lonDegS + (lonMinS / 60.0) + ((lonSecS / 60.0) / 60.0)) * lonSign;

                station.put("latitude",lat);
                station.put("longitude",lon);

                String frequency = rowArray[y+2].trim();
                String power   = rowArray[y+14].trim();

                station.put("frequency",frequency);
                station.put("power",power);

                dataArray.add(station);
            }


            PreferenceSerializer preferenceSerializer = new PreferenceSerializer();
            preferenceSerializer.setObject(prefs,"lastTowerSearch",dataArray);
            reloadData();
        }
    }

}
