package de.louis.nowither.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(WitherSkullEntity.class)
public class BalancedWitherSkullMixin {

    // Enhance the skull when it's created - ONLY 33% chance for blue skulls
    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;DDD)V", at = @At("TAIL"))
    private void enhanceWitherSkull(World world, LivingEntity owner, double velocityX, double velocityY, double velocityZ, CallbackInfo ci) {
        WitherSkullEntity skull = (WitherSkullEntity) (Object) this;
        
        // Only 33% chance to make it a dangerous (blue) skull
        if (world.getRandom().nextFloat() < 0.33f) {
            skull.setCharged(true);
        }
        
        // Increase velocity for more impact
        Vec3d currentVelocity = skull.getVelocity();
        skull.setVelocity(currentVelocity.multiply(1.5)); // 50% faster
    }

    // Override the collision to deal balanced damage
    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void balancedCollision(HitResult hitResult, CallbackInfo ci) {
        WitherSkullEntity skull = (WitherSkullEntity) (Object) this;
        
        if (!skull.getWorld().isClient) {
            Vec3d pos = skull.getPos();
            World world = skull.getWorld();
            
            // Create a smaller explosion with minimal block damage
            world.createExplosion(
                skull,
                pos.x, pos.y, pos.z,
                1.5f, // Small explosion (barely bigger than default)
                false, // No fires to reduce griefing
                World.ExplosionSourceType.MOB
            );
            
            // Deal direct damage to nearby entities
            dealDirectDamage(skull, world, pos);
            
            // Apply moderate wither effect
            applyModerateWitherEffect(skull, world, pos);
            
            // Remove the skull
            skull.discard();
        }
        
        // Cancel the original collision handling
        ci.cancel();
    }
    
    // Deal direct damage to entities in a moderate radius
    private void dealDirectDamage(WitherSkullEntity skull, World world, Vec3d pos) {
        double radius = 8.0; // Damage radius
        Box damageBox = new Box(
            pos.x - radius, pos.y - radius, pos.z - radius,
            pos.x + radius, pos.y + radius, pos.z + radius
        );
        
        List<LivingEntity> entities = world.getEntitiesByClass(
            LivingEntity.class, 
            damageBox, 
            entity -> entity != skull.getOwner() && entity.isAlive()
        );
        
        for (LivingEntity target : entities) {
            double distance = target.getPos().distanceTo(pos);
            
            if (distance <= radius) {
                // Calculate damage based on distance (closer = more damage)
                float baseDamage = 8.0f; // Moderate base damage
                float distanceMultiplier = (float) (1.0 - (distance / radius));
                float finalDamage = baseDamage * distanceMultiplier;
                
                // Create damage source
                DamageSource damageSource = world.getDamageSources().witherSkull(skull, skull.getOwner());
                
                // Deal the damage
                target.damage(damageSource, finalDamage);
                
                // Add minimal knockback effect
                Vec3d knockback = target.getPos().subtract(pos).normalize().multiply(0.5);
                target.addVelocity(knockback.x, knockback.y + 0.1, knockback.z);
                target.velocityModified = true;
            }
        }
    }
    
    // Apply moderate wither effect to nearby entities
    private void applyModerateWitherEffect(WitherSkullEntity skull, World world, Vec3d pos) {
        double effectRadius = 6.0; // Smaller effect radius
        Box effectBox = new Box(
            pos.x - effectRadius, pos.y - effectRadius, pos.z - effectRadius,
            pos.x + effectRadius, pos.y + effectRadius, pos.z + effectRadius
        );
        
        List<LivingEntity> entities = world.getEntitiesByClass(
            LivingEntity.class,
            effectBox,
            entity -> entity != skull.getOwner() && entity.isAlive()
        );
        
        for (LivingEntity target : entities) {
            double distance = target.getPos().distanceTo(pos);
            
            if (distance <= effectRadius) {
                // Moderate wither effect - shorter duration
                int duration = (int) (300 - (distance * 20)); // 15s to 10s based on distance
                int amplifier = distance <= 3.0 ? 1 : 0; // Level 2 if very close, Level 1 if far
                
                StatusEffectInstance witherEffect = new StatusEffectInstance(
                    StatusEffects.WITHER, 
                    Math.max(duration, 120), // Minimum 6 seconds
                    amplifier,
                    false, // Not ambient
                    true,  // Show particles
                    true   // Show icon
                );
                
                target.addStatusEffect(witherEffect);
                
                // Light debuffs only for very close targets
                if (distance <= 3.0) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 0)); // 5s slowness I
                }
            }
        }
    }
}