package com.tridevmc.compound.config;

// TODO: Commented out so we can compile, not sure if any of this is salvageable.
//import com.google.common.collect.Maps;
//import com.tridevmc.compound.core.reflect.WrappedField;
//import net.minecraftforge.common.config.Configuration;
//import net.minecraftforge.common.config.Property;
//import org.apache.commons.lang3.ArrayUtils;
//
//import javax.annotation.Nonnull;
//import java.lang.reflect.Array;
//import java.lang.reflect.ParameterizedType;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//
//public class ConfigField {
//
//    private static final Map<Class, Property.Type> propertyMap = Maps.newHashMap();
//
//    static {
//        propertyMap.put(Integer.class, Property.Type.INTEGER);
//        propertyMap.put(int.class, Property.Type.INTEGER);
//
//        propertyMap.put(Double.class, Property.Type.DOUBLE);
//        propertyMap.put(double.class, Property.Type.DOUBLE);
//
//        propertyMap.put(Boolean.class, Property.Type.BOOLEAN);
//        propertyMap.put(boolean.class, Property.Type.BOOLEAN);
//
//        propertyMap.put(String.class, Property.Type.STRING);
//    }
//
//    @Nonnull
//    private final WrappedField field;
//    private final Class fieldType;
//    private final CompoundConfig config;
//
//    private final Object defaultValue;
//
//    private final String name;
//    private final String comment;
//    private final String langKey;
//    private final String category;
//
//    private final boolean requiresGameRestart;
//    private final boolean requiresWorldRestart;
//    private final boolean visibleInGui;
//
//    private final Property.Type type;
//    private final boolean isValueArray;
//
//    protected ConfigField(CompoundConfig config, @Nonnull WrappedField field) {
//        this.field = field;
//        this.config = config;
//
//        ConfigValue configValue = (ConfigValue) field.getAnnotation(ConfigValue.class);
//        this.name = configValue.name().isEmpty() ? this.getField().getName() : configValue.name();
//        this.comment = configValue.comment();
//        this.langKey = configValue.langKey().isEmpty() ? config.getModId() + ".compoundconfig.gui.property." + this.getName() : configValue.langKey();
//        this.category = configValue.category().isEmpty() ? "general" : configValue.category();
//
//        this.requiresGameRestart = configValue.requiresGameRestart();
//        this.requiresWorldRestart = configValue.requiresWorldRestart();
//        this.visibleInGui = configValue.visibleInGui();
//
//        // Special case for arrays and lists as they're not the same type of object.
//        this.isValueArray = field.getType().isArray() || List.class.isAssignableFrom(field.getType());
//
//        Class fieldType = field.getType();
//        if (fieldType.isArray()) {
//            fieldType = fieldType.getComponentType();
//        } else if (ArrayList.class.isAssignableFrom(fieldType)) {
//            fieldType = (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
//        }
//
//        this.fieldType = fieldType;
//        this.type = propertyMap.get(fieldType);
//
//        if (this.type == null) {
//            throw new IllegalArgumentException("Unable to create config field from field type " + fieldType.getName());
//        }
//
//        this.defaultValue = this.genDefaultValue();
//    }
//
//    private Object genDefaultValue() {
//        Object value = field.getValue(config.getConfigInstance(), true);
//
//        Class fieldType = field.getType();
//        if (fieldType.isArray()) {
//            value = toPrimitive(value);
//        } else if (ArrayList.class.isAssignableFrom(fieldType)) {
//            List valueList = (List) value;
//            value = toPrimitive(valueList.toArray((Object[]) Array.newInstance(this.fieldType, valueList.size())));
//        }
//
//        return value;
//    }
//
//    public void loadFromProperty() {
//        Object configInstance = config.getConfigInstance();
//        Configuration forgeConfig = config.getForgeConfig();
//        Property property = null;
//
//        if (!this.isValueArray()) {
//            switch (this.getType()) {
//                case INTEGER: {
//                    property = forgeConfig.get(this.getCategory(), this.getName(), (Integer) getDefaultValue(), this.getComment());
//                    getField().setValue(configInstance, property.getInt(), true);
//                    break;
//                }
//                case BOOLEAN: {
//                    property = forgeConfig.get(this.getCategory(), this.getName(), (Boolean) getDefaultValue(), this.getComment());
//                    getField().setValue(configInstance, property.getBoolean());
//                    break;
//                }
//                case DOUBLE: {
//                    property = forgeConfig.get(this.getCategory(), this.getName(), (Double) getDefaultValue(), this.getComment());
//                    getField().setValue(configInstance, property.getDouble());
//                    break;
//                }
//                case STRING: {
//                    property = forgeConfig.get(this.getCategory(), this.getName(), (String) getDefaultValue(), this.getComment());
//                    getField().setValue(configInstance, property.getString());
//                    break;
//                }
//            }
//        } else {
//            switch (this.getType()) {
//                case INTEGER: {
//                    property = forgeConfig.get(this.getCategory(), this.getName(), (int[]) getDefaultValue(), this.getComment());
//                    this.setListValue(property.getIntList());
//                    break;
//                }
//                case BOOLEAN: {
//                    property = forgeConfig.get(this.getCategory(), this.getName(), (boolean[]) getDefaultValue(), this.getComment());
//                    this.setListValue(property.getBooleanList());
//                    break;
//                }
//                case DOUBLE: {
//                    property = forgeConfig.get(this.getCategory(), this.getName(), (double[]) getDefaultValue(), this.getComment());
//                    this.setListValue(property.getDoubleList());
//                    break;
//                }
//                case STRING: {
//                    property = forgeConfig.get(this.getCategory(), this.getName(), (String[]) getDefaultValue(), this.getComment());
//                    this.setListValue(property.getStringList());
//                    break;
//                }
//            }
//        }
//
//        property.setLanguageKey(this.getLangKey());
//
//        property.setRequiresMcRestart(this.requiresGameRestart());
//        property.setRequiresWorldRestart(this.requiresWorldRestart());
//        property.setShowInGui(this.isVisibleInGui());
//    }
//
//    private Object toPrimitive(Object values) {
//        if (values == null)
//            return values;
//        Class<?> clazz = values.getClass().getComponentType();
//        if (!clazz.isPrimitive()) {
//            if (Boolean.class.equals(clazz)) {
//                // Special case for booleans because the method in ArrayUtils doesnt have one built in.
//                return ArrayUtils.toPrimitive((Boolean[]) values);
//            } else {
//                return ArrayUtils.toPrimitive(values);
//            }
//        } else {
//            return values;
//        }
//    }
//
//    private Object toObject(Object values) {
//        if (values == null)
//            return values;
//        Class<?> clazz = values.getClass().getComponentType();
//        if (clazz.isPrimitive()) {
//            if (int.class.equals(clazz)) {
//                return ArrayUtils.toObject((int[]) values);
//            }
//            if (double.class.equals(clazz)) {
//                return ArrayUtils.toObject((double[]) values);
//            }
//            if (boolean.class.equals(clazz)) {
//                return ArrayUtils.toObject((boolean[]) values);
//            }
//            return values;
//        } else {
//            return values;
//        }
//    }
//
//    private void setListValue(Object values) {
//        Object configInstance = this.config.getConfigInstance();
//        if (this.getField().getType().isArray()) {
//            if (!this.getField().getType().getComponentType().isPrimitive()) {
//                this.getField().setValue(configInstance, toObject(values));
//            } else {
//                this.getField().setValue(configInstance, values);
//            }
//        } else {
//            List valueList = (List) getField().getValue(configInstance);
//            valueList.clear();
//            valueList.addAll(Arrays.asList(toObject(values)));
//        }
//    }
//
//    @Nonnull
//    public WrappedField getField() {
//        return field;
//    }
//
//    public boolean isValueArray() {
//        return isValueArray;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public String getComment() {
//        return comment;
//    }
//
//    public Property.Type getType() {
//        return type;
//    }
//
//    public Object getDefaultValue() {
//        return defaultValue;
//    }
//
//    public String getLangKey() {
//        return langKey;
//    }
//
//    public String getCategory() {
//        return category;
//    }
//
//    public boolean requiresGameRestart() {
//        return requiresGameRestart;
//    }
//
//    public boolean requiresWorldRestart() {
//        return requiresWorldRestart;
//    }
//
//    public boolean isVisibleInGui() {
//        return visibleInGui;
//    }
//}
//