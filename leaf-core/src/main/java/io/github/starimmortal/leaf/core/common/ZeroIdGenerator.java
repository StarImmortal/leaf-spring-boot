package io.github.starimmortal.leaf.core.common;

import io.github.starimmortal.leaf.core.IdGenerator;
import io.github.starimmortal.leaf.core.enumeration.StatusEnum;
import io.github.starimmortal.leaf.core.vo.ResultVO;

public class ZeroIdGenerator implements IdGenerator {
    @Override
    public ResultVO get(String key) {
        return new ResultVO(0, StatusEnum.SUCCESS);
    }

    @Override
    public boolean init() {
        return true;
    }
}
