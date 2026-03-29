package com.example.myapplication;

public class TripNode {
    private String name;    // 地点名称，如 "喀纳斯湖"
    private String type;    // 类型，如 "景点", "交通"
    private String detail;  // 详情，如 "910.8公里 · 12小时"

    public TripNode(String name, String type, String detail) {
        this.name = name;
        this.type = type;
        this.detail = detail;
    }

    // Getter 方法
    public String getName() { return name; }
    public String getType() { return type; }
    public String getDetail() { return detail; }
}