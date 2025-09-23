package de.louis.nowither.mixin;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Mixin(WitherEntity.class)
public class WitherDeathItemProtectionMixin {
    
    private boolean itemsProtected = false; // Flag to prevent multiple protections
    
    // Hook into damage method to detect when wither is about to die
    @Inject(method = "damage", at = @At("HEAD"))
    private void protectItemsBeforeDeath(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        WitherEntity wither = (WitherEntity) (Object) this;
        
        if (!wither.getWorld().isClient && !itemsProtected) {
            // Check if this damage will kill the wither
            if (wither.getHealth() - amount <= 0) {
                // Make all nearby items invulnerable before the death explosion
                makeNearbyItemsInvulnerable(wither);
                itemsProtected = true; // Prevent multiple calls
            }
        }
    }
    
    // Make items invulnerable temporarily
    private void makeNearbyItemsInvulnerable(WitherEntity wither) {
        World world = wither.getWorld();
        double explosionRadius = 20.0; // Larger than death explosion radius
        
        // Create search box around wither
        Box searchBox = new Box(
            wither.getX() - explosionRadius, wither.getY() - explosionRadius, wither.getZ() - explosionRadius,
            wither.getX() + explosionRadius, wither.getY() + explosionRadius, wither.getZ() + explosionRadius
        );
        
        // Find all item entities in the area
        List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, searchBox, item -> true);
        
        for (ItemEntity item : items) {
            // Make item invulnerable temporarily
            item.setInvulnerable(true);
            
            // Schedule to remove invulnerability after explosion
            scheduleInvulnerabilityRemoval(item);
        }
    }
    
    // Remove invulnerability after explosion is done using Timer
    private void scheduleInvulnerabilityRemoval(ItemEntity item) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // Check if item still exists and remove invulnerability
                if (item.isAlive() && !item.isRemoved()) {
                    item.setInvulnerable(false);
                }
                timer.cancel(); // Clean up timer
            }
        }, 5000); // 5 seconds delay to be safe
    }
}