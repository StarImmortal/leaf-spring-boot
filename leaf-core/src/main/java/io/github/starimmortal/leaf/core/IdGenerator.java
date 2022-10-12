package io.github.starimmortal.leaf.core;

import io.github.starimmortal.leaf.core.vo.ResultVO;

/**
 * @author william@StarImmortal
 */
public interface IdGenerator {
    /**
     * 生成分布式ID
     *
     * @param key 业务名
     * @return Result View Object
     */
    ResultVO get(String key);

    /**
     * 初始化
     *
     * @return true || false
     */
    boolean init();
}
