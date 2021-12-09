package ph.mcmod.bow_api;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public final class Main {
public static final String NAMESPACE = "bow_api";

public static void init() {

}

public static <T extends Entity> EntityType<T> register(Identifier id, EntityType.EntityFactory<T> constructor) {
	return Registry.register(Registry.ENTITY_TYPE, id, EntityType.Builder.create(constructor, SpawnGroup.MISC).setDimensions(0.5F, 0.5F).maxTrackingRange(4).trackingTickInterval(20).build(id.toString()));
}
}
