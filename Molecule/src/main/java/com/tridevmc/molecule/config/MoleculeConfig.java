package com.tridevmc.molecule.config;

import com.google.common.collect.Lists;
import com.tridevmc.compound.config.ConfigType;
import com.tridevmc.compound.config.ConfigValue;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ConfigType(ModConfig.Type.COMMON)
public class MoleculeConfig {

    @ConfigValue(comment = "Test Integer")
    public int testInt = 32;

    @ConfigValue(comment = "Test Double")
    public double testDouble = 32.32D;

    @ConfigValue(comment = "Test Boolean")
    public boolean testBoolean = false;

    @ConfigValue(comment = "Test String")
    public String testString = "Thirty-Two";

    @ConfigValue(comment = "Test Block")
    public Block testBlock = Blocks.STONE;

    @ConfigValue(comment = "Test Integer Array")
    public int[] testIntArray = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    @ConfigValue(comment = "Test Double Array")
    public double[] testDoubleArray = new double[]{0.01D, 0.02D, 0.03D, 0.04D, 0.05D, 0.06D, 0.07D, 0.08D, 0.09D, 0.1D};

    @ConfigValue(comment = "Test Boolean Array")
    public boolean[] testBooleanArray = new boolean[]{true, false, true, false};

    @ConfigValue(comment = "Test String Array")
    public String[] testStringArray = new String[]{"One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten"};

    @ConfigValue(comment = "Test Block Array")
    public Block[] testBlockArray = new Block[]{Blocks.DIRT, Blocks.STONE, Blocks.OAK_PLANKS};

    @ConfigValue(comment = "Test Integer List")
    public List<Integer> testIntList = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

    @ConfigValue(comment = "Test Double List")
    public List<Double> testDoubleList = new ArrayList<>(Arrays.asList(0.01D, 0.02D, 0.03D, 0.04D, 0.05D, 0.06D, 0.07D, 0.08D, 0.09D, 0.1D));

    @ConfigValue(comment = "Test Boolean List")
    public List<Boolean> testBooleanList = new ArrayList<>(Arrays.asList(true, false, true, false));

    @ConfigValue(comment = "Test String List")
    public List<String> testStringList = new ArrayList<>(Arrays.asList("One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten"));

    @ConfigValue(comment = "Test Block List")
    public List<Block> testBlockList = Lists.newArrayList(Blocks.DIRT, Blocks.STONE, Blocks.OAK_PLANKS);

}
