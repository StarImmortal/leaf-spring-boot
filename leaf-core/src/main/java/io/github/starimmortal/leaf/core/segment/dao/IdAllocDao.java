package io.github.starimmortal.leaf.core.segment.dao;

import io.github.starimmortal.leaf.core.segment.model.LeafAlloc;

import java.util.List;

public interface IdAllocDao {
    LeafAlloc getLeafAllocByTag(String tag);

    List<LeafAlloc> listLeafAllocs();

    LeafAlloc updateMaxId(String tag);

    LeafAlloc updateMaxIdByCustomStep(LeafAlloc leafAlloc);

    List<String> listTags();
}
