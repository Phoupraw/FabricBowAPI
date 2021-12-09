package ph.mcmod.bow_api;

import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class SpawnerArrowEntity extends ListenableArrowEntity {
public static final Identifier ID = new Identifier(Main.NAMESPACE, "spawner_arrow");
public static final EntityType<SpawnerArrowEntity> ENTITY_TYPE = Main.register(ID, SpawnerArrowEntity::new);
public static final String KEY = "entities";
public final List<EntityType<?>> entities = new LinkedList<>();

public SpawnerArrowEntity(World world) {
	this(ENTITY_TYPE,world);
}
protected SpawnerArrowEntity(EntityType<? extends SpawnerArrowEntity> entityType, World world) {
	super(entityType, world);
	followingHit.add((this0, hitResult) -> {
		for (var type : entities)
			this0.world.spawnEntity(type.create(this0.world));
	});
}

public SpawnerArrowEntity add(EntityType<?> entityType) {
	entities.add(entityType);
	return this;
}

@Override
public void writeCustomDataToNbt(NbtCompound nbt) {
	super.writeCustomDataToNbt(nbt);
	nbt.put(KEY, entities.stream().map(entry -> NbtString.of(Objects.toString(Registry.ENTITY_TYPE.getId(entry)))).collect(NbtList::new, NbtList::add, NbtList::addAll));
}

@Override
public void readCustomDataFromNbt(NbtCompound nbt) {
	super.readCustomDataFromNbt(nbt);
	entities.clear();
	entities.addAll(nbt.getList(KEY, NbtElement.STRING_TYPE).stream().map(nbtElement -> Registry.ENTITY_TYPE.get(new Identifier(nbtElement.toString()))).toList());
}

}
