package com.jamex.refereestaffer.model.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record IDRequest(

        @NotNull
        List<Long> ids
) {
}
