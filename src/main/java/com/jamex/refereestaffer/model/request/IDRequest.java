package com.jamex.refereestaffer.model.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class IDRequest {

    @NotNull
    private List<Long> ids;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}
