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
package org.apache.juneau.pojotools;

import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;

import org.apache.juneau.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Tests the PojoPaginator class.
 */
public class PojoViewerTest {

	PojoViewer p = new PojoViewer();
	BeanSession bs = BeanContext.DEFAULT.createBeanSession();

	//-----------------------------------------------------------------------------------------------------------------
	// Null input
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void nullInput() {
		assertNull(p.run(bs, null, null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple bean
	//-----------------------------------------------------------------------------------------------------------------

	public static class A {
		public String f1,f2;

		public static A create(String f1, String f2) {
			A a = new A();
			a.f1 = f1;
			a.f2 = f2;
			return a;
		}
	}

	@Test
	public void simpleBean() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = A.create("x1","x2");
		assertObjectEquals("{f1:'x1'}", p.run(bs, in, sa));
	}

	@Test
	public void simpleBean_reverseColumns() {
		ViewArgs sa = new ViewArgs("f2","f1");
		Object in = A.create("x1","x2");
		assertObjectEquals("{f2:'x2',f1:'x1'}", p.run(bs, in, sa));
	}

	@Test
	public void simpleBean_dupColumns() {
		ViewArgs sa = new ViewArgs("f1","f1");
		Object in = A.create("x1","x2");
		assertObjectEquals("{f1:'x1'}", p.run(bs, in, sa));
	}

	@Test
	public void simpleBean_nonExistentColumns() {
		ViewArgs sa = new ViewArgs("fx");
		Object in = A.create("x1","x2");
		assertObjectEquals("{}", p.run(bs, in, sa));
	}

	@Test
	public void simpleBean_nullColumn() {
		ViewArgs sa = new ViewArgs("f1",null);
		Object in = A.create("x1","x2");
		assertObjectEquals("{f1:'x1'}", p.run(bs, in, sa));
	}

	@Test
	public void simpleBean_emptyArgs() {
		ViewArgs sa = new ViewArgs();
		Object in = A.create("x1","x2");
		assertObjectEquals("{}", p.run(bs, in, sa));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple BeanMap
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void simpleBeanMap() {
		ViewArgs sa = new ViewArgs("f1");
		Object in = bs.toBeanMap(A.create("x1","x2"));
		assertObjectEquals("{f1:'x1'}", p.run(bs, in, sa));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple map
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void simpleMap() {
		ViewArgs sa = new ViewArgs("f1");
		Object in = AMap.create().append("f1","x1").append("f2","x2");
		assertObjectEquals("{f1:'x1'}", p.run(bs, in, sa));
	}

	@Test
	public void simpleMap_reverseColumns() {
		ViewArgs sa = new ViewArgs("f2","f1");
		Object in = AMap.create().append("f1","x1").append("f2","x2");
		assertObjectEquals("{f2:'x2',f1:'x1'}", p.run(bs, in, sa));
	}

	@Test
	public void simpleMap_nonExistentColumns() {
		ViewArgs sa = new ViewArgs("fx");
		Object in = AMap.create().append("f1","x1").append("f2","x2");
		assertObjectEquals("{}", p.run(bs, in, sa));
	}

	@Test
	public void simpleMap_nullColumn() {
		ViewArgs sa = new ViewArgs("f1",null);
		Object in = AMap.create().append("f1","x1").append("f2","x2");
		assertObjectEquals("{f1:'x1'}", p.run(bs, in, sa));
	}

	@Test
	public void simpleMap_emptyView() {
		ViewArgs sa = new ViewArgs();
		Object in = AMap.create().append("f1","x1").append("f2","x2");
		assertObjectEquals("{}", p.run(bs, in, sa));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean array
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void beanArray() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = new A[]{A.create("x1","x2")};
		assertObjectEquals("[{f1:'x1'}]", p.run(bs, in, sa));
	}

	@Test
	public void beanArray_reverseColumns() {
		ViewArgs sa = new ViewArgs("f2","f1");
		Object in = new A[]{A.create("x1","x2")};
		assertObjectEquals("[{f2:'x2',f1:'x1'}]", p.run(bs, in, sa));
	}

	@Test
	public void beanArray_dupColumns() {
		ViewArgs sa = new ViewArgs("f1","f1");
		Object in = new A[]{A.create("x1","x2")};
		assertObjectEquals("[{f1:'x1'}]", p.run(bs, in, sa));
	}

	@Test
	public void beanArray_nonExistentColumns() {
		ViewArgs sa = new ViewArgs("fx");
		Object in = new A[]{A.create("x1","x2")};
		assertObjectEquals("[{}]", p.run(bs, in, sa));
	}

	@Test
	public void beanArray_nullColumn() {
		ViewArgs sa = new ViewArgs("f1",null);
		Object in = new A[]{A.create("x1","x2")};
		assertObjectEquals("[{f1:'x1'}]", p.run(bs, in, sa));
	}

	@Test
	public void beanArray_emptyArgs() {
		ViewArgs sa = new ViewArgs();
		Object in = new A[]{A.create("x1","x2")};
		assertObjectEquals("[{}]", p.run(bs, in, sa));
	}

	@Test
	public void beanArray_withNull() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = new A[]{A.create("x1","x2"),null};
		assertObjectEquals("[{f1:'x1'},null]", p.run(bs, in, sa));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean list
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void beanList() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = AList.create(A.create("x1","x2"));
		assertObjectEquals("[{f1:'x1'}]", p.run(bs, in, sa));
	}

	@Test
	public void beanList_reverseColumns() {
		ViewArgs sa = new ViewArgs("f2","f1");
		Object in = AList.create(A.create("x1","x2"));
		assertObjectEquals("[{f2:'x2',f1:'x1'}]", p.run(bs, in, sa));
	}

	@Test
	public void beanList_dupColumns() {
		ViewArgs sa = new ViewArgs("f1","f1");
		Object in = AList.create(A.create("x1","x2"));
		assertObjectEquals("[{f1:'x1'}]", p.run(bs, in, sa));
	}

	@Test
	public void beanList_nonExistentColumns() {
		ViewArgs sa = new ViewArgs("fx");
		Object in = AList.create(A.create("x1","x2"));
		assertObjectEquals("[{}]", p.run(bs, in, sa));
	}

	@Test
	public void beanList_nullColumn() {
		ViewArgs sa = new ViewArgs("f1",null);
		Object in = AList.create(A.create("x1","x2"));
		assertObjectEquals("[{f1:'x1'}]", p.run(bs, in, sa));
	}

	@Test
	public void beanList_emptyArgs() {
		ViewArgs sa = new ViewArgs();
		Object in = AList.create(A.create("x1","x2"));
		assertObjectEquals("[{}]", p.run(bs, in, sa));
	}

	@Test
	public void beanList_withNull() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = AList.create(A.create("x1","x2"),null);
		assertObjectEquals("[{f1:'x1'},null]", p.run(bs, in, sa));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean set
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void beanSet() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = ASet.create(A.create("x1","x2"));
		assertObjectEquals("[{f1:'x1'}]", p.run(bs, in, sa));
	}

	@Test
	public void beanSet_reverseColumns() {
		ViewArgs sa = new ViewArgs("f2","f1");
		Object in = ASet.create(A.create("x1","x2"));
		assertObjectEquals("[{f2:'x2',f1:'x1'}]", p.run(bs, in, sa));
	}

	@Test
	public void beanSet_dupColumns() {
		ViewArgs sa = new ViewArgs("f1","f1");
		Object in = ASet.create(A.create("x1","x2"));
		assertObjectEquals("[{f1:'x1'}]", p.run(bs, in, sa));
	}

	@Test
	public void beanSet_nonExistentColumns() {
		ViewArgs sa = new ViewArgs("fx");
		Object in = ASet.create(A.create("x1","x2"));
		assertObjectEquals("[{}]", p.run(bs, in, sa));
	}

	@Test
	public void beanSet_nullColumn() {
		ViewArgs sa = new ViewArgs("f1",null);
		Object in = ASet.create(A.create("x1","x2"));
		assertObjectEquals("[{f1:'x1'}]", p.run(bs, in, sa));
	}

	@Test
	public void beanSet_emptyArgs() {
		ViewArgs sa = new ViewArgs();
		Object in = ASet.create(A.create("x1","x2"));
		assertObjectEquals("[{}]", p.run(bs, in, sa));
	}

	@Test
	public void beanSet_withNull() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = ASet.create(A.create("x1","x2"),null);
		assertObjectEquals("[{f1:'x1'},null]", p.run(bs, in, sa));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other object
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void otherObject() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = "foobar";
		assertObjectEquals("'foobar'", p.run(bs, in, sa));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Map list
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void mapList() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = AList.create(AMap.create().append("f1","x1").append("f2","x2"));
		assertObjectEquals("[{f1:'x1'}]", p.run(bs, in, sa));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BeanMap list
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void beanMapList() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = AList.create(bs.toBeanMap(A.create("x1","x2")));
		assertObjectEquals("[{f1:'x1'}]", p.run(bs, in, sa));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other object list
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void otherObjectList() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = AList.create("foobar");
		assertObjectEquals("['foobar']", p.run(bs, in, sa));
	}

}
