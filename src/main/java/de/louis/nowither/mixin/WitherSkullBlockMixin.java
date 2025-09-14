package de.louis.nowither.mixin;

import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.block.WitherSkullBlock.class)
public class WitherSkullBlockMixin {
    @Inject(method = "onPlaced(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/SkullBlockEntity;)V", at = @At("HEAD"), cancellable = true)
    private static void cancelWitherSpawning(World world, BlockPos pos, SkullBlockEntity blockEntity, CallbackInfo ci) {
        // Only allow wither spawning in the Nether dimension
        if (!world.getDimensionEntry().matchesKey(DimensionTypes.THE_NETHER)) {
            ci.cancel();
        }
    }
}