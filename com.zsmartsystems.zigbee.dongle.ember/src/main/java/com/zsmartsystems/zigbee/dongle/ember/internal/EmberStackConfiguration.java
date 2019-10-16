/**
 * Copyright (c) 2016-2019 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.zsmartsystems.zigbee.dongle.ember.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.zsmartsystems.zigbee.dongle.ember.EmberNcp;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspGetConfigurationValueRequest;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspGetConfigurationValueResponse;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EzspConfigId;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EzspDecisionId;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EzspPolicyId;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EzspStatus;
import com.zsmartsystems.zigbee.dongle.ember.internal.transaction.EzspSingleResponseTransaction;
import com.zsmartsystems.zigbee.dongle.ember.internal.transaction.EzspTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides utility functions to configure, and read the configuration from the Ember stack.
 *
 * @author Chris Jackson
 *
 */
public class EmberStackConfiguration {
    private static Logger logger = LoggerFactory.getLogger(EmberStackConfiguration.class);
    /**
     * The {@link EmberNcp} used to send the EZSP frames to the NCP
     */
    private EmberNcp ncp;

    /**
     * Constructor to set the {@link EmberNcp}
     *
     * @param ncp the {@link EmberNcp} used to communicate with the NCP
     */
    public EmberStackConfiguration(EmberNcp ncp) {
        this.ncp = ncp;
    }

    /**
     * Configuration utility. Takes a {@link Map} of {@link EzspConfigId} to {@link Integer} and will work through
     * setting them before returning.
     *
     * @param configuration {@link Map} of {@link EzspConfigId} to {@link Integer} with configuration to set
     * @return true if all configuration were set successfully
     */
    public boolean setConfiguration(Map<EzspConfigId, Integer> configuration) {
        boolean success = true;

        for (Entry<EzspConfigId, Integer> config : configuration.entrySet()) {
            final Integer oldValue = getConfig(config.getKey());
            if (!oldValue.equals(config.getValue())) {
                logger.debug("Updating {} from {} to {}", config.getKey(), oldValue, config.getValue());
                if (ncp.setConfiguration(config.getKey(), config.getValue()) != EzspStatus.EZSP_SUCCESS) {
                    logger.warn("Configuration failed setting  {} to {}", config.getKey(), config.getValue());
                    success = false;
                } else {
                    logger.debug("Configuration succeded setting{} to {}", config.getKey(), config.getValue());
                }
            } else {
                logger.debug("Not updating {} because value is already {}", config.getKey(), config.getValue());
            }
        }
        return success;
    }

    /**
     * Configuration utility. Takes a {@link Set} of {@link EzspConfigId} and will work through
     * requesting them before returning.
     *
     * @param configuration {@link Set} of {@link EzspConfigId} to request
     * @return map of configuration data mapping {@link EzspConfigId} to {@link Integer}. Value will be null if error
     *         occurred.
     */
    public Map<EzspConfigId, Integer> getConfiguration(Set<EzspConfigId> configuration) {
        Map<EzspConfigId, Integer> response = new HashMap<>();
        Map<EzspConfigId, Future<EzspGetConfigurationValueResponse>> futures = new HashMap<>(configuration.size());

        for (EzspConfigId configId : configuration) {
            final Future<EzspGetConfigurationValueResponse> future = getConfigValueAsync(configId);
            futures.put(configId, future);
        }
        for (Entry<EzspConfigId, Future<EzspGetConfigurationValueResponse>> entry : futures.entrySet()) {
            try {
                final EzspGetConfigurationValueResponse valueResponse = entry.getValue().get();
                if (valueResponse != null && valueResponse.getStatus() == EzspStatus.EZSP_SUCCESS) {
                    response.put(entry.getKey(), valueResponse.getValue());
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted, current thread interrupted status= {}",
                        Thread.currentThread().isInterrupted(), e);

            } catch (ExecutionException e) {
                logger.error("Caught Exception", e);
            }

        }

        return response;
    }

    private Integer getConfig(EzspConfigId configId) {
        final Future<EzspGetConfigurationValueResponse> responseFuture = getConfigValueAsync(configId);
        EzspGetConfigurationValueResponse response;
        try {
            response = responseFuture.get();
            if (response.getStatus() != EzspStatus.EZSP_SUCCESS) {
                return null;
            }

            return response.getValue();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Caught Exception", e);
            return null;
        }

    }

    private Future<EzspGetConfigurationValueResponse> getConfigValueAsync(EzspConfigId configId) {
        EzspGetConfigurationValueRequest request = new EzspGetConfigurationValueRequest();
        request.setConfigId(configId);
        return ncp.sendAsyncRequest(request, EzspGetConfigurationValueResponse.class);
    }

    /**
     * Configuration utility. Takes a {@link Map} of {@link EzspConfigId} to {@link EzspDecisionId} and will work
     * through setting them before returning.
     *
     * @param policies {@link Map} of {@link EzspPolicyId} to {@link EzspDecisionId} with configuration to set
     *
     * @return true if all policies were set successfully
     */
    public boolean setPolicy(Map<EzspPolicyId, EzspDecisionId> policies) {
        boolean success = true;

        for (Entry<EzspPolicyId, EzspDecisionId> policy : policies.entrySet()) {
            if (ncp.setPolicy(policy.getKey(), policy.getValue()) != EzspStatus.EZSP_SUCCESS) {
                success = false;
            }
        }
        return success;
    }

    /**
     * Configuration utility. Takes a {@link Set} of {@link EzspPolicyId} and will work through
     * requesting them before returning.
     *
     * @param policies {@link Set} of {@link EzspPolicyId} to request
     * @return map of configuration data mapping {@link EzspPolicyId} to {@link EzspDecisionId}. Value will be null if
     *         error occurred.
     */
    public Map<EzspPolicyId, EzspDecisionId> getPolicy(Set<EzspPolicyId> policies) {
        Map<EzspPolicyId, EzspDecisionId> response = new HashMap<>();

        for (EzspPolicyId policyId : policies) {
            response.put(policyId, ncp.getPolicy(policyId));
        }

        return response;
    }

}
