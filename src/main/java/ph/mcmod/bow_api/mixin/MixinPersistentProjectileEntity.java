package ph.mcmod.bow_api.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ph.mcmod.bow_api.ListenableArrowEntity;

@Mixin(PersistentProjectileEntity.class)
public abstract class MixinPersistentProjectileEntity {
/**
 * @param entityHitResult {@link PersistentProjectileEntity#onEntityHit(EntityHitResult)}的参数
 * @see ListenableArrowEntity#followingDamage
 */
@Redirect(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
private boolean onDamage(Entity entity, DamageSource source, float amount, EntityHitResult entityHitResult) {
	boolean success0 = entity.damage(source, amount);
	//noinspection ConstantConditions
	if ((Object) this instanceof ListenableArrowEntity a) {
		boolean success = success0 && !(entity instanceof EndermanEntity);
		for (var callback : a.followingDamage)
			callback.accept(entityHitResult, source, amount, success);
	}
	return success0;
}

/**
 * @see ListenableArrowEntity#followingHit
 */
@Inject(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/PersistentProjectileEntity;playSound(Lnet/minecraft/sound/SoundEvent;FF)V", shift = At.Shift.AFTER))
private void endIf(EntityHitResult entityHitResult, CallbackInfo ci) {
	//noinspection ConstantConditions
	if ((Object) this instanceof ListenableArrowEntity a) {
		for (var callback : a.afterHit)
			callback.accept(a, entityHitResult);
	}
}
}
