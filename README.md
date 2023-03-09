# RDKit-Neo4j project
[![Build Status](https://travis-ci.com/rdkit/neo4j-rdkit.svg?branch=master)](https://travis-ci.com/rdkit/neo4j-rdkit) `Open Chemistry`, `RDKit & Neo4j` GSoC 2019 project  

***
## Abstract
> Chemical and pharmaceutical R&D produce large amounts of data of completely different nature, such as chemical structures, recipe and process data, formulation data, and data from various application tests. Altogether these data rarely follow a schema. Consequently, relational data models and databases have frequetly disadvantages mapping these data appropriately. Here, chemical data frequently leads to rather abstract data models, which are difficult to develop, align, and maintain with the domain experts. Upon retrieval computationally expesive joins in not predetermined depths may cause issues.  

> Graph data models promise here advantages:  
>  - they can easily be understood by non IT experts from the research domains  
>  - due to their plasticity, they can easily be extended and refactored  
>  - graph databases such as neo4j are made for coping with arbitrary path lengths  

> Chemical data models usually require a database to be able to deal with chemical structures to be utilized for structure based queries to either identify records or as filtering criteria.   

> The project will be focused on development of extension for neo4j graph database for querying knowledge graphs storing molecular and chemical information.
> Task is to enable identification of entry points into the graph via exact/substructure/similarity searches (UC1). UC2 is closely related to UC1, but here the intention is to use chemical structures as limiting conditions in graph traversals originating from different entry points. Both use cases rely on the same integration of RDkit and Neo4j and will only differ in their CYPHER statements.

__Mentors:__
* Greg Landrum  
* Christian Pilger  
* Stefan Armbruster  

## Build & run

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
3) Put generated .jar file into `plugins/` folder of your neo4j instance and start the server  
4) add `server.rdkit.index.sanitize=false` to `neo4j.conf`if you want to switch of sanitizing for indexing. If not provided `true` is assumed as default.
5) By executing `CALL dbms.procedures()`, you are expected to see `org.rdkit.*` procedures  

### usage within Docker

The native libraries of rdkit do have a dependency on [libFreetype](https://www.freetype.org/) and [libPng](http://www.libpng.org/pub/png/libpng.html). On desktop Linux systems those are typically installed by default. The [Neo4j docker image](https://hub.docker.com/_/neo4j) is based on `openjdk:11-jdk-slim` which itself is based on a minimal Debian linux image. This does not contain these to libraries. To solve that you need to make sure these packages get installed. 

In [docker_example](docker_example) there's a script [run_docker.sh](docker_example/run_docker.sh) mounting a volume with these debian packages and using an extension script to install these images upon startup of the docker container. Before using that make sure to populate the [plugins](docker_example/plugins) folder with the plugin's jar file.

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
2) `CALL org.rdkit.search.substructure.smiles(['Chemical', 'Structure'], 'CC(=O)Nc1nnc(S(N)(=O)=O)s1', <santize> (true/false))`  
3) `CALL org.rdkit.search.substructure.mol(['Chemical', 'Structure'], '<mol value>', <santize> (true/false))`  

#### Execution of similarity search (currently slow)

1) `CALL org.rdkit.fingerprint.create(['Chemical, 'Structure'], 'torsion_fp', 'torsion', <santize> (true/false))` - new property `torsion_fp` is created  
2) `CALL org.rdkit.fingerprint.search.smiles(['Chemical', 'Structure'], 'CC(=O)Nc1nnc(S(N)(=O)=O)s1', 'torsion', 'torsion_fp', 0.4, <santize> (true/false))`  
3) `CALL org.rdkit.fingerprint.search.smiles(['Chemical', 'Structure'], 'CC(=O)Nc1nnc(S(N)(=O)=O)s1', 'pattern', 'fp', 0.7, <santize> (true/false))`  

#### Usage of `org.rdkit.search.substructure.is.smiles` function in complex queries

