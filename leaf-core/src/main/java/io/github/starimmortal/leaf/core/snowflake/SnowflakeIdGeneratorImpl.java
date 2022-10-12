package io.github.starimmortal.leaf.core.snowflake;

import io.github.starimmortal.leaf.core.IdGenerator;
import io.github.starimmortal.leaf.core.enumeration.StatusEnum;
import io.github.starimmortal.leaf.core.exception.OverMaxTimeStampException;
import io.github.starimmortal.leaf.core.snowflake.exception.CheckLastTimeException;
import io.github.starimmortal.leaf.core.util.CommonUtil;
import io.github.starimmortal.leaf.core.vo.ResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SnowflakeIdGeneratorImpl implements IdGenerator {

    @Override
    public boolean init() {
        return true;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeIdGeneratorImpl.class);

    private final long twepoch;

    private final long maxTimeStamp;

    private static final long TIME_STAMP_BITS = 41L;

    private static final long WORKER_ID_BITS = 10L;

    /**
     * 最大能够分配的workerId =1023
     */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    private static final long SEQUENCE_BITS = 12L;

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private long workerId;

    private long sequence = 0L;

    private long lastTimestamp = -1L;

    private static final Random RANDOM = new Random();

    public SnowflakeIdGeneratorImpl(String zkAddress, int port) {
        //Thu Nov 04 2010 09:42:54 GMT+0800 (中国标准时间) 
        this(zkAddress, port, 1288834974657L);
    }

    /**
     * @param zkAddress zk地址
     * @param port      snowflake监听端口
     * @param twepoch   起始的时间戳
     */
    public SnowflakeIdGeneratorImpl(String zkAddress, int port, long twepoch) {
        this.twepoch = twepoch;
        this.maxTimeStamp = ~(-1L << TIME_STAMP_BITS) + twepoch;
        Assert.state(timeGenerator() > twepoch, "Snowflake not support twepoch gt currentTime");
        final String ip = CommonUtil.getIp();
        SnowflakeZookeeperHolder holder = new SnowflakeZookeeperHolder(ip, String.valueOf(port), zkAddress);
        LOGGER.info("twepoch:{} ,ip:{} ,zkAddress:{} port:{}", twepoch, ip, zkAddress, port);
        boolean initFlag = holder.init();
        if (initFlag) {
            workerId = holder.getWorkerID();
            LOGGER.info("START SUCCESS USE ZK WORKERID-{}", workerId);
        } else {
            Assert.state(initFlag, "Snowflake Id Gen is not init ok");
        }
        Assert.state(workerId >= 0 && workerId <= MAX_WORKER_ID, "workerID must gte 0 and lte 1023");
    }

    @Override
    public synchronized ResultVO get(String key) {
        long timestamp = timeGenerator();
        if (timestamp>maxTimeStamp) {
            throw new OverMaxTimeStampException("current timestamp is over maxTimeStamp, the generate id will be negative");
        }
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                try {
                    wait(offset << 1);
                    timestamp = timeGenerator();
                    if (timestamp < lastTimestamp) {
                        return new ResultVO(-1, StatusEnum.EXCEPTION);
                    }
                } catch (InterruptedException e) {
                    LOGGER.error("wait interrupted");
                    return new ResultVO(-2, StatusEnum.EXCEPTION);
                }
            } else {
                return new ResultVO(-3, StatusEnum.EXCEPTION);
            }
        }
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                //seq 为0的时候表示是下一毫秒时间开始对seq做随机
                sequence = RANDOM.nextInt(100);
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            //如果是新的ms开始
            sequence = RANDOM.nextInt(100);
        }
        lastTimestamp = timestamp;
        long id = ((timestamp - twepoch) << TIMESTAMP_LEFT_SHIFT) | (workerId << WORKER_ID_SHIFT) | sequence;
        return new ResultVO(id, StatusEnum.SUCCESS);

    }

    /**
     * 等待下个毫秒，防止等待期间系统时钟被回调，导致方法一直轮询
     */
    protected long tilNextMillis(long lastTimestamp) {
        long timestamp;
        long offset;
        while (true) {
            timestamp = timeGenerator();
            offset = lastTimestamp - timestamp;
            if (offset < 0) {
                return timestamp;
            }
            // 系统时钟回调时间大于5ms
            if (offset >= 5) {
                throw new CheckLastTimeException("timestamp check error,last timestamp " + lastTimestamp + ",now " + timestamp);
            }
            // 系统时钟回调时间大于等于2ms
            if (offset >= 2) {
                try {
                    TimeUnit.MILLISECONDS.sleep(offset);
                } catch (InterruptedException ignore) {
                }
            }
        }
    }

    protected long timeGenerator() {
        return System.currentTimeMillis();
    }

    public long getWorkerId() {
        return workerId;
    }
}
