package org.ovirt.engine.core.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ovirt.engine.core.common.utils.IObjectDescriptorContainer;
import org.ovirt.engine.core.compat.backendcompat.PropertyInfo;
import org.ovirt.engine.core.compat.backendcompat.TypeCompat;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

public class ObjectIdentityChecker {
    private IObjectDescriptorContainer container;
    private static Map<String, Class<?>> aliases =
            new HashMap<String, Class<?>>();
    private static Map<Class<?>, ObjectIdentityChecker> identities =
            new HashMap<Class<?>, ObjectIdentityChecker>();
    private Map<Enum<?>, Set<String>> dictionary =
            new HashMap<Enum<?>, Set<String>>();
    private Set<String> permitted = new HashSet<String>();

    public ObjectIdentityChecker(Class<?> type) {
        identities.put(type, this);
    }

    public ObjectIdentityChecker(Class<?> type, Iterable<Class<?>> aliases) {
        this(type);
        for (Class<?> alias : aliases) {
            ObjectIdentityChecker.aliases.put(alias.getSimpleName(), type);
        }
    }

    public final void setContainer(IObjectDescriptorContainer value) {
        container = value;
    }

    public static boolean CanUpdateField(Object fieldContainer, String fieldName, Enum<?> status) {
        return CanUpdateField(fieldContainer.getClass().getSimpleName(), fieldName, status, fieldContainer);
    }

    public static boolean CanUpdateField(String objectType, String fieldName, Enum<?> status, Object fieldContainer) {
        Class<?> type = aliases.get(objectType);
        if (type != null) {
            return CanUpdateField(type, fieldName, status, fieldContainer);
        } else {
            throw new RuntimeException("status type [null] not exist");
        }
    }

    public static boolean CanUpdateField(Class<?> objectType, String fieldName, Enum<?> status,
            Object fieldContainer) {
        ObjectIdentityChecker checker = identities.get(objectType);
        if (checker != null) {
            return checker.IsFieldUpdatable(status, fieldName, fieldContainer);
        }
        return true;
    }

    public final <T extends Enum<T>> void AddField(T status, String fieldName) {
        Set<String> values = dictionary.get(status);
        if (values == null) {
            values = new HashSet<String>();
            dictionary.put(status, values);
        }
        values.add(fieldName);
    }

    public final <T extends Enum<T>> void AddField(Iterable<T> statuses, String fieldName) {
        for (T status : statuses) {
            AddField(status, fieldName);
        }
    }

    public final void AddPermittedFields(String... fieldNames) {
        for (String fieldName : fieldNames) {
            permitted.add(fieldName);
        }
    }

    public final boolean IsFieldUpdatable(String name) {
        return permitted.contains(name);
    }

    private boolean IsFieldUpdatable(Enum<?> status, String name, Object fieldContainer) {
        boolean returnValue = true;
        if (!IsFieldUpdatable(name)) {
            if (fieldContainer != null && container != null
                    && !container.canUpdateField(fieldContainer, name, status)) {
                returnValue = false;
            } else {
                Set<String> values = dictionary.get(status);
                returnValue = values != null ? values.contains(name) : false;
            }
            if (!returnValue) {
                LogError(name, status);
            }
        }
        return returnValue;
    }

    public final boolean IsUpdateValid(Object source, Object destination) {
        if (source.getClass() != destination.getClass()) {
            return false;
        }
        for (String fieldName : GetChangedFields(source, destination)) {
            if (!IsFieldUpdatable(fieldName)) {
                return false;
            }
        }
        return true;
    }

    public final boolean IsUpdateValid(Object source, Object destination, Enum<?> status) {
        if (source.getClass() != destination.getClass()) {
            return false;
        }
        for (String fieldName : GetChangedFields(source, destination)) {
            if (!IsFieldUpdatable(status, fieldName, null)) {
                log.warn(String.format("ObjectIdentityChecker.IsUpdateValid:: Not updatable field '%1$s' was updated",
                        fieldName));
                return false;
            }
        }
        return true;
    }

    public final boolean IsFieldsUpdated(Object source, Object destination, Iterable<String> fields) {
        List<String> changedFields = GetChangedFields(source, destination);
        for (String field : fields) {
            if (changedFields.contains(field)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> GetChangedFields(Object source, Object destination) {
        final List<String> returnValue = new ArrayList<String>();
        if (source.getClass().isInstance(destination)) {
            Class<?> objectType = source.getClass();
            List<PropertyInfo> properties = TypeCompat.getProperties(objectType);
            for (PropertyInfo property : properties) {
                Object sourceValue = property.getValue(source, null);
                Object destinationValue = property.getValue(destination, null);

                if (property.getCanWrite()
                        && sourceValue != null && !sourceValue.equals(destinationValue)
                        || ((sourceValue == null && destinationValue != null))) {
                    returnValue.add(property.getName());
                }
            }
        }
        return returnValue;
    }

    /**
     * Logs the error.
     *
     * @param name
     *            The name.
     * @param status
     *            The status.
     */
    private static void LogError(String name, Enum<?> status) {
        log.errorFormat("Field {0} can not be updated when status is {1}", name, status);
    }

    private static Log log = LogFactory.getLog(ObjectIdentityChecker.class);
}
