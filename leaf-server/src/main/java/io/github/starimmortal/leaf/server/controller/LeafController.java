package io.github.starimmortal.leaf.server.controller;

import io.github.starimmortal.leaf.core.enumeration.StatusEnum;
import io.github.starimmortal.leaf.core.service.SegmentService;
import io.github.starimmortal.leaf.core.service.SnowflakeService;
import io.github.starimmortal.leaf.core.vo.ResultVO;
import io.github.starimmortal.leaf.server.exception.LeafServerException;
import io.github.starimmortal.leaf.server.exception.NoKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LeafController {

    @Autowired
    private SegmentService segmentService;

    @Autowired
    private SnowflakeService snowflakeService;

    @GetMapping(value = "/api/segment/get/{key}")
    public String getSegmentId(@PathVariable("key") String key) {
        return get(key, segmentService.getId(key));
    }

    @GetMapping(value = "/api/snowflake/get/{key}")
    public String getSnowflakeId(@PathVariable("key") String key) {
        return get(key, snowflakeService.getId(key));
    }

    private String get(@PathVariable("key") String key, ResultVO id) {
        ResultVO resultVO;
        if (key == null || key.isEmpty()) {
            throw new NoKeyException();
        }
        resultVO = id;
        if (resultVO.getStatus().equals(StatusEnum.EXCEPTION)) {
            throw new LeafServerException(resultVO.toString());
        }
        return String.valueOf(resultVO.getId());
    }
}
