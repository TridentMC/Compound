package com.tridevmc.compound.config;

import com.google.common.collect.Maps;
import com.tridevmc.compound.core.reflect.WrappedField;
import net.minecraft.util.Tuple;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class ConfigField {

    private enum EnumFieldType {
        INTEGER,
        LONG,
        DOUBLE,
        BOOLEAN,
        ENUM,
        OBJECT,
        LIST
    }

    private static final Map<Class, EnumFieldType> TYPE_MAP = Maps.newHashMap();

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
    private final WrappedField field;
    private final Class fieldType;
    private final CompoundConfig config;
    private final EnumFieldType type;
    private ForgeConfigSpec.ConfigValue value;

    private final Object defaultValue;
    private final Object minValue, maxValue;

    private final String name;
    private final String comment;
    private final String langKey;

    private final boolean requiresWorldRestart;

    private final boolean isValueArray;

    protected ConfigField(CompoundConfig config, @Nonnull WrappedField field) {
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

        Class fieldType = field.getType();
        if (fieldType.isArray()) {
            fieldType = fieldType.getComponentType();
        } else if (List.class.isAssignableFrom(fieldType)) {
            fieldType = (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
        }

        this.fieldType = fieldType;
        this.defaultValue = this.genDefaultValue();
        Tuple<Object, Object> rangeData = this.getRangeData();
        this.minValue = rangeData.getA();
        this.maxValue = rangeData.getB();
    }

    private Tuple<Object, Object> getRangeData() {
        if (this.field.isAnnotationPresent(RangedInt.class)) {
            RangedInt annotation = (RangedInt) this.field.getAnnotation(RangedInt.class);
            return new Tuple<>(annotation.min(), annotation.max());
        } else if (this.field.isAnnotationPresent(RangedDouble.class)) {
            RangedDouble annotation = (RangedDouble) this.field.getAnnotation(RangedDouble.class);
            return new Tuple<>(annotation.min(), annotation.max());
        } else if (this.fieldType.isAnnotationPresent(RangedLong.class)) {
            RangedLong annotation = (RangedLong) this.field.getAnnotation(RangedLong.class);
            return new Tuple<>(annotation.min(), annotation.max());
        } else {
            switch (this.type) {
                case INTEGER:
                    return new Tuple<>(Integer.MIN_VALUE, Integer.MAX_VALUE);
                case DOUBLE:
                    return new Tuple<>(Double.MIN_VALUE, Double.MAX_VALUE);
                default:
                    return new Tuple<>(Long.MIN_VALUE, Long.MAX_VALUE);
            }
        }
    }

    private Object genDefaultValue() {
        Object value = field.getValue(config.getConfigInstance(), true);

        Class fieldType = field.getType();
        if (fieldType.isArray()) {
            value = toObject(value);
        }

        return value;
    }

    public void addToSpec(ForgeConfigSpec.Builder builder) {
        builder = builder.comment(this.getComment()).translation(this.getLangKey());
        if (this.requiresWorldRestart())
            builder = builder.worldRestart();

        switch (this.type) {
            case INTEGER:
                this.value = builder.defineInRange(this.getName(),
                        (int) this.getDefaultValue(),
                        (int) this.minValue,
                        (int) this.maxValue);
                break;
            case LONG:
                this.value = builder.defineInRange(this.getName(),
                        (long) this.getDefaultValue(),
                        (long) this.minValue,
                        (long) this.maxValue);
                break;
            case DOUBLE:
                this.value = builder.defineInRange(this.getName(),
                        (double) this.getDefaultValue(),
                        (double) this.minValue,
                        (double) this.maxValue);
                break;
            case BOOLEAN:
                this.value = builder.define(this.getName(), (boolean) this.getDefaultValue());
                break;
            case ENUM:
                this.value = builder.defineEnum(this.getName(), (Enum) this.getDefaultValue());
                break;
            case LIST:
                this.value = builder.defineList(this.getName(), (List<?>) this.getDefaultValue(), (o) -> true);
                break;
            case OBJECT:
                this.value = builder.define(this.getName(), this.getDefaultValue());
                break;
        }
    }

    public void loadField() {
        if (this.isValueArray()) {
            // Value arrays are always stored as a list internally, we adjust the value to match the field type later.
            this.setListValue((List) this.value.get());
        } else {
            this.getField().setValue(this.config.getConfigInstance(), this.value.get());
        }
    }

    private Object toPrimitive(Object values) {
        if (values == null)
            return values;
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
        if (values == null)
            return values;
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
                this.getField().setValue(configInstance, toPrimitive(valueArray));
            } else {
                this.getField().setValue(configInstance, valueArray);
            }
        } else {
            List valueList = (List) getField().getValue(configInstance);
            valueList.clear();
            valueList.addAll(values);
        }
    }

    @Nonnull
    public WrappedField getField() {
        return field;
    }

    public boolean isValueArray() {
        return isValueArray;
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public String getLangKey() {
        return langKey;
    }

    public boolean requiresWorldRestart() {
        return requiresWorldRestart;
    }

}
