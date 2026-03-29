package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONObject;

public class AIResultActivity extends AppCompatActivity {

    private String rawJson;
    private String targetCity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_result);

        TextView tvAiContent = findViewById(R.id.tv_ai_content);
        ImageButton btnBack = findViewById(R.id.btn_back);
        MaterialButton btnSave = findViewById(R.id.btn_save_itinerary);

        rawJson = getIntent().getStringExtra("raw_json_content");
        targetCity = getIntent().getStringExtra("target_city"); // 这里接收的是用户输入的纯净地名

        if (rawJson != null) {
            tvAiContent.setText(formatJsonToDisplay(rawJson));
        }

        btnBack.setOnClickListener(v -> finish());
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveItinerary());
        }
    }

    private String formatJsonToDisplay(String jsonStr) {
        try {
            JSONObject root = new JSONObject(jsonStr.replace("\\\"", "\"").replace("\\n", "\n"));
            StringBuilder sb = new StringBuilder();
            JSONObject overview = root.optJSONObject("trip_overview");
            if (overview != null) {
                sb.append("✨ ").append(overview.optString("title")).append("\n\n");
            }
            JSONArray itinerary = root.optJSONArray("daily_itinerary");
            if (itinerary != null) {
                for (int i = 0; i < itinerary.length(); i++) {
                    JSONObject day = itinerary.getJSONObject(i);
                    sb.append("📅 DAY ").append(day.optInt("day")).append("\n");
                    JSONArray sc = day.optJSONArray("schedule");
                    for (int j = 0; j < sc.length(); j++) {
                        JSONObject item = sc.getJSONObject(j);
                        if (!"通勤".equals(item.optString("action"))) {
                            sb.append(" • ").append(item.optString("time_window")).append(" ").append(item.optString("poi_name")).append("\n");
                        }
                    }
                    sb.append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) { return jsonStr; }
    }

    private void saveItinerary() {
        if (rawJson == null || rawJson.isEmpty()) return;

        SharedPreferences sp = getSharedPreferences("MyTravelPlans", Context.MODE_PRIVATE);
        String existingPlans = sp.getString("plans_json", "[]");

        try {
            JSONArray plansArray = new JSONArray(existingPlans);
            JSONObject newPlan = new JSONObject();
            newPlan.put("id", System.currentTimeMillis());
            newPlan.put("content", rawJson);
            
            // 提取 AI 生成的华丽标题作为显示
            String displayTitle = (targetCity != null ? targetCity : "") + "之旅";
            try {
                JSONObject root = new JSONObject(rawJson.replace("\\\"", "\"").replace("\\n", "\n"));
                JSONObject overview = root.optJSONObject("trip_overview");
                if (overview != null && overview.has("title")) {
                    displayTitle = overview.getString("title");
                }
            } catch (Exception ignored) {}
            
            newPlan.put("title", displayTitle);
            
            // 【关键修复】显式保存创建时的纯净地名，供详情页精准定位使用
            newPlan.put("city_name", targetCity);

            plansArray.put(newPlan);
            sp.edit().putString("plans_json", plansArray.toString()).apply();

            Toast.makeText(this, "行程已保存", Toast.LENGTH_SHORT).show();
            // 返回主页
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }
}
