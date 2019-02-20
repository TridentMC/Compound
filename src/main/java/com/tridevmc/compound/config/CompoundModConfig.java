package com.tridevmc.compound.config;

import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.config.ModConfig;

public class CompoundModConfig extends ModConfig {

    private CompoundConfig parentConfig;

    protected CompoundModConfig(CompoundConfig config, ModContainer activeContainer, String fileName) {
        super(config.getConfigType(), config.getForgeConfig(), activeContainer, fileName);
        this.parentConfig = config;
        activeContainer.addConfig(this);
    }

    @Override
    public void save() {
        super.save();

        this.parentConfig.loadFields();
    }
}
