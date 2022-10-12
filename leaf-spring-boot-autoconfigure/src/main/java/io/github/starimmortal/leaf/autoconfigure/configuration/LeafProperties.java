package io.github.starimmortal.leaf.autoconfigure.configuration;

import io.github.starimmortal.leaf.core.common.PropertyFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author zhaodong.xzd (github.com/yaccc)
 * @date 2019/10/09
 */
@ConfigurationProperties(prefix = "leaf")
public class LeafProperties {
    /**
     * 服务名
     */
    private String name;

    /**
     * 号段模式
     */
    private Segment segment;

    /**
     * 雪花算法
     */
    private Snowflake snowflake;

    public static class Segment {
        /**
         * 是否开启号段模式
         */
        private boolean enable = false;

        /**
         * 驱动名称
         */
        private String driverClassName;

        /**
         * 连接地址
         */
        private String url;

        /**
         * 用户名
         */
        private String username;

        /**
         * 密码
         */
        private String password;

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        @Override
        public String toString() {
            return "Segment{" +
                    "enable=" + enable +
                    '}';
        }
    }

    public static class Snowflake {
        /**
         * 是否开启雪花算法
         */
        private boolean enable = false;

        private String address;

        private int port;

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        @Override
        public String toString() {
            return "Snowflake{" +
                    "enable=" + enable +
                    '}';
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        PropertyFactory.setLeafName(name);
        this.name = name;
    }

    public Segment getSegment() {
        return segment;
    }

    public void setSegment(Segment segment) {
        this.segment = segment;
    }

    public Snowflake getSnowflake() {
        return snowflake;
    }

    public void setSnowflake(Snowflake snowflake) {
        this.snowflake = snowflake;
    }

    @Override
    public String toString() {
        return "LeafSpringBootProperties{" +
                "name='" + name + '\'' +
                ", segment=" + segment +
                ", snowflake=" + snowflake +
                '}';
    }
}
