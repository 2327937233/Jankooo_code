package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.services.core.ServiceSettings;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.InputtipsQuery;
import com.amap.api.services.help.Tip;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity implements Inputtips.InputtipsListener {

    private EditText etSearchInput;
    private RecyclerView rvSearchResults;
    private LinearLayout llHotDestinations;
    private SuggestionAdapter adapter;
    private List<SearchResult> displayList = new ArrayList<>();
    private JSONArray localCityData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ServiceSettings.updatePrivacyShow(this, true, true);
        ServiceSettings.updatePrivacyAgree(this, true);

        setContentView(R.layout.activity_search);

        initViews();
        loadLocalCityJson();
        setupListeners();
    }

    private void initViews() {
        etSearchInput = findViewById(R.id.et_search_input);
        rvSearchResults = findViewById(R.id.rv_search_results);
        llHotDestinations = findViewById(R.id.ll_hot_destinations);
        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());

        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SuggestionAdapter(displayList);
        rvSearchResults.setAdapter(adapter);
    }

    private void setupListeners() {
        // 【增强】监听键盘搜索/回车键
        etSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || 
               (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                
                String keyword = etSearchInput.getText().toString().trim();
                if (!keyword.isEmpty()) {
                    jumpToCreate(keyword);
                    return true;
                }
            }
            return false;
        });

        etSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String keyword = s.toString().trim();
                if (keyword.length() > 0) {
                    llHotDestinations.setVisibility(View.GONE);
                    rvSearchResults.setVisibility(View.VISIBLE);
                    doHybridSearch(keyword);
                } else {
                    llHotDestinations.setVisibility(View.VISIBLE);
                    rvSearchResults.setVisibility(View.GONE);
                    displayList.clear();
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void jumpToCreate(String city) {
        Intent intent = new Intent(SearchActivity.this, CreateItineraryActivity.class);
        intent.putExtra("preset_destination", city);
        startActivity(intent);
        finish();
    }

    private void doHybridSearch(String keyword) {
        displayList.clear();
        searchLocalJson(keyword);
        try {
            InputtipsQuery query = new InputtipsQuery(keyword, "");
            query.setCityLimit(false); 
            Inputtips inputTips = new Inputtips(this, query);
            inputTips.setInputtipsListener(this);
            inputTips.requestInputtipsAsyn();
        } catch (Exception e) {
            Log.e("AMAP", "搜索组件初始化失败: " + e.getMessage());
        }
    }

    private void loadLocalCityJson() {
        new Thread(() -> {
            try {
                StringBuilder builder = new StringBuilder();
                BufferedReader bf = new BufferedReader(new InputStreamReader(getAssets().open("city_list.json")));
                String line;
                while ((line = bf.readLine()) != null) builder.append(line);
                localCityData = new JSONArray(builder.toString());
            } catch (Exception e) {
                Log.e("SearchActivity", "JSON读取失败: " + e.getMessage());
            }
        }).start();
    }

    private void searchLocalJson(String keyword) {
        if (localCityData == null) return;
        try {
            int count = 0;
            for (int i = 0; i < localCityData.length(); i++) {
                JSONObject city = localCityData.getJSONObject(i);
                String name = city.optString("中文名");
                if (name.contains(keyword)) {
                    displayList.add(new SearchResult(name, "城市 - " + city.optString("adcode"), true));
                    count++;
                }
                if (count > 5) break; 
            }
            adapter.notifyDataSetChanged();
        } catch (Exception ignored) {}
    }

    @Override
    public void onGetInputtips(List<Tip> tipList, int rCode) {
        if (rCode == 1000 && tipList != null) {
            for (Tip tip : tipList) {
                if (tip.getName() != null) {
                    displayList.add(new SearchResult(tip.getName(), tip.getDistrict(), false));
                }
            }
            adapter.notifyDataSetChanged();
        }
    }

    private static class SearchResult {
        String name, desc;
        boolean isLocal;
        SearchResult(String n, String d, boolean l) { name = n; desc = d; isLocal = l; }
    }

    private class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> {
        private List<SearchResult> list;
        SuggestionAdapter(List<SearchResult> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_suggestion, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SearchResult item = list.get(position);
            holder.tvName.setText(item.name);
            holder.tvDistrict.setText(item.desc);
            holder.tvName.setTextColor(item.isLocal ? 0xFF2196F3 : 0xFF333333);
            
            holder.itemView.setOnClickListener(v -> jumpToCreate(item.name));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDistrict;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tv_suggestion_name);
                tvDistrict = v.findViewById(R.id.tv_suggestion_district);
            }
        }
    }
}
