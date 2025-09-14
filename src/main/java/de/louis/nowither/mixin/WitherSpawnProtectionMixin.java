package de.louis.nowither.mixin;

import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WitherEntity.class)
public class WitherSpawnProtectionMixin {
    
    // Fix HP on spawn - set to max HP immediately
    @Inject(method = "mobTick", at = @At("HEAD"))
    private void fixHealthOnSpawn(CallbackInfo ci) {
        WitherEntity wither = (WitherEntity) (Object) this;
        
        // During spawn period, ensure health matches max health
        if (wither.age < 220) { // Spawn period is ~220 ticks
            float maxHealth = wither.getMaxHealth();
            float currentHealth = wither.getHealth();
            
            // If current health is less than max, set it to max
            if (currentHealth < maxHealth) {
                wither.setHealth(maxHealth);
            }
        }
    }
    
    // Additional protection: prevent damage during spawn
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void immuneToExplosionDuringSpawn(DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
        WitherEntity wither = (WitherEntity) (Object) this;
        
        // Check if wither is still spawning
        if (wither.age < 220) { // Spawn period
            // Block explosion damage completely during spawn
            if (damageSource.isOf(DamageTypes.EXPLOSION) || 
                damageSource.isOf(DamageTypes.PLAYER_EXPLOSION) ||
                damageSource.getName().equals("explosion") ||
                damageSource.getName().equals("explosion.player")) {
                cir.setReturnValue(false);
                return;
            }
            
            // Block damage from other withers during spawn
            if (damageSource.getAttacker() instanceof WitherEntity) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}