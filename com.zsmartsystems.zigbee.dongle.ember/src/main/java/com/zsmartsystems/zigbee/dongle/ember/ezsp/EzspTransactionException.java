package com.zsmartsystems.zigbee.dongle.ember.ezsp;

import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EzspStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An EzspException is used to signal an error in an EZSP operation. The EZSP status code is carried in the status
 * property.
 */
public class EzspTransactionException extends EzspException {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(EzspTransactionException.class);
    private final EzspStatus status;

    public EzspTransactionException(EzspStatus status, String message) {
        super(message);
        this.status = status;
    }

    public EzspTransactionException(EzspStatus status) {
        this.status = status;
    }

    public EzspStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + ":: Ezsp Status: " + status;
    }
}
