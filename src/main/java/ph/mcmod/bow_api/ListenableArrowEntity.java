package ph.mcmod.bow_api;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ph.mcmod.bow_api.Serialization.FBiConsumer;
import ph.mcmod.bow_api.Serialization.FConsumer;
import ph.mcmod.bow_api.mixin.MixinPersistentProjectileEntity;
import ph.mcmod.bow_api.mixin.PersistentProjectileEntityAccessor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class ListenableArrowEntity extends ArrowEntity {
@FunctionalInterface
public interface AfterEntityDamage {
	void accept(EntityHitResult entityHitResult, DamageSource source, float amount, boolean success);
}

/**
 * 在{@link PersistentProjectileEntity#onEntityHit(EntityHitResult)}里调用{@link Entity#damage(DamageSource, float)}后紧跟着调用
 *
 * @see MixinPersistentProjectileEntity#onDamage(Entity, DamageSource, float, EntityHitResult)
 */
public final List<AfterEntityDamage> followingDamage = new LinkedList<>();
/**
 * 在击中方块或伤害实体之前调用<br>
 * 具体来说，是在{@link #onBlockHit(BlockHitResult)}的开头和与{@link #followingDamage}相同的位置调用
 */
public final List<BiConsumer<ListenableArrowEntity, HitResult>> followingHit = new LinkedList<>();
/**
 * 在击中方块或伤害实体之后调用<br>
 * 具体来说，是在{@link #onBlockHit(BlockHitResult)}的末尾和{@link PersistentProjectileEntity#onEntityHit(EntityHitResult)}里调用{@link PersistentProjectileEntity#playSound(SoundEvent, float, float)}后调用
 *
 * @see MixinPersistentProjectileEntity#endIf(EntityHitResult, CallbackInfo)
 */
public final List<BiConsumer<ListenableArrowEntity, HitResult>> afterHit = new LinkedList<>();

public ListenableArrowEntity(EntityType<? extends ListenableArrowEntity> entityType, World world) {
	super(entityType, world);
	followingDamage.add((entityHitResult, source, amount, success) -> {
		if (success) {
			for (BiConsumer<ListenableArrowEntity, HitResult> callback : followingHit)
				callback.accept(this, entityHitResult);
		}
	});
}

@Override
public void onBlockHit(BlockHitResult blockHitResult) {
	for (BiConsumer<ListenableArrowEntity, HitResult> callback : followingHit)
		callback.accept(this, blockHitResult);
	super.onBlockHit(blockHitResult);
	for (BiConsumer<ListenableArrowEntity, HitResult> callback : afterHit)
		callback.accept(this, blockHitResult);
}
public ListenableArrowEntity setDiscard() {
	((PersistentProjectileEntityAccessor)this).setLife(1200);;
	return this;
}
}
