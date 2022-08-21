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

import com.google.common.collect.Maps;
import com.tridevmc.compound.core.reflect.WrappedField;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConfigField<T> {

    private static final Map<Class<?>, EnumFieldType> TYPE_MAP = Maps.newHashMap();

    static {
        TYPE_MAP.put(Integer.class, EnumFieldType.INTEGER);
        TYPE_MAP.put(int.class, EnumFieldType.INTEGER);

        TYPE_MAP.put(Long.class, EnumFieldType.LONG);
        TYPE_MAP.put(long.class, EnumFieldType.LONG);

        TYPE_MAP.put(Double.class, EnumFieldType.DOUBLE);
        TYPE_MAP.put(double.class, EnumFieldType.DOUBLE);

        TYPE_MAP.put(Boolean.class, EnumFieldType.BOOLEAN);
        TYPE_MAP.put(boolean.class, EnumFieldType.BOOLEAN);
    }

    @Nonnull
    private final WrappedField<T> field;
    private final Class<T> fieldType;
    private final IConfigFieldSerializer<T> serializer;
    private final CompoundConfig<Object> config;
    private final EnumFieldType type;
    private final Object defaultValue;
    private final Object minValue, maxValue;
    private final ResourceLocation registryName;
    private final String name;
    private final String comment;
    private final String langKey;
    private final boolean requiresWorldRestart;
    private final boolean isValueArray;
    private ForgeConfigSpec.ConfigValue<T> specValue;
    private IForgeRegistry<T> registry;

    protected ConfigField(CompoundConfig<Object> config, @Nonnull WrappedField<T> field) {
        this.field = field;
        this.config = config;
        this.type = TYPE_MAP.computeIfAbsent(field.getType(), aClass -> {
            if (aClass.isEnum()) {
                return EnumFieldType.ENUM;
            } else if (aClass.isArray() || List.class.isAssignableFrom(aClass)) {
                return EnumFieldType.LIST;
            } else {
                return EnumFieldType.OBJECT;
            }
        });

        ConfigValue configValue = (ConfigValue) field.getAnnotation(ConfigValue.class);
        this.name = configValue.name().isEmpty() ? this.getField().getName() : configValue.name();
        this.comment = configValue.comment();
        this.langKey = configValue.langKey().isEmpty() ? config.getModId() + ".compoundconfig.gui.property." + this.getName() : configValue.langKey();

        this.requiresWorldRestart = configValue.requiresWorldRestart();

        // Special case for arrays and lists as they're not the same type of object.
        this.isValueArray = field.getType().isArray() || List.class.isAssignableFrom(field.getType());

        Class<?> fieldType = field.getType();
        if (fieldType.isArray()) {
            fieldType = fieldType.getComponentType();
        } else if (List.class.isAssignableFrom(fieldType)) {
            fieldType = (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
        }

        this.fieldType = (Class<T>) fieldType;
        this.serializer = config.getSerializerFor(this);
        this.defaultValue = this.genDefaultValue();
        Tuple<Object, Object> rangeData = this.generateRangeData();
        this.minValue = rangeData.getA();
        this.maxValue = rangeData.getB();
        this.registryName = this.generateRegistryName();
    }

    private Tuple<Object, Object> generateRangeData() {
        if (this.field.isAnnotationPresent(RangedInt.class)) {
            var annotation = this.field.getAnnotation(RangedInt.class);
            return new Tuple<>(annotation.min(), annotation.max());
        } else if (this.field.isAnnotationPresent(RangedDouble.class)) {
            var annotation = this.field.getAnnotation(RangedDouble.class);
            return new Tuple<>(annotation.min(), annotation.max());
        } else if (this.fieldType.isAnnotationPresent(RangedLong.class)) {
            var annotation = this.field.getAnnotation(RangedLong.class);
            return new Tuple<>(annotation.min(), annotation.max());
        } else {
            return switch (this.type) {
                case INTEGER -> new Tuple<>(Integer.MIN_VALUE, Integer.MAX_VALUE);
                case DOUBLE -> new Tuple<>(Double.MIN_VALUE, Double.MAX_VALUE);
                default -> new Tuple<>(Long.MIN_VALUE, Long.MAX_VALUE);
            };
        }
    }

    @Nullable
    private ResourceLocation generateRegistryName() {
        if (this.field.isAnnotationPresent(RegisteredValue.class)) {
            var annotation = this.field.getAnnotation(RegisteredValue.class);
            return new ResourceLocation(annotation.value());
        }
        return null;
    }

    @Nullable
    protected IForgeRegistry<T> getRegistry() {
        if (this.registry == null && this.registryName != null) {
            this.registry = RegistryManager.ACTIVE.getRegistry(this.registryName);
        }
        return this.registry;
    }

    protected ResourceLocation getRegistryName() {
        return this.registryName;
    }

    private Object genDefaultValue() {
        Object value = this.field.get(this.config.getConfigInstance(), true);

        Class<T> fieldType = this.field.getType();
        if (fieldType.isArray()) {
            value = this.toObject(value);
        }

        if (this.serializer != null) {
            if (this.type == EnumFieldType.LIST) {
                var values = (List) value;
                value = values.stream().map((o) -> this.serializer.toString(this, (T) o)).collect(Collectors.toList());
            } else {
                value = this.serializer.toString(this, (T) value);
            }
        }

        return value;
    }

    public void addToSpec(ForgeConfigSpec.Builder builder) {
        builder = builder.comment(this.getComment()).translation(this.getLangKey());
        if (this.requiresWorldRestart()) builder = builder.worldRestart();
        switch (this.type) {
            case INTEGER ->
                    this.specValue = (ForgeConfigSpec.ConfigValue<T>) builder.defineInRange(this.getName(), (int) this.getDefaultValue(), (int) this.minValue, (int) this.maxValue);
            case LONG ->
                    this.specValue = (ForgeConfigSpec.ConfigValue<T>) builder.defineInRange(this.getName(), (long) this.getDefaultValue(), (long) this.minValue, (long) this.maxValue);
            case DOUBLE ->
                    this.specValue = (ForgeConfigSpec.ConfigValue<T>) builder.defineInRange(this.getName(), (double) this.getDefaultValue(), (double) this.minValue, (double) this.maxValue);
            case BOOLEAN ->
                    this.specValue = (ForgeConfigSpec.ConfigValue<T>) builder.define(this.getName(), (boolean) this.getDefaultValue());
            case ENUM -> this.specValue = builder.defineEnum(this.getName(), (Enum) this.getDefaultValue());
            case LIST ->
                    this.specValue = (ForgeConfigSpec.ConfigValue<T>) builder.defineList(this.getName(), (List<?>) this.getDefaultValue(), (o) -> true);
            case OBJECT ->
                    this.specValue = (ForgeConfigSpec.ConfigValue<T>) builder.define(this.getName(), this.getDefaultValue());
        }
    }

    public void loadField() {
        if (this.isValueArray()) {
            // Value arrays are always stored as a list internally, we adjust the value to match the field type later.
            var newValue = (List) this.getConfigSpecValue();
            if (this.serializer != null && newValue.stream().allMatch((o) -> o instanceof String)) {
                newValue = (List) newValue.stream().map((o) -> this.serializer.fromString(this, (String) o)).collect(Collectors.toList());
            }

            this.setListValue(newValue);
        } else {
            T newValue = this.getConfigSpecValue();
            if (this.serializer != null && newValue instanceof String) {
                newValue = this.serializer.fromString(this, (String) newValue);
            }

            this.getField().set(this.config.getConfigInstance(), newValue);
        }
    }

    private Object toPrimitive(Object values) {
        if (values == null) return null;
        Class<?> clazz = values.getClass().getComponentType();
        if (!clazz.isPrimitive()) {
            if (Boolean.class.equals(clazz)) {
                // Special case for booleans because the method in ArrayUtils doesnt have one built in.
                return ArrayUtils.toPrimitive((Boolean[]) values);
            } else {
                return ArrayUtils.toPrimitive(values);
            }
        } else {
            return values;
        }
    }

    private Object toObject(Object values) {
        if (values == null) return null;
        Class<?> clazz = values.getClass().getComponentType();
        if (clazz.isPrimitive()) {
            if (int.class.equals(clazz)) {
                values = ArrayUtils.toObject((int[]) values);
            }
            if (double.class.equals(clazz)) {
                values = ArrayUtils.toObject((double[]) values);
            }
            if (long.class.equals(clazz)) {
                values = ArrayUtils.toObject((long[]) values);
            }
            if (boolean.class.equals(clazz)) {
                values = ArrayUtils.toObject((boolean[]) values);
            }
        }

        return Arrays.asList((Object[]) values);
    }

    private void setListValue(List values) {
        Object configInstance = this.config.getConfigInstance();
        if (this.getField().getType().isArray()) {
            Object valueArray = Array.newInstance(this.getField().getType().getComponentType(), (values).size());
            IntStream.range(0, values.size()).forEach(i -> Array.set(valueArray, i, values.get(i)));

            if (this.getField().getType().getComponentType().isPrimitive()) {
                this.getField().set(configInstance, (T) this.toPrimitive(valueArray));
            } else {
                this.getField().set(configInstance, (T) valueArray);
            }
        } else {
            var valueList = (List) this.getField().get(configInstance);
            valueList.clear();
            valueList.addAll(values);
        }
    }

    private <V> V getConfigSpecValue() {
        return (V) this.specValue.get();
    }

    @Nonnull
    public WrappedField<T> getField() {
        return this.field;
    }

    public boolean isValueArray() {
        return this.isValueArray;
    }

    public Class<T> getFieldType() {
        return fieldType;
    }

    public String getName() {
        return this.name;
    }

    public String getComment() {
        return this.comment;
    }

    public Object getDefaultValue() {
        return this.defaultValue;
    }

    public String getLangKey() {
        return this.langKey;
    }

    public boolean requiresWorldRestart() {
        return this.requiresWorldRestart;
    }

    private enum EnumFieldType {
        INTEGER, LONG, DOUBLE, BOOLEAN, ENUM, OBJECT, LIST
    }

}
