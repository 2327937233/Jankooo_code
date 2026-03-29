package com.example.myapplication;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.network.PlanTravelRequest;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateItineraryActivity extends AppCompatActivity {

    private TextInputEditText etDestination, etDepartureDate, etEndDate;
    private Calendar departureCalendar = Calendar.getInstance();
    private Calendar endCalendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_itinerary);

        etDestination = findViewById(R.id.et_destination);
        etDepartureDate = findViewById(R.id.et_departure_date);
        etEndDate = findViewById(R.id.et_end_date);
        ImageButton btnBack = findViewById(R.id.btn_back);
        MaterialButton btnCreate = findViewById(R.id.btn_create_plan);

        String presetDest = getIntent().getStringExtra("preset_destination");
        if (presetDest != null && !presetDest.isEmpty()) {
            etDestination.setText(presetDest);
        }

        String today = dateFormat.format(departureCalendar.getTime());
        etDepartureDate.setText(today);
        etEndDate.setText(today);

        etDepartureDate.setOnClickListener(v -> showDatePicker(true));
        etEndDate.setOnClickListener(v -> showDatePicker(false));
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        btnCreate.setOnClickListener(v -> performPlanTravel());
    }

    private void showDatePicker(boolean isDeparture) {
        Calendar currentCal = isDeparture ? departureCalendar : endCalendar;
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            currentCal.set(Calendar.YEAR, year);
            currentCal.set(Calendar.MONTH, month);
            currentCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            String selectedDate = dateFormat.format(currentCal.getTime());
            if (isDeparture) etDepartureDate.setText(selectedDate);
            else etEndDate.setText(selectedDate);
        }, currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH), currentCal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void performPlanTravel() {
        String location = etDestination.getText().toString().trim();
        if (location.isEmpty()) {
            Toast.makeText(this, "请输入目的地", Toast.LENGTH_SHORT).show();
            return;
        }

        long diff = endCalendar.getTimeInMillis() - departureCalendar.getTimeInMillis();
        int days = (int) (diff / (1000 * 60 * 60 * 24)) + 1;
        if (days <= 0) days = 1;
        
        List<String> prefList = new ArrayList<>();
        android.widget.GridLayout gridLayout = findViewById(R.id.gl_preferences);
        if (gridLayout != null) {
            for (int i = 0; i < gridLayout.getChildCount(); i++) {
                if (gridLayout.getChildAt(i) instanceof Chip) {
                    Chip chip = (Chip) gridLayout.getChildAt(i);
                    if (chip.isChecked()) prefList.add(chip.getText().toString());
                }
            }
        }
        String preferencesStr = prefList.isEmpty() ? "无" : String.join(",", prefList);

        // 【核心恢复】构造请求对象，确保参数名与后端对齐
        PlanTravelRequest request = new PlanTravelRequest(location, etDepartureDate.getText().toString(), etEndDate.getText().toString(), days, preferencesStr);

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("正在定制行程规划...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // 执行 Retrofit 异步调用
        RetrofitClient.getApiService().planTravel(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String rawStream = response.body().string();
                        // 依然使用 SSE 解析
                        String finalJson = parseSseContent(rawStream);
                        
                        // 【恢复跳转】
                        Intent intent = new Intent(CreateItineraryActivity.this, AIResultActivity.class);
                        intent.putExtra("raw_json_content", finalJson); 
                        intent.putExtra("target_city", location);
                        startActivity(intent);
                    } catch (IOException e) {
                        Toast.makeText(CreateItineraryActivity.this, "数据解析异常", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CreateItineraryActivity.this, "后端响应失败: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressDialog.dismiss();
                Log.e("NETWORK", "Connection failed: " + t.getMessage());
                Toast.makeText(CreateItineraryActivity.this, "无法连接到服务器", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String parseSseContent(String raw) {
        StringBuilder sb = new StringBuilder();
        String[] lines = raw.split("\\r?\\n");
        for (String line : lines) {
            if (line.startsWith("data:")) {
                String dataJson = line.substring(5).trim();
                try {
                    JSONObject jsonObject = new JSONObject(dataJson);
                    if (jsonObject.has("content")) sb.append(jsonObject.getString("content"));
                } catch (JSONException ignored) {}
            }
        }
        return sb.toString();
    }
}
