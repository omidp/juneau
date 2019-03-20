// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.internal;

import static org.apache.juneau.internal.CollectionUtils.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.reflection.*;
import org.apache.juneau.utils.*;

/**
 * Class-related utility methods.
 */
public final class ClassUtils {

	/**
	 * Shortcut for calling {@link ClassInfo#create(Type)}.
	 *
	 * @param t The class being wrapped.
	 * @return The wrapped class.
	 */
	public static ClassInfo getClassInfo(Type t) {
		return ClassInfo.create(t);
	}

	/**
	 * Shortcut for calling {@link MethodInfo#create(Method)}.
	 *
	 * @param m The method being wrapped.
	 * @return The wrapped method.
	 */
	public static MethodInfo getMethodInfo(Method m) {
		return MethodInfo.create(m);
	}

	/**
	 * Shortcut for calling {@link FieldInfo#create(Field)}.
	 *
	 * @param f The field being wrapped.
	 * @return The wrapped field.
	 */
	public static FieldInfo getFieldInfo(Field f) {
		return FieldInfo.create(f);
	}

	/**
	 * Shortcut for calling {@link ConstructorInfo#create(Constructor)}.
	 *
	 * @param c The constructor being wrapped.
	 * @return The wrapped constructor.
	 */
	public static ConstructorInfo getConstructorInfo(Constructor<?> c) {
		return ConstructorInfo.create(c);
	}

	/**
	 * Given the specified list of objects, return readable names for the class types of the objects.
	 *
	 * @param o The objects.
	 * @return An array of readable class type strings.
	 */
	public static ObjectList getReadableClassNames(Object[] o) {
		ObjectList l = new ObjectList();
		for (int i = 0; i < o.length; i++)
			l.add(o[i] == null ? "null" : getReadableClassName(o[i].getClass()));
		return l;
	}

	/**
	 * Shortcut for calling <code><jsm>getReadableClassName</jsm>(c.getName())</code>
	 *
	 * @param c The class.
	 * @return A readable class type name, or <jk>null</jk> if parameter is <jk>null</jk>.
	 */
	public static String getReadableClassName(Class<?> c) {
		if (c == null)
			return null;
		return getReadableClassName(c.getName());
	}

	/**
	 * Shortcut for calling <code><jsm>getReadableClassName</jsm>(c.getClass().getName())</code>
	 *
	 * @param o The object whose class we want to render.
	 * @return A readable class type name, or <jk>null</jk> if parameter is <jk>null</jk>.
	 */
	public static String getReadableClassNameForObject(Object o) {
		if (o == null)
			return null;
		return getReadableClassName(o.getClass().getName());
	}

	/**
	 * Converts the specified class name to a readable form when class name is a special construct like <js>"[[Z"</js>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jsm>getReadableClassName</jsm>(<js>"java.lang.Object"</js>);  <jc>// Returns "java.lang.Object"</jc>
	 * 	<jsm>getReadableClassName</jsm>(<js>"boolean"</js>);  <jc>// Returns "boolean"</jc>
	 * 	<jsm>getReadableClassName</jsm>(<js>"[Z"</js>);  <jc>// Returns "boolean[]"</jc>
	 * 	<jsm>getReadableClassName</jsm>(<js>"[[Z"</js>);  <jc>// Returns "boolean[][]"</jc>
	 * 	<jsm>getReadableClassName</jsm>(<js>"[Ljava.lang.Object;"</js>);  <jc>// Returns "java.lang.Object[]"</jc>
	 * 	<jsm>getReadableClassName</jsm>(<jk>null</jk>);  <jc>// Returns null</jc>
	 * </p>
	 *
	 * @param className The class name.
	 * @return A readable class type name, or <jk>null</jk> if parameter is <jk>null</jk>.
	 */
	public static String getReadableClassName(String className) {
		if (className == null)
			return null;
		if (! StringUtils.startsWith(className, '['))
			return className;
		int depth = 0;
		for (int i = 0; i < className.length(); i++) {
			if (className.charAt(i) == '[')
				depth++;
			else
				break;
		}
		char type = className.charAt(depth);
		String c;
		switch (type) {
			case 'Z': c = "boolean"; break;
			case 'B': c = "byte"; break;
			case 'C': c = "char"; break;
			case 'D': c = "double"; break;
			case 'F': c = "float"; break;
			case 'I': c = "int"; break;
			case 'J': c = "long"; break;
			case 'S': c = "short"; break;
			default: c = className.substring(depth+1, className.length()-1);
		}
		StringBuilder sb = new StringBuilder(c.length() + 2*depth).append(c);
		for (int i = 0; i < depth; i++)
			sb.append("[]");
		return sb.toString();
	}

