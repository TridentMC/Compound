package com.tridevmc.compound.config;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryManager;

public class ForgeRegistryEntrySerializer implements IConfigObjectSerializer<IForgeRegistryEntry> {

    @Override
    public String toString(Class fieldType, IForgeRegistryEntry value) {
        IForgeRegistry registry = RegistryManager.ACTIVE.getRegistry(fieldType);
        return registry.getKey(value).toString();
    }

    @Override
    public IForgeRegistryEntry fromString(Class fieldType, String value) {
        IForgeRegistry registry = RegistryManager.ACTIVE.getRegistry(fieldType);
        return registry.getValue(new ResourceLocation(value));
    }

    @Override
    public boolean accepts(Class clazz) {
        return IForgeRegistryEntry.class.isAssignableFrom(clazz);
    }
}
