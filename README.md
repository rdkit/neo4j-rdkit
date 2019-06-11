# RDKit-Neo4j project
 `Open Chemistry`, `RDKit & Neo4j` GSoC 2019 project  

***

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
2) Generate .jar file with `mvn clean package`  

__Abstract:__
> The project will be focused on development of extension for neo4j graph database for querying knowledge graphs storing molecular and chemical information. That would be implemented on top of neo4j-java-driver.

> Task is to enable identification of entry points into the graph via exact/substructure/similarity searches (UC1). UC2 is closely related to UC1, but here the intention is to use chemical structures as limiting conditions in graph traversals originating from different entry points. Both use cases rely on the same integration of RDkit and Neo4j and will only differ in their CYPHER statements.

__Mentors:__
* Greg Landrum  
* Christian Pilger  
* Stefan Armbruster  


## Useful links:
- https://github.com/neo4j/neo4j  
- https://github.com/neo4j-contrib/neo4j-lucene5-index  

