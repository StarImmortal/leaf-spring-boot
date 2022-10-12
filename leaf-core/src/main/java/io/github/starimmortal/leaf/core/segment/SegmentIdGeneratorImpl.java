package io.github.starimmortal.leaf.core.segment;

import io.github.starimmortal.leaf.core.IdGenerator;
import io.github.starimmortal.leaf.core.enumeration.StatusEnum;
import io.github.starimmortal.leaf.core.segment.dao.IdAllocDao;
import io.github.starimmortal.leaf.core.segment.model.LeafAlloc;
import io.github.starimmortal.leaf.core.segment.model.Segment;
import io.github.starimmortal.leaf.core.segment.model.SegmentBuffer;
import io.github.starimmortal.leaf.core.vo.ResultVO;
import lombok.NonNull;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author william@StarImmortal
 * @date 2022/09/16
 */
public class SegmentIdGeneratorImpl implements IdGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SegmentIdGeneratorImpl.class);

    /**
     * IdCache未初始化成功时的异常码
     */
    private static final long EXCEPTION_ID_CACHE_INIT_FALSE = -1;

    /**
     * key不存在时的异常码
     */
    private static final long EXCEPTION_ID_KEY_NOT_EXISTS = -2;

    /**
     * SegmentBuffer中的两个Segment均未从DB中装载时的异常码
     */
    private static final long EXCEPTION_ID_TWO_SEGMENTS_ARE_NULL = -3;

    /**
     * 最大步长不超过100,0000
     */
    private static final int MAX_STEP = 1000000;

    /**
     * 一个Segment维持时间为15分钟
     */
    private static final long SEGMENT_DURATION = 15 * 60 * 1000L;

    private final ExecutorService executorService = new ThreadPoolExecutor(5, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new UpdateThreadFactory());

    private volatile boolean initOk = false;

    private final Map<String, SegmentBuffer> cache = new ConcurrentHashMap<>();

    private IdAllocDao idAllocDao;

    public static class UpdateThreadFactory implements ThreadFactory {

        private static int threadInitNumber = 0;

        private static synchronized int nextThreadNum() {
            return threadInitNumber++;
        }

        @Override
        public Thread newThread(@NonNull Runnable r) {
            return new Thread(r, "Thread-Segment-Update-" + nextThreadNum());
        }
    }

    @Override
    public boolean init() {
        logger.info("Init ...");
        // 确保加载到kv后才初始化成功
        updateCacheFromDb();
        initOk = true;
        updateCacheFromDbAtEveryMinute();
        return initOk;
    }

    private void updateCacheFromDbAtEveryMinute() {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable r) {
                Thread t = new Thread(r);
                t.setName("check-idCache-thread");
                t.setDaemon(true);
                return t;
            }
        });
        service.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                updateCacheFromDb();
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    private void updateCacheFromDb() {
        logger.info("update cache from db");
        StopWatch stopWatch = new Slf4JStopWatch();
        try {
            List<String> tags = idAllocDao.listTags();
            if (tags == null || tags.isEmpty()) {
                return;
            }
            // 将dbTags中新加的tag添加cache，通过遍历dbTags，判断是否在cache中存在，不存在就添加到cache
            for (String tag : tags) {
                if (!cache.containsKey(tag)) {
                    SegmentBuffer buffer = new SegmentBuffer();
                    buffer.setKey(tag);
                    Segment segment = buffer.getCurrent();
                    segment.setValue(new AtomicLong(0));
                    segment.setMax(0);
                    segment.setStep(0);
                    cache.put(tag, buffer);
                    logger.info("Add tag {} from db to IdCache, SegmentBuffer {}", tag, buffer);
                }
            }
            List<String> cacheTags = new ArrayList<>(cache.keySet());
            Set<String> tagSet = new HashSet<>(tags);
            // 将cache中已失效的tag从cache删除，通过遍历cacheTags，判断是否在dbTagSet中存在，不存在说明过期，直接删除
            for (String cacheTag : cacheTags) {
                if (!tagSet.contains(cacheTag)) {
                    cache.remove(cacheTag);
                    logger.info("Remove tag {} from IdCache", cacheTag);
                }
            }
        } catch (Exception e) {
            logger.warn("update cache from db exception", e);
        } finally {
            stopWatch.stop("updateCacheFromDb");
        }
    }

    @Override
    public ResultVO get(final String key) {
        if (!initOk) {
            return new ResultVO(EXCEPTION_ID_CACHE_INIT_FALSE, StatusEnum.EXCEPTION);
        }
        if (cache.containsKey(key)) {
            SegmentBuffer buffer = cache.get(key);
            if (!buffer.isInitOk()) {
                synchronized (buffer) {
                    if (!buffer.isInitOk()) {
                        try {
                            updateSegmentFromDb(key, buffer.getCurrent());
                            logger.info("Init buffer. Update leaf key {} {} from db", key, buffer.getCurrent());
                            buffer.setInitOk(true);
                        } catch (Exception e) {
                            logger.warn("Init buffer {} exception", buffer.getCurrent(), e);
                        }
                    }
                }
            }
            return getIdFromSegmentBuffer(cache.get(key));
        }
        return new ResultVO(EXCEPTION_ID_KEY_NOT_EXISTS, StatusEnum.EXCEPTION);
    }

    public void updateSegmentFromDb(String key, Segment segment) {
        StopWatch stopWatch = new Slf4JStopWatch();
        SegmentBuffer buffer = segment.getBuffer();
        LeafAlloc leafAlloc;
        if (!buffer.isInitOk()) {
            leafAlloc = idAllocDao.updateMaxId(key);
            buffer.setStep(leafAlloc.getStep());
            // leafAlloc中的step为DB中的step
            buffer.setMinStep(leafAlloc.getStep());
        } else if (buffer.getUpdateTimestamp() == 0) {
            leafAlloc = idAllocDao.updateMaxId(key);
            buffer.setUpdateTimestamp(System.currentTimeMillis());
            buffer.setStep(leafAlloc.getStep());
            // leafAlloc中的step为DB中的step
            buffer.setMinStep(leafAlloc.getStep());
        } else {
            long duration = System.currentTimeMillis() - buffer.getUpdateTimestamp();
            int nextStep = buffer.getStep();
            if (duration < SEGMENT_DURATION) {
                if (nextStep * 2 > MAX_STEP) {
                    // do nothing
                } else {
                    nextStep = nextStep * 2;
                }
            } else if (duration < SEGMENT_DURATION * 2) {
                // do nothing with nextStep
            } else {
                nextStep = nextStep / 2 >= buffer.getMinStep() ? nextStep / 2 : nextStep;
            }
            logger.info("leafKey[{}], step[{}], duration[{}mins], nextStep[{}]", key, buffer.getStep(), String.format("%.2f", ((double) duration / (1000 * 60))), nextStep);
            LeafAlloc temp = new LeafAlloc();
            temp.setKey(key);
            temp.setStep(nextStep);
            leafAlloc = idAllocDao.updateMaxIdByCustomStep(temp);
            buffer.setUpdateTimestamp(System.currentTimeMillis());
            buffer.setStep(nextStep);
            // leafAlloc的step为DB中的step
            buffer.setMinStep(leafAlloc.getStep());
        }
        // must set value before set max
        long value = leafAlloc.getMaxId() - buffer.getStep();
        segment.getValue().set(value);
        segment.setMax(leafAlloc.getMaxId());
        segment.setStep(buffer.getStep());
        stopWatch.stop("updateSegmentFromDb", key + " " + segment);
    }

    public ResultVO getIdFromSegmentBuffer(final SegmentBuffer buffer) {
        boolean continueTake = true;
        while (true) {
            buffer.rLock().lock();
            try {
                final Segment segment = buffer.getCurrent();
                if (!buffer.isNextReady() && (segment.getIdle() < 0.9 * segment.getStep()) && buffer.getThreadRunning().compareAndSet(false, true)) {
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            Segment next = buffer.getSegments()[buffer.nextPos()];
                            boolean updateOk = false;
                            try {
                                updateSegmentFromDb(buffer.getKey(), next);
                                updateOk = true;
                                logger.info("update segment {} from db {}", buffer.getKey(), next);
                            } catch (Exception e) {
                                logger.warn(buffer.getKey() + " updateSegmentFromDb exception", e);
                            } finally {
                                if (updateOk) {
                                    buffer.wLock().lock();
                                    buffer.setNextReady(true);
                                    buffer.getThreadRunning().set(false);
                                    buffer.wLock().unlock();
                                } else {
                                    buffer.getThreadRunning().set(false);
                                }
                            }
                        }
                    });
                }
                long value = segment.getValue().getAndIncrement();
                if (value < segment.getMax()) {
                    return new ResultVO(value, StatusEnum.SUCCESS);
                } else {
                    if (!continueTake) {
                        logger.error("Both two segments in {} are not ready!", buffer);
                        return new ResultVO(EXCEPTION_ID_TWO_SEGMENTS_ARE_NULL, StatusEnum.EXCEPTION);
                    }
                }
            } finally {
                buffer.rLock().unlock();
            }
            waitAndSleep(buffer);
            if (!buffer.isNextReady()) {
                continueTake = false;
                continue;
            }
            buffer.wLock().lock();
            try {
                final Segment segment = buffer.getCurrent();
                long value = segment.getValue().getAndIncrement();
                if (value < segment.getMax()) {
                    return new ResultVO(value, StatusEnum.SUCCESS);
                }
                if (buffer.isNextReady()) {
                    buffer.switchPos();
                    buffer.setNextReady(false);
                }
            } finally {
                buffer.wLock().unlock();
            }
        }
    }

    private void waitAndSleep(SegmentBuffer buffer) {
        int roll = 0;
        while (buffer.getThreadRunning().get()) {
            roll += 1;
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                logger.warn("Thread {} Interrupted", Thread.currentThread().getName());
                break;
            }
            if (roll > 10000) {
                break;
            }
        }
    }

    public List<LeafAlloc> listLeafAllocs() {
        return idAllocDao.listLeafAllocs();
    }

    public Map<String, SegmentBuffer> getCache() {
        return cache;
    }

    public IdAllocDao getIdAllocDao() {
        return idAllocDao;
    }

    public void setIdAllocDao(IdAllocDao idAllocDao) {
        this.idAllocDao = idAllocDao;
    }
}
