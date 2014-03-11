package org.apache.cassandra.db.index.stratio;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.cassandra.db.index.stratio.query.MatchQuery;
import org.apache.cassandra.db.index.stratio.query.RangeQuery;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * A {@link CellMapper} to map a date field.
 * 
 * @author adelapena
 */
public class CellMapperDate extends CellMapper<Long> {

	public static final String DEFAULT_PATTERN = "yyyy/MM/dd HH:mm:ss||yyyy/MM/dd";

	/** The date and time pattern. */
	@JsonProperty("pattern")
	private final String pattern;

	/** The thread safe date format */
	@JsonIgnore
	private final ThreadLocal<DateFormat> concurrentDateFormat;

	@JsonCreator
	public CellMapperDate(@JsonProperty("pattern") String pattern) {
		this.pattern = pattern == null ? DEFAULT_PATTERN : pattern;
		concurrentDateFormat = new ThreadLocal<DateFormat>() {
			@Override
			protected DateFormat initialValue() {
				return new SimpleDateFormat(CellMapperDate.this.pattern);
			}
		};
	}

	@Override
	public Analyzer analyzer() {
		return EMPTY_ANALYZER;
	}

	@Override
    public Field field(String name, Object value) {
		return new LongField(name, parseColumnValue(value), STORE);
	}

	@Override
	public Query query(MatchQuery matchQuery) {
		String name = matchQuery.getField();
		Long value = parseColumnValue(matchQuery.getValue());
		return NumericRangeQuery.newLongRange(name, value, value, true, true);
	}

	@Override
	public Query query(RangeQuery rangeQuery) {
		String name = rangeQuery.getField();
		Long lowerValue = parseColumnValue(rangeQuery.getLowerValue());
		Long upperValue = parseColumnValue(rangeQuery.getUpperValue());
		boolean includeLower = rangeQuery.getIncludeLower();
		boolean includeUpper = rangeQuery.getIncludeUpper();
		return NumericRangeQuery.newLongRange(name, lowerValue, upperValue, includeLower, includeUpper);
	}

	@Override
	protected Long parseColumnValue(Object value) {
		if (value == null) {
			return null;
		} else if (value instanceof Date) {
			return ((Date) value).getTime();
		} else if (value instanceof Number) {
			return ((Number) value).longValue();
		} else if (value instanceof String) {
			return parseQueryValue((String) value);
		} else {
			throw new MappingException("Value '%s' cannot be cast to Date", value);
		}
	}

	@Override
	protected Long parseQueryValue(String value) {
		if (value == null) {
			return null;
		} else {
			try {
				return concurrentDateFormat.get().parse(value).getTime();
			} catch (ParseException e) {
				throw new MappingException(e, "The string '%s' does not satisfy the pattern %s", value, pattern);
			}
		}
	}

	@Override
    public String toString() {
	    StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName());
	    builder.append(" [pattern=");
	    builder.append(pattern);
	    builder.append("]");
	    return builder.toString();
    }

}
