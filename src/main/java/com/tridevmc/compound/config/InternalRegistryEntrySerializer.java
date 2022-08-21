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
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.StructureModifier;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import net.minecraftforge.registries.holdersets.HolderSetType;

public class InternalRegistryEntrySerializer<T> implements IConfigObjectSerializer<T> {

    protected static final InternalRegistryEntrySerializer<?>[] DEFAULT_SERIALIZERS = new InternalRegistryEntrySerializer[]{
            new InternalRegistryEntrySerializer<>(Block.class, ForgeRegistries.Keys.BLOCKS),
            new InternalRegistryEntrySerializer<>(Fluid.class, ForgeRegistries.Keys.FLUIDS),
            new InternalRegistryEntrySerializer<>(Item.class, ForgeRegistries.Keys.ITEMS),
            new InternalRegistryEntrySerializer<>(MobEffect.class, ForgeRegistries.Keys.MOB_EFFECTS),
            new InternalRegistryEntrySerializer<>(Potion.class, ForgeRegistries.Keys.POTIONS),
            new InternalRegistryEntrySerializer<>(Attribute.class, ForgeRegistries.Keys.ATTRIBUTES),
            new InternalRegistryEntrySerializer<>(StatType.class, ForgeRegistries.Keys.STAT_TYPES),
            new InternalRegistryEntrySerializer<>(SoundEvent.class, ForgeRegistries.Keys.SOUND_EVENTS),
            new InternalRegistryEntrySerializer<>(Enchantment.class, ForgeRegistries.Keys.ENCHANTMENTS),
            new InternalRegistryEntrySerializer<>(EntityType.class, ForgeRegistries.Keys.ENTITY_TYPES),
            new InternalRegistryEntrySerializer<>(PaintingVariant.class, ForgeRegistries.Keys.PAINTING_VARIANTS),
            new InternalRegistryEntrySerializer<>(ParticleType.class, ForgeRegistries.Keys.PARTICLE_TYPES),
            new InternalRegistryEntrySerializer<>(MenuType.class, ForgeRegistries.Keys.MENU_TYPES),
            new InternalRegistryEntrySerializer<>(BlockEntityType.class, ForgeRegistries.Keys.BLOCK_ENTITY_TYPES),
            new InternalRegistryEntrySerializer<>(RecipeType.class, ForgeRegistries.Keys.RECIPE_TYPES),
            new InternalRegistryEntrySerializer<>(RecipeSerializer.class, ForgeRegistries.Keys.RECIPE_SERIALIZERS),
            new InternalRegistryEntrySerializer<>(VillagerProfession.class, ForgeRegistries.Keys.VILLAGER_PROFESSIONS),
            new InternalRegistryEntrySerializer<>(PoiType.class, ForgeRegistries.Keys.POI_TYPES),
            new InternalRegistryEntrySerializer<>(MemoryModuleType.class, ForgeRegistries.Keys.MEMORY_MODULE_TYPES),
            new InternalRegistryEntrySerializer<>(SensorType.class, ForgeRegistries.Keys.SENSOR_TYPES),
            new InternalRegistryEntrySerializer<>(Schedule.class, ForgeRegistries.Keys.SCHEDULES),
            new InternalRegistryEntrySerializer<>(Activity.class, ForgeRegistries.Keys.ACTIVITIES),
            new InternalRegistryEntrySerializer<>(WorldCarver.class, ForgeRegistries.Keys.WORLD_CARVERS),
            new InternalRegistryEntrySerializer<>(Feature.class, ForgeRegistries.Keys.FEATURES),
            new InternalRegistryEntrySerializer<>(ChunkStatus.class, ForgeRegistries.Keys.CHUNK_STATUS),
            new InternalRegistryEntrySerializer<>(BlockStateProviderType.class, ForgeRegistries.Keys.BLOCK_STATE_PROVIDER_TYPES),
            new InternalRegistryEntrySerializer<>(FoliagePlacerType.class, ForgeRegistries.Keys.FOLIAGE_PLACER_TYPES),
            new InternalRegistryEntrySerializer<>(TreeDecoratorType.class, ForgeRegistries.Keys.TREE_DECORATOR_TYPES),
            new InternalRegistryEntrySerializer<>(Biome.class, ForgeRegistries.Keys.BIOMES),
            new InternalRegistryEntrySerializer<>(FluidType.class, ForgeRegistries.Keys.FLUID_TYPES),
            new InternalRegistryEntrySerializer<>(HolderSetType.class, ForgeRegistries.Keys.HOLDER_SET_TYPES),
            new InternalRegistryEntrySerializer<>(BiomeModifier.class, ForgeRegistries.Keys.BIOME_MODIFIERS),
            new InternalRegistryEntrySerializer<>(StructureModifier.class, ForgeRegistries.Keys.STRUCTURE_MODIFIERS)
    };

    private final Class<?> registryType;
    private final ResourceKey<Registry<T>> registryKey;
    private IForgeRegistry<T> registry;

    public InternalRegistryEntrySerializer(Class<?> registryType, ResourceKey<Registry<T>> registryKey) {
        this.registryType = registryType;
        this.registryKey = registryKey;
    }

    private IForgeRegistry<T> getRegistry() {
        if (this.registry == null) {
            this.registry = RegistryManager.ACTIVE.getRegistry(this.registryKey);
        }
        return this.registry;
    }

    @Override
    public String toString(Class<T> fieldType, T value) {
        return String.valueOf(this.getRegistry().getKey(value));
    }

    @Override
    public T fromString(Class<T> fieldType, String value) {
        return this.getRegistry().getValue(new ResourceLocation(value));
    }

    @Override
    public boolean accepts(Class<?> clazz) {
        return registryType.isAssignableFrom(clazz);
    }
}
