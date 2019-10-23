package com.zsmartsystems.zigbee;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.zsmartsystems.zigbee.transaction.ZigBeeTransaction;
import com.zsmartsystems.zigbee.transaction.ZigBeeTransactionMatcher;
import com.zsmartsystems.zigbee.zcl.ZclFieldDeserializer;
import com.zsmartsystems.zigbee.zcl.ZclFieldSerializer;
import com.zsmartsystems.zigbee.zcl.protocol.ZclDataType;
import com.zsmartsystems.zigbee.zdo.ZdoRequest;
import com.zsmartsystems.zigbee.zdo.ZdoStatus;
import com.zsmartsystems.zigbee.zdo.command.ManagementNetworkUpdateNotify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ZigBeeNetworkChannelManager {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(ZigBeeNetworkChannelManager.class);
    private final ZigBeeNetworkManager networkManager;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicInteger nextTid = new AtomicInteger();

    ZigBeeNetworkChannelManager(ZigBeeNetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    Collection<ManagementNetworkUpdateNotify> scanNodes(Collection<ZigBeeNode> nodes) {
        Collection<ZigBeeNode> nodes1 = new ArrayList<>(nodes);
        List<ManagementNetworkUpdateNotify> result = new ArrayList<>(nodes1.size());
        for (ZigBeeNode node : nodes1) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            if (node.isFullFuntionDevice()) {
                try {
                    logger.info("trying to scan {}", node);
                    ManagementNetworkUpdateNotify scanResult = doEnergyScan(node.getNetworkAddress(),
                            ZigBeeChannelMask.CHANNEL_MASK_2GHZ, 5, 5);
                    result.add(scanResult);
                    logger.info("Scan result: {}", scanResult);
                } catch (ExecutionException e) {
                    logger.error("Caught Exception", e);
                } catch (InterruptedException e) {
                    logger.error("Caught Exception", e);
                }
            }
        }
        return result;
    }

    ManagementNetworkUpdateNotify doEnergyScan(int target, int mask, int duration, int count)
            throws ExecutionException, InterruptedException {
        if (duration < 0) {
            duration = 0;
        }
        if (duration > 5) {
            duration = 5;
        }
        ChannelScanRequest request = new ChannelScanRequest(target, mask, duration, count);
        request.setTransactionId(nextTid.incrementAndGet());
        ZigBeeTransactionMatcher matcher = (x, y) -> {
            if (y instanceof ManagementNetworkUpdateNotify) {
                logger.debug("got a notify - is it us? ");
                if (x.getTransactionId().equals(y.getTransactionId())) {
                    logger.debug("got match {} for request {}", y, x);
                    return true;
                } else {
                    logger.debug("notify -  TID non match x = {}, y= {}", x.getTransactionId(), y.getTransactionId());
                }
            } else {
                logger.debug("not a Notify - it's a {}", y.getClass().getSimpleName());
            }
            return false;
        };
        Future<CommandResult> fut = networkManager.sendTransaction(request, matcher);
        CommandResult commandResult = fut.get();
        if (fut.isCancelled()) {
            throw new InterruptedException("requested energy scan was cancelled");
        }
        if (commandResult.getStatusCode() != ZdoStatus.SUCCESS.getId()) {
            return null;
        } else {
            return commandResult.getResponse();
        }
    }

    private static abstract class BaseManagementNetworkUpdateRequest extends ZdoRequest {

        int channels;
        int scanDuration;

        BaseManagementNetworkUpdateRequest() {
            clusterId = 0x0038;
        }

        BaseManagementNetworkUpdateRequest(int target, int channels, int scanDuration) {
            this();
            this.setDestinationAddress(new ZigBeeEndpointAddress(target));
            this.channels = channels;
            this.scanDuration = scanDuration;
        }

        @Override
        public void serialize(final ZclFieldSerializer serializer) {
            super.serialize(serializer);

            serializer.serialize(channels, ZclDataType.BITMAP_32_BIT);
            serializer.serialize(scanDuration, ZclDataType.UNSIGNED_8_BIT_INTEGER);
        }

        @Override
        public void deserialize(final ZclFieldDeserializer deserializer) {
            super.deserialize(deserializer);

            channels = (Integer) deserializer.deserialize(ZclDataType.BITMAP_32_BIT);
            scanDuration = (Integer) deserializer.deserialize(ZclDataType.UNSIGNED_8_BIT_INTEGER);
        }

    }

    private static abstract class BaseUpdateRequest extends BaseManagementNetworkUpdateRequest {
        int updateId;

        public BaseUpdateRequest(int channelMask, int duration, int updateId) {
            super(ZigBeeBroadcastDestination.BROADCAST_RX_ON.getKey(), channelMask, duration);
            this.updateId = updateId;
        }

        @Override
        public void serialize(ZclFieldSerializer serializer) {
            super.serialize(serializer);
            serializer.serialize(updateId, ZclDataType.UNSIGNED_8_BIT_INTEGER);
        }

        @Override
        public void deserialize(ZclFieldDeserializer deserializer) {
            super.deserialize(deserializer);
            this.updateId = (Integer) deserializer.deserialize(ZclDataType.UNSIGNED_8_BIT_INTEGER);
        }
    }

    private static class ChannelScanRequest extends BaseManagementNetworkUpdateRequest {
        int count;

        public ChannelScanRequest(int target, int channelMask, int duration, int count) {
            super(target, channelMask, duration);
            if (duration < 0 || duration > 5) {
                throw new IllegalArgumentException("Channel scan request with illegal duration given");
            }
            this.count = count;
        }

        @Override
        public void serialize(ZclFieldSerializer serializer) {
            super.serialize(serializer);
            serializer.serialize(count, ZclDataType.UNSIGNED_8_BIT_INTEGER);
        }

        @Override
        public void deserialize(ZclFieldDeserializer deserializer) {
            super.deserialize(deserializer);
            this.count = (Integer) deserializer.deserialize(ZclDataType.UNSIGNED_8_BIT_INTEGER);
        }

        private static int durationToMillis(int duration) {
            int baseSuperframeDuration = 60 * 16;
            float scanDurationInSymbols = baseSuperframeDuration << duration;
            float ms = scanDurationInSymbols / 62.5f;
            return Math.round(ms);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("ChannelScanRequest{");
            sb.append("channelMask=").append(channels);
            sb.append(", scanDuration=").append(durationToMillis(scanDuration)).append("ms");
            sb.append(", count=").append(count);
            sb.append('}');
            return sb.toString();
        }
    }

    private static class ChangeChannelRequest extends BaseUpdateRequest {
        public ChangeChannelRequest(int channel, int updateId) {
            super(channelToMask(channel), 0xfe, updateId);
        }

        private static int channelToMask(int channel) {
            validateChannel(channel);
            return 1 << channel;
        }

        private static void validateChannel(int channel) {
            if (channel < 11 || channel > 26) {
                throw new IllegalArgumentException("Illegal value for channel: " + channel);
            }
        }

        private static int maskToChannel(int mask) {
            if (Integer.bitCount(mask) != 1) {
                throw new IllegalArgumentException(
                        "Expected channel mask with single channel selected but got " + mask);
            }
            int channel = Integer.highestOneBit(mask);
            validateChannel(channel);
            return channel;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("[ChangeChannelRequest ");
            sb.append("target=").append(getDestinationAddress());
            sb.append("channel=").append(maskToChannel(channels));
            sb.append(", updateId=").append(updateId);
            sb.append(']');
            return sb.toString();
        }
    }

    private static class UpdateNetworkManagerRequest extends BaseUpdateRequest {

        int managerAddress;

        public UpdateNetworkManagerRequest(int channelMask, int managerAddress) {
            super(channelMask, 0xff, 0x00);
            this.managerAddress = managerAddress;
        }

        @Override
        public void serialize(ZclFieldSerializer serializer) {
            super.serialize(serializer);
            serializer.serialize(managerAddress, ZclDataType.NWK_ADDRESS);
        }

        @Override
        public void deserialize(ZclFieldDeserializer deserializer) {
            super.deserialize(deserializer);
            this.managerAddress = (int) deserializer.deserialize(ZclDataType.NWK_ADDRESS);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("[UpdateNetworkManagerRequest ");
            sb.append("channel mask=").append(channels);
            sb.append(", managerAddress=").append(managerAddress);
            sb.append(']');
            return sb.toString();
        }
    }

}
