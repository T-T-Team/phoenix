package dev.tnt.phoenix.client.screen;

import dev.tnt.phoenix.client.PhoenixClient;
import dev.tnt.phoenix.client.screen.widget.BalanceWidget;
import dev.tnt.phoenix.data.ItemValueDefinition;
import dev.tnt.phoenix.data.ItemValueHolder;
import dev.tnt.phoenix.network.C2S_SlotMachinePayoutRequest;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class PayoutScreen extends Screen {

    public static final Component TITLE = Component.translatable("screen.phoenix.payout");

    private final Screen parent;
    private final BlockPos pos;
    private final List<ItemValueDefinition> definitions;
    private final int availableBalance;
    private final Reference2IntMap<Item> priceCache = new Reference2IntOpenHashMap<>();
    private final Reference2IntMap<Item> cart = new Reference2IntOpenHashMap<>();
    private int checkoutPrice;

    public PayoutScreen(Screen parent, BlockPos pos, List<ItemValueDefinition> definitions, int availableBalance) {
        super(TITLE);
        this.parent = parent;
        this.pos = pos;
        this.definitions = definitions;
        this.availableBalance = availableBalance;
        this.computePriceCache();
    }

    @Override
    protected void init() {
        // shop buttons in grid
        List<ItemValueHolder> items = this.definitions.stream()
                .flatMap(ItemValueHolder::unwrap)
                .toList();
        int itemWidth = 120;
        int itemHeight = 24;
        int columns = (this.width - 10) / itemWidth;
        int rows = (this.height - 25) / itemHeight;
        for (int x = 0; x < columns; x++) {
            for (int y = 0; y < rows; y++) {
                int index = y + x * rows;
                if (index >= items.size()) {
                    break;
                }
                ItemValueHolder item = items.get(index);
                int px = 5 + x * itemWidth;
                int py = 5 + y * itemHeight;
                int quantity = this.cart.getInt(item.item());
                this.addRenderableOnly(new ItemValueWidget(px, py, itemWidth, itemHeight, this.font, item, quantity));
                Button add = this.addRenderableWidget(
                        Button.builder(Component.literal("+"), _ -> this.updateQuantity(item.item(), 1))
                                .bounds(px + itemWidth - 10, py + 1, 10, 10)
                                .build()
                );
                add.active = (this.availableBalance - this.checkoutPrice) >= item.value();
                Button remove = this.addRenderableWidget(
                        Button.builder(Component.literal("-"), _ -> this.updateQuantity(item.item(), -1))
                                .bounds(px + itemWidth - 10, py + itemHeight - 11, 10, 10)
                                .build()
                );
                remove.active = this.cart.getInt(item.item()) > 0;
            }
        }

        // remaining balance
        BalanceWidget widget = this.addRenderableOnly(new BalanceWidget(this.width - 105, this.height - 21, 100, 16, () -> this.availableBalance - this.checkoutPrice, this.font));
        widget.setDigits(15);
        widget.setTextColor(0xFF00FF00);
        widget.setTextCorrectionOffset(0.5F, 0.5F);

        // confirm button
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_PROCEED, this::confirmButtonClicked)
                .bounds(this.width - 160, this.height - 21, 50, 16)
                .build()
        );
        // cancel button
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, this::cancelButtonClicked)
                .bounds(this.width - 215, this.height - 21, 50, 16)
                .build()
        );
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void confirmButtonClicked(Button button) {
        List<ItemValueHolder> payout = this.preparePayout();
        PhoenixClient.PLATFORM.sendPacket(new C2S_SlotMachinePayoutRequest(this.pos, payout));
        this.minecraft.gui.setScreen(null);
    }

    private void cancelButtonClicked(Button button) {
        this.minecraft.gui.setScreen(this.parent);
    }

    private void updateQuantity(Item item, int quantity) {
        this.cart.mergeInt(item, quantity, Integer::sum);
        this.checkoutPrice = this.cart.reference2IntEntrySet().stream()
                .mapToInt(e -> this.priceCache.getInt(e.getKey()) * e.getIntValue())
                .sum();
        this.init(this.width, this.height);
    }

    private void computePriceCache() {
        this.priceCache.clear();
        this.definitions.stream()
                .flatMap(ItemValueHolder::unwrap)
                .forEach(holder -> this.priceCache.put(holder.item(), holder.value()));
    }

    private List<ItemValueHolder> preparePayout() {
        return this.cart.reference2IntEntrySet().stream()
                .map(e -> new ItemValueHolder(e.getKey(), e.getIntValue()))
                .toList();
    }

    private static final class ItemValueWidget extends AbstractWidget {

        private final Font font;
        private final ItemValueHolder holder;
        private final int quantity;
        private final ItemStack itemStack;

        public ItemValueWidget(int x, int y, int width, int height, Font font, ItemValueHolder holder, int quantity) {
            super(x, y, width, height, CommonComponents.EMPTY);
            this.font = font;
            this.holder = holder;
            this.quantity = quantity;
            this.itemStack = holder.item().getDefaultInstance();
            this.setMessage(this.itemStack.getStyledHoverName());
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            graphics.item(this.itemStack, this.getX() + 2, this.getY() + 4);

            Component quantityLabel = Component.literal(String.valueOf(this.quantity));
            int quantityWidth = this.font.width(quantityLabel);
            graphics.enableScissor(this.getX(), this.getY(), this.getRight() - 14 - quantityWidth, this.getBottom());
            graphics.text(this.font, this.getMessage(), this.getX() + 24, this.getY() + 2, 0xFFFFFFFF);
            graphics.text(this.font, Component.literal(String.valueOf(this.holder.value())), this.getX() + 24, this.getY() + 14, 0xFFFFFFFF);
            graphics.disableScissor();

            graphics.text(this.font, quantityLabel, this.getX() + this.getWidth() - quantityWidth - 12, this.getY() + (this.getHeight() - this.font.lineHeight) / 2 + 1, 0xFFFFFFFF);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }
    }
}
