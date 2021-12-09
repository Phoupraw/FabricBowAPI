package ph.mcmod.bow_api;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.function.BiFunction;
import java.util.function.Function;

public class SpawnerArrowEntity extends ListenableArrowEntity {
public static final Identifier ID = new Identifier(Main.NAMESPACE, "spawner_arrow");
public static final EntityType<SpawnerArrowEntity> ENTITY_TYPE = Registry.register(Registry.ENTITY_TYPE, ID, EntityType.Builder.create(SpawnerArrowEntity::new, SpawnGroup.MISC).setDimensions(0.5F, 0.5F).maxTrackingRange(4).trackingTickInterval(20).build(ID.toString()));
public final LimitedCollection<BiFunction<SpawnerArrowEntity, HitResult, Entity>> entities = new LimitedLinkedLit<>();

public SpawnerArrowEntity(EntityType<? extends SpawnerArrowEntity> entityType, World world) {
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
