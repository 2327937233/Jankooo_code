package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast; // 补全了缺失的导入

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.ServiceSettings;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONObject;

public class ItineraryDetailActivity extends AppCompatActivity implements GeocodeSearch.OnGeocodeSearchListener {

    private MapView mapView;
    private AMap aMap;
    private TabLayout tabLayout;
    private LinearLayout llDayContent;
    private String rawPlanContent;
    private JSONArray dailyItinerary;
    private String itineraryTitle;
    private String cityNameForLocation;
    private long itineraryId = -1; // 接收传过来的唯一ID
    
    // 专题卡片 UI
    private MaterialCardView cardSpecialTopic;
    private ImageView ivTopicThumb;
    private TextView tvTopicTitle, tvTopicDesc;
    private ImageView ivCloseTopicCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 隐私合规
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);
        ServiceSettings.updatePrivacyShow(this, true, true);
        ServiceSettings.updatePrivacyAgree(this, true);

        setContentView(R.layout.activity_itinerary_detail);

        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        tabLayout = findViewById(R.id.tab_days);
        llDayContent = findViewById(R.id.ll_day_content);
        
        // 初始化专题卡片组件
        cardSpecialTopic = findViewById(R.id.card_special_topic_info);
        ivTopicThumb = findViewById(R.id.iv_topic_thumb);
        tvTopicTitle = findViewById(R.id.tv_topic_title);
        tvTopicDesc = findViewById(R.id.tv_topic_desc);
        ivCloseTopicCard = findViewById(R.id.iv_close_topic_card);
        
        itineraryId = getIntent().getLongExtra("itinerary_id", -1);
        itineraryTitle = getIntent().getStringExtra("itinerary_title");
        rawPlanContent = getIntent().getStringExtra("plan_content");
        cityNameForLocation = getIntent().getStringExtra("target_city");

        initMap();
        
        // 1. 处理 UI 显示
        if (rawPlanContent != null && !rawPlanContent.isEmpty()) {
            cardSpecialTopic.setVisibility(View.GONE);
            View bottomSheet = findViewById(R.id.bottom_sheet);
            bottomSheet.setVisibility(View.VISIBLE);
            
            // 显示顶部删除按钮
            findViewById(R.id.btn_delete_itinerary).setVisibility(View.VISIBLE);
            
            // 设置默认显示高度为屏幕的 30%
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int peekHeight = (int) (displayMetrics.heightPixels * 0.3);
            behavior.setPeekHeight(peekHeight);
            
            parseAndDisplayData();
        } else {
            // 精选专题模式
            setupSpecialTopicUI();
        }
        
        // 2. 定位
        determineCityAndLocate();

        setupClickListeners();
    }

    private void setupSpecialTopicUI() {
        cardSpecialTopic.setVisibility(View.VISIBLE);
        findViewById(R.id.bottom_sheet).setVisibility(View.GONE);
        
        // 【关键】精选专题模式下隐藏右上角的删除按钮
        findViewById(R.id.btn_delete_itinerary).setVisibility(View.GONE);

        TextView tagText = (TextView) cardSpecialTopic.findViewById(R.id.tv_tag_text);
        ImageView tagIcon = (ImageView) cardSpecialTopic.findViewById(R.id.iv_tag_icon);

        if ("济南".equals(cityNameForLocation)) {
            tvTopicTitle.setText("济南的泉：北方大地上的温润玉石");
            tvTopicDesc.setText("趵突泉天下闻名，除此之外惊喜比比皆是。开启一场济南冬日旅行吧。");
            ivTopicThumb.setImageResource(R.drawable.jinan_spring);
            cityNameForLocation = "济南趵突泉";
            if(tagText != null) tagText.setText("目的地");
        } else if ("上海".equals(cityNameForLocation)) {
            tvTopicTitle.setText("上海的外滩：黄浦江畔的万国史诗");
            tvTopicDesc.setText("外滩的晚风裹挟着万国建筑的灯火，与你并肩，就是上海最温柔的浪漫。");
            ivTopicThumb.setImageResource(R.drawable.shanghai);
            cityNameForLocation = "上海外滩";
            if(tagText != null) tagText.setText("目的地");
            if(tagIcon != null) tagIcon.setImageResource(R.drawable.ic_location_outline);
        } else if ("北京".equals(cityNameForLocation)) {
            tvTopicTitle.setText("北京的故宫：红墙黄瓦里的千年星河");
            tvTopicDesc.setText("站在景山之巅俯瞰紫禁城，那抹跨越千年的朱红，是流淌在京城中轴线上最壮丽的诗篇。");
            ivTopicThumb.setImageResource(R.drawable.beijing);
            cityNameForLocation = "故宫博物院";
            if(tagText != null) tagText.setText("目的地");
        } else if ("南京".equals(cityNameForLocation)) {
            tvTopicTitle.setText("南京的湖：金陵烟水里的温婉诗意");
            tvTopicDesc.setText("玄武湖的烟雨，笼着六朝的旧梦。漫步湖畔，听柳浪闻莺，看远方紫金山的剪影，这是金陵城最温柔的底色。");
            ivTopicThumb.setImageResource(R.drawable.nanjing);
            cityNameForLocation = "南京玄武湖";
            if(tagText != null) tagText.setText("目的地");
        } else if ("武汉".equals(cityNameForLocation)) {
            tvTopicTitle.setText("武汉的江：两江交汇里的豪迈灵秀");
            tvTopicDesc.setText("两江交汇潮涌江城，江风漫卷烟火繁华，一湾碧水藏尽武汉的豪迈与温柔。");
            ivTopicThumb.setImageResource(R.drawable.wuhan);
            cityNameForLocation = "武汉长江大桥";
            if(tagText != null) tagText.setText("目的地");
        }

        ivCloseTopicCard.setOnClickListener(v -> cardSpecialTopic.setVisibility(View.GONE));
    }

    private void determineCityAndLocate() {
        if (cityNameForLocation == null || cityNameForLocation.isEmpty()) {
            if (itineraryTitle != null) {
                cityNameForLocation = itineraryTitle.split("之旅")[0]
                        .split("日游")[0].replace("行程", "").trim();
            }
        }
        if (cityNameForLocation == null || cityNameForLocation.isEmpty()) cityNameForLocation = "济南";
        locateToCity(cityNameForLocation);
    }

    private void initMap() {
        if (aMap == null) aMap = mapView.getMap();
        aMap.getUiSettings().setZoomControlsEnabled(true);
        aMap.moveCamera(CameraUpdateFactory.zoomTo(14));
    }

    private void locateToCity(String city) {
        try {
            GeocodeSearch search = new GeocodeSearch(this);
            search.setOnGeocodeSearchListener(this);
            search.getFromLocationNameAsyn(new GeocodeQuery(city, ""));
        } catch (AMapException e) { e.printStackTrace(); }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult result, int rCode) {
        if (rCode == 1000 && result != null && !result.getGeocodeAddressList().isEmpty()) {
            com.amap.api.services.geocoder.GeocodeAddress addr = result.getGeocodeAddressList().get(0);
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new com.amap.api.maps.model.LatLng(addr.getLatLonPoint().getLatitude(), addr.getLatLonPoint().getLongitude()), 15));
        }
    }

    @Override public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {}

    private void parseAndDisplayData() {
        try {
            if (rawPlanContent == null || rawPlanContent.isEmpty()) return;
            String cleanJson = rawPlanContent.replace("\\\"", "\"").replace("\\n", "\n");
            if (cleanJson.startsWith("\"")) cleanJson = cleanJson.substring(1, cleanJson.length() - 1);

            JSONObject root = new JSONObject(cleanJson);
            dailyItinerary = root.optJSONArray("daily_itinerary");
            if (dailyItinerary != null) {
                tabLayout.removeAllTabs();
                tabLayout.addTab(tabLayout.newTab().setText("总览"));
                for (int i = 0; i < dailyItinerary.length(); i++) {
                    tabLayout.addTab(tabLayout.newTab().setText("DAY " + (i + 1)));
                }
                displayOverview(); 
                tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        int pos = tab.getPosition();
                        if (pos == 0) displayOverview(); else displayDayPlan(pos - 1);
                    }
                    @Override public void onTabUnselected(TabLayout.Tab tab) {}
                    @Override public void onTabReselected(TabLayout.Tab tab) {}
                });
            }
        } catch (Exception e) { Log.e("DATA_ERR", e.getMessage()); }
    }

    private void displayOverview() {
        llDayContent.removeAllViews();
        View v = LayoutInflater.from(this).inflate(R.layout.item_timeline_plan, llDayContent, false);
        ((TextView)v.findViewById(R.id.tv_time)).setText("✨");
        ((TextView)v.findViewById(R.id.tv_poi_name)).setText(itineraryTitle);
        ((TextView)v.findViewById(R.id.tv_reason)).setText(getFormattedFullContent(rawPlanContent));
        llDayContent.addView(v);
    }

    private String getFormattedFullContent(String jsonStr) {
        try {
            String clean = jsonStr.replace("\\\"", "\"").replace("\\n", "\n");
            if (clean.startsWith("\"")) clean = clean.substring(1, clean.length()-1);
            JSONObject root = new JSONObject(clean);
            StringBuilder sb = new StringBuilder();
            JSONArray days = root.optJSONArray("daily_itinerary");
            if (days != null) {
                for (int i = 0; i < days.length(); i++) {
                    JSONObject d = days.getJSONObject(i);
                    sb.append("📅 DAY ").append(d.optInt("day")).append("\n");
                    JSONArray sc = d.optJSONArray("schedule");
                    for (int j = 0; j < sc.length(); j++) {
                        JSONObject item = sc.getJSONObject(j);
                        sb.append(" • ").append(item.optString("time_window"))
                          .append(" ").append(item.optString("poi_name")).append("\n");
                    }
                    sb.append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) { return jsonStr; }
    }

    private void displayDayPlan(int dayIdx) {
        llDayContent.removeAllViews();
        try {
            JSONArray schedule = dailyItinerary.getJSONObject(dayIdx).optJSONArray("schedule");
            if (schedule != null) {
                for (int i = 0; i < schedule.length(); i++) {
                    JSONObject item = schedule.getJSONObject(i);
                    View v = LayoutInflater.from(this).inflate(R.layout.item_timeline_plan, llDayContent, false);
                    ((TextView)v.findViewById(R.id.tv_time)).setText(item.optString("time_window"));
                    ((TextView)v.findViewById(R.id.tv_poi_name)).setText(item.optString("poi_name"));
                    ((TextView)v.findViewById(R.id.tv_reason)).setText(item.optString("reason"));
                    llDayContent.addView(v);
                }
            }
        } catch (Exception ignored) {}
    }

    private void setupClickListeners() {
        findViewById(R.id.btn_back_map).setOnClickListener(v -> finish());
        findViewById(R.id.btn_delete_itinerary).setOnClickListener(v -> {
            new AlertDialog.Builder(this).setMessage("确定要删除吗？")
                .setPositiveButton("删除", (d, w) -> deleteItinerary()).setNegativeButton("取消", null).show();
        });
    }

    private void deleteItinerary() {
        if (itineraryId == -1) {
            Toast.makeText(this, "无法定位行程ID，删除失败", Toast.LENGTH_SHORT).show();
            return;
        }
        SharedPreferences sp = getSharedPreferences("MyTravelPlans", Context.MODE_PRIVATE);
        try {
            JSONArray array = new JSONArray(sp.getString("plans_json", "[]"));
            JSONArray newArray = new JSONArray();
            boolean deleted = false;
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                // 使用 ID 进行唯一匹配删除，不再依赖不稳定的 Title
                if (obj.optLong("id", -2) != itineraryId) {
                    newArray.put(obj);
                } else {
                    deleted = true;
                }
            }
            if (deleted) {
                sp.edit().putString("plans_json", newArray.toString()).apply();
                Toast.makeText(this, "行程已删除", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "未找到匹配行程", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); if (mapView != null) mapView.onDestroy(); }
}