	/**
	 * Returns <jk>true</jk> if <code>parent</code> is a parent class of <code>child</code>.
	 *
	 * @param parent The parent class.
	 * @param child The child class.
	 * @param strict If <jk>true</jk> returns <jk>false</jk> if the classes are the same.
	 * @return <jk>true</jk> if <code>parent</code> is a parent class of <code>child</code>.
	 */
	public static boolean isParentClass(Class<?> parent, Class<?> child, boolean strict) {
		return parent.isAssignableFrom(child) && ((!strict) || ! parent.equals(child));
	}

	/**
	 * Returns <jk>true</jk> if <code>parent</code> is a parent class or the same as <code>child</code>.
	 *
	 * @param parent The parent class.
	 * @param child The child class.
	 * @return <jk>true</jk> if <code>parent</code> is a parent class or the same as <code>child</code>.
	 */
	public static boolean isParentClass(Class<?> parent, Class<?> child) {
		return isParentClass(parent, child, false);
	}

	/**
	 * Returns <jk>true</jk> if <code>parent</code> is a parent class or the same as <code>child</code>.
	 *
	 * @param parent The parent class.
	 * @param child The child class.
	 * @return <jk>true</jk> if <code>parent</code> is a parent class or the same as <code>child</code>.
	 */
	public static boolean isParentClass(Class<?> parent, Type child) {
		if (child instanceof Class)
			return isParentClass(parent, (Class<?>)child);
		return false;
	}

	private static final Map<Class<?>, Class<?>>
		pmap1 = new HashMap<>(),
		pmap2 = new HashMap<>();
	static {
		pmap1.put(boolean.class, Boolean.class);
		pmap1.put(byte.class, Byte.class);
		pmap1.put(short.class, Short.class);
		pmap1.put(char.class, Character.class);
		pmap1.put(int.class, Integer.class);
		pmap1.put(long.class, Long.class);
		pmap1.put(float.class, Float.class);
		pmap1.put(double.class, Double.class);
		pmap2.put(Boolean.class, boolean.class);
		pmap2.put(Byte.class, byte.class);
		pmap2.put(Short.class, short.class);
		pmap2.put(Character.class, char.class);
		pmap2.put(Integer.class, int.class);
		pmap2.put(Long.class, long.class);
		pmap2.put(Float.class, float.class);
		pmap2.put(Double.class, double.class);
	}

	/**
	 * Returns <jk>true</jk> if the {@link #getPrimitiveWrapper(Class)} class returns a value for the specified class.
	 *
	 * @param c The class.
	 * @return <jk>true</jk> if the {@link #getPrimitiveWrapper(Class)} class returns a value for the specified class.
	 */
	public static boolean hasPrimitiveWrapper(Class<?> c) {
		return pmap1.containsKey(c);
	}

	/**
	 * If the specified class is a primitive (e.g. <code><jk>int</jk>.<jk>class</jk></code>) returns it's wrapper class
	 * (e.g. <code>Integer.<jk>class</jk></code>).
	 *
	 * @param c The class.
	 * @return The wrapper class, or <jk>null</jk> if class is not a primitive.
	 */
	public static Class<?> getPrimitiveWrapper(Class<?> c) {
		return pmap1.get(c);
	}

