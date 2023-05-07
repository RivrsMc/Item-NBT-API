package de.tr7zw.changeme.nbtapi.utils.nmsmappings;

import de.tr7zw.changeme.nbtapi.NbtApiException;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;

import java.lang.reflect.Field;

public enum ReflectionField {

    ITEMSTACK_HANDLE(ClassWrapper.CRAFT_ITEMSTACK, MinecraftVersion.MC1_7_R4,
            new Since(MinecraftVersion.MC1_7_R4, "handle")),
    ;
    private MinecraftVersion removedAfter;
    private Since targetVersion;
    private Field field;
    private boolean loaded = false;
    private boolean compatible = false;
    private String fieldName = null;
    private ClassWrapper parentClassWrapper;

    ReflectionField(ClassWrapper targetClass, MinecraftVersion addedSince,
                    MinecraftVersion removedAfter, Since... fieldNames) {
        this.removedAfter = removedAfter;
        this.parentClassWrapper = targetClass;
        // Special Case for Modded 1.7.10
        boolean specialCase = (MinecraftVersion.isForgePresent() && this.name().equals("COMPOUND_MERGE")
                && MinecraftVersion.getVersion() == MinecraftVersion.MC1_7_R4); // COMPOUND_MERGE is only present on
        // Crucible, not on vanilla 1.7.10
        if (!specialCase && (!MinecraftVersion.isAtLeastVersion(addedSince)
                || (this.removedAfter != null && MinecraftVersion.isNewerThan(removedAfter))))
            return;
        compatible = true;
        MinecraftVersion server = MinecraftVersion.getVersion();
        Since target = fieldNames[0];
        for (Since s : fieldNames) {
            if (s.version.getVersionId() <= server.getVersionId()
                    && target.version.getVersionId() < s.version.getVersionId())
                target = s;
        }
        targetVersion = target;
        String targetFieldName = targetVersion.name;
        try {
            if (MinecraftVersion.isForgePresent() && MinecraftVersion.getVersion() == MinecraftVersion.MC1_7_R4) {
                targetFieldName = Forge1710Mappings.getMethodMapping().getOrDefault(this.name(), targetFieldName);
            } else if (targetVersion.version.isMojangMapping()) {
                targetFieldName = MojangToMapping.getMapping().getOrDefault(
                        targetClass.getMojangName() + "#" + targetVersion.name, "Unmapped" + targetVersion.name);
            }
            field = targetClass.getClazz().getDeclaredField(targetFieldName);
            field.setAccessible(true);
            loaded = true;
            fieldName = targetVersion.name;
        } catch (NullPointerException | NoSuchFieldException | SecurityException ex) {
            try {
                if (targetVersion.version.isMojangMapping())
                    targetFieldName = MojangToMapping.getMapping().getOrDefault(
                            targetClass.getMojangName() + "#" + targetVersion.name, "Unmapped" + targetVersion.name);
                field = targetClass.getClazz().getField(targetFieldName);
                field.setAccessible(true);
                loaded = true;
                fieldName = targetVersion.name;
            } catch (NullPointerException | NoSuchFieldException | SecurityException ex2) {
                MinecraftVersion.getLogger()
                        .warning("[NBTAPI] Unable to find the field '" + targetFieldName + "' in '"
                                + (targetClass.getClazz() == null ? targetClass.getMojangName()
                                : targetClass.getClazz().getSimpleName())
                                + " Enum: " + this); // NOSONAR This gets loaded
                // before the logger is loaded
            }
        }
    }

    ReflectionField(ClassWrapper targetClass, MinecraftVersion addedSince, Since... methodnames) {
        this(targetClass,  addedSince, null, methodnames);
    }

    /**
     * @return Returns the field name used in the current version
     */
    public Object get(Object target) {
        if (field == null)
            throw new NbtApiException("Field not loaded! '" + this + "'");
        try {
            return field.get(target);
        } catch (Exception ex) {
            throw new NbtApiException("Error while calling the field '" + fieldName + "', loaded: " + loaded
                    + ", Enum: " + this + ", Passed Class: " + (target == null ? "null" : target.getClass()), ex);
        }
    }

    public String getFieldName() {
        return fieldName;
    }


    /**
     * @return Has this field been linked
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * @return Is this field available in this Minecraft Version
     */
    public boolean isCompatible() {
        return compatible;
    }

    public Since getSelectedVersionInfo() {
        return targetVersion;
    }

    /**
     * @return Get Wrapper of the parent class
     */
    public ClassWrapper getParentClassWrapper() {
        return parentClassWrapper;
    }

    public static class Since {
        public final MinecraftVersion version;
        public final String name;

        public Since(MinecraftVersion version, String name) {
            this.version = version;
            this.name = name;
        }
    }
}
