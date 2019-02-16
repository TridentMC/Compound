package com.tridevmc.compound.config;

//TODO: Commented out so we can compile, not sure if any of this is salvageable.
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.gui.GuiScreen;
//import net.minecraft.client.resources.I18n;
//import net.minecraftforge.common.config.ConfigElement;
//import net.minecraftforge.common.config.Configuration;
//import net.minecraftforge.fml.client.IModGuiFactory;
//import net.minecraftforge.fml.client.config.GuiConfig;
//import net.minecraftforge.fml.client.config.IConfigElement;
//
//import java.util.Comparator;
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//public class CompoundGuiFactory implements IModGuiFactory {
//
//    private final CompoundConfig config;
//    private final String guiTitle;
//
//    public CompoundGuiFactory(Object config) {
//        this.config = CompoundConfig.KNOWN_CONFIGS.get(config);
//        this.guiTitle = I18n.format(this.config.getModId() + ".compoundconfig.gui.title");
//    }
//
//    @Override
//    public void initialize(Minecraft minecraft) {
//    }
//
//    @Override
//    public boolean hasConfigGui() {
//        return true;
//    }
//
//    @Override
//    public GuiScreen createConfigGui(GuiScreen parentScreen) {
//        return new GuiConfig(parentScreen, collectConfigElements(), this.config.getModId(), false, false, guiTitle);
//    }
//
//    private List<IConfigElement> collectConfigElements() {
//        Configuration forgeConfig = this.config.getForgeConfig();
//        Set<String> categoryNames = forgeConfig.getCategoryNames();
//        List<IConfigElement> configElements = categoryNames.stream().map(this::createElement).collect(Collectors.toList());
//        if (configElements.size() == 1) {
//            configElements = configElements.get(0).getChildElements();
//        }
//        configElements.sort(Comparator.comparing(e -> I18n.format(e.getLanguageKey())));
//        return configElements;
//    }
//
//    private ConfigElement createElement(String categoryName) {
//        return new ConfigElement(this.config.getForgeConfig().getCategory(categoryName));
//    }
//
//    @Override
//    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
//        return null;
//    }
//}
//