/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.xml;

import static javax.xml.stream.XMLStreamConstants.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.lang.reflect.*;
import java.util.*;

import javax.xml.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Parses text generated by the {@link XmlSerializer} class back into a POJO model.
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Content-Type</code> types: <code>text/xml</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	See the {@link XmlSerializer} class for a description of Juneau-generated XML.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link XmlParserContext}
 * 	<li>{@link BeanContext}
 * </ul>
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Consumes({"text/xml","application/xml"})
public class XmlParser extends ReaderParser {

	/** Default parser, all default settings.*/
	public static final XmlParser DEFAULT = new XmlParser().lock();

	private static final int UNKNOWN=0, OBJECT=1, ARRAY=2, STRING=3, NUMBER=4, BOOLEAN=5, NULL=6;


	private <T> T parseAnything(XmlParserSession session, ClassMeta<T> nt, String currAttr, XMLStreamReader r, Object outer, boolean isRoot) throws Exception {

		BeanContext bc = session.getBeanContext();
		if (nt == null)
			nt = (ClassMeta<T>)object();
		PojoSwap<T,Object> transform = (PojoSwap<T,Object>)nt.getPojoSwap();
		ClassMeta<?> ft = nt.getTransformedClassMeta();
		session.setCurrentClass(ft);

		String wrapperAttr = (isRoot && session.isPreserveRootElement()) ? r.getName().getLocalPart() : null;
		String typeAttr = r.getAttributeValue(null, "type");
		int jsonType = getJsonType(typeAttr);
		String b = r.getAttributeValue(session.getXsiNs(), "nil");
		if (b == null)
			b = r.getAttributeValue(null, "nil");
		boolean isNull = b != null && b.equals("true");
		if (jsonType == 0) {
			String elementName = session.decodeString(r.getLocalName());
			if (elementName == null || elementName.equals(currAttr))
				jsonType = UNKNOWN;
			else
				jsonType = getJsonType(elementName);
		}
		if (! ft.canCreateNewInstance(outer)) {
			String c = r.getAttributeValue(null, "_class");
			if (c != null) {
				ft = nt = (ClassMeta<T>)bc.getClassMetaFromString(c);
			}
		}
		Object o = null;

		if (jsonType == NULL) {
			r.nextTag();	// Discard end tag
			return null;
		}
		if (isNull) {
			while (true) {
				int e = r.next();
				if (e == END_ELEMENT)
					return null;
			}
		}

		if (ft.isObject()) {
			if (jsonType == OBJECT) {
				ObjectMap m = new ObjectMap(bc);
				parseIntoMap(session, r, m, string(), object());
				if (wrapperAttr != null)
					m = new ObjectMap(bc).append(wrapperAttr, m);
				o = m.cast();
			} else if (jsonType == ARRAY)
				o = parseIntoCollection(session, r, new ObjectList(bc), object());
			else if (jsonType == STRING) {
				o = session.decodeString(r.getElementText());
				if (ft.isChar())
					o = o.toString().charAt(0);
			}
			else if (jsonType == NUMBER)
				o = parseNumber(session.decodeLiteral(r.getElementText()), null);
			else if (jsonType == BOOLEAN)
				o = Boolean.parseBoolean(session.decodeLiteral(r.getElementText()));
			else if (jsonType == UNKNOWN)
				o = getUnknown(session, r);
		} else if (ft.isBoolean()) {
			o = Boolean.parseBoolean(session.decodeLiteral(r.getElementText()));
		} else if (ft.isCharSequence()) {
			o = session.decodeString(r.getElementText());
		} else if (ft.isChar()) {
			o = session.decodeString(r.getElementText()).charAt(0);
		} else if (ft.isMap()) {
			Map m = (ft.canCreateNewInstance(outer) ? (Map)ft.newInstance(outer) : new ObjectMap(bc));
			o = parseIntoMap(session, r, m, ft.getKeyType(), ft.getValueType());
			if (wrapperAttr != null)
				o = new ObjectMap(bc).append(wrapperAttr, m);
		} else if (ft.isCollection()) {
			Collection l = (ft.canCreateNewInstance(outer) ? (Collection)ft.newInstance(outer) : new ObjectList(bc));
			o = parseIntoCollection(session, r, l, ft.getElementType());
		} else if (ft.isNumber()) {
			o = parseNumber(session.decodeLiteral(r.getElementText()), (Class<? extends Number>)ft.getInnerClass());
		} else if (ft.canCreateNewInstanceFromObjectMap(outer)) {
			ObjectMap m = new ObjectMap(bc);
			parseIntoMap(session, r, m, string(), object());
			o = ft.newInstanceFromObjectMap(outer, m);
		} else if (ft.canCreateNewBean(outer)) {
			if (ft.getExtendedMeta(XmlClassMeta.class).getFormat() == XmlFormat.COLLAPSED) {
				String fieldName = r.getLocalName();
				BeanMap<?> m = bc.newBeanMap(outer, ft.getInnerClass());
				BeanPropertyMeta bpm = m.getMeta().getExtendedMeta(XmlBeanMeta.class).getPropertyMeta(fieldName);
				ClassMeta<?> cm = m.getMeta().getClassMeta();
				Object value = parseAnything(session, cm, currAttr, r, m.getBean(false), false);
				setName(cm, value, currAttr);
				bpm.set(m, value);
				o = m.getBean();
			} else {
				BeanMap m = bc.newBeanMap(outer, ft.getInnerClass());
				o = parseIntoBean(session, r, m).getBean();
			}
		} else if (ft.isArray()) {
			ArrayList l = (ArrayList)parseIntoCollection(session, r, new ArrayList(), ft.getElementType());
			o = bc.toArray(ft, l);
		} else if (ft.canCreateNewInstanceFromString(outer)) {
			o = ft.newInstanceFromString(outer, session.decodeString(r.getElementText()));
		} else if (ft.canCreateNewInstanceFromNumber(outer)) {
			o = ft.newInstanceFromNumber(outer, parseNumber(session.decodeLiteral(r.getElementText()), ft.getNewInstanceFromNumberClass()));
		} else {
			throw new ParseException(session, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''", ft.getInnerClass().getName(), ft.getNotABeanReason());
		}

		if (transform != null && o != null)
			o = transform.unswap(o, nt, bc);

		if (outer != null)
			setParent(nt, o, outer);

		return (T)o;
	}

	private <K,V> Map<K,V> parseIntoMap(XmlParserSession session, XMLStreamReader r, Map<K,V> m, ClassMeta<K> keyType, ClassMeta<V> valueType) throws Exception {
		BeanContext bc = session.getBeanContext();
		int depth = 0;
		for (int i = 0; i < r.getAttributeCount(); i++) {
			String a = r.getAttributeLocalName(i);
			// TODO - Need better handling of namespaces here.
			if (! (a.equals("type"))) {
				K key = session.trim(convertAttrToType(session, m, a, keyType));
				V value = session.trim(convertAttrToType(session, m, r.getAttributeValue(i), valueType));
				setName(valueType, value, key);
				m.put(key, value);
			}
		}
		do {
			int event = r.nextTag();
			String currAttr;
			if (event == START_ELEMENT) {
				depth++;
				currAttr = session.decodeString(r.getLocalName());
				K key = convertAttrToType(session, m, currAttr, keyType);
				V value = parseAnything(session, valueType, currAttr, r, m, false);
				setName(valueType, value, currAttr);
				if (valueType.isObject() && m.containsKey(key)) {
					Object o = m.get(key);
					if (o instanceof List)
						((List)o).add(value);
					else
						m.put(key, (V)new ObjectList(o, value).setBeanContext(bc));
				} else {
					m.put(key, value);
				}
			} else if (event == END_ELEMENT) {
				depth--;
				return m;
			}
		} while (depth > 0);
		return m;
	}

	private <E> Collection<E> parseIntoCollection(XmlParserSession session, XMLStreamReader r, Collection<E> l, ClassMeta<E> elementType) throws Exception {
		int depth = 0;
		do {
			int event = r.nextTag();
			if (event == START_ELEMENT) {
				depth++;
				E value = parseAnything(session, elementType, null, r, l, false);
				l.add(value);
			} else if (event == END_ELEMENT) {
				depth--;
				return l;
			}
		} while (depth > 0);
		return l;
	}

	private Object[] doParseArgs(XmlParserSession session, XMLStreamReader r, ClassMeta<?>[] argTypes) throws Exception {
		int depth = 0;
		Object[] o = new Object[argTypes.length];
		int i = 0;
		do {
			int event = r.nextTag();
			if (event == START_ELEMENT) {
				depth++;
				o[i] = parseAnything(session, argTypes[i], null, r, null, false);
				i++;
			} else if (event == END_ELEMENT) {
				depth--;
				return o;
			}
		} while (depth > 0);
		return o;
	}

	private int getJsonType(String s) {
		if (s == null)
			return UNKNOWN;
		char c = s.charAt(0);
		switch(c) {
			case 'o': return (s.equals("object") ? OBJECT : UNKNOWN);
			case 'a': return (s.equals("array") ? ARRAY : UNKNOWN);
			case 's': return (s.equals("string") ? STRING : UNKNOWN);
			case 'b': return (s.equals("boolean") ? BOOLEAN : UNKNOWN);
			case 'n': {
				c = s.charAt(2);
				switch(c) {
					case 'm': return (s.equals("number") ? NUMBER : UNKNOWN);
					case 'l': return (s.equals("null") ? NULL : UNKNOWN);
				}
				//return NUMBER;
			}
		}
		return UNKNOWN;
	}

	private <T> BeanMap<T> parseIntoBean(XmlParserSession session, XMLStreamReader r, BeanMap<T> m) throws Exception {
		BeanMeta<?> bMeta = m.getMeta();
		XmlBeanMeta xmlMeta = bMeta.getExtendedMeta(XmlBeanMeta.class);

		for (int i = 0; i < r.getAttributeCount(); i++) {
			String key = session.decodeString(r.getAttributeLocalName(i));
			String val = r.getAttributeValue(i);
			BeanPropertyMeta bpm = xmlMeta.getPropertyMeta(key);
			if (bpm == null) {
				if (m.getMeta().isSubTyped()) {
					m.put(key, val);
				} else {
					Location l = r.getLocation();
					onUnknownProperty(session, key, m, l.getLineNumber(), l.getColumnNumber());
				}
			} else {
				bpm.set(m, val);
			}
		}

		BeanPropertyMeta cp = xmlMeta.getXmlContentProperty();
		if (cp != null) {
			XmlContentHandler h = xmlMeta.getXmlContentHandler();
			if (h != null) {
				h.parse(r, m.getBean());
			} else {
				String text = r.getElementText();
				cp.set(m, session.decodeString(text));
			}
			return m;
		}

		int depth = 0;
		do {
			int event = r.nextTag();
			String currAttr;
			if (event == START_ELEMENT) {
				depth++;
				currAttr = session.decodeString(r.getLocalName());
				BeanPropertyMeta pMeta = xmlMeta.getPropertyMeta(currAttr);
				if (pMeta == null) {
					if (m.getMeta().isSubTyped()) {
						Object value = parseAnything(session, string(), currAttr, r, m.getBean(false), false);
						m.put(currAttr, value);
					} else {
						Location l = r.getLocation();
						onUnknownProperty(session, currAttr, m, l.getLineNumber(), l.getColumnNumber());
						skipCurrentTag(r);
					}
				} else {
					session.setCurrentProperty(pMeta);
					XmlFormat xf = pMeta.getExtendedMeta(XmlBeanPropertyMeta.class).getXmlFormat();
					if (xf == COLLAPSED) {
						ClassMeta<?> et = pMeta.getClassMeta().getElementType();
						Object value = parseAnything(session, et, currAttr, r, m.getBean(false), false);
						setName(et, value, currAttr);
						pMeta.add(m, value);
					} else if (xf == ATTR)  {
						pMeta.set(m, session.decodeString(r.getAttributeValue(0)));
						r.nextTag();
					} else {
						ClassMeta<?> cm = pMeta.getClassMeta();
						Object value = parseAnything(session, cm, currAttr, r, m.getBean(false), false);
						setName(cm, value, currAttr);
						pMeta.set(m, value);
					}
					session.setCurrentProperty(null);
				}
			} else if (event == END_ELEMENT) {
				depth--;
				return m;
			}
		} while (depth > 0);
		return m;
	}

	private void skipCurrentTag(XMLStreamReader r) throws XMLStreamException {
		int depth = 1;
		do {
			int event = r.next();
			if (event == START_ELEMENT)
				depth++;
			else if (event == END_ELEMENT)
				depth--;
		} while (depth > 0);
	}

	private Object getUnknown(XmlParserSession session, XMLStreamReader r) throws Exception {
		BeanContext bc = session.getBeanContext();
		if (r.getEventType() != XMLStreamConstants.START_ELEMENT) {
			throw new XMLStreamException("parser must be on START_ELEMENT to read next text", r.getLocation());
		}
		ObjectMap m = null;

		// If this element has attributes, then it's always an ObjectMap.
		if (r.getAttributeCount() > 0) {
			m = new ObjectMap(bc);
			for (int i = 0; i < r.getAttributeCount(); i++) {
				String key = session.decodeString(r.getAttributeLocalName(i));
				String val = r.getAttributeValue(i);
				if (! key.equals("type"))
					m.put(key, val);
			}
		}
		int eventType = r.next();
		StringBuilder sb = new StringBuilder();
		while (eventType != XMLStreamConstants.END_ELEMENT) {
			if (eventType == XMLStreamConstants.CHARACTERS || eventType == XMLStreamConstants.CDATA || eventType == XMLStreamConstants.SPACE || eventType == XMLStreamConstants.ENTITY_REFERENCE) {
				sb.append(r.getText());
			} else if (eventType == XMLStreamConstants.PROCESSING_INSTRUCTION || eventType == XMLStreamConstants.COMMENT) {
				// skipping
			} else if (eventType == XMLStreamConstants.END_DOCUMENT) {
				throw new XMLStreamException("Unexpected end of document when reading element text content", r.getLocation());
			} else if (eventType == XMLStreamConstants.START_ELEMENT) {
				// Oops...this has an element in it.
				// Parse it as a map.
				if (m == null)
					m = new ObjectMap(bc);
				int depth = 0;
				do {
					int event = (eventType == -1 ? r.nextTag() : eventType);
					String currAttr;
					if (event == START_ELEMENT) {
						depth++;
						currAttr = session.decodeString(r.getLocalName());
						String key = convertAttrToType(session, null, currAttr, string());
						Object value = parseAnything(session, object(), currAttr, r, null, false);
						if (m.containsKey(key)) {
							Object o = m.get(key);
							if (o instanceof ObjectList)
								((ObjectList)o).add(value);
							else
								m.put(key, new ObjectList(o, value).setBeanContext(bc));
						} else {
							m.put(key, value);
						}

					} else if (event == END_ELEMENT) {
						depth--;
						break;
					}
					eventType = -1;
				} while (depth > 0);
				break;
			} else {
				throw new XMLStreamException("Unexpected event type " + eventType, r.getLocation());
			}
			eventType = r.next();
		}
		String s = sb.toString();
		s = session.decodeString(s);
		if (m != null) {
			if (! s.isEmpty())
				m.put("contents", s);
			return m;
		}
		return s;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	public XmlParserSession createSession(Object input, ObjectMap properties, Method javaMethod, Object outer) {
		return new XmlParserSession(getContext(XmlParserContext.class), getBeanContext(), input, properties, javaMethod, outer);
	}

	@Override /* Parser */
	protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
		XmlParserSession s = (XmlParserSession)session;
		type = s.getBeanContext().normalizeClassMeta(type);
		return parseAnything(s, type, null, s.getXmlStreamReader(), s.getOuter(), true);
	}

	@Override /* ReaderParser */
	protected <K,V> Map<K,V> doParseIntoMap(ParserSession session, Map<K,V> m, Type keyType, Type valueType) throws Exception {
		XmlParserSession s = (XmlParserSession)session;
		ClassMeta cm = s.getBeanContext().getMapClassMeta(m.getClass(), keyType, valueType);
		return parseIntoMap(s, m, cm.getKeyType(), cm.getValueType());
	}

	@Override /* ReaderParser */
	protected <E> Collection<E> doParseIntoCollection(ParserSession session, Collection<E> c, Type elementType) throws Exception {
		XmlParserSession s = (XmlParserSession)session;
		ClassMeta cm = s.getBeanContext().getCollectionClassMeta(c.getClass(), elementType);
		return parseIntoCollection(s,c, cm.getElementType());
	}

	@Override /* ReaderParser */
	protected Object[] doParseArgs(ParserSession session, ClassMeta<?>[] argTypes) throws Exception {
		XmlParserSession s = (XmlParserSession)session;
		return doParseArgs(s, s.getXmlStreamReader(), argTypes);
	}

	@Override /* CoreApi */
	public XmlParser setProperty(String property, Object value) throws LockedException {
		super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public XmlParser setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public XmlParser addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public XmlParser addBeanFilters(Class<?>...classes) throws LockedException {
		super.addBeanFilters(classes);
		return this;
	}

	@Override /* CoreApi */
	public XmlParser addPojoSwaps(Class<?>...classes) throws LockedException {
		super.addPojoSwaps(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> XmlParser addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public XmlParser setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public XmlParser lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public XmlParser clone() {
		try {
			XmlParser c = (XmlParser)super.clone();
			return c;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen.
		}
	}
}
