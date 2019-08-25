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

### User scenario:

#### Feeding the data into database

##### way A: 
1) Plugin not present  
2) Feed Neo4j DB  
3) then `CALL org.rdkit.update(['Chemical', 'Structure'])` & `CALL org.rdkit.search.createIndex(['Structure', 'Chemical'])`  

> That triggers computation of additional properties (fp, etc.) and fp index creation  
> Automated computation of properties enabled only after `update` procedure  

##### way B: 
1) Plugin present  
2) Feed Neo4j DB  
3) then `CALL org.rdkit.search.createIndex(['Structure', 'Chemical'])`  

> Automated computation of additional properties (fp, etc.) and triggered index
> Fp index automatically updated when new :Structure:Chemical records arrive

##### way C (the most suitable)
1) Plugin present
2) `CALL org.rdkit.search.createIndex(['Structure', 'Chemical'])`
3) Then feed Knime

> Automated computation of additional properties (fp, etc.) and index
> Empty Neo4j instance is prepared in advance
> Whenever a new :Structure:Chemical entries comes, property calculation and fp index update are automatically conducted

#### Execution of exact search 
_It is possible to check index existence with `CALL db.indexes`_

0) It would strongly affect performance of exact search if `createIndex` procedure was called earlier (it creates a property index).
1) `CALL org.rdkit.search.exact.smiles(['Chemical', 'Structure'], 'CC(=O)Nc1nnc(S(N)(=O)=O)s1')`
2) `CALL org.rdkit.search.exact.mol(['Chemical', 'Structure'], '<mdlmol>')` (refer to tests for examples)

#### Execution of substructure search

1) Make sure the fulltext index exists with `CALL db.indexes`, `fp_index` must exist. (It should be created with `createIndex` procedure)  
2) `CALL org.rdkit.search.substructure.smiles(['Chemical', 'Structure'], 'CC(=O)Nc1nnc(S(N)(=O)=O)s1')`
3) `CALL org.rdkit.search.substructure.mol(['Chemical', 'Structure'], '<mol value>')`

#### Execution of similarity search (currently slow)

1) `CALL org.rdkit.fingerprint.create(['Chemical, 'Structure'], 'torsion_fp', 'torsion')` - new property `morgan_fp` is created
2) `CALL org.rdkit.fingerprint.search.smiles(['Chemical', 'Structure'], 'CC(=O)Nc1nnc(S(N)(=O)=O)s1', 'torsion', 'torsion_fp', 0.4)`
3) `CALL org.rdkit.fingerprint.search.smiles(['Chemical', 'Structure'], 'CC(=O)Nc1nnc(S(N)(=O)=O)s1', 'pattern', 'fp', 0.7)`

---
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

### User-defined procedures & functions

1) `CALL org.rdkit.search.exact.smiles(['Chemical', 'Structure'], 'CC(=O)Nc1nnc(S(N)(=O)=O)s1')`
2) `CALL org.rdkit.search.exact.mol(['Chemical', 'Structure'], '<mdlmol block>')`
    * RDKit provides functionality to use `exact search` on top of `smiles` and `mdlmol blocks`, returns a node which satisfies `canonical smiles`  
3) `CALL org.rdkit.update(['Chemical', 'Structure'])`
    * Update procedure (manual properties initialization from `mdlmol` property) 
    * _Current implementation uses single thread and on a huge database may take a lot of time (>3 minutes)_
4) `CALL org.rdkit.search.createIndex(['Chemical', 'Structure'])`
    * Create fulltext index (called `rdkitIndex`) on property `fp`, which is required for substructure search  
    * Create index for `:Chemical(canonical_smiles)` property   
5)  `CALL org.rdkit.search.deleteIndex()`
        * Delete fulltext index (called `rdkitIndex`) on property `fp`, which is required for substructure search  
        * Delete index for `:Chemical(canonical_smiles)` property   
6) `CALL org.rdkit.search.substructure.smiles(['Chemical', 'Structure'], 'CC(=O)Nc1nnc(S(N)(=O)=O)s1')`  
    * SSS based on smiles substructure
7) `CALL org.rdkit.search.substructure.mol(['Chemical', 'Structure'], '<mol value>')`
    * SSS based on mdlmol block substructure
8) `CALL org.rdkit.fingerprint.create(['Chemical, 'Structure'], 'morgan_fp', 'morgan')`
    * Create a new property called `morgan_fp` with fingerprint type `morgan` on all nodes 
    * Creates fulltext index on this property. 
    * Node is skipped if it's not possible to convert its smiles with this fingerprint type
    * It is __not allowed__ to use property name equal to predefined 
9) `CALL org.rdkit.fingerprint.search.smiles(['Chemical', 'Structure'], 'CC(=O)Nc1nnc(S(N)(=O)=O)s1', 'pattern', 'fp', 0.7)`
    * Call similarity search with next parameters:  
      - Node labels: `['Chemical', 'Structure']`  
      - Smiles: `'CC(=O)Nc1nnc(S(N)(=O)=O)s1'`  
      - Fingerprint type: `'pattern'`  
      - Property name: `'fp'`  
      - Threshold: `0.7`  
    * Smiles value is converted into specfied _fingerprint type_ (if possible) and compared with nodes which have _property_ (`'fp'` in this case)  
    * Threshold is a lower bound for the score value  
    * _Current implementation uses single thread and on a huge database may take a lot of time (>3 minutes)_
10) User-defined function `org.rdkit.search.substructure.is(<node object>, '<smiles_string>')`
    * Return boolean answer: does specified `node` object have substructure match provided by `smiles_string`.

## Useful links:
- https://github.com/neo4j/neo4j  
- https://github.com/rdkit/org.rdkit.lucene  
