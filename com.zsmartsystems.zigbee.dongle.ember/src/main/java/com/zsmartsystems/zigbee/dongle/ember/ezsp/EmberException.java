package com.zsmartsystems.zigbee.dongle.ember.ezsp;

import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EmberStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An EmberException is used to signal an error in an EzspTransaction. The Ember status code is carried in the status
 * property.
 */
public class EmberException extends EzspException {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(EmberException.class);
    private final EmberStatus status;

    public EmberException(EmberStatus status, String message) {
        super(message);
        this.status = status;
    }

    public EmberException(EmberStatus status) {
        this.status = status;
    }

    public EmberStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + ":: Ember Status: " + status;
    }
}
