/*
 * Copyright 2018 - 2024 TridentMC
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

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.decoration.PaintingVariants;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

import static net.minecraft.stats.Stats.BLOCK_MINED;

public class InternalRegistryEntrySerializer<T> implements IConfigObjectSerializer<T> {

    protected static final InternalRegistryEntrySerializer<?>[] DEFAULT_SERIALIZERS = new InternalRegistryEntrySerializer[]{
            new InternalRegistryEntrySerializer<>(Block.class, Registries.BLOCK, Blocks.AIR),
            new InternalRegistryEntrySerializer<>(Fluid.class, Registries.FLUID, Fluids.EMPTY),
            new InternalRegistryEntrySerializer<>(Item.class, Registries.ITEM, Items.AIR),
            new InternalRegistryEntrySerializer<>(MobEffect.class, Registries.MOB_EFFECT, MobEffects.JUMP),
            new InternalRegistryEntrySerializer<>(Potion.class, Registries.POTION, Potions.AWKWARD),
            new InternalRegistryEntrySerializer<>(Attribute.class, Registries.ATTRIBUTE, Attributes.ARMOR),
            new InternalRegistryEntrySerializer<>(StatType.class, Registries.STAT_TYPE, BLOCK_MINED),
            new InternalRegistryEntrySerializer<>(SoundEvent.class, Registries.SOUND_EVENT, SoundEvents.EMPTY),
            new InternalRegistryEntrySerializer<>(Enchantment.class, Registries.ENCHANTMENT, Enchantments.UNBREAKING),
            new InternalRegistryEntrySerializer<>(EntityType.class, Registries.ENTITY_TYPE, EntityType.AREA_EFFECT_CLOUD),
            new InternalRegistryEntrySerializer<>(PaintingVariant.class, Registries.PAINTING_VARIANT, PaintingVariants.BUST),
            new InternalRegistryEntrySerializer<>(ParticleType.class, Registries.PARTICLE_TYPE, ParticleTypes.BLOCK),
            new InternalRegistryEntrySerializer<>(MenuType.class, Registries.MENU, MenuType.GENERIC_9x2),
            new InternalRegistryEntrySerializer<>(BlockEntityType.class, Registries.BLOCK_ENTITY_TYPE, BlockEntityType.FURNACE),
            new InternalRegistryEntrySerializer<>(RecipeType.class, Registries.RECIPE_TYPE, RecipeType.CRAFTING),
            new InternalRegistryEntrySerializer<>(RecipeSerializer.class, Registries.RECIPE_SERIALIZER, RecipeSerializer.SHAPED_RECIPE),
            new InternalRegistryEntrySerializer<>(VillagerProfession.class, Registries.VILLAGER_PROFESSION, VillagerProfession.FARMER),
            new InternalRegistryEntrySerializer<>(PoiType.class, Registries.POINT_OF_INTEREST_TYPE, PoiTypes.FARMER),
            new InternalRegistryEntrySerializer<>(MemoryModuleType.class, Registries.MEMORY_MODULE_TYPE, MemoryModuleType.BREED_TARGET),
            new InternalRegistryEntrySerializer<>(SensorType.class, Registries.SENSOR_TYPE, SensorType.DUMMY),
            new InternalRegistryEntrySerializer<>(Schedule.class, Registries.SCHEDULE, Schedule.EMPTY),
            new InternalRegistryEntrySerializer<>(Activity.class, Registries.ACTIVITY, Activity.AVOID),
            new InternalRegistryEntrySerializer<>(WorldCarver.class, Registries.CARVER, WorldCarver.CAVE),
            new InternalRegistryEntrySerializer<>(Feature.class, Registries.FEATURE, Feature.NO_OP),
            new InternalRegistryEntrySerializer<>(ChunkStatus.class, Registries.CHUNK_STATUS, ChunkStatus.EMPTY),
            new InternalRegistryEntrySerializer<>(BlockStateProviderType.class, Registries.BLOCK_STATE_PROVIDER_TYPE, BlockStateProviderType.NOISE_PROVIDER),
            new InternalRegistryEntrySerializer<>(FoliagePlacerType.class, Registries.FOLIAGE_PLACER_TYPE, FoliagePlacerType.BLOB_FOLIAGE_PLACER),
            new InternalRegistryEntrySerializer<>(TreeDecoratorType.class, Registries.TREE_DECORATOR_TYPE, TreeDecoratorType.BEEHIVE),
            new InternalRegistryEntrySerializer<>(Biome.class, Registries.BIOME, Biomes.BEACH),
            new InternalRegistryEntrySerializer<FluidType>(FluidType.class, NeoForgeRegistries.Keys.FLUID_TYPES, Fluids.WATER::getFluidType),
    };

    private final Class<?> registryType;
    private final ResourceKey<Registry<T>> registryKey;
    private Registry<T> registry;
    private Supplier<T> defaultValue;

    public InternalRegistryEntrySerializer(Class<?> registryType, ResourceKey<Registry<T>> registryKey, T defaultValue) {
        this.registryType = registryType;
        this.registryKey = registryKey;
        this.defaultValue = () -> defaultValue;
    }

    public InternalRegistryEntrySerializer(Class<?> registryType, ResourceKey<Registry<T>> registryKey, Supplier<T> defaultValue) {
        this.registryType = registryType;
        this.registryKey = registryKey;
        this.defaultValue = () -> null;
    }

    public InternalRegistryEntrySerializer(Class<?> registry, ResourceKey<Registry<T>> registryKey, Holder<T> defaultValue) {
        this.registryType = registry;
        this.registryKey = registryKey;
        this.defaultValue = defaultValue::value;
    }

    public InternalRegistryEntrySerializer(Class<?> registry, ResourceKey<Registry<T>> registryKey, ResourceKey<T> defaultValue) {
        this.registryType = registry;
        this.registryKey = registryKey;
        this.defaultValue = () -> this.getRegistry().get(defaultValue);
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
        return this.getRegistry().get(ResourceLocation.parse(value));
    }

    @Override
    public boolean accepts(Class<?> clazz) {
        return registryType.isAssignableFrom(clazz);
    }

    @Override
    public String defaultListValue(Class<T> fieldType) {
        var def = this.defaultValue.get();
        return def != null ? this.toString(fieldType, def) : null;
    }

}
