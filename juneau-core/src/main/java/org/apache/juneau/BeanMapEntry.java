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
package org.apache.juneau;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.transform.*;

/**
 * Represents a single entry in a bean map.
 * <p>
 * 	This class can be used to get and set property values on a bean, or to get metadata on a property.
 *
 * <h6 class='topic'>Example:</h6>
 * <p class='bcode'>
 * 	<jc>// Construct a new bean</jc>
 * 	Person p = <jk>new</jk> Person();
 *
 * 	<jc>// Wrap it in a bean map</jc>
 * 	BeanMap&lt;Person&gt; b = BeanContext.<jsf>DEFAULT</jsf>.forBean(p);
 *
 * 	<jc>// Get a reference to the birthDate property</jc>
 * 	BeanMapEntry birthDate = b.getProperty(<js>"birthDate"</js>);
 *
 * 	<jc>// Set the property value</jc>
 * 	birthDate.setValue(<jk>new</jk> Date(1, 2, 3, 4, 5, 6));
 *
 * 	<jc>// Or if the DateSwap.DEFAULT_ISO8601DT is registered with the bean context, set a transformed value</jc>
 * 	birthDate.setValue(<js>"'1901-03-03T04:05:06-5000'"</js>);
 * </p>
 */
public class BeanMapEntry implements Map.Entry<String,Object> {
	private final BeanMap<?> beanMap;
	private final BeanPropertyMeta meta;

	/**
	 * Constructor.
	 *
	 * @param beanMap The bean map that this entry belongs to.
	 * @param property The bean property.
	 */
	protected BeanMapEntry(BeanMap<?> beanMap, BeanPropertyMeta property) {
		this.beanMap = beanMap;
		this.meta = property;
	}

	@Override /* Map.Entry */
	public String getKey() {
		return meta.getName();
	}

	/**
	 * Returns the value of this property.
	 * <p>
	 * If there is a {@link PojoSwap} associated with this bean property or bean property type class, then
	 * 	this method will return the transformed value.
	 * For example, if the bean property type class is a {@link Date} and the bean property has the
	 * 	{@link org.apache.juneau.transforms.DateSwap.ISO8601DT} swap associated with it through the
	 * 	{@link BeanProperty#swap() @BeanProperty.swap()} annotation, this method will return a String
	 * 	containing an ISO8601 date-time string value.
	 */
	@Override /* Map.Entry */
	public Object getValue() {
		return meta.get(this.beanMap);
	}

	/**
	 * Sets the value of this property.
	 * <p>
	 * If the property is an array of type {@code X}, then the value can be a {@code Collection<X>} or {@code X[]} or {@code Object[]}.
	 * <p>
	 * If the property is a bean type {@code X}, then the value can either be an {@code X} or a {@code Map}.
	 * <p>
	 * If there is a {@link PojoSwap} associated with this bean property or bean property type class, then
	 * 	you must pass in a transformed value.
	 * For example, if the bean property type class is a {@link Date} and the bean property has the
	 * 	{@link org.apache.juneau.transforms.DateSwap.ISO8601DT} swap associated with it through the
	 * 	{@link BeanProperty#swap() @BeanProperty.swap()} annotation, the value being passed in must be
	 * 	a String containing an ISO8601 date-time string value.
	 *
	 * @return  The set value after it's been converted.
	 */
	@Override /* Map.Entry */
	public Object setValue(Object value) {
		return meta.set(this.beanMap, value);
	}

	/**
	 * Returns the bean map that contains this property.
	 *
	 * @return The bean map that contains this property.
	 */
	public BeanMap<?> getBeanMap() {
		return this.beanMap;
	}

	/**
	 * Returns the metadata about this bean property.
	 *
	 * @return Metadata about this bean property.
	 */
	public BeanPropertyMeta getMeta() {
		return this.meta;
	}

	@Override /* Object */
	public String toString() {
		return this.getKey() + "=" + this.getValue();
	}
}