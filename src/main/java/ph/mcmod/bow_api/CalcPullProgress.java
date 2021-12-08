package ph.mcmod.bow_api;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public interface CalcPullProgress {
double calcPullProgress(ItemStack bowStack, ItemStack arrowStack, LivingEntity user, int usingTicks);
}
