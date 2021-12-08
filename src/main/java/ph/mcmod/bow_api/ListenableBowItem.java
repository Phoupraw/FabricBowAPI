package ph.mcmod.bow_api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import ph.mcmod.bow_api.mixin.PersistentProjectileEntityAccessor;


public class ListenableBowItem extends BowItem implements RenderedAsBow, CalcPullProgress {
protected final double damageAddend;
protected final double damageFactor;
protected final double pullSpeed;
protected final double velocityAddend;
protected final double velocityFactor;
protected final boolean arrowUnrecyclable;

public ListenableBowItem(@NotNull BowSettings settings) {
	super(settings);
	damageAddend = settings.getDamageAddend();
	damageFactor = settings.getDamageFactor();
	pullSpeed = settings.getPullSpeed();
	velocityAddend = settings.getVelocityAddend();
	velocityFactor = settings.getVelocityFactor();
	arrowUnrecyclable = settings.isArrowUnrecyclable();
}

@Override
public void onStoppedUsing(@NotNull ItemStack bowStack, @NotNull World world, @NotNull LivingEntity user, int remainingUseTicks) {
	if (world.isClient())
		return;
	int usingTicks = getMaxUseTime(bowStack) - remainingUseTicks;
	ItemStack arrowStack = calcArrowStack(bowStack, world, user, usingTicks);
	if (arrowStack.isEmpty())
		return;
	double pullProgress = calcPullProgress(bowStack, arrowStack, user, usingTicks);
	if (Double.isNaN(pullProgress))
		return;
	var arrow = calcArrowEntity(bowStack, arrowStack, world, user, pullProgress);
	arrow.setProperties(user, user.getPitch(), user.getYaw(), 0.0F, (float) (pullProgress * 3), 1);
	arrow.setDamage(arrow.getDamage() + getDamageAddend());
	arrow.setVelocity(arrow.getVelocity().add(arrow.getVelocity().normalize().multiply(pullProgress * getVelocityAddend())));
	if (pullProgress >= 1)
		arrow.setCritical(true);
	int power = EnchantmentHelper.getLevel(Enchantments.POWER, bowStack);
	if (power > 0)
		arrow.setDamage(arrow.getDamage() + power * 0.5 + 0.5);
	int punch = EnchantmentHelper.getLevel(Enchantments.PUNCH, bowStack);
	if (punch > 0)
		arrow.setPunch(punch);
	int flame = EnchantmentHelper.getLevel(Enchantments.FLAME, bowStack);
	if (flame > 0)
		arrow.setOnFireFor(100 * flame);
	bowStack.damage(1, user, living -> living.sendToolBreakStatus(user.getActiveHand()));
	if (user instanceof PlayerEntity player) {
		player.incrementStat(Stats.USED.getOrCreateStat(this));
		if (calcInfinity(bowStack, arrowStack, world, player, pullProgress))
			arrow.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
		else
			arrowStack.decrement(1);
	}
	arrow.setDamage(arrow.getDamage() * getDamageFactor());
	arrow.setVelocity(arrow.getVelocity().multiply(getVelocityFactor()));
	if (isArrowUnrecyclable())
		((PersistentProjectileEntityAccessor) arrow).setLife(Integer.MAX_VALUE);
	world.spawnEntity(arrow);
	world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0F, (float) (1.0F / (world.getRandom().nextFloat() * 0.4F + 1.2F) + pullProgress * 0.5F));
}

/**
 * @see BowSettings#setDamageAddend(double)
 */
public double getDamageAddend() {
	return damageAddend;
}

/**
 * @see BowSettings#setDamageFactor(double)
 */
public double getDamageFactor() {
	return damageFactor;
}

/**
 * @see BowSettings#setPullSpeed(double)
 */
public double getPullSpeed() {
	return pullSpeed;
}

/**
 * @see BowSettings#setVelocityAddend(double)
 */
public double getVelocityAddend() {
	return velocityAddend;
}

/**
 * @see BowSettings#setVelocityFactor(double)
 */
public double getVelocityFactor() {
	return velocityFactor;
}

