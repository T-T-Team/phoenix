package dev.tnt.phoenix.client.screen;

import dev.tnt.phoenix.client.PhoenixClient;
import dev.tnt.phoenix.client.screen.widget.BalanceWidget;
import dev.tnt.phoenix.data.PayoutRequestEntry;
import dev.tnt.phoenix.data.payout.SlotMachinePayout;
import dev.tnt.phoenix.network.C2S_SlotMachinePayoutRequest;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class PayoutScreen extends Screen {

    public static final Component TITLE = Component.translatable("screen.phoenix.payout");

    private final Screen parent;
    private final BlockPos pos;
    private final List<SlotMachinePayout> definitions;
    private final int availableBalance;
    private final Reference2IntMap<SlotMachinePayout> cart = new Reference2IntOpenHashMap<>();
    private int checkoutPrice;
    private int pageDisplayLimit;
    private int currentPage;

    public PayoutScreen(Screen parent, BlockPos pos, List<SlotMachinePayout> definitions, int availableBalance) {
        super(TITLE);
        this.parent = parent;
        this.pos = pos;
        this.definitions = definitions;
        this.availableBalance = availableBalance;
        this.definitions.sort(null);
    }

    @Override
    protected void init() {
        // shop buttons in grid
        int itemWidth = 140;
        int itemHeight = 24;
        int columns = (this.width - 10) / itemWidth;
        int rows = (this.height - 25) / itemHeight;
        this.pageDisplayLimit = columns * rows;
        int pages = this.getPageCount();
        this.currentPage = Mth.clamp(this.currentPage, 0, pages - 1);
        for (int x = 0; x < columns; x++) {
            for (int y = 0; y < rows; y++) {
                int index = (this.currentPage * this.pageDisplayLimit) + y + x * rows;
                if (index >= this.definitions.size()) {
                    break;
                }
                SlotMachinePayout payout = this.definitions.get(index);
                int px = 5 + x * itemWidth;
                int py = 5 + y * itemHeight;
                int quantity = this.cart.getInt(payout);
                this.addRenderableOnly(new ItemValueWidget(px, py, itemWidth, itemHeight, this.font, payout, quantity));
                Button add = this.addRenderableWidget(
                        Button.builder(Component.literal("+"), _ -> this.updateQuantity(payout, 1))
                                .bounds(px + itemWidth - 10, py + 1, 10, 10)
                                .build()
                );
                add.active = (this.availableBalance - this.checkoutPrice) >= payout.price();
                Button remove = this.addRenderableWidget(
                        Button.builder(Component.literal("-"), _ -> this.updateQuantity(payout, -1))
                                .bounds(px + itemWidth - 10, py + itemHeight - 11, 10, 10)
                                .build()
                );
                remove.active = this.cart.getInt(payout) > 0;
            }
        }

        // remaining balance
        BalanceWidget widget = this.addRenderableOnly(new BalanceWidget(this.width - 105, this.height - 21, 100, 16, () -> this.availableBalance - this.checkoutPrice, this.font));
        widget.setDigits(15);
        widget.setTextColor(0xFF00FF00);
        widget.setTextCorrectionOffset(0.5F, 0.5F);

        // confirm button
        Button confirmButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_PROCEED, this::confirmButtonClicked)
                .bounds(this.width - 160, this.height - 21, 50, 16)
                .build()
        );
        confirmButton.active = this.checkoutPrice > 0;
        // cancel button
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, this::cancelButtonClicked)
                .bounds(this.width - 215, this.height - 21, 50, 16)
                .build()
        );

        if (pages > 1) {
            Button prevPage = this.addRenderableWidget(Button.builder(Component.translatable("gui.phoenix.previous"), btn -> this.changePage(-1))
                    .bounds(5, this.height - 21, 50, 16)
                    .build()
            );
            prevPage.active = this.currentPage > 0;

            Button nextPage = this.addRenderableWidget(Button.builder(Component.translatable("gui.phoenix.next"), btn -> this.changePage(1))
                    .bounds(100, this.height - 21, 50, 16)
                    .build()
            );
            nextPage.active = this.currentPage < pages - 1;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int pages = this.getPageCount();
        if (pages > 1) {
            Component label = Component.literal(this.currentPage + 1 + "/" + pages);
            graphics.text(this.font, label, 55 + (45 - this.font.width(label)) / 2, this.height - 17, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void confirmButtonClicked(Button button) {
        List<PayoutRequestEntry> payout = this.preparePayoutsForRequest();
        PhoenixClient.PLATFORM.sendPacket(new C2S_SlotMachinePayoutRequest(this.pos, payout));
        this.minecraft.gui.setScreen(null);
    }

    private void cancelButtonClicked(Button button) {
        this.minecraft.gui.setScreen(this.parent);
    }

    private void updateQuantity(SlotMachinePayout payout, int quantity) {
        this.cart.mergeInt(payout, quantity, Integer::sum);
        this.checkoutPrice = this.cart.reference2IntEntrySet().stream()
                .mapToInt(e -> e.getIntValue() * e.getKey().price())
                .sum();
        this.init(this.width, this.height);
    }

    private List<PayoutRequestEntry> preparePayoutsForRequest() {
        return this.cart.reference2IntEntrySet().stream()
                .map(e -> new PayoutRequestEntry(e.getKey().payoutId(), e.getIntValue()))
                .toList();
    }

    private void changePage(int offset) {
        this.currentPage += offset;
        this.init(this.width, this.height);
    }

    private int getPageCount() {
        return 1 + (this.definitions.size() - 1) / this.pageDisplayLimit;
    }

    private static final class ItemValueWidget extends AbstractWidget {

        private final Font font;
        private final SlotMachinePayout payout;
        private final int quantity;
        private final ItemStack itemStack;

        public ItemValueWidget(int x, int y, int width, int height, Font font, SlotMachinePayout payout, int quantity) {
            super(x, y, width, height, CommonComponents.EMPTY);
            this.font = font;
            this.payout = payout;
            this.quantity = quantity;
            this.itemStack = this.payout.template().create();
            this.setMessage(this.itemStack.getStyledHoverName());
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            graphics.item(this.itemStack, this.getX() + 2, this.getY() + 4);

            Component quantityLabel = Component.literal(String.valueOf(this.quantity));
            int quantityWidth = this.font.width(quantityLabel);
            ActiveTextCollector textRenderer = graphics.textRenderer();
            textRenderer.acceptScrolling(this.getMessage(), this.getX() + 24, this.getX() + 24, this.getRight() - 14 - quantityWidth, this.getY() + 2, this.getY() + 12);
            graphics.text(this.font, Component.literal(String.valueOf(this.payout.price())), this.getX() + 24, this.getY() + 14, 0xFFFFFFFF);

            graphics.text(this.font, quantityLabel, this.getX() + this.getWidth() - quantityWidth - 12, this.getY() + (this.getHeight() - this.font.lineHeight) / 2 + 1, 0xFFFFFFFF);

            if (mouseX >= this.getX() + 2 && mouseX <= this.getX() + 18 && mouseY >= this.getY() + 4 && mouseY <= this.getY() + 20) {
                graphics.setTooltipForNextFrame(this.font, this.itemStack, mouseX, mouseY);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }
    }
}
