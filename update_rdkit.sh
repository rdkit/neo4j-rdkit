#!/bin/sh

# download rdkit lib files and java bindings, amend java bindings to local maven repo

curl https://github.com/rdkit/knime-rdkit/raw/dev/new_stuff/org.rdkit.knime.bin.linux.x86_64/os/linux/x86_64/libGraphMolWrap.so --output native/linux.x86_64/libGraphMolWrap.so
curl https://github.com/rdkit/knime-rdkit/raw/dev/new_stuff/org.rdkit.knime.bin.macosx.x86_64/os/macosx/x86_64/libGraphMolWrap.jnilib --output native/macosx.x86_64/libGraphMolWrap.jnilib
curl https://github.com/rdkit/knime-rdkit/raw/dev/new_stuff/org.rdkit.knime.bin.win32.x86_64/os/win32/x86_64/GraphMolWrap.dll --output native/win32.x86_64/GraphMolWrap.dll

curl https://github.com/rdkit/knime-rdkit/raw/dev/new_stuff/org.rdkit.knime.types/lib/org.RDKit.jar --output lib/org.RDKit.jar
curl https://github.com/rdkit/knime-rdkit/raw/dev/new_stuff/org.rdkit.knime.types/lib/org.RDKitDoc.jar --output lib/org.RDKitDoc.jar

mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file \
                         -Dfile=lib/org.RDKit.jar -DgroupId=org.rdkit \
                         -DartifactId=rdkit -Dversion=1.0.0 \
                         -Dpackaging=jar

mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file \
                         -Dfile=lib/org.RDKitDoc.jar -DgroupId=org.rdkit \
                         -DartifactId=rdkit-doc -Dversion=1.0.0 \
                         -Dpackaging=jar
