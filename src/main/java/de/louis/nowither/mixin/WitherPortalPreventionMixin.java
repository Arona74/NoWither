package de.louis.nowither.mixin;

import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.world.dimension.DimensionTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WitherEntity.class)
public class WitherPortalPreventionMixin {
    
    // Prevent wither from using portals by disabling portal usage in Nether
    @Inject(method = "canUsePortals", at = @At("HEAD"), cancellable = true)
    private void disablePortalsInNether(CallbackInfoReturnable<Boolean> cir) {
        WitherEntity wither = (WitherEntity) (Object) this;
        
        // If wither is in Nether, disable portal usage completely
        if (wither.getWorld().getDimensionEntry().matchesKey(DimensionTypes.THE_NETHER)) {
            cir.setReturnValue(false);
        }
    }
    
    // Reset portal timer to prevent portal activation
    @Inject(method = "mobTick", at = @At("TAIL"))
    private void preventPortalEntry(CallbackInfo ci) {
        WitherEntity wither = (WitherEntity) (Object) this;
        
        // If wither is in Nether, constantly reset portal cooldown
        if (wither.getWorld().getDimensionEntry().matchesKey(DimensionTypes.THE_NETHER)) {
            // Reset portal cooldown to prevent portal usage
            wither.resetPortalCooldown();
        }
    }
}