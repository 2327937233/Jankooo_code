package com.example.myapplication;

import java.util.ArrayList;
import java.util.List;

public class DayItinerary {
    private int dayNumber;      // 第几天，如 1
    private String area;        // 区域，如 "阿勒泰地区"
    private String routeTitle;  // 路线标题，如 "乌鲁木齐 -> 喀纳斯"
    private List<TripNode> nodes; //这一天的所有景点

    public DayItinerary(int dayNumber, String area, String routeTitle) {
        this.dayNumber = dayNumber;
        this.area = area;
        this.routeTitle = routeTitle;
        this.nodes = new ArrayList<>();
    }

    public int getDayNumber() { return dayNumber; }
    public String getArea() { return area; }
    public String getRouteTitle() { return routeTitle; }
    public List<TripNode> getNodes() { return nodes; }
}