/**
 * @see BowSettings#setArrowUnrecyclable(boolean)
 */
public boolean isArrowUnrecyclable() {
	return arrowUnrecyclable;
}

/**
 * 计算拉弓进度
 *
 * @param bowStack   弓
 * @param arrowStack 箭
 * @param user       拉弓的生物
 * @param usingTicks 已经拉了多久的弓，单位是游戏刻
 * @return 如果大于1，则箭会暴击；如果是{@link Double#NaN}，则视为拉弓失败，箭不会射出
 */
public double calcPullProgress(ItemStack bowStack, ItemStack arrowStack, LivingEntity user, int usingTicks) {
	return getPullProgress((int) (usingTicks * getPullSpeed()));
}

/**
 * 乘以了{@link #getPullSpeed()}
 */
@Override
public double calcPullProgress(ItemStack bow, AbstractClientPlayerEntity player, int usingTicks) {
	return RenderedAsBow.super.calcPullProgress(bow, player, (int) (usingTicks * getPullSpeed()));
}

/**
 * 计算是否无限
 *
 * @param bowStack     弓
 * @param arrowStack   箭
 * @param world        玩家所处的世界
 * @param player       玩家
 * @param pullProgress 拉弓进度，见{@link #calcPullProgress(ItemStack, ItemStack, LivingEntity, int)}
 * @return 如果为 {@code true}，则箭{@code arrowStack}不会消耗
 */
@SuppressWarnings("unused")
public boolean calcInfinity(ItemStack bowStack, ItemStack arrowStack, World world, PlayerEntity player, double pullProgress) {
	return player.isCreative() || EnchantmentHelper.getLevel(Enchantments.INFINITY, bowStack) >= 1 && arrowStack.isOf(Items.ARROW);
}

/**
 * 计算要使用的箭。<br>
 * 默认情况下，当玩家身上有好几种箭时，该方法优先返回副手的、或者靠近物品栏左下角的箭。
 *
 * @param bowStack   弓
 * @param world      玩家所处的世界
 * @param user       玩家
 * @param usingTicks 已经拉了多久的弓，单位是游戏刻
 * @return 要使用的箭。请直接返回原物品的引用而不是复制（{@link ItemStack#copy()}）一个新的物品，因为之后如果要消耗箭，就是直接调用这个引用的{@link ItemStack#decrement(int)}
 */
@SuppressWarnings("unused")
public ItemStack calcArrowStack(ItemStack bowStack, World world, LivingEntity user, int usingTicks) {
	ItemStack arrowStack;
	if (user instanceof PlayerEntity player) {
		arrowStack = player.getArrowType(bowStack);
		if (arrowStack.isEmpty() && player.isCreative())
			arrowStack = Items.ARROW.getDefaultStack();
	} else {
		arrowStack = user.getStackInHand(Hand.MAIN_HAND);
		if (arrowStack.isEmpty())
			arrowStack = user.getStackInHand(Hand.OFF_HAND);
	}
	return arrowStack;
}

/**
 * 计算要生成的箭实体。<br>
 * 默认情况下，如果使用的是普通箭或者药箭，那么返回{@link ArrowEntity}；如果使用的是光灵箭，那么返回{@link SpectralArrowEntity}。
 *
 * @param bowStack     弓
 * @param arrowStack   箭
 * @param world        世界
 * @param user         射箭的生物
 * @param pullProgress 拉弓进度，见{@link #calcPullProgress(ItemStack, ItemStack, LivingEntity, int)}
 * @return 要生成的箭实体
 */
@SuppressWarnings("unused")
public PersistentProjectileEntity calcArrowEntity(ItemStack bowStack, ItemStack arrowStack, World world, LivingEntity user, double pullProgress) {
	ArrowItem arrowItem = arrowStack.getItem() instanceof ArrowItem a ? a : (ArrowItem) Items.ARROW;
	return arrowItem.createArrow(world, arrowStack, user);
}

@Override
public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
	super.usageTick(world, user, stack, remainingUseTicks);
}
}
