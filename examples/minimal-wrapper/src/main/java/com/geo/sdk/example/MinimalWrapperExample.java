package com.geo.sdk.example;

import com.geo.sdk.core.CoreSdk;
import com.geo.sdk.core.CoreSdk.Budget;
import com.geo.sdk.core.CoreSdk.Context;
import com.geo.sdk.core.CoreSdk.InputEvent;
import com.geo.sdk.core.CoreSdk.ModelState;
import com.geo.sdk.core.CoreSdk.PipelineEngine;
import com.geo.sdk.core.CoreSdk.PipelineOutcome;

import java.util.Map;

public final class MinimalWrapperExample {
    private MinimalWrapperExample() {
    }

    public static void main(String[] args) {
        PipelineEngine engine = CoreSdk.defaultEngine();
        ModelState model = CoreSdk.defaultModel();

        InputEvent input = new InputEvent(
                "sample-event",
                35.681236,
                139.767125,
                System.currentTimeMillis(),
                Map.of("dwell_minutes", "15")
        );

        Context ctx = new Context(
                System.currentTimeMillis(),
                "Asia/Tokyo",
                Map.of("time_band", "day")
        );

        Budget budget = new Budget(
                3,
                0,
                3,
                30_000L,
                0L
        );

        PipelineOutcome outcome = engine.run(input, ctx, budget, model);
        System.out.println("Decision: " + outcome.trace().actionPlan().type() + " / " + outcome.trace().actionPlan().reason());
    }
}
