package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.json.JSONArray;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AddItineraryBottomSheet extends BottomSheetDialogFragment {

    private EditText etSearch;
    private RecyclerView rvSuggestions;
    private SuggestionAdapter adapter;
    private List<String> allCities = new ArrayList<>();
    private List<String> filteredCities = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_itinerary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearch = view.findViewById(R.id.et_dialog_search);
        rvSuggestions = view.findViewById(R.id.rv_dialog_suggestions);
        Button btnCreateNew = view.findViewById(R.id.btn_create_new);

        adapter = new SuggestionAdapter(filteredCities);
        rvSuggestions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSuggestions.setAdapter(adapter);

        loadCityData();

        // 搜索建议监听：输入一个字母也能弹出列表
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                filteredCities.clear();
                if (!query.isEmpty()) {
                    for (String city : allCities) {
                        if (city.toLowerCase().contains(query.toLowerCase())) {
                            filteredCities.add(city);
                        }
                    }
                }
                rvSuggestions.setVisibility(filteredCities.isEmpty() ? View.GONE : View.VISIBLE);
                adapter.notifyDataSetChanged();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 【最终同步设计】强制同步按钮逻辑
        if (btnCreateNew != null) {
            btnCreateNew.setOnClickListener(v -> {
                // 直接从 EditText 抓取当前显示的文字，无论它是什么
                String rawInput = etSearch.getText().toString().trim();
                if (rawInput.isEmpty()) {
                    Toast.makeText(getContext(), "请输入目的地", Toast.LENGTH_SHORT).show();
                    return;
                }
                goToCreate(rawInput);
            });
        }
    }

    private void goToCreate(String cityName) {
        Intent intent = new Intent(getActivity(), CreateItineraryActivity.class);
        // 关键：参数名必须与接收端完全匹配
        intent.putExtra("preset_destination", cityName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        dismiss();
    }

    private void loadCityData() {
        try {
            InputStream is = getContext().getAssets().open("city_list.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                allCities.add(jsonArray.getJSONObject(i).getString("中文名"));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> {
        private List<String> data;
        public SuggestionAdapter(List<String> data) { this.data = data; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String name = data.get(position);
            holder.tv.setText(name);
            holder.itemView.setOnClickListener(v -> goToCreate(name));
        }

        @Override
        public int getItemCount() { return Math.min(data.size(), 8); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tv;
            ViewHolder(View itemView) {
                super(itemView);
                tv = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}
