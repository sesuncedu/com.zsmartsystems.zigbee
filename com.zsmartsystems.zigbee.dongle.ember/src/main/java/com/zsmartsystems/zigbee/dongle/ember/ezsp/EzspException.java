package com.zsmartsystems.zigbee.dongle.ember.ezsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EzspException extends Exception {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(EzspException.class);

    protected EzspException() {
        super();
    }

    protected EzspException(String message) {
        super(message);
    }

    protected EzspException(String message, Throwable cause) {
        super(message, cause);
    }

    protected EzspException(Throwable cause) {
        super(cause);
    }

    protected EzspException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