```$cypher
CALL org.rdkit.search.exact.smiles(['Chemical', 'Structure'], 'CC(C)(C)OC(=O)N1CCC(COc2ccc(OCc3ccccc3)cc2)CC1') YIELD luri
MATCH (finalProduct:Entity{luri:luri})
CALL apoc.path.expand(finalProduct, "<HAS_PRODUCT,>HAS_INGREDIENT", ">Reaction", 0, 4) yield path
WITH nodes(path)[-1] as reaction, path, (length(path)+1)/2 as depths
MATCH (reaction)-[:HAS_INGREDIENT]->(c:Compound) where org.rdkit.search.substructure.is(c, 'CC(C)C(O)=O')
RETURN path
```

```$cypher
CALL org.rdkit.search.exact.smiles(['Chemical', 'Structure'], 'CC(C)(C)OC(=O)N1CCC(COc2ccc(OCc3ccccc3)cc2)CC1') YIELD luri
MATCH (finalProduct:Entity{luri:luri})
CALL apoc.path.expand(finalProduct, "<HAS_PRODUCT,>HAS_INGREDIENT", ">Reaction", 0, 4) yield path
WITH nodes(path)[-1] AS reaction, path, (length(path)+1)/2 AS depths
MATCH (reaction)-[:HAS_INGREDIENT]->(c:Compound)
WITH path, COLLECT(c) as compounds
WHERE ANY( x IN compounds where org.rdkit.search.substructure.is.mol(x, '
  Ketcher  9 71921 82D 1   1.00000     0.00000     0
  6  5  0     0  0            999 V2000
  8.9170  -12.3000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0
  9.7830  -11.8000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0
  10.6490  -12.3000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0
  9.7830  -10.8000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0
  10.6490  -10.3000    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0
  8.9170  -10.3000    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0
  1  2  1  0     0  0
  2  3  1  0     0  0
  2  4  1  0     0  0
  4  5  1  0     0  0
  4  6  2  0     0  0
  M  END'))
RETURN path
```

#### Usage of `org.rdkit.utils.svg` function

```$cypher
CALL org.rdkit.search.exact.smiles(['Chemical', 'Structure'], 'CCCC(C(=O)Nc1ccc(S(N)(=O)=O)cc1)C(C)(C)C') 
YIELD canonical_smiles 
RETURN org.rdkit.utils.svg(canonical_smiles) as svg
```


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
    * Supporting properties are: `morgan_fp_type`, `morgan_fp_ones` are also added  
    * Creates fulltext index on this property  
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
10) User-defined functions 
    * `org.rdkit.search.substructure.is.smiles(<node object>, '<smiles_string>')`
    * `org.rdkit.search.substructure.is.mol(<node object>, '<mol_string>')`
    * Return boolean answer: does specified `node` object have substructure match provided by `smiles_string` or `mol_string`.
11) User-defined function `org.rdkit.utils.svg('<smiles_string>')`  
    * Return svg image in text format from smiles  

---

# Results overview 

## What was achieved

1) Implementation of exact search (100%)  
2) Implementation of substructure search (90%, several minor bugs)  
3) Implementation of condition based graph traversal - usage of function calls in complex queries (100%)
4) Implementation of similarity search (70%, major performance issues)    
5) Coverage with unit tests (80%, not all invalid arguments for procedures are tested)

## What remains to be done

<!-- 0) Query features in substructure search (blocking of position in molecule from further substitution; using atom lists on certain positions in molecule) -->
1) Speed up batch tasks by utilizing several threads (currently waiting for resolving issue on native level)  
2) Speed up the `similarity search` procedures  
3) Solve minor bugs (todos) like unclosed `query` object during SSS  

## What problems were encountered

1) Compatability of native libraries for win64 (beginning of the development)  
2) Lazy streams evaluation and not resolved issue with `query` object during SSS  
3) Parallelization of stream evaluations    

## Java requirements

Plugin supports openjdk and oraclejdk java versions (< 12).  
Further versions upgraded _security sensitive fields_ [policy](https://bugs.openjdk.java.net/browse/JDK-8210496), those are currently not supported.  
