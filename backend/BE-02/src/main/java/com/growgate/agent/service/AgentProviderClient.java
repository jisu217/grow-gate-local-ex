package com.growgate.agent.service;

import java.util.Optional;

public interface AgentProviderClient {
    String providerName();

    boolean isEnabled();

    Optional<AgentInferenceResult> infer(AgentInferenceRequest request);
}
