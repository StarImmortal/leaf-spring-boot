package io.github.starimmortal.leaf.server.controller;

import io.github.starimmortal.leaf.core.segment.SegmentIdGeneratorImpl;
import io.github.starimmortal.leaf.core.segment.model.LeafAlloc;
import io.github.starimmortal.leaf.core.segment.model.SegmentBuffer;
import io.github.starimmortal.leaf.core.service.SegmentService;
import io.github.starimmortal.leaf.server.model.SegmentBufferView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class LeafMonitorController {
    private final Logger logger = LoggerFactory.getLogger(LeafMonitorController.class);

    @Autowired
    private SegmentService segmentService;

    @GetMapping(value = "cache")
    public String getCache(Model model) {
        Map<String, SegmentBufferView> data = new HashMap<>();
        SegmentIdGeneratorImpl segmentIdGenerator = segmentService.getIdGenerator();
        if (segmentIdGenerator == null) {
            throw new IllegalArgumentException("You should config leaf.segment.enable=true first");
        }
        Map<String, SegmentBuffer> cache = segmentIdGenerator.getCache();
        for (Map.Entry<String, SegmentBuffer> entry : cache.entrySet()) {
            SegmentBufferView sv = new SegmentBufferView();
            SegmentBuffer buffer = entry.getValue();
            sv.setInitOk(buffer.isInitOk());
            sv.setKey(buffer.getKey());
            sv.setPos(buffer.getCurrentPos());
            sv.setNextReady(buffer.isNextReady());
            sv.setMax0(buffer.getSegments()[0].getMax());
            sv.setValue0(buffer.getSegments()[0].getValue().get());
            sv.setStep0(buffer.getSegments()[0].getStep());

            sv.setMax1(buffer.getSegments()[1].getMax());
            sv.setValue1(buffer.getSegments()[1].getValue().get());
            sv.setStep1(buffer.getSegments()[1].getStep());

            data.put(entry.getKey(), sv);
        }
        logger.info("Cache info {}", data);
        model.addAttribute("data", data);
        return "segment";
    }

    @GetMapping(value = "db")
    public String getDb(Model model) {
        SegmentIdGeneratorImpl segmentIdGenerator = segmentService.getIdGenerator();
        if (segmentIdGenerator == null) {
            throw new IllegalArgumentException("You should config leaf.segment.enable=true first");
        }
        List<LeafAlloc> items = segmentIdGenerator.listLeafAllocs();
        logger.info("DB info {}", items);
        model.addAttribute("items", items);
        return "db";
    }

    /**
     * the output is like this:
     * {
     * "timestamp": "1567733700834(2019-09-06 09:35:00.834)",
     * "sequenceId": "3448",
     * "workerId": "39"
     * }
     */
    @GetMapping(value = "decodeSnowflakeId")
    @ResponseBody
    public Map<String, String> decodeSnowflakeId(@RequestParam("snowflakeId") String snowflakeIdStr) {
        Map<String, String> map = new HashMap<>(16);
        try {
            long snowflakeId = Long.parseLong(snowflakeIdStr);

            long originTimestamp = (snowflakeId >> 22) + 1288834974657L;
            Date date = new Date(originTimestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            map.put("timestamp", String.valueOf(originTimestamp) + "(" + sdf.format(date) + ")");

            long workerId = (snowflakeId >> 12) ^ (snowflakeId >> 22 << 10);
            map.put("workerId", String.valueOf(workerId));

            long sequence = snowflakeId ^ (snowflakeId >> 12 << 12);
            map.put("sequenceId", String.valueOf(sequence));
        } catch (NumberFormatException e) {
            map.put("errorMsg", "snowflake Id反解析发生异常!");
        }
        return map;
    }
}
