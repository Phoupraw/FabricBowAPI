package ph.mcmod.bow_api;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

import java.util.function.BiFunction;
import java.util.function.Function;

public class SpawnerArrowEntity extends ListenableArrowEntity {
public final LimitedCollection<BiFunction<SpawnerArrowEntity, HitResult, Entity>> entities = new LimitedLinkedLit<>();

public SpawnerArrowEntity(EntityType<? extends ArrowEntity> entityType, World world) {
	super(entityType, world);
	followingHit.add((this0, hitResult) -> {
		for (var spawner : entities)
			this0.world.spawnEntity(spawner.apply((SpawnerArrowEntity) this0, hitResult));
	});
}

public SpawnerArrowEntity add(EntityType<?> entityType) {
	add(entityType::create);
	return this;
}

public SpawnerArrowEntity add(Function<World, ? extends Entity> create) {
	entities.add((spawnerArrowEntity, hitResult) -> {
		var entity = create.apply(spawnerArrowEntity.world);
		entity.setPosition(hitResult.getPos());
		return entity;
	});
	return this;
}
}
