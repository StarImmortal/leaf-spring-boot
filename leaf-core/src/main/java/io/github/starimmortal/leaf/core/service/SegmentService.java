package io.github.starimmortal.leaf.core.service;

import com.alibaba.druid.pool.DruidDataSource;
import io.github.starimmortal.leaf.core.IdGenerator;
import io.github.starimmortal.leaf.core.exception.InitException;
import io.github.starimmortal.leaf.core.segment.SegmentIdGeneratorImpl;
import io.github.starimmortal.leaf.core.segment.dao.IdAllocDao;
import io.github.starimmortal.leaf.core.segment.dao.impl.IdAllocDaoImpl;
import io.github.starimmortal.leaf.core.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import java.sql.SQLException;

@Slf4j
public class SegmentService {

    private final IdGenerator idGenerator;

    public SegmentService(String driverClassName, String url, String username, String password) throws SQLException, InitException {
        Assert.notNull(url, "database url can not be null");
        Assert.notNull(username, "username can not be null");
        Assert.notNull(password, "password can not be null");
        // Config dataSource
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        dataSource.init();
        // Config Dao
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        IdAllocDao idAllocDao = new IdAllocDaoImpl(jdbcTemplate);
        // Config ID Generator
        idGenerator = new SegmentIdGeneratorImpl();
        ((SegmentIdGeneratorImpl) idGenerator).setIdAllocDao(idAllocDao);
        if (!idGenerator.init()) {
            throw new InitException("Segment Service Init Fail");
        }
        log.info("Segment Service Init Successfully");
    }

    public ResultVO getId(String key) {
        return idGenerator.get(key);
    }

    public SegmentIdGeneratorImpl getIdGenerator() {
        if (idGenerator instanceof SegmentIdGeneratorImpl) {
            return (SegmentIdGeneratorImpl) idGenerator;
        }
        return null;
    }
}
