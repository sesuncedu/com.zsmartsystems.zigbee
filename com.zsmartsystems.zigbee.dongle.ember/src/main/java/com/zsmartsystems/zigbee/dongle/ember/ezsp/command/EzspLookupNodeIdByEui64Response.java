/**
 * Copyright (c) 2016-2019 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.zsmartsystems.zigbee.dongle.ember.ezsp.command;

import com.zsmartsystems.zigbee.dongle.ember.ezsp.EzspFrameResponse;

/**
 * Class to implement the Ember EZSP command <b>lookupNodeIdByEui64</b>.
 * <p>
 * Returns the node ID that corresponds to the specified EUI64. The node ID is found by searching
 * through all stack tables for the specified EUI64.
 * <p>
 * This class provides methods for processing EZSP commands.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
public class EzspLookupNodeIdByEui64Response extends EzspFrameResponse {
    public static final int FRAME_ID = 0x60;

    /**
     * The short ID of the node or EMBER_NULL_NODE_ID if the short ID is not known.
     * <p>
     * EZSP type is <i>EmberNodeId</i> - Java type is {@link int}
     */
    private int nodeId;

    /**
     * Response and Handler constructor
     */
    public EzspLookupNodeIdByEui64Response(int[] inputBuffer) {
        // Super creates deserializer and reads header fields
        super(inputBuffer);

        // Deserialize the fields
        nodeId = deserializer.deserializeUInt16();
    }

    /**
     * The short ID of the node or EMBER_NULL_NODE_ID if the short ID is not known.
     * <p>
     * EZSP type is <i>EmberNodeId</i> - Java type is {@link int}
     *
     * @return the current nodeId as {@link int}
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     * The short ID of the node or EMBER_NULL_NODE_ID if the short ID is not known.
     *
     * @param nodeId the nodeId to set as {@link int}
     */
    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(59);
        builder.append("EzspLookupNodeIdByEui64Response [nodeId=");
        builder.append(String.format("%04X", nodeId));
        builder.append(']');
        return builder.toString();
    }
}
