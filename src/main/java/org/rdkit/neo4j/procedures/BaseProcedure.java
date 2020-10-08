package org.rdkit.neo4j.procedures;

/*-
 * #%L
 * RDKit-Neo4j
 * %%
 * Copyright (C) 2019 RDKit
 * %%
 * Copyright (C) 2019 Evgeny Sorokin
 * @@ All Rights Reserved @@
 * This file is part of the RDKit Neo4J integration.
 * The contents are covered by the terms of the BSD license
 * which is included in the file LICENSE, found at the root
 * of the neo4j-rdkit source tree.
 * #L%
 */

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.internal.helpers.collection.Iterators;
import org.neo4j.internal.helpers.collection.MapUtil;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.rdkit.neo4j.models.Constants;
import org.rdkit.neo4j.models.NodeFields;
import org.rdkit.neo4j.utils.PagingIterator;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * BaseProcedure class
 * Stores reusable objects and defines conventions of operations (property names and batch tasks)
 */
public abstract class BaseProcedure {
    static final String fingerprintProperty = NodeFields.FingerprintEncoded.getValue();
    static final String fingerprintOnesProperty = NodeFields.FingerprintOnes.getValue();
    static final String canonicalSmilesProperty = NodeFields.CanonicalSmiles.getValue();
    static final String indexName = Constants.IndexName.getValue();

    static final int PAGE_SIZE = 10_000;

    @Context
    public Transaction tx;

    @Context
    public GraphDatabaseService db;

    @Context
    public Log log;

    /**
     * Method checks existence of nodeIndex
     * If it does not exist, fulltext query will not be executed (lucene does not contain the data)
     *
     * @param labelNames to query on
     * @param indexName  to look for
     */
    void checkIndexExistence(List<String> labelNames, String indexName) {
        Set<Label> labels = labelNames.stream().map(Label::label).collect(Collectors.toSet());

        try {
            IndexDefinition index = tx.schema().getIndexByName(indexName);
            assert index.isNodeIndex();
            assert StreamSupport.stream(index.getLabels().spliterator(), false).allMatch(labels::contains);
        } catch (AssertionError e) {
            log.error("No `%s` node index found", indexName);
            throw e;
        }
    }

    /**
     * Method creates a fulltext index in a db
     *
     * @param indexName  - name of the index
     * @param labelNames - labels
     * @param properties - properties to set index on top of
     */
    void createFullTextIndex(final String indexName, final List<String> labelNames, final List<String> properties) {
        Map<String, Object> params = MapUtil.map(
                "index", indexName,
                "labels", labelNames,
                "property", properties
        );

        tx.execute("CALL db.index.fulltext.createNodeIndex($index, $labels, $property, {analyzer: 'whitespace'} )", params);
//    tx.execute("CALL db.index.fulltext.createNodeIndex($index, $labels, $property, {analyzer: 'whitespace'} )", params);
    }

    /**
     * Method returns nodes with specified labels
     *
     * @param labelNames list
     * @return stream of nodes
     */
    Stream<Node> getLabeledNodes(List<String> labelNames) {
        final String firstLabel = labelNames.get(0);
        final List<Label> labels = labelNames.stream().map(Label::label).collect(Collectors.toList());

        return tx.findNodes(Label.label(firstLabel))
                .stream()
//        .parallel()
                .filter(node -> labels.stream().allMatch(node::hasLabel));
    }

    /**
     * Method allows to execute huge amount of transactions as a batch task.
     * As it is a batch process, it must be executed in a separate transaction, so a separate thread is created.
     * todo: refactor and speed up this process
     *
     * @param nodes      - to make updates on top of
     * @param batchSize  - default 10_000
     * @param nodeAction - Consumer on Node object
     * @throws InterruptedException if the thread is interrupted
     */
    void executeBatches(final Stream<Node> nodes, final int batchSize, Consumer<? super Node> nodeAction) throws InterruptedException {
        Iterator<Node> nodeIterator = nodes.iterator();
        final PagingIterator<Node> pagingIterator = new PagingIterator<>(nodeIterator, batchSize);
        int numberOfBatches = 0;

        while (pagingIterator.hasNext()) {
            try (Transaction localTx = db.beginTx()) {
                Iterator<Node> page = pagingIterator.nextPage();
                Iterators.stream(page).map(n -> localTx.getNodeById(n.getId())).forEach(nodeAction);

                numberOfBatches++;
                log.info("batch # %d", numberOfBatches);
                localTx.commit();
            }
        }
        log.info("done, ran %d batches successfully", numberOfBatches);
    }
}
