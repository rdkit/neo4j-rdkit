# RDKit-Neo4j project
[![Build Status](https://travis-ci.com/rdkit/neo4j-rdkit.svg?branch=master)](https://travis-ci.com/rdkit/neo4j-rdkit) `Open Chemistry`, `RDKit & Neo4j` GSoC 2019 project  

***
## Abstract
> The project will be focused on development of extension for neo4j graph database for querying knowledge graphs storing molecular and chemical information. That would be implemented on top of neo4j-java-driver.

> Task is to enable identification of entry points into the graph via exact/substructure/similarity searches (UC1). UC2 is closely related to UC1, but here the intention is to use chemical structures as limiting conditions in graph traversals originating from different entry points. Both use cases rely on the same integration of RDkit and Neo4j and will only differ in their CYPHER statements.

__Mentors:__
* Greg Landrum  
* Christian Pilger  
* Stefan Armbruster  

## Build

1) Install `lib/org.RDKit.jar` and `lib/org.RDKitDoc.jar` into your local maven repository  
```
mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file \
                         -Dfile=lib/org.RDKit.jar -DgroupId=org.rdkit \ 
                         -DartifactId=rdkit -Dversion=1.0.0 \
                         -Dpackaging=jar
                         
mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file \
                         -Dfile=lib/org.RDKitDoc.jar -DgroupId=org.rdkit \ 
                         -DartifactId=rdkit-doc -Dversion=1.0.0 \
                         -Dpackaging=jar
  ```
2) Generate .jar file with all dependencies with `mvn package`  

## Extension functionality

### Node labels: [`Chemical`, `Structure`] - strict rule (!)

* __Whenever a new node added with labels__, an `rdkit` event handler is applied and new node properties are constructed from `mdlmol` property.
Those are also reserved property names  

1) `canonical_smiles`  
2) `inchi`  
3) `formula`  
4) `molecular_weight`  
5) `fp` - bit-vector fingerprint in form of indexes of positive bits (`"1 4 19 23"`)  
6) `fp_ones` - count of positive bits  
7) `mdlmol`    

Additional reserved property names:

- `smiles`  

* If the graph was fulfilled with nodes before the extension was loaded, it is possible to apply a procedure:  
  `CALL org.rdkit.update(['Chemical', 'Structure'])` - which iterates through nodes with specified labels and creates properties described before.  

* In order to speed up an exact search, create an index on top of `canonical_smiles` property  

### User-defined procedures

1) `CALL org.rdkit.search.exact.smiles(['Chemical', 'Structure'], 'CC(=O)Nc1nnc(S(N)(=O)=O)s1')`
2) `CALL org.rdkit.search.exact.mol(['Chemical', 'Structure'], '<mdlmol block>')`
    * RDKit provides functionality to use `exact search` on top of `smiles` and `mdlmol blocks`, returns a node which satisfies `canonical smiles`  
3) `CALL org.rdkit.update(['Chemical', 'Structure'])`
    * Update procedure (manual properties initialization from `mdlmol` property)  
4) `CALL org.rdkit.search.createIndex(['Chemical', 'Structure'])`
    * Create fulltext index (called `rdkitIndex`) on property `fp`, which is required for substructure search  
    * Create index for `:Chemical(canonical_smiles)` property   
5)  `CALL org.rdkit.search.deleteIndex()`
        * Delete fulltext index (called `rdkitIndex`) on property `fp`, which is required for substructure search  
        * Delete index for `:Chemical(canonical_smiles)` property   
6) `CALL org.rdkit.search.substructure.smiles(['Chemical', 'Structure'], 'CC(=O)Nc1nnc(S(N)(=O)=O)s1')`  
    * Subscture search based on smiles substructure
7) `CALL org.rdkit.search.substructure.mol(['Chemical', 'Structure'], '<mol value>')`
    * Subscture search based on mdlmol block substructure
8) `CALL org.rdkit.fingerprint.create(['Chemical, 'Structure'], 'morgan_fp', 'morgan')`
    * Create a new property called `morgan_fp` with fingerprint type `morgan` on all nodes 
    * Creates fulltext index on this property. 
    * Node is skipped if it's not possible to convert its smiles with this fingerprint type
    * It is __not allowed__ to use property name equal to predefined 
9) User-defined function `org.rdkit.search.substructure.is(<node object>, '<smiles_string>')`
    * Return boolean answer: does specified `node` object have substructure match provided by `smiles_string`.

## Useful links:
- https://github.com/neo4j/neo4j  
- https://github.com/rdkit/org.rdkit.lucene  
