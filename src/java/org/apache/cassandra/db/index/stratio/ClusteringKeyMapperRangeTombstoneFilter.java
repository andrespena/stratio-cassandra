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
package org.apache.cassandra.db.index.stratio;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.cassandra.db.RangeTombstone;
import org.apache.cassandra.db.index.stratio.util.ByteBufferUtils;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.OpenBitSet;

/**
 * {@link Filter} that filters documents which clustering key field satisfies a certain
 * {@link RangeTombstone}. This means that the clustering key value must be contained in the slice
 * query filter specified in the tombstone range.
 * 
 * @author Andres de la Pena <adelapena@stratio.com>
 * 
 */
public class ClusteringKeyMapperRangeTombstoneFilter extends Filter {

	/** The minimum accepted column name. */
	private final ByteBuffer min;

	/** The maximum accepted column name. */
	private final ByteBuffer max;

	/** The type of the column names to be filtered. */
	private final AbstractType<?> columnNameType;

	/**
	 * Returns a new {@code ClusteringKeyMapperRangeTombstoneFilter} for {@code rangeTombstone}
	 * using {@code clusteringKeyMapper}.
	 * 
	 * @param clusteringKeyMapper
	 *            The {@link ClusteringKeyMapper} to be used.
	 * @param rangeTombstone
	 *            The filtering tombstone range.
	 */
	public ClusteringKeyMapperRangeTombstoneFilter(ClusteringKeyMapper clusteringKeyMapper,
	                                               RangeTombstone rangeTombstone) {
		super();
		this.min = rangeTombstone.min;
		this.max = rangeTombstone.max;
		this.columnNameType = clusteringKeyMapper.getType();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DocIdSet getDocIdSet(AtomicReaderContext context, final Bits acceptDocs) throws IOException {
		AtomicReader atomicReader = context.reader();
		Bits liveDocs = atomicReader.getLiveDocs();

		OpenBitSet bitSet = new OpenBitSet(atomicReader.maxDoc());

		Terms terms = atomicReader.terms(ClusteringKeyMapper.FIELD_NAME);
		if (terms == null) {
			return null;
		}

		final TermsEnum termsEnum = terms.iterator(null);

		DocsEnum docsEnum = null;
		BytesRef bytesRef = termsEnum.next();

		while (bytesRef != null) {
			String string = bytesRef.utf8ToString();
			ByteBuffer value = ByteBufferUtils.fromString(string);
			boolean accepted = true;
			if (min != null && !ByteBufferUtils.isEmpty(min)) {
				accepted &= columnNameType.compare(min, value) <= 0;
			}
			if (max != null && !ByteBufferUtils.isEmpty(max)) {
				accepted &= columnNameType.compare(max, value) >= 0;
			}

			docsEnum = termsEnum.docs(liveDocs, docsEnum);
			if (accepted) {
				Integer docID = docsEnum.nextDoc();
				while (docID != DocIdSetIterator.NO_MORE_DOCS) {
					bitSet.set(docID);
					docID = docsEnum.nextDoc();
				}
			}
			bytesRef = termsEnum.next();
		}
		return bitSet;
	}

}
