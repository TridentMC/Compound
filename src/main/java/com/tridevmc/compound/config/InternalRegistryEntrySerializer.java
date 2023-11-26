/*
 * Copyright 2018 - 2022 TridentMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tridevmc.compound.config;

import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.StatType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.StructureModifier;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.holdersets.HolderSetType;

public class InternalRegistryEntrySerializer<T> implements IConfigObjectSerializer<T> {

    protected static final InternalRegistryEntrySerializer<?>[] DEFAULT_SERIALIZERS = new InternalRegistryEntrySerializer[]{
            new InternalRegistryEntrySerializer<>(Block.class, Registries.BLOCK),
            new InternalRegistryEntrySerializer<>(Fluid.class, Registries.FLUID),
            new InternalRegistryEntrySerializer<>(Item.class, Registries.ITEM),
            new InternalRegistryEntrySerializer<>(MobEffect.class, Registries.MOB_EFFECT),
            new InternalRegistryEntrySerializer<>(Potion.class, Registries.POTION),
            new InternalRegistryEntrySerializer<>(Attribute.class, Registries.ATTRIBUTE),
            new InternalRegistryEntrySerializer<>(StatType.class, Registries.STAT_TYPE),
            new InternalRegistryEntrySerializer<>(SoundEvent.class, Registries.SOUND_EVENT),
            new InternalRegistryEntrySerializer<>(Enchantment.class, Registries.ENCHANTMENT),
            new InternalRegistryEntrySerializer<>(EntityType.class, Registries.ENTITY_TYPE),
            new InternalRegistryEntrySerializer<>(PaintingVariant.class, Registries.PAINTING_VARIANT),
            new InternalRegistryEntrySerializer<>(ParticleType.class, Registries.PARTICLE_TYPE),
            new InternalRegistryEntrySerializer<>(MenuType.class, Registries.MENU),
            new InternalRegistryEntrySerializer<>(BlockEntityType.class, Registries.BLOCK_ENTITY_TYPE),
            new InternalRegistryEntrySerializer<>(RecipeType.class, Registries.RECIPE_TYPE),
            new InternalRegistryEntrySerializer<>(RecipeSerializer.class, Registries.RECIPE_SERIALIZER),
            new InternalRegistryEntrySerializer<>(VillagerProfession.class, Registries.VILLAGER_PROFESSION),
            new InternalRegistryEntrySerializer<>(PoiType.class, Registries.POINT_OF_INTEREST_TYPE),
            new InternalRegistryEntrySerializer<>(MemoryModuleType.class, Registries.MEMORY_MODULE_TYPE),
            new InternalRegistryEntrySerializer<>(SensorType.class, Registries.SENSOR_TYPE),
            new InternalRegistryEntrySerializer<>(Schedule.class, Registries.SCHEDULE),
            new InternalRegistryEntrySerializer<>(Activity.class, Registries.ACTIVITY),
            new InternalRegistryEntrySerializer<>(WorldCarver.class, Registries.CARVER),
            new InternalRegistryEntrySerializer<>(Feature.class, Registries.FEATURE),
            new InternalRegistryEntrySerializer<>(ChunkStatus.class, Registries.CHUNK_STATUS),
            new InternalRegistryEntrySerializer<>(BlockStateProviderType.class, Registries.BLOCK_STATE_PROVIDER_TYPE),
            new InternalRegistryEntrySerializer<>(FoliagePlacerType.class, Registries.FOLIAGE_PLACER_TYPE),
            new InternalRegistryEntrySerializer<>(TreeDecoratorType.class, Registries.TREE_DECORATOR_TYPE),
            new InternalRegistryEntrySerializer<>(Biome.class, Registries.BIOME),
            new InternalRegistryEntrySerializer<>(FluidType.class, NeoForgeRegistries.Keys.FLUID_TYPES),
            new InternalRegistryEntrySerializer<>(HolderSetType.class, NeoForgeRegistries.Keys.HOLDER_SET_TYPES),
            new InternalRegistryEntrySerializer<>(BiomeModifier.class, NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS),
            new InternalRegistryEntrySerializer<>(StructureModifier.class, NeoForgeRegistries.Keys.STRUCTURE_MODIFIER_SERIALIZERS)
    };

    private final Class<?> registryType;
    private final ResourceKey<Registry<T>> registryKey;
    private Registry<T> registry;

    public InternalRegistryEntrySerializer(Class<?> registryType, ResourceKey<Registry<T>> registryKey) {
        this.registryType = registryType;
        this.registryKey = registryKey;
    }

    private Registry<T> getRegistry() {
        if (this.registry == null) {
            this.registry = (Registry<T>) BuiltInRegistries.REGISTRY.get(this.registryKey.location());
        }
        return this.registry;
    }

    @Override
    public String toString(Class<T> fieldType, T value) {
        return String.valueOf(this.getRegistry().getKey(value));
    }

    @Override
    public T fromString(Class<T> fieldType, String value) {
        return this.getRegistry().get(new ResourceLocation(value));
    }

    @Override
    public boolean accepts(Class<?> clazz) {
        return registryType.isAssignableFrom(clazz);
    }

}
