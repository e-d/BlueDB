package org.bluedb.disk.serialization.validation;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ObjectValidation {
	
	protected ObjectValidation() {} // just to get 100% test coverage

	public static void validateFieldValueTypesForObject(Object obj) throws IllegalArgumentException, IllegalAccessException, SerializationException {
		validateFieldValueTypesForObject(obj, new HashSet<>());
	}
	
	private static void validateFieldValueTypesForObject(Object obj, Set<Object> previouslyValidatedObjects) throws IllegalArgumentException, IllegalAccessException, SerializationException {
		if(obj == null) {
			return;
		}
		
		if(obj.getClass().isArray()) {
			handleArray(obj, previouslyValidatedObjects);
		} else {
			handleObject(obj, previouslyValidatedObjects);
		}
	}

	private static void handleArray(Object objArray, Set<Object> previouslyValidatedObjects) throws IllegalArgumentException, IllegalAccessException, SerializationException {
		for(int i = 0; i < Array.getLength(objArray); i++) {
			Object obj = Array.get(objArray, i);
			if(!previouslyValidatedObjects.contains(obj)) {
				previouslyValidatedObjects.add(obj);
				validateFieldValueTypesForObject(obj, previouslyValidatedObjects);
			}
		}
	}

	private static void handleObject(Object obj, Set<Object> previouslyValidatedObjects) throws IllegalArgumentException, IllegalAccessException, SerializationException {
		Class<?> currentClass = obj.getClass();
		while(currentClass != Object.class && currentClass != null) {
			Field[] fields = currentClass.getDeclaredFields();
			if(fields != null) {
				for(Field field : fields) {
					if(!Modifier.isStatic(field.getModifiers())) {
						field.setAccessible(true);
						Object fieldValue = field.get(obj);
						if(fieldValue != null) {
							validateFieldValueType(field, fieldValue);
							
							if(shouldDelveIntoObject(field, fieldValue, previouslyValidatedObjects)) {
								previouslyValidatedObjects.add(fieldValue);
								validateFieldValueTypesForObject(fieldValue, previouslyValidatedObjects);
							}
						}
					}
				}
			}
			currentClass = currentClass.getSuperclass();
		}
	}

	protected static void validateFieldValueType(Field field, Object value) throws SerializationException {
		Class<?> fieldType = field.getType();
		Class<? extends Object> valueType = value.getClass();
		
		if(fieldType.equals(boolean.class) && valueType.equals(Boolean.class)) {
			return;
		}
		if(fieldType.equals(int.class) && valueType.equals(Integer.class)) {
			return;
		}
		if(fieldType.equals(long.class) && valueType.equals(Long.class)) {
			return;
		}
		if(fieldType.equals(float.class) && valueType.equals(Float.class)) {
			return;
		}
		if(fieldType.equals(double.class) && valueType.equals(Double.class)) {
			return;
		}
		if(fieldType.equals(byte.class) && valueType.equals(Byte.class)) {
			return;
		}
		if(fieldType.equals(char.class) && valueType.equals(Character.class)) {
			return;
		}
		if(fieldType.equals(short.class) && valueType.equals(Short.class)) {
			return;
		}
		if(!fieldType.isAssignableFrom(valueType)) {
			throw new SerializationException("Field " + field + " cannot hold a value of type " + valueType);
		}
	}

	private static boolean shouldDelveIntoObject(Field field, Object fieldValue, Set<Object> previouslyValidatedObjects) {
		return 	
			!field.getType().isPrimitive() && 
			field.getType() != String.class && 
			!field.getType().isEnum() &&
			isNotEmptyCollection(fieldValue) &&
			!previouslyValidatedObjects.contains(fieldValue);
	}

	private static boolean isNotEmptyCollection(Object obj) {
		if(obj instanceof Collection<?>) {
			return !((Collection<?>)obj).isEmpty();
		}
		return true;
	}
}
