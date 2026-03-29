package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class NodeAdapter extends RecyclerView.Adapter<NodeAdapter.NodeViewHolder> {

    private List<TripNode> nodes;
    private boolean isEditMode = false;

    public NodeAdapter(List<TripNode> nodes) {
        this.nodes = nodes;
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip_node, parent, false);
        return new NodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NodeViewHolder holder, int position) {
        TripNode node = nodes.get(position);

        holder.tvName.setText(node.getName());
        holder.tvType.setText(node.getType());
        holder.tvDetail.setText(node.getDetail());

        // 根据类型设置不同的 Emoji
        String emoji = "📍";
        String type = node.getType();
        if (type != null) {
            if (type.contains("交通") || type.contains("机场") || type.contains("抵达")) {
                emoji = "✈️";
            } else if (type.contains("景点") || type.contains("游览")) {
                emoji = "🏛️";
            } else if (type.contains("拍照")) {
                emoji = "📸";
            } else if (type.contains("美食") || type.contains("餐厅")) {
                emoji = "🍴";
            } else if (type.contains("酒店") || type.contains("住宿")) {
                emoji = "🏨";
            }
        }
        holder.tvEmoji.setText(emoji);

        if (isEditMode) {
            holder.ivDelete.setVisibility(View.VISIBLE);
            holder.ivDrag.setVisibility(View.VISIBLE);
        } else {
            holder.ivDelete.setVisibility(View.GONE);
            holder.ivDrag.setVisibility(View.GONE);
        }

        holder.ivDelete.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                nodes.remove(currentPos);
                notifyItemRemoved(currentPos);
                notifyItemRangeChanged(currentPos, nodes.size());
            }
        });
    }

    @Override
    public int getItemCount() {
        return nodes.size();
    }

    static class NodeViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvType, tvDetail, tvEmoji;
        ImageView ivDelete, ivDrag;

        public NodeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_node_name);
            tvType = itemView.findViewById(R.id.tv_node_type);
            tvDetail = itemView.findViewById(R.id.tv_node_detail);
            tvEmoji = itemView.findViewById(R.id.tv_node_emoji);
            ivDelete = itemView.findViewById(R.id.iv_delete);
            ivDrag = itemView.findViewById(R.id.iv_drag);
        }
    }
}
