package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.google.android.material.card.MaterialCardView;

public class NearbyActivity extends AppCompatActivity implements GeocodeSearch.OnGeocodeSearchListener {

    private MapView mapView;
    private AMap aMap;
    private GeocodeSearch geocoderSearch;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);
        setContentView(R.layout.activity_nearby);

        mapView = findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        if (aMap == null) aMap = mapView.getMap();

        try {
            geocoderSearch = new GeocodeSearch(this);
            geocoderSearch.setOnGeocodeSearchListener(this);
        } catch (AMapException e) { e.printStackTrace(); }

        findViewById(R.id.btn_back_map).setOnClickListener(v -> finish());
        handleEntrySource();
        checkPermissions();
    }

    private void handleEntrySource() {
        MaterialCardView card = findViewById(R.id.card_special_info);
        Intent intent = getIntent();
        if (intent == null) return;

        boolean isFromItinerary = intent.getBooleanExtra("is_from_itinerary", false);
        String name = intent.getStringExtra("target_name");

        if (isFromItinerary) {
            if (card != null) card.setVisibility(View.GONE);
        } else if (name != null) {
            if (card != null) card.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tv_special_title)).setText(name);
            ((TextView) findViewById(R.id.tv_special_desc)).setText(intent.getStringExtra("target_desc"));
            ((TextView) findViewById(R.id.tv_special_tag)).setText(intent.getStringExtra("target_tag"));
            String imgName = intent.getStringExtra("target_img");
            if (imgName != null) {
                int resId = getResources().getIdentifier(imgName, "drawable", getPackageName());
                if (resId != 0) ((ImageView) findViewById(R.id.iv_special_img)).setImageResource(resId);
            }
        }

        if (intent.hasExtra("target_lat")) {
            double lat = intent.getDoubleExtra("target_lat", 0);
            double lng = intent.getDoubleExtra("target_lng", 0);
            LatLng pos = new LatLng(lat, lng);
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
            aMap.addMarker(new MarkerOptions().position(pos).title(name));
        } else if (intent.hasExtra("target_city")) {
            searchCityLocation(intent.getStringExtra("target_city"));
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            initLocation();
        }
    }

    /**
     * 【核心修复】处理权限请求的回调结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户授权，立即启动定位
                initLocation();
            } else {
                // 用户拒绝，给出提示
                Toast.makeText(this, "需要定位权限才能查看附近哦", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initLocation() {
        MyLocationStyle style = new MyLocationStyle();
        Intent intent = getIntent();
        // 如果是去特定目的地（专题或行程），定位蓝点不自动移动地图中心
        if (intent != null && (intent.hasExtra("target_lat") || intent.hasExtra("target_city"))) {
            style.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
        } else {
            // 如果是“附近”功能，自动定位到当前位置
            style.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);
        }
        aMap.setMyLocationStyle(style);
        aMap.setMyLocationEnabled(true);
    }

    private void searchCityLocation(String cityName) {
        if (geocoderSearch != null) {
            geocoderSearch.getFromLocationNameAsyn(new GeocodeQuery(cityName, ""));
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult result, int rCode) {
        if (rCode == 1000 && result != null && !result.getGeocodeAddressList().isEmpty()) {
            GeocodeAddress addr = result.getGeocodeAddressList().get(0);
            LatLng lp = new LatLng(addr.getLatLonPoint().getLatitude(), addr.getLatLonPoint().getLongitude());
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lp, 13f));
        }
    }

    @Override public void onRegeocodeSearched(RegeocodeResult r, int c) {}
    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
}