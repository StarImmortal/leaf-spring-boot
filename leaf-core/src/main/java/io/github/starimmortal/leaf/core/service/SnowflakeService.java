package io.github.starimmortal.leaf.core.service;

import io.github.starimmortal.leaf.core.IdGenerator;
import io.github.starimmortal.leaf.core.exception.InitException;
import io.github.starimmortal.leaf.core.snowflake.SnowflakeIdGeneratorImpl;
import io.github.starimmortal.leaf.core.vo.ResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

public class SnowflakeService {
    private Logger logger = LoggerFactory.getLogger(SnowflakeService.class);

    private IdGenerator idGenerator;

    public SnowflakeService(String zkpath, int port) throws InitException {
        Assert.notNull(zkpath, "zookeeper path can not be null");
        Assert.notNull(port, "zookeeper port  can not be null");
        idGenerator = new SnowflakeIdGeneratorImpl(zkpath, port);
        if (idGenerator.init()) {
            logger.info("Snowflake Service Init Successfully");
        } else {
            throw new InitException("Snowflake Service Init Fail");
        }
    }

    public ResultVO getId(String key) {
        return idGenerator.get(key);
    }
}
