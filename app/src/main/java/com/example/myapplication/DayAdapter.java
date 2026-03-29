package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DayAdapter extends RecyclerView.Adapter<DayAdapter.DayViewHolder> {

    private Context context;
    private List<DayItinerary> days;
    private boolean isEditMode = false;

    public DayAdapter(Context context, List<DayItinerary> days) {
        this.context = context;
        this.days = days;
    }

    // 接收 Activity 传来的编辑状态，并通知刷新
    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day_card, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        DayItinerary day = days.get(position);

        holder.tvDayNum.setText("DAY " + day.getDayNumber());
        holder.tvArea.setText(day.getArea());
        holder.tvRoute.setText(day.getRouteTitle());

        // --- 嵌套列表的核心 ---
        // 为每一天创建一个独立的 NodeAdapter
        NodeAdapter innerAdapter = new NodeAdapter(day.getNodes());
        innerAdapter.setEditMode(isEditMode); // 将编辑状态透传给内部 Adapter

        holder.rvInnerNodes.setLayoutManager(new LinearLayoutManager(context));
        holder.rvInnerNodes.setAdapter(innerAdapter);

        // --- “添加”按钮逻辑 ---
        if (isEditMode) {
            holder.btnAdd.setVisibility(View.VISIBLE);
            holder.btnAdd.setOnClickListener(v -> {
                // 演示：点击添加，直接加一个假数据
                day.getNodes().add(new TripNode("新选定地点", "待定", ""));
                innerAdapter.notifyItemInserted(day.getNodes().size() - 1);
            });
        } else {
            holder.btnAdd.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayNum, tvArea, tvRoute;
        RecyclerView rvInnerNodes;
        Button btnAdd;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            // 绑定 item_day_card.xml 中的控件
            tvDayNum = itemView.findViewById(R.id.tv_day_number);
            tvArea = itemView.findViewById(R.id.tv_area);
            tvRoute = itemView.findViewById(R.id.tv_route_title);
            rvInnerNodes = itemView.findViewById(R.id.rv_inner_nodes);
            btnAdd = itemView.findViewById(R.id.btn_add_node);
        }
    }
}