/*
 * Copyright (C) 2015 Robert Bagge
 * Copyright (C) 2015 William Martinsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.locationlocationlocation.wifiscanner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private final static String MENU_ACTION_SEARCH_MESSAGE = "Search not implemented";
    private final static String MENU_ACTION_SCAN_MESSAGE = "Scan not implemented";
    private final static String MENU_ACTION_SAVE_MESSAGE = "Save completed";
    private final static String MENU_ACTION_SAVE_FAILED_MESSAGE = "No WiFis to save";
    private final static String MENU_ACTION_SETTINGS_MESSAGE = "Settings not implemented";

    private Context context;

    WifiManager mainWifiObj;
    WifiScanReceiver wifiReciever;
    ListView list;
    List<ScanResult> wifiScanList;
    long scanStarted;
    LinearLayout linlaHeaderProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        list = (ListView)findViewById(R.id.listView1);
        mainWifiObj = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiReciever = new WifiScanReceiver();
        linlaHeaderProgress = (LinearLayout) findViewById(R.id.linlaHeaderProgress);
    }

    protected void onPause() {
        unregisterReceiver(wifiReciever);
        super.onPause();
    }

    protected void onResume() {
        registerReceiver(wifiReciever, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_play:
                startNewScan();
                return true;
            case R.id.action_save_icon:
                saveResults();
                return true;
            case R.id.action_exit:
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            case R.id.action_save:
                saveResults();
                return true;
            case R.id.action_scan:
                startNewScan();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    class WifiScanReceiver extends BroadcastReceiver {
        @SuppressLint("UseValueOf")
        public void onReceive(Context c, Intent intent) {
            linlaHeaderProgress.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);

            Log.d(TAG, "Scan finished after: " + Long.toString(System.currentTimeMillis() - scanStarted) + "ms");
            wifiScanList = mainWifiObj.getScanResults();
            list.setAdapter(new ScanResultsAdapter(context, wifiScanList));
        }
    }

    private void openSearch(){
        Toast.makeText(context, MENU_ACTION_SEARCH_MESSAGE, Toast.LENGTH_SHORT).show();
    }

    private void openSettings(){
        Toast.makeText(context, MENU_ACTION_SETTINGS_MESSAGE, Toast.LENGTH_SHORT).show();
    }

    private void saveResults(){
        if(wifiScanList != null){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy_dd_MM_HH_mm_ss");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(scanStarted);
            String timestamp = sdf.format(calendar.getTime());


            String folderPath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/WifiScanner";
            if(folderExists(folderPath)){
                String csv = folderPath + "/" + timestamp + "_snap.csv";
                CSVWriter writer = null;
                try {
                    writer = new CSVWriter(new FileWriter(csv), ',');
                    List<String[]> data = new ArrayList<String[]>();
                    ScanResult row = null;
                    for(int i = 0; i < wifiScanList.size(); i++){
                        row = wifiScanList.get(i);
                        data.add(new String[] {row.BSSID, row.SSID, String.valueOf(convertFrequencyToChannel(row.frequency)), String.valueOf(row.level)});
                    }
                    writer.writeAll(data);
                    writer.close();
                    Toast.makeText(context, MENU_ACTION_SAVE_MESSAGE, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Save to path: " + csv);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
                Toast.makeText(context, MENU_ACTION_SAVE_FAILED_MESSAGE, Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(context, MENU_ACTION_SAVE_FAILED_MESSAGE, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean folderExists(String folderPath){
        File folder = new File(folderPath);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
        }
        return success;
    }

    private void startNewScan(){
        mainWifiObj.startScan();
        scanStarted = System.currentTimeMillis();
        wifiScanList = null;
        Log.d(TAG, "Scan started");
        linlaHeaderProgress.setVisibility(View.VISIBLE);
        list.setVisibility(View.GONE);
    }

    private static int convertFrequencyToChannel(int freq) {
        if (freq >= 2412 && freq <= 2484) {
            return (freq - 2412) / 5 + 1;
        } else if (freq >= 5170 && freq <= 5825) {
            return (freq - 5170) / 5 + 34;
        } else {
            return -1;
        }
    }


    private class ScanResultsAdapter extends ArrayAdapter<ScanResult> {
        public ScanResultsAdapter(Context context, List<ScanResult> scanResults) {
            super(context, 0, scanResults);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            ScanResult scanResult = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_scan_result, parent, false);
            }
            // Lookup view for data population
            TextView tvBSSID = (TextView) convertView.findViewById(R.id.bssid);
            TextView tvSSID = (TextView) convertView.findViewById(R.id.ssid);
            TextView tvRSSI = (TextView) convertView.findViewById(R.id.rssi);
            TextView tvFrequency = (TextView) convertView.findViewById(R.id.frequency);

            // Populate the data into the template view using the data object
            tvBSSID.setText(scanResult.BSSID);
            tvSSID.setText(scanResult.SSID);
            int level = scanResult.level;
            tvRSSI.setText(Integer.toString(level) + " dB");

            int frequency = scanResult.frequency;
            String frequencyStr = "?? Hz";
            if(frequency > 2000 && frequency < 3000) {
                frequencyStr = "2.4 GHz";

            }
            else if(frequency > 4500 && frequency < 5500){
                frequencyStr = "5 GHz";
            }
            tvFrequency.setText(frequencyStr);


            if(level > -50){
                tvRSSI.setTextColor(getResources().getColor(R.color.flat_ui_turqouise));
            }else if(level > -70){
                tvRSSI.setTextColor(getResources().getColor(R.color.flat_ui_sun_flower));
            }else{
                tvRSSI.setTextColor(getResources().getColor(R.color.flat_ui_pumpkin));
            }
            // Return the completed view to render on screen
            return convertView;
        }
    }
}