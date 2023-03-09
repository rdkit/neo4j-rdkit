package org.rdkit.neo4j.utils;

/*-
 * #%L
 * RDKit-Neo4j plugin
 * %%
 * Copyright (C) 2019 - 2020 RDKit
 * %%
 * Copyright (C) 2019 Evgeny Sorokin
 * @@ All Rights Reserved @@
 * This file is part of the RDKit Neo4J integration.
 * The contents are covered by the terms of the BSD license
 * which is included in the file LICENSE, found at the root
 * of the neo4j-rdkit source tree.
 * #L%
 */

import org.neo4j.internal.helpers.collection.PrefetchingIterator;

import java.util.Iterator;

public class PagingIterator<T> extends CachingIterator<T>
{
    private final int pageSize;

    /**
     * Creates a new paging iterator with {@code source} as its underlying
     * {@link Iterator} to lazily get items from.
     *
     * @param source the underlying {@link Iterator} to lazily get items from.
     * @param pageSize the max number of items in each page.
     */
    public PagingIterator( Iterator<T> source, int pageSize )
    {
        super( source );
        this.pageSize = pageSize;
    }

    /**
     * @return the page the iterator is currently at, starting a {@code 0}.
     * This value is based on the {@link #position()} and the page size.
     */
    public int page()
    {
        return position() / pageSize;
    }

    /**
     * Sets the current page of the iterator. {@code 0} means the first page.
     * @param newPage the current page to set for the iterator, must be
     * non-negative. The next item returned by the iterator will be the first
     * item in that page.
     * @return the page before changing to the new page.
     */
    public int page( int newPage )
    {
        int previousPage = page();
        position( newPage * pageSize );
        return previousPage;
    }

    /**
     * Returns a new {@link Iterator} instance which exposes the current page
     * as its own iterator, which fetches items lazily from the underlying
     * iterator. It is discouraged to use an {@link Iterator} returned from
     * this method at the same time as using methods like {@link #next()} or
     * {@link #previous()}, where the results may be unpredictable. So either
     * use only {@link #nextPage()} (in conjunction with {@link #page(int)} if
     * necessary) or go with regular {@link #next()}/{@link #previous()}.
     *
     * @return the next page as an {@link Iterator}.
     */
    public Iterator<T> nextPage()
    {
        page( page() );
        return new PrefetchingIterator<T>()
        {
            private final int end = position() + pageSize;

            @Override
            protected T fetchNextOrNull()
            {
                if ( position() >= end )
                {
                    return null;
                }
                return PagingIterator.this.hasNext() ? PagingIterator.this.next() : null;
            }
        };
    }
}
