package io.github.starimmortal.leaf.core.vo;

import io.github.starimmortal.leaf.core.enumeration.StatusEnum;

public class ResultVO {
    private long id;
    private StatusEnum statusEnum;

    public ResultVO() {

    }

    public ResultVO(long id, StatusEnum statusEnum) {
        this.id = id;
        this.statusEnum = statusEnum;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public StatusEnum getStatus() {
        return statusEnum;
    }

    public void setStatus(StatusEnum statusEnum) {
        this.statusEnum = statusEnum;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Result{");
        sb.append("id=").append(id);
        sb.append(", status=").append(statusEnum);
        sb.append('}');
        return sb.toString();
    }
}
