package com.stool.miniredis.netty.common;

import java.util.List;

public class RedisInput {
    private List<String> params;

    public RedisInput(List<String> params) {
        this.params = params;
    }

    public List<String> getParams() {
        return params;
    }

    public void setParams(List<String> params) {
        this.params = params;
    }
}
