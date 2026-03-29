package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.ServiceSettings;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements GeocodeSearch.OnGeocodeSearchListener {

    private TextView tvWeatherInfo;
    private AMapLocationClient locationClient;
    private GeocodeSearch geocodeSearch;
    
    // Web服务Key - 专门用于REST API天气查询
    private final String WEB_SERVICE_KEY = "d7d1f7483b14a234df2859c706e5371e";

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (fineGranted || coarseGranted) {
                    startLocation();
                } else {
                    tvWeatherInfo.setText("点击开启定位权限");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 1. 严格合规初始化
        AMapLocationClient.updatePrivacyShow(this, true, true);
        AMapLocationClient.updatePrivacyAgree(this, true);
        ServiceSettings.updatePrivacyShow(this, true, true);
        ServiceSettings.updatePrivacyAgree(this, true);
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);

        setContentView(R.layout.activity_main);
        tvWeatherInfo = findViewById(R.id.tv_weather_info);
        tvWeatherInfo.setText("正在获取定位...");

        try {
            geocodeSearch = new GeocodeSearch(this);
            geocodeSearch.setOnGeocodeSearchListener(this);
        } catch (AMapException e) { e.printStackTrace(); }

        initClickListeners();
        checkLocationPermission();
    }

    private void initClickListeners() {
        tvWeatherInfo.setOnClickListener(v -> {
            tvWeatherInfo.setText("正在重新定位...");
            startLocation();
        });
        findViewById(R.id.fab_center_card).setOnClickListener(v -> new AddItineraryBottomSheet().show(getSupportFragmentManager(), "add"));
        findViewById(R.id.btn_nearby).setOnClickListener(v -> startActivity(new Intent(this, NearbyActivity.class)));
        setupSpecialTopicListeners();
    }

    private void setupSpecialTopicListeners() {
        View.OnClickListener listener = v -> {
            String city = "", title = "";
            int id = v.getId();
            if (id == R.id.card_jinan_spring) { city = "济南"; title = "济南的泉：北方大地上的温润玉石"; }
            else if (id == R.id.card_shanghai_bund) { city = "上海"; title = "上海的外滩：黄浦江畔的万国史诗"; }
            else if (id == R.id.card_beijing_palace) { city = "北京"; title = "北京的故宫：红墙黄瓦里的千年星河"; }
            else if (id == R.id.card_nanjing_lake) { city = "南京"; title = "南京的湖：金陵烟水里的温婉诗意"; }
            else if (id == R.id.card_wuhan_river) { city = "武汉"; title = "武汉的江：两江交汇里的豪迈灵秀"; }

            if (!city.isEmpty()) {
                Intent intent = new Intent(this, ItineraryDetailActivity.class);
                intent.putExtra("itinerary_title", title);
                intent.putExtra("target_city", city);
                startActivity(intent);
            }
        };
        if (findViewById(R.id.card_jinan_spring) != null) findViewById(R.id.card_jinan_spring).setOnClickListener(listener);
        if (findViewById(R.id.card_shanghai_bund) != null) findViewById(R.id.card_shanghai_bund).setOnClickListener(listener);
        if (findViewById(R.id.card_beijing_palace) != null) findViewById(R.id.card_beijing_palace).setOnClickListener(listener);
        if (findViewById(R.id.card_nanjing_lake) != null) findViewById(R.id.card_nanjing_lake).setOnClickListener(listener);
        if (findViewById(R.id.card_wuhan_river) != null) findViewById(R.id.card_wuhan_river).setOnClickListener(listener);
    }

    private void loadAllDynamicItineraries() {
        LinearLayout container = findViewById(R.id.ll_itinerary_container);
        if (container == null) return;
        container.removeAllViews();
        SharedPreferences sp = getSharedPreferences("MyTravelPlans", Context.MODE_PRIVATE);
        try {
            JSONArray plansArray = new JSONArray(sp.getString("plans_json", "[]"));
            for (int i = plansArray.length() - 1; i >= 0; i--) { addItineraryCard(container, plansArray.getJSONObject(i)); }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void addItineraryCard(LinearLayout container, JSONObject plan) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_itinerary_card, container, false);
        TextView tvTitle = cardView.findViewById(R.id.tv_plan_title);
        final String realTitle = plan.optString("title", "我的行程");
        tvTitle.setText(realTitle);
        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(this, ItineraryDetailActivity.class);
            intent.putExtra("itinerary_id", plan.optLong("id", -1));
            intent.putExtra("itinerary_title", realTitle);
            intent.putExtra("plan_content", plan.optString("content"));
            intent.putExtra("target_city", plan.optString("city_name"));
            startActivity(intent);
        });
        container.addView(cardView);
    }

    @Override protected void onResume() { super.onResume(); loadAllDynamicItineraries(); }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocation();
        } else {
            requestPermissionsLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    private void startLocation() {
        try {
            if (locationClient != null) { locationClient.stopLocation(); locationClient.onDestroy(); }
            locationClient = new AMapLocationClient(getApplicationContext());
            AMapLocationClientOption option = new AMapLocationClientOption();
            option.setOnceLocation(true);
            option.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            option.setNeedAddress(true);
            option.setHttpTimeOut(20000);
            locationClient.setLocationOption(option);
            locationClient.setLocationListener(location -> {
                if (location != null && location.getErrorCode() == 0) {
                    String adCode = location.getAdCode();
                    String city = location.getCity();
                    if (adCode != null && !adCode.isEmpty()) {
                        fetchWeatherViaWebAPI(adCode, (location.getDistrict() != null ? location.getDistrict() : city));
                    } else {
                        tvWeatherInfo.setText(city + " (获取区域码失败)");
                    }
                } else {
                    int err = (location != null) ? location.getErrorCode() : -1;
                    tvWeatherInfo.setText("定位失败: " + err);
                }
            });
            locationClient.startLocation();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void fetchWeatherViaWebAPI(String adCode, String locationName) {
        new Thread(() -> {
            try {
                String apiUrl = "https://restapi.amap.com/v3/weather/weatherInfo?city=" + adCode + "&key=" + WEB_SERVICE_KEY + "&extensions=base";
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                
                // 【核心修复点】先获取状态码
                int responseCode = conn.getResponseCode(); 
                
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    JSONObject json = new JSONObject(sb.toString());
                    if ("1".equals(json.optString("status"))) {
                        JSONArray lives = json.optJSONArray("lives");
                        if (lives != null && lives.length() > 0) {
                            JSONObject live = lives.getJSONObject(0);
                            final String weatherStr = locationName + " " + live.optString("weather") + " " + live.optString("temperature") + "℃";
                            runOnUiThread(() -> tvWeatherInfo.setText(weatherStr));
                        } else { runOnUiThread(() -> tvWeatherInfo.setText(locationName + " 天气未知")); }
                    } else {
                        final String info = json.optString("info");
                        runOnUiThread(() -> tvWeatherInfo.setText("服务失败: " + info));
                    }
                } else {
                    // 这里直接使用上面获取到的 responseCode 变量，不再在 Lambda 内部调用 conn.getResponseCode()
                    runOnUiThread(() -> tvWeatherInfo.setText("网络异常: " + responseCode)); 
                }
            } catch (Exception e) {
                Log.e("WeatherApp", "请求崩溃", e);
                runOnUiThread(() -> tvWeatherInfo.setText("查询异常"));
            }
        }).start();
    }

    @Override public void onRegeocodeSearched(RegeocodeResult result, int rCode) {}
    @Override public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {}
    @Override protected void onDestroy() { super.onDestroy(); if (locationClient != null) locationClient.onDestroy(); }
}
