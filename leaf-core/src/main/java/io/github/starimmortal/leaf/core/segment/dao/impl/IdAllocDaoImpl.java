package io.github.starimmortal.leaf.core.segment.dao.impl;

import io.github.starimmortal.leaf.core.segment.dao.IdAllocDao;
import io.github.starimmortal.leaf.core.segment.model.LeafAlloc;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class IdAllocDaoImpl implements IdAllocDao {

    private final JdbcTemplate jdbcTemplate;

    public IdAllocDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LeafAlloc getLeafAllocByTag(String tag) {
        String sql = "SELECT biz_tag AS `key`, max_id, step FROM leaf_alloc WHERE biz_tag = ?";
        return this.jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(LeafAlloc.class), tag);
    }

    @Override
    public List<LeafAlloc> listLeafAllocs() {
        return this.jdbcTemplate.queryForList("SELECT biz_tag AS `key`, max_id, step, update_time FROM leaf_alloc", LeafAlloc.class);
    }

    @Override
    public LeafAlloc updateMaxId(String tag) {
        String sql = "UPDATE leaf_alloc SET max_id = max_id + step WHERE biz_tag = ?";
        this.jdbcTemplate.update(sql, tag);
        return this.getLeafAllocByTag(tag);
    }

    @Override
    public LeafAlloc updateMaxIdByCustomStep(LeafAlloc leafAlloc) {
        String sql = "UPDATE leaf_alloc SET max_id = max_id + ? WHERE biz_tag = ?";
        this.jdbcTemplate.update(sql, leafAlloc.getStep(), leafAlloc.getKey());
        return this.getLeafAllocByTag(leafAlloc.getKey());
    }

    @Override
    public List<String> listTags() {
        return this.jdbcTemplate.queryForList("SELECT biz_tag FROM leaf_alloc", String.class);
    }
}
