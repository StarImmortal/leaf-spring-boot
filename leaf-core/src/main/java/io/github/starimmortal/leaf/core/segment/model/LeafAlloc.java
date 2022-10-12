package io.github.starimmortal.leaf.core.segment.model;

import lombok.Data;

@Data
public class LeafAlloc {
    private String key;

    private long maxId;

    private int step;

    private String updateTime;
}
