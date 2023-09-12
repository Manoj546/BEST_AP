package com.example.myapplication;

import static java.lang.Math.pow;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
public class MainActivity extends AppCompatActivity {
    private WifiManager wifiManager;
    private HashMap<String, Integer> ssidHashMap = new HashMap<>();
    private SharedPreferences sharedPrefs;
    private static final int REQUEST_CODE_PERMISSIONS = 123;
    private final String[] requiredPermissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
    };
    private final Handler handler = new Handler(Looper.getMainLooper());
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize SharedPreferences and Gson
        sharedPrefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        sharedPrefs = getSharedPreferences("sharedPref", MODE_PRIVATE);
        Gson gson = new Gson();
        // Load stored data from SharedPreferences
        String storedData = sharedPrefs.getString("ssidHashMap", null);
        TypeToken<HashMap<String, Integer>> typeToken = new TypeToken<>() {
        };
        ssidHashMap = gson.fromJson(storedData, typeToken.getType());
        if (ssidHashMap == null) {
            ssidHashMap = new HashMap<>();
        }
        // Request runtime permissions
        String[] permissions = {Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
        };
        ActivityCompat.requestPermissions(this, permissions, 0);
        // Start scanning periodically
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(requiredPermissions, REQUEST_CODE_PERMISSIONS);
        }
        handler.post(scanningRunnable);
    }
    private final Runnable scanningRunnable = new Runnable() {
        @Override
        public void run() {
            scanWifi();
            handler.postDelayed(this, 10000); // Scan every 10 seconds
        }
    };
    private void scanWifi() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
        Toast.makeText(this, "Scanning Wi-Fi...", Toast.LENGTH_SHORT).show();
    }
    private static double calculateDistance(int signalLevel, double frequency) {
        double distance;
        double exp = (27.55 - (20 * Math.log10(frequency)) + Math.abs(signalLevel)) / 20.0;
        distance = pow(10.0, exp);
        return distance;
    }
    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            unregisterReceiver(this);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            List<ScanResult> results = wifiManager.getScanResults();
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            String current_SSID = wifiInfo.getSSID();
            System.out.println("Current ssid"+wifiInfo.getSSID());
            if (current_SSID != null) {
                Integer count = ssidHashMap.get(current_SSID);
                if (count != null) {
                    ssidHashMap.put(current_SSID, count + 1);
                } else {
                    ssidHashMap.put(current_SSID, 1);
                }
            }
            ListView resultsListView = findViewById(R.id.results_listview);
            ArrayList<String> resultList = new ArrayList<>();
            for (ScanResult scanResult : results) {
                int rssi = scanResult.level;
                int freq = scanResult.frequency;
                String capabilities = scanResult.capabilities;
                int channel_bandwidth = 0;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    channel_bandwidth = scanResult.channelWidth;
                }
                int level = WifiManager.calculateSignalLevel(rssi, 7);
                int secured = 0;
                if (capabilities.contains("WPA") || capabilities.contains("WEP") || capabilities.contains("WPS")) {
                    secured = 2;
                }
                long rxBytes= TrafficStats.getMobileRxBytes();
                long rxPackets=TrafficStats.getMobileRxPackets();
                long txBytes=TrafficStats.getMobileTxBytes();
                long txPackets=TrafficStats.getMobileTxPackets();
                double distance2 = calculateDistance(scanResult.level, scanResult.frequency);
                String ssid = scanResult.SSID;
                int a = 0;
                if (ssidHashMap.containsKey("\"" + ssid + "\"")) {
                    Integer ab = ssidHashMap.get("\"" + ssid + "\"");
                    a = Objects.requireNonNullElse(ab, 0);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    ssidHashMap.entrySet().forEach(System.out::println);
                }
                int bandwidth=getChannelBandwidth(scanResult);
                double bandwidth1 = 20.0; // 20 MHz channel bandwidth for 2.4 GHz band
                double dataRate = bandwidth1 * getMCS(rssi);
                double maxDataRate = bandwidth * 72;
                double channelUtilization = dataRate / maxDataRate * 100;
                String channel_util=String.valueOf(channelUtilization);
                String RSSI= String.valueOf(rssi), Freq= String.valueOf(freq), Secured= String.valueOf(secured), Distance= String.valueOf(distance2);
                String ChannelBandwidth= String.valueOf(channel_bandwidth), rxbytes= String.valueOf(rxBytes), rxpackets= String.valueOf(rxPackets);
                String txbytes= String.valueOf(txBytes),  txpackets= String.valueOf(txPackets), Count= String.valueOf(a), Level= String.valueOf(level);
                resultList.add("SSID: " + ssid + "\nRssi: " + RSSI + "\nFreq: " + Freq + "\nSecured: " + Secured + "\nChannel Bandwidth: " + ChannelBandwidth + "\nRxbytes: " + rxbytes + "\nRxpackets: " + rxpackets + "\nTxbytes: " + txbytes + "\nTxpackets: " + txpackets + "\nCount: " + Count + "\nDistacnce: " + distance2 + "\nchannel_util: " + channel_util + "\nLevel: " + Level + "\t\n\n");
                ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, resultList);
                resultsListView.setAdapter(adapter);
                callMongoApi(context, ssid, RSSI, Freq, Secured, ChannelBandwidth, rxbytes, rxpackets, txbytes, txpackets, Count, Distance, Level, channel_util, new VolleyCallback() {
                    @Override
                    public void onSuccess(String response) {}
                    @Override
                    public void onError(String error) {}
                });
                saveData();
            }
        }
        public int getChannelBandwidth(ScanResult scanResult) {
            int frequency = scanResult.frequency;
            if (frequency >= 2412 && frequency <= 2472) {
                return 20;
            } else if (frequency >= 5170 && frequency <= 5825) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return scanResult.channelWidth;
                }
            }
            return 0;
        }
        private int getMCS(int signalLevel) {
            if (signalLevel >= -50) {
                return 7;
            } else if (signalLevel >= -58) {
                return 6;
            } else if (signalLevel >= -65) {
                return 5;
            } else if (signalLevel >= -72) {
                return 4;
            } else if (signalLevel >= -78) {
                return 3;
            } else if (signalLevel >= -84) {
                return 2;
            } else if (signalLevel >= -90) {
                return 1;
            } else {
                return 0;
            }
        }

        public void callMongoApi(Context context, String SSID, String RSSI, String Freq, String Secured, String ChannelBandwidth, String rxbytes, String rxpackets, String txbytes, String txpackets, String count, String distance, String Level, String channel_util, VolleyCallback callback) {
            String url = "https://mongo-api-connect.onrender.com/upload";
            JSONObject jsonParams = new JSONObject();
            try {
                jsonParams.put("SSID", SSID);
                jsonParams.put("RSSI", RSSI);
                jsonParams.put("Freq", Freq);
                jsonParams.put("Secured", Secured);
                jsonParams.put("ChannelBandwidth", ChannelBandwidth);
                jsonParams.put("rxbytes", rxbytes);
                jsonParams.put("rxpackets", rxpackets);
                jsonParams.put("txbytes", txbytes);
                jsonParams.put("txpackets", txpackets);
                jsonParams.put("count", count);
                jsonParams.put("distance", distance);
                jsonParams.put("channel_utilisation", channel_util);
                jsonParams.put("Level", Level);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, jsonParams,
                    response -> {
                        String stringResponse = response.toString();
                        Log.d("FlaskAPI", "Response from Flask API: " + stringResponse);
                        callback.onSuccess(stringResponse);
                    },
                    error -> {
                        String errorMsg = error != null ? error.toString() : "Unknown error occurred";
                        Log.e("FlaskAPI", "Error calling Flask API: " + errorMsg);
                        callback.onError(errorMsg);
                    }
            );
            // Add the request to the RequestQueue.
            VolleySingleton.getInstance(context).addToRequestQueue(postRequest);
        }
    };
    private void saveData() {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        Gson gson = new Gson();
        String data = gson.toJson(ssidHashMap);
        editor.putString("ssidHashMap", data);
        editor.apply();
    }
    interface VolleyCallback {
        void onSuccess(String response);
        void onError(String error);
    }
    @Override
    protected void onDestroy() {
        handler.removeCallbacks(scanningRunnable);
        super.onDestroy();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0 && grantResults.length >= 3 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scanWifi();
        }
    }
}