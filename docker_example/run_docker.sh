docker run --rm \
--name neo4j \
-e NEO4J_AUTH=neo4j/123 \
-e NEO4J_ACCEPT_LICENSE_AGREEMENT=yes \
-e NEO4J_dbms_security_procedures_unrestricted=\\\* \
-e EXTENSION_SCRIPT=/deb/install_debs.sh \
-v $PWD/plugins:/plugins \
-v $PWD/deb:/deb \
-p 7474:7474 \
-p 7687:7687 \
neo4j:4.1.3-enterprise
#--user=$(id -u):$(id -g) \
# enable for remote debugging:
#-e NEO4J_dbms_jvm_additional=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=\\\*:5005 \
#-p 5005:5005 \
