package com.tridevmc.compound.core.registration;

import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.registries.IForgeRegistryEntry;

public interface IRegistrator<T extends IForgeRegistryEntry<T>> {

    void registerObjects(RegistryEvent.Register<T> registry);
}
