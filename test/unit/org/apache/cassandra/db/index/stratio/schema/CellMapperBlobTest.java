/*
 * Copyright 2014, Stratio.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.db.index.stratio.schema;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.Hex;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.junit.Assert;
import org.junit.Test;

public class CellMapperBlobTest {

	@Test()
	public void testValueNull() {
		CellMapperBlob mapper = new CellMapperBlob();
		String parsed = mapper.indexValue("test", null);
		Assert.assertNull(parsed);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValueInteger() {
		CellMapperBlob mapper = new CellMapperBlob();
		mapper.indexValue("test", 3);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValueLong() {
		CellMapperBlob mapper = new CellMapperBlob();
		mapper.indexValue("test", 3l);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValueFloat() {
		CellMapperBlob mapper = new CellMapperBlob();
		mapper.indexValue("test", 3.5f);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValueDouble() {
		CellMapperBlob mapper = new CellMapperBlob();
		mapper.indexValue("test", 3.6d);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValueUUID() {
		CellMapperBlob mapper = new CellMapperBlob();
		mapper.indexValue("test", UUID.randomUUID());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValueStringInvalid() {
		CellMapperBlob mapper = new CellMapperBlob();
		mapper.indexValue("test", "Hello");
	}

	@Test
	public void testValueStringLowerCaseWithoutPrefix() {
		CellMapperBlob mapper = new CellMapperBlob();
		String parsed = mapper.indexValue("test", "f1");
		Assert.assertEquals("f1", parsed);
	}

	@Test
	public void testValueStringUpperCaseWithoutPrefix() {
		CellMapperBlob mapper = new CellMapperBlob();
		String parsed = mapper.indexValue("test", "F1");
		Assert.assertEquals("f1", parsed);
	}

	@Test
	public void testValueStringMixedCaseWithoutPrefix() {
		CellMapperBlob mapper = new CellMapperBlob();
		String parsed = mapper.indexValue("test", "F1a2B3");
		Assert.assertEquals("f1a2b3", parsed);
	}

	@Test
	public void testValueStringLowerCaseWithPrefix() {
		CellMapperBlob mapper = new CellMapperBlob();
		String parsed = mapper.indexValue("test", "0xf1");
		Assert.assertEquals("f1", parsed);
	}

	@Test
	public void testValueStringUpperCaseWithPrefix() {
		CellMapperBlob mapper = new CellMapperBlob();
		String parsed = mapper.indexValue("test", "0xF1");
		Assert.assertEquals("f1", parsed);
	}

	@Test
	public void testValueStringMixedCaseWithPrefix() {
		CellMapperBlob mapper = new CellMapperBlob();
		String parsed = mapper.indexValue("test", "0xF1a2B3");
		Assert.assertEquals("f1a2b3", parsed);
	}

	@Test(expected = NumberFormatException.class)
	public void testValueStringOdd() {
		CellMapperBlob mapper = new CellMapperBlob();
		mapper.indexValue("test", "f");
	}

	@Test
	public void testValueByteBuffer() {
		CellMapperBlob mapper = new CellMapperBlob();
		ByteBuffer bb = ByteBufferUtil.hexToBytes("f1");
		String parsed = mapper.indexValue("test", bb);
		Assert.assertEquals("f1", parsed);
	}

	@Test
	public void testValueBytes() {
		CellMapperBlob mapper = new CellMapperBlob();
		byte[] bytes = Hex.hexToBytes("f1");
		String parsed = mapper.indexValue("test", bytes);
		Assert.assertEquals("f1", parsed);
	}

	@Test
	public void testField() {
		CellMapperBlob mapper = new CellMapperBlob();
		Field field = mapper.field("name", "f1B2");
		Assert.assertNotNull(field);
		Assert.assertEquals("f1b2", field.stringValue());
		Assert.assertEquals("name", field.name());
		Assert.assertEquals(false, field.fieldType().stored());
	}

	@Test
	public void testExtractAnalyzers() {
		CellMapperBlob mapper = new CellMapperBlob();
		Analyzer analyzer = mapper.analyzer();
		Assert.assertEquals(CellMapper.EMPTY_ANALYZER, analyzer);
	}

	@Test
	public void testParseJSON() throws IOException {
		String json = "{fields:{age:{type:\"bytes\"}}}";
		Schema schema = Schema.fromJson(json);
		CellMapper<?> cellMapper = schema.getMapper("age");
		Assert.assertNotNull(cellMapper);
		Assert.assertEquals(CellMapperBlob.class, cellMapper.getClass());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseJSONEmpty() throws IOException {
		String json = "{fields:{}}";
		Schema schema = Schema.fromJson(json);
		schema.getMapper("age");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseJSONInvalid() throws IOException {
		String json = "{fields:{age:{}}";
		Schema.fromJson(json);
	}
}
