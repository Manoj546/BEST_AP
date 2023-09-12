package com.example.myapplication;
import static java.lang.Math.pow;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
public class MainActivity extends AppCompatActivity {
    private WifiManager wifiManager;
    private HashMap<String, Integer> ssidHashMap = new HashMap<>();
    private SharedPreferences sharedPrefs;
    double max=0.0;
    String best_ap="";
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
    public void myButtonClick(View view) {
        if(!Objects.equals(best_ap, ""))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Enter password for " + best_ap);
            // Set up the input
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);
            // Set up the buttons
            builder.setPositiveButton("Submit", (dialog, which) -> {
                String userInput = input.getText().toString();
                // Handle the user input (e.g., validate, process, etc.)
                // You can replace this with your desired functionality
                connectToWifi(best_ap, userInput);
                Toast.makeText(getApplicationContext(), "Submitted: " + userInput, Toast.LENGTH_SHORT).show();
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            // Create and show the dialog
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        else{
            Toast.makeText(getApplicationContext(), "Wait for scores from API", Toast.LENGTH_SHORT).show();
        }
    }
    private void connectToWifi(String ssiD, String pass) {
        WifiNetworkSuggestion suggestion = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            suggestion = new WifiNetworkSuggestion.Builder()
                    .setSsid(ssiD)
                    .setWpa2Passphrase(pass)
                    .build();
        }
        List<WifiNetworkSuggestion> suggestionsList = new ArrayList<>();
        suggestionsList.add(suggestion);
        int status = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            status = wifiManager.addNetworkSuggestions(suggestionsList);
        }
        if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            Toast.makeText(this, "Wi-Fi network suggestion added.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to add Wi-Fi network suggestion.", Toast.LENGTH_SHORT).show();
        }
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
            max=0.0;
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
                if(Integer.parseInt(String.valueOf(rssi)) >-85) {
                    callFlaskApi(context, RSSI, Freq, ChannelBandwidth, rxbytes, rxpackets, txbytes, txpackets, Count, Distance, channel_util, new VolleyCallback() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onSuccess(String response) {
                            try {
                                JSONObject jsonResponse = new JSONObject(response);
                                String res1 = jsonResponse.getString("prediction");
                                res1 = res1.substring(1, res1.length() - 1);
                                double value8 = Double.parseDouble(res1);
                                resultList.add("SSID: " + ssid + "\nRssi: " + RSSI + "\nFreq: " + Freq + "\nSecured: " + Secured + "\nChannel Bandwidth: " + ChannelBandwidth + "\nRxbytes: " + rxbytes + "\nRxpackets: " + rxpackets + "\nTxbytes: " + txbytes + "\nTxpackets: " + txpackets + "\nCount: " + Count + "\nDistacnce: " + distance2 + "\nchannel_util: " + channel_util + "\nLevel: " + Level + "\tResponse: " + res1 + "\n\n");
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, resultList);
                                resultsListView.setAdapter(adapter);
                                if (max <= value8) {
                                    max = value8;
                                    best_ap = ssid;
                                    TextView textView1 = findViewById(R.id.topTextView);
                                    textView1.setText("Best Wifi is\nSSID: " + best_ap + "\tScore: " + value8);
                                }
                                String FILENAME = "wifi_details12.csv";
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    String entry = ssid + ", " + RSSI + ", " + Freq + ", " + Secured + ", " + ChannelBandwidth + ", " + rxbytes + ", " + rxpackets + ", " + txbytes + ", " + txpackets + ", " + Count + ", " + distance2 + ", " + channel_util + ", " + Level + ", " + res1 + "\n";
                                    try {
                                        FileOutputStream out = openFileOutput(FILENAME, Context.MODE_APPEND);
                                        out.write(entry.getBytes());
                                        out.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } catch (JSONException e) {
                                Log.e("FlaskAPI", "Error parsing JSON response: " + e.getMessage());
                            }
                        }
                        @Override
                        public void onError(String error) {
                            Log.e("FlaskAPI", "Error calling Flask API: " + error);
                        }
                    });
                }
                else{
                    resultList.add("SSID: " + ssid + "\nRssi: " + RSSI + "\nFreq: " + Freq + "\nSecured: " + Secured + "\nChannel Bandwidth: " + ChannelBandwidth + "\nRxbytes: " + rxbytes + "\nRxpackets: " + rxpackets + "\nTxbytes: " + txbytes + "\nTxpackets: " + txpackets + "\nCount: " + Count + "\nDistacnce: " + distance2 + "\nchannel_util: " + channel_util + "\nLevel: " + Level + "\tResponse: " + "Weak Signal" + "\n\n");
                    // Create an ArrayAdapter to handle the ListView items
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, resultList);
                    resultsListView.setAdapter(adapter);
                }
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
        public void callFlaskApi(Context context, String RSSI, String Freq, String ChannelBandwidth, String rxbytes, String rxpackets, String txbytes, String txpackets, String count, String distance, String channel_util, VolleyCallback callback) {
            String url = "https://ap-selection-result.onrender.com/predict";
            JSONObject jsonParams = new JSONObject();
            try {
                jsonParams.put("ChannelBandwidth", ChannelBandwidth);
                jsonParams.put("Freq", Freq);
                jsonParams.put("RSSI", RSSI);
                jsonParams.put("channel_utilisation", channel_util);
                jsonParams.put("count", count);
                jsonParams.put("distance", distance);
                jsonParams.put("rxbytes", rxbytes);
                jsonParams.put("rxpackets", rxpackets);
                jsonParams.put("txbytes", txbytes);
                jsonParams.put("txpackets", txpackets);
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