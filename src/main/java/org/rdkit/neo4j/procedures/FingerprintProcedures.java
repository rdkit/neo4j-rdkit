package org.rdkit.neo4j.procedures;

import static org.rdkit.neo4j.models.NodeFields.CanonicalSmiles;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.neo4j.graphdb.Node;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.rdkit.fingerprint.FingerprintType;
import org.rdkit.neo4j.models.Constants;
import org.rdkit.neo4j.models.LuceneQuery;
import org.rdkit.neo4j.models.NodeFields;
import org.rdkit.neo4j.utils.Converter;

public class FingerprintProcedures extends BaseProcedure {

  @Procedure(name = "org.rdkit.fingerprint.create", mode = Mode.SCHEMA) // todo: is it SCHEMA or WRITE ? (create index + create property)
  @Description("RDKit create a `fpType` fingerprint and add to all nodes with `labelNames` a property `propertyName` with specified fingerprint. "
      + "Creates a fulltext index on that property. \n"
      + "Possible values for `fpType`: ['morgan', 'topological', 'pattern']. \n"
      + "Restriction for `propertyName`: it must not be equal to rdkit properties of nodes.")
  public void createFingerprintProperty(@Name("label") List<String> labelNames, @Name("propertyName") String propertyName, @Name("fingerprintType") String fpType) throws InterruptedException {
    log.info("Create fingerprint property with parameters: labelsNames=%s, propertyName=%s, fingerprintType=%s", labelNames, propertyName, fpType);

    // start checking parameters
    checkPropertyName(propertyName);

    FingerprintType fingerprintType = FingerprintType.parseString(fpType);
    if (fingerprintType == null) {
      throw new IllegalStateException(String.format("Fingerprint type=%s not found", fpType)); // todo: logically there should be thrown IllegalArgumentException...
    }
    // end checking parameters

    Stream<Node> nodes = getLabeledNodes(labelNames);
    Converter converter = Converter.createConverter(fingerprintType);

    executeBatches(nodes, PAGE_SIZE, node -> {
      final String smiles = (String) node.getProperty(CanonicalSmiles.getValue());
      final LuceneQuery fp = converter.getLuceneFingerprint(smiles);
      // todo: save fp_type?
      node.setProperty(propertyName + "_ones", fp.getPositiveBits()); // TODO: THINK ABOUT STANDARTIZATION
      node.setProperty(propertyName + "_type", fingerprintType.toString()); // TODO: THINK ABOUT STANDARTIZATION
      node.setProperty(propertyName, fp.getLuceneQuery());
    });

    final String indexName = propertyName; // todo: how should I name it each time unique, may be use property as a name for this index ?
    createFullTextIndex(indexName, labelNames, Collections.singletonList(propertyName));
  }

  private void checkPropertyName(final String propertyName) {
    try {
      Constants.valueOf(propertyName);
      throw new IllegalStateException("This property name is protected");
    } catch (IllegalArgumentException e) {
      // no intersection of names found, valueOf thrown an exception
    }

    try {
      NodeFields.valueOf(propertyName);
      throw new IllegalStateException("This property name is protected");
    } catch (IllegalArgumentException e) {
      // no intersection of names found, valueOf thrown an exception
    }
  }
}
