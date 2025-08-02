package com.railse.hiring.workforcemgmt.model;

import lombok.Data;

@Data
public class Activity {
    private String description;
    private Long timestamp = System.currentTimeMillis();

    public Activity(String description) {
        this.description = description;
    }
}
