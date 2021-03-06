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
package org.apache.cassandra.db.index.stratio.query;

import org.apache.cassandra.db.index.stratio.schema.CellMapper;
import org.apache.cassandra.db.index.stratio.schema.Schema;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * A {@link Condition} implementation that matches documents containing a value for a field.
 * 
 * @author Andres de la Pena <adelapena@stratio.com>
 */
public class MatchCondition extends Condition {

	/** The field name */
	private final String field;

	/** The field value */
	private Object value;

	/**
	 * Constructor using the field name and the value to be matched.
	 * 
	 * @param boost
	 *            The boost for this query clause. Documents matching this clause will (in addition
	 *            to the normal weightings) have their score multiplied by {@code boost}. If
	 *            {@code null}, then {@link DEFAULT_BOOST} is used as default.
	 * @param field
	 *            the field name.
	 * @param value
	 *            the field value.
	 */
	@JsonCreator
	public MatchCondition(@JsonProperty("boost") Float boost,
	                      @JsonProperty("field") String field,
	                      @JsonProperty("value") Object value) {
		super(boost);
		this.field = field;
		this.value = value;
	}

	/**
	 * Returns the field name.
	 * 
	 * @return the field name.
	 */
	public String getField() {
		return field;
	}

	/**
	 * Returns the field value.
	 * 
	 * @return the field value.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Query query(Schema schema) {

		if (field == null || field.trim().isEmpty()) {
			throw new IllegalArgumentException("Field name required");
		}
		if (value == null || value instanceof String && ((String) value).trim().isEmpty()) {
			throw new IllegalArgumentException("Field value required");
		}

		CellMapper<?> cellMapper = schema.getMapper(field);
		Class<?> clazz = cellMapper.baseClass();
		Query query;
		if (clazz == String.class) {
			String value = (String) cellMapper.queryValue(field, this.value);
			String analyzedValue = analyze(field, value, schema.analyzer());
			if (analyzedValue == null) {
				throw new IllegalArgumentException("Value discarded by analyzer");
			}
			Term term = new Term(field, analyzedValue);
			query = new TermQuery(term);
		} else if (clazz == Integer.class) {
			Integer value = (Integer) cellMapper.queryValue(field, this.value);
			query = NumericRangeQuery.newIntRange(field, value, value, true, true);
		} else if (clazz == Long.class) {
			Long value = (Long) cellMapper.queryValue(field, this.value);
			query = NumericRangeQuery.newLongRange(field, value, value, true, true);
		} else if (clazz == Float.class) {
			Float value = (Float) cellMapper.queryValue(field, this.value);
			query = NumericRangeQuery.newFloatRange(field, value, value, true, true);
		} else if (clazz == Double.class) {
			Double value = (Double) cellMapper.queryValue(field, this.value);
			query = NumericRangeQuery.newDoubleRange(field, value, value, true, true);
		} else {
			String message = String.format("Match queries are not supported by %s mapper", clazz.getSimpleName());
			throw new UnsupportedOperationException(message);
		}
		query.setBoost(boost);
		return query;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName());
		builder.append(" [boost=");
		builder.append(boost);
		builder.append(", field=");
		builder.append(field);
		builder.append(", value=");
		builder.append(value);
		builder.append("]");
		return builder.toString();
	}

}