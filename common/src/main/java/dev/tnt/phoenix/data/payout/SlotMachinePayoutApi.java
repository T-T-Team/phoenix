package dev.tnt.phoenix.data.payout;

import dev.tnt.phoenix.data.PayoutRequestEntry;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public interface SlotMachinePayoutApi {

    Optional<SlotMachinePayout> findPayout(ResourceLocation id);

    default Optional<SlotMachinePayout> findPayout(PayoutRequestEntry entry) {
        return this.findPayout(entry.payoutId());
    }

    List<SlotMachinePayout> listAvailablePayouts();
}
