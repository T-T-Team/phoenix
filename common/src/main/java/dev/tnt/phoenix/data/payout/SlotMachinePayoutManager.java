package dev.tnt.phoenix.data.payout;

import com.google.common.collect.ImmutableList;
import dev.tnt.phoenix.Phoenix;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SlotMachinePayoutManager extends SimpleJsonResourceReloadListener<List<SlotMachinePayout>> {

    public static final Identifier DATA_MANAGER_IDENTIFIER = Phoenix.identifier("slot_machine_payout_manager");
    private final Map<Identifier, SlotMachinePayout> payouts = new HashMap<>();

    public SlotMachinePayoutManager() {
        super(SlotMachinePayout.CODEC.listOf(), FileToIdConverter.json("slot_machine/payout"));
    }

    public List<SlotMachinePayout> getAvailablePayouts() {
        return ImmutableList.copyOf(this.payouts.values());
    }

    public Optional<SlotMachinePayout> getPayout(Identifier id) {
        return Optional.ofNullable(this.payouts.get(id));
    }

    @Override
    protected void apply(Map<Identifier, List<SlotMachinePayout>> preparations, ResourceManager manager, ProfilerFiller profiler) {
        this.payouts.clear();
        for (List<SlotMachinePayout> payoutFile : preparations.values()) {
            payoutFile.forEach(payout -> this.payouts.put(payout.payoutId(), payout));
        }
    }
}
