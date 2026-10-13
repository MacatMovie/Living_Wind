package com.macatmovie.livingwind.item;

import com.macatmovie.livingwind.client.windmill.WindmillItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import java.util.function.Consumer;

/** Forge 1.20.1 uses initializeClient rather than NeoForge's registration event. */
public final class WindmillBlockItem extends BlockItem {
    public WindmillBlockItem(Block block, Properties properties) { super(block, properties); }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return Holder.INSTANCE;
            }
        });
    }

    /** Lazy, client-only creation; shared by the two windmill items. */
    private static final class Holder {
        private static final WindmillItemRenderer INSTANCE = new WindmillItemRenderer();
    }
}