	/**
	 * If the specified class is a primitive wrapper (e.g. <code><jk>Integer</jk>.<jk>class</jk></code>) returns it's
	 * primitive class (e.g. <code>int.<jk>class</jk></code>).
	 *
	 * @param c The class.
	 * @return The primitive class, or <jk>null</jk> if class is not a primitive wrapper.
	 */
	public static Class<?> getPrimitiveForWrapper(Class<?> c) {
		return pmap2.get(c);
	}

	/**
	 * If the specified class is a primitive (e.g. <code><jk>int</jk>.<jk>class</jk></code>) returns it's wrapper class
	 * (e.g. <code>Integer.<jk>class</jk></code>).
	 *
	 * @param c The class.
	 * @return The wrapper class if it's primitive, or the same class if class is not a primitive.
	 */
	public static Class<?> getWrapperIfPrimitive(Class<?> c) {
		if (! c.isPrimitive())
			return c;
		return pmap1.get(c);
	}

	/**
	 * Returns <jk>true</jk> if the specified class is public.
	 *
	 * @param c The class.
	 * @return <jk>true</jk> if the specified class is public.
	 */
	public static boolean isStatic(Class<?> c) {
		return Modifier.isStatic(c.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if the specified class is abstract.
	 *
	 * @param c The class.
	 * @return <jk>true</jk> if the specified class is abstract.
	 */
	public static boolean isAbstract(Class<?> c) {
		return Modifier.isAbstract(c.getModifiers());
	}

	/**
	 * Finds the real parameter type of the specified class.
	 *
	 * @param c The class containing the parameters (e.g. PojoSwap&lt;T,S&gt;)
	 * @param index The zero-based index of the parameter to resolve.
	 * @param oc The class we're trying to resolve the parameter type for.
	 * @return The resolved real class.
	 */
	public static Class<?> resolveParameterType(Class<?> c, int index, Class<?> oc) {

		// We need to make up a mapping of type names.
		Map<Type,Type> typeMap = new HashMap<>();
		while (c != oc.getSuperclass()) {
			extractTypes(typeMap, oc);
			oc = oc.getSuperclass();
		}

		Type gsc = oc.getGenericSuperclass();

		// Not actually a parameterized type.
		if (! (gsc instanceof ParameterizedType))
			return Object.class;

		ParameterizedType opt = (ParameterizedType)gsc;
		Type actualType = opt.getActualTypeArguments()[index];

		if (typeMap.containsKey(actualType))
			actualType = typeMap.get(actualType);

		if (actualType instanceof Class) {
			return (Class<?>)actualType;

		} else if (actualType instanceof GenericArrayType) {
			Class<?> cmpntType = (Class<?>)((GenericArrayType)actualType).getGenericComponentType();
			return Array.newInstance(cmpntType, 0).getClass();

		} else if (actualType instanceof TypeVariable) {
			TypeVariable<?> typeVariable = (TypeVariable<?>)actualType;
			List<Class<?>> nestedOuterTypes = new LinkedList<>();
			for (Class<?> ec = oc.getEnclosingClass(); ec != null; ec = ec.getEnclosingClass()) {
				try {
					Class<?> outerClass = oc.getClass();
					nestedOuterTypes.add(outerClass);
					Map<Type,Type> outerTypeMap = new HashMap<>();
					extractTypes(outerTypeMap, outerClass);
					for (Map.Entry<Type,Type> entry : outerTypeMap.entrySet()) {
						Type key = entry.getKey(), value = entry.getValue();
						if (key instanceof TypeVariable) {
							TypeVariable<?> keyType = (TypeVariable<?>)key;
							if (keyType.getName().equals(typeVariable.getName()) && isInnerClass(keyType.getGenericDeclaration(), typeVariable.getGenericDeclaration())) {
								if (value instanceof Class)
									return (Class<?>)value;
								typeVariable = (TypeVariable<?>)entry.getValue();
							}
						}
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			throw new FormattedRuntimeException("Could not resolve type: {0}", actualType);
		} else {
			throw new FormattedRuntimeException("Invalid type found in resolveParameterType: {0}", actualType);
		}
	}

	private static boolean isInnerClass(GenericDeclaration od, GenericDeclaration id) {
		if (od instanceof Class && id instanceof Class) {
			Class<?> oc = (Class<?>)od;
			Class<?> ic = (Class<?>)id;
			while ((ic = ic.getEnclosingClass()) != null)
				if (ic == oc)
					return true;
		}
		return false;
	}

	private static void extractTypes(Map<Type,Type> typeMap, Class<?> c) {
		Type gs = c.getGenericSuperclass();
		if (gs instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)gs;
			Type[] typeParameters = ((Class<?>)pt.getRawType()).getTypeParameters();
			Type[] actualTypeArguments = pt.getActualTypeArguments();
			for (int i = 0; i < typeParameters.length; i++) {
				if (typeMap.containsKey(actualTypeArguments[i]))
					actualTypeArguments[i] = typeMap.get(actualTypeArguments[i]);
				typeMap.put(typeParameters[i], actualTypeArguments[i]);
			}
		}
	}

	/**
	 * Returns <jk>true</jk> if the specified argument types are valid for the specified parameter types.
	 *
	 * @param paramTypes The parameters types specified on a method.
	 * @param argTypes The class types of the arguments being passed to the method.
	 * @return <jk>true</jk> if the arguments match the parameters.
	 */
	public static boolean argsMatch(Class<?>[] paramTypes, Class<?>[] argTypes) {
		if (paramTypes.length == argTypes.length) {
			for (int i = 0; i < paramTypes.length; i++)
				if (! isParentClass(paramTypes[i], argTypes[i]))
					return false;
			return true;
		}
		return false;
	}

	/**
	 * Returns a number representing the number of arguments that match the specified parameters.
	 *
	 * @param paramTypes The parameters types specified on a method.
	 * @param argTypes The class types of the arguments being passed to the method.
	 * @return The number of matching arguments, or <code>-1</code> a parameter was found that isn't in the list of args.
	 */
	public static int fuzzyArgsMatch(Class<?>[] paramTypes, Class<?>... argTypes) {
		int matches = 0;
		outer: for (Class<?> p : paramTypes) {
			p = getWrapperIfPrimitive(p);
			for (Class<?> a : argTypes) {
				if (isParentClass(p, a)) {
					matches++;
					continue outer;
				}
			}
			return -1;
		}
		return matches;
	}

	/**
	 * Returns the class types for the specified arguments.
	 *
	 * @param args The objects we're getting the classes of.
	 * @return The classes of the arguments.
	 */
	public static Class<?>[] getClasses(Object...args) {
		Class<?>[] pt = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++)
			pt[i] = args[i] == null ? null : args[i].getClass();
		return pt;
	}

	/**
	 * Creates an instance of the specified class.
	 *
	 * @param c
	 * 	The class to cast to.
	 * @param c2
	 * 	The class to instantiate.
	 * 	Can also be an instance of the class.
	 * @return
	 * 	The new class instance, or <jk>null</jk> if the class was <jk>null</jk> or is abstract or an interface.
	 * @throws
	 * 	RuntimeException if constructor could not be found or called.
	 */
	public static <T> T newInstance(Class<T> c, Object c2) {
		return newInstanceFromOuter(null, c, c2, false);
	}

	/**
	 * Creates an instance of the specified class.
	 *
	 * @param c
	 * 	The class to cast to.
	 * @param c2
	 * 	The class to instantiate.
	 * 	Can also be an instance of the class.
	 * @param fuzzyArgs
	 * 	Use fuzzy constructor arg matching.
	 * 	<br>When <jk>true</jk>, constructor args can be in any order and extra args are ignored.
	 * 	<br>No-arg constructors are also used if no other constructors are found.
	 * @param args
	 * 	The arguments to pass to the constructor.
	 * @return
	 * 	The new class instance, or <jk>null</jk> if the class was <jk>null</jk> or is abstract or an interface.
	 * @throws
	 * 	RuntimeException if constructor could not be found or called.
	 */
	public static <T> T newInstance(Class<T> c, Object c2, boolean fuzzyArgs, Object...args) {
		return newInstanceFromOuter(null, c, c2, fuzzyArgs, args);
	}

	/**
	 * Creates an instance of the specified class from within the context of another object.
	 *
	 * @param outer
	 * 	The outer object.
	 * 	Can be <jk>null</jk>.
	 * @param c
	 * 	The class to cast to.
	 * @param c2
	 * 	The class to instantiate.
	 * 	Can also be an instance of the class.
	 * @param fuzzyArgs
	 * 	Use fuzzy constructor arg matching.
	 * 	<br>When <jk>true</jk>, constructor args can be in any order and extra args are ignored.
	 * 	<br>No-arg constructors are also used if no other constructors are found.
	 * @param args
	 * 	The arguments to pass to the constructor.
	 * @return
	 * 	The new class instance, or <jk>null</jk> if the class was <jk>null</jk> or is abstract or an interface.
	 * @throws
	 * 	RuntimeException if constructor could not be found or called.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T newInstanceFromOuter(Object outer, Class<T> c, Object c2, boolean fuzzyArgs, Object...args) {
		if (c2 == null)
			return null;
		if (c2 instanceof Class) {
			try {
				Class<?> c3 = (Class<?>)c2;
				if (c3.isInterface() || isAbstract(c3))
					return null;
				ClassInfo c3i = ClassInfo.lookup(c3);

				// First look for an exact match.
				Constructor<?> con = c3i.findPublicConstructor(false, args);
				if (con != null)
					return (T)con.newInstance(args);

				// Next look for an exact match including the outer.
				if (outer != null) {
					args = new AList<>().append(outer).appendAll(args).toArray();
					con = c3i.findPublicConstructor(false, args);
					if (con != null)
						return (T)con.newInstance(args);
				}

				// Finally use fuzzy matching.
				if (fuzzyArgs) {
					con = c3i.findPublicConstructor(true, args);
					if (con != null)
						return (T)con.newInstance(getMatchingArgs(con.getParameterTypes(), args));
				}

				throw new FormattedRuntimeException("Could not instantiate class {0}/{1}.  Constructor not found.", c.getName(), c2);
			} catch (Exception e) {
				throw new FormattedRuntimeException(e, "Could not instantiate class {0}", c.getName());
			}
		} else if (isParentClass(c, c2.getClass())) {
			return (T)c2;
		} else {
			throw new FormattedRuntimeException("Object of type {0} found but was expecting {1}.", c2.getClass(), c.getClass());
		}
	}

	/**
	 * Matches arguments to a list of parameter types.
	 *
	 * <p>
	 * Extra parameters are ignored.
	 * <br>Missing parameters are left null.
	 *
	 * @param paramTypes The parameter types.
	 * @param args The arguments to match to the parameter types.
	 * @return
	 * 	An array of parameters.
	 */
	public static Object[] getMatchingArgs(Class<?>[] paramTypes, Object... args) {
		Object[] params = new Object[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			Class<?> pt = getWrapperIfPrimitive(paramTypes[i]);
			for (int j = 0; j < args.length; j++) {
				if (isParentClass(pt, args[j].getClass())) {
					params[i] = args[j];
					break;
				}
			}
		}
		return params;
	}

	/**
	 * Returns a list of all the parent classes of the specified class including the class itself.
	 *
	 * @param c The class to retrieve the parent classes.
	 * @param parentFirst In parent-to-child order, otherwise child-to-parent.
	 * @param includeInterfaces Include interfaces.
	 * @return An iterator of parent classes in the class hierarchy.
	 */
	public static Iterator<Class<?>> getParentClasses(final Class<?> c, boolean parentFirst, boolean includeInterfaces) {
		List<Class<?>> l = getParentClasses(new ArrayList<Class<?>>(), c, parentFirst, includeInterfaces);
		return l.iterator();
	}

	private static List<Class<?>> getParentClasses(List<Class<?>> l, Class<?> c, boolean parentFirst, boolean includeInterfaces) {
		if (parentFirst) {
			if (includeInterfaces)
				for (Class<?> i : c.getInterfaces())
					l.add(i);
			if (c.getSuperclass() != Object.class && c.getSuperclass() != null)
				getParentClasses(l, c.getSuperclass(), parentFirst, includeInterfaces);
			l.add(c);
		} else {
			l.add(c);
			if (c.getSuperclass() != Object.class && c.getSuperclass() != null)
				getParentClasses(l, c.getSuperclass(), parentFirst, includeInterfaces);
			if (includeInterfaces)
				for (Class<?> i : c.getInterfaces())
					l.add(i);
		}
		return l;
	}

	/**
	 * Returns the default value for the specified primitive class.
	 *
	 * @param primitiveClass The primitive class to get the default value for.
	 * @return The default value, or <jk>null</jk> if the specified class is not a primitive class.
	 */
	public static Object getPrimitiveDefault(Class<?> primitiveClass) {
		return primitiveDefaultMap.get(primitiveClass);
	}

	private static final Map<Class<?>,Object> primitiveDefaultMap = Collections.unmodifiableMap(
		new AMap<Class<?>,Object>()
			.append(Boolean.TYPE, false)
			.append(Character.TYPE, (char)0)
			.append(Short.TYPE, (short)0)
			.append(Integer.TYPE, 0)
			.append(Long.TYPE, 0l)
			.append(Float.TYPE, 0f)
			.append(Double.TYPE, 0d)
			.append(Byte.TYPE, (byte)0)
			.append(Boolean.class, false)
			.append(Character.class, (char)0)
			.append(Short.class, (short)0)
			.append(Integer.class, 0)
			.append(Long.class, 0l)
			.append(Float.class, 0f)
			.append(Double.class, 0d)
			.append(Byte.class, (byte)0)
	);

	/**
	 * Returns a readable representation of the specified method.
	 *
	 * <p>
	 * The format of the string is <js>"full-qualified-class.method-name(parameter-simple-class-names)"</js>.
	 *
	 * @param m The method to stringify.
	 * @return The stringified method.
	 */
	public static String toString(Method m) {
		StringBuilder sb = new StringBuilder(m.getDeclaringClass().getName() + "." + m.getName() + "(");
		for (int i = 0; i < m.getParameterTypes().length; i++) {
			if (i > 0)
				sb.append(",");
			sb.append(m.getParameterTypes()[i].getSimpleName());
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Returns a readable representation of the specified field.
	 *
	 * <p>
	 * The format of the string is <js>"full-qualified-class.field-name"</js>.
	 *
	 * @param f The field to stringify.
	 * @return The stringified field.
	 */
	public static String toString(Field f) {
		return f.getDeclaringClass().getName() + "." + f.getName();
	}

	/**
	 * Constructs a new instance of the specified class from the specified string.
	 *
	 * <p>
	 * Class must be one of the following:
	 * <ul>
	 * 	<li>Have a public constructor that takes in a single <code>String</code> argument.
	 * 	<li>Have a static <code>fromString(String)</code> (or related) method.
	 * 		<br>See {@link ClassInfo#findPublicFromStringMethod()} for the list of possible static method names.
	 * 	<li>Be an <code>enum</code>.
	 * </ul>
	 *
	 * @param c The class.
	 * @param s The string to create the instance from.
	 * @return A new object instance, or <jk>null</jk> if a method for converting the string to an object could not be found.
	 */
	public static <T> T fromString(Class<T> c, String s) {
		Transform<String,T> t = TransformCache.get(String.class, c);
		return t == null ? null : t.transform(s);
	}

	/**
	 * Converts an object to a string.
	 *
	 * <p>
	 * Normally, this is just going to call <code>toString()</code> on the object.
	 * However, the {@link Locale} and {@link TimeZone} objects are treated special so that the returned value
	 * works with the {@link #fromString(Class, String)} method.
	 *
	 * @param o The object to convert to a string.
	 * @return The stringified object, or <jk>null</jk> if the object was <jk>null</jk>.
	 */
	@SuppressWarnings({ "unchecked" })
	public static String toString(Object o) {
		if (o == null)
			return null;
		Transform<Object,String> t = (Transform<Object,String>)TransformCache.get(o.getClass(), String.class);
		return t == null ? o.toString() : t.transform(o);
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @param x The constructor.
	 * @param ignoreExceptions Ignore {@link SecurityException SecurityExceptions} and just return <jk>false</jk> if thrown.
	 * @return <jk>true</jk> if call was successful.
	 */
	public static boolean setAccessible(Constructor<?> x, boolean ignoreExceptions) {
		try {
			if (! (x == null || x.isAccessible()))
				x.setAccessible(true);
			return true;
		} catch (SecurityException e) {
			if (ignoreExceptions)
				return false;
			throw new ClassMetaRuntimeException("Could not set accessibility to true on constructor ''{0}''", x);
		}
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @param x The method.
	 * @param ignoreExceptions Ignore {@link SecurityException SecurityExceptions} and just return <jk>false</jk> if thrown.
	 * @return <jk>true</jk> if call was successful.
	 */
	public static boolean setAccessible(Method x, boolean ignoreExceptions) {
		try {
			if (! (x == null || x.isAccessible()))
				x.setAccessible(true);
			return true;
		} catch (SecurityException e) {
			if (ignoreExceptions)
				return false;
			throw new ClassMetaRuntimeException("Could not set accessibility to true on method ''{0}''", x);
		}
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @param x The field.
	 * @param ignoreExceptions Ignore {@link SecurityException SecurityExceptions} and just return <jk>false</jk> if thrown.
	 * @return <jk>true</jk> if call was successful.
	 */
	public static boolean setAccessible(Field x, boolean ignoreExceptions) {
		try {
			if (! (x == null || x.isAccessible()))
				x.setAccessible(true);
			return true;
		} catch (SecurityException e) {
			if (ignoreExceptions)
				return false;
			throw new ClassMetaRuntimeException("Could not set accessibility to true on field ''{0}''", x);
		}
	}

	/**
	 * Returns the simple name of a class.
	 *
	 * <p>
	 * Similar to {@link Class#getSimpleName()}, but includes the simple name of an enclosing or declaring class.
	 *
	 * @param c The class to get the simple name on.
	 * @return The simple name of a class.
	 */
	public static String getSimpleName(Class<?> c) {
		if (c.isLocalClass())
			return getSimpleName(c.getEnclosingClass()) + '.' + c.getSimpleName();
		if (c.isMemberClass())
			return getSimpleName(c.getDeclaringClass()) + '.' + c.getSimpleName();
		return c.getSimpleName();
	}

	/**
	 * Returns the simple name of a class.
	 *
	 * <p>
	 * Similar to {@link Class#getSimpleName()}, but includes the simple name of an enclosing or declaring class.
	 *
	 * @param t The class to get the simple name on.
	 * @return The simple name of a class.
	 */
	public static String getSimpleName(Type t) {
		if (t instanceof Class)
			return getSimpleName((Class<?>)t);
		if (t instanceof ParameterizedType) {
			StringBuilder sb = new StringBuilder();
			ParameterizedType pt = (ParameterizedType)t;
			sb.append(getSimpleName(pt.getRawType()));
			sb.append("<");
			boolean first = true;
			for (Type t2 : pt.getActualTypeArguments()) {
				if (! first)
					sb.append(',');
				first = false;
				sb.append(getSimpleName(t2));
			}
			sb.append(">");
			return sb.toString();
		}
		return null;
	}

	/**
	 * Returns the specified annotation only if it's been declared on the specified class.
	 *
	 * <p>
	 * More efficient than calling {@link Class#getAnnotation(Class)} since it doesn't recursively look for the class
	 * up the parent chain.
	 *
	 * @param <T> The annotation class type.
	 * @param a The annotation class.
	 * @param t The annotated class.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Annotation> T getDeclaredAnnotation(Class<T> a, Type t) {
		Class<?> c = toClass(t);
		if (c != null)
			for (Annotation a2 : c.getDeclaredAnnotations())
				if (a2.annotationType() == a)
					return (T)a2;
		return null;
	}

	/**
	 * Same as getAnnotations(Class, Type) except returns the annotations as a map with the keys being the
	 * class on which the annotation was found.
	 *
	 * <p>
	 * Results are ordered child-to-parent.
	 *
	 * @param <T> The annotation class type.
	 * @param a The annotation class type.
	 * @param t The class being searched.
	 * @return The found matches, or an empty map if annotation was not found.
	 */
	public static <T extends Annotation> LinkedHashMap<Class<?>,T> getAnnotationsMap(Class<T> a, Type t) {
		LinkedHashMap<Class<?>,T> m = new LinkedHashMap<>();
		findAnnotationsMap(a, t, m);
		return m;
	}

	/**
	 * Same as {@link #getAnnotationsMap(Class, Type)} except returns results in parent-to-child order.
	 *
	 * @param <T> The annotation class type.
	 * @param a The annotation class type.
	 * @param t The class being searched.
	 * @return The found matches, or an empty map if annotation was not found.
	 */
	public static <T extends Annotation> LinkedHashMap<Class<?>,T> getAnnotationsMapParentFirst(Class<T> a, Type t) {
		return CollectionUtils.reverse(getAnnotationsMap(a, t));
	}

	private static <T extends Annotation> void findAnnotationsMap(Class<T> a, Type t, Map<Class<?>,T> m) {
		Class<?> c = toClass(t);
		if (c != null) {

			T t2 = getDeclaredAnnotation(a, c);
			if (t2 != null)
				m.put(c, t2);

			findAnnotationsMap(a, c.getSuperclass(), m);

			for (Class<?> c2 : c.getInterfaces())
				findAnnotationsMap(a, c2, m);
		}
	}

	/**
	 * Finds and appends the specified annotation on the specified class and superclasses/interfaces to the specified
	 * list.
	 *
	 * @param a The annotation.
	 * @param t The class.
	 * @param l The list of annotations.
	 */
	public static <T extends Annotation> void appendAnnotations(Class<T> a, Type t, List<T> l) {
		Class<?> c = toClass(t);
		if (c != null) {
			addIfNotNull(l, getDeclaredAnnotation(a, c));

			if (c.getPackage() != null)
				addIfNotNull(l, c.getPackage().getAnnotation(a));

			appendAnnotations(a, c.getSuperclass(), l);

			for (Class<?> c2 : c.getInterfaces())
				appendAnnotations(a, c2, l);
		}
	}

	/**
	 * Returns the specified type as a <code>Class</code>.
	 *
	 * <p>
	 * If it's already a <code>Class</code>, it just does a cast.
	 * <br>If it's a <code>ParameterizedType</code>, it returns the raw type.
	 *
	 * @param t The type to convert.
	 * @return The type converted to a <code>Class</code>, or <jk>null</jk> if it could not be converted.
	 */
	public static Class<?> toClass(Type t) {
		if (t instanceof Class)
			return (Class<?>)t;
		if (t instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)t;
			// The raw type should always be a class (right?)
			return (Class<?>)pt.getRawType();
		}
		return null;
	}

	/**
	 * Similar to {@link Class#getResourceAsStream(String)} except looks up the parent hierarchy for the existence of
	 * the specified resource.
	 *
	 * @param c The class to return the resource on.
	 * @param name The resource name.
	 * @return An input stream on the specified resource, or <jk>null</jk> if the resource could not be found.
	 */
	public static InputStream getResource(Class<?> c, String name) {
		if (name == null)
			return null;
		while (c != null) {
			InputStream is = c.getResourceAsStream(name);
			if (is != null)
				return is;
			c = c.getSuperclass();
		}
		return null;
	}
}
