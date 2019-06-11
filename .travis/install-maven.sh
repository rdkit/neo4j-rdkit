#!/bin/bash
set -e

# script to install maven
TRAVIS_BUILD_DIR=$1
maven_fname="apache-maven-3.6.1-bin"
url="https://archive.apache.org/dist/maven/maven-3/3.6.1/binaries/apache-maven-3.6.1-bin.zip"
install_dir="${TRAVIS_BUILD_DIR}/../${maven_fname}"


mkdir ${install_dir}
curl -fsSL ${url} -o "${install_dir}.zip"
unzip "${install_dir}.zip"
rm "${install_dir}.zip"

mkdir tmp

cat << EOF > ${install_dir}/temp.sh
#!/bin/sh
export MAVEN_HOME=${install_dir}
export M2_HOME=${install_dir}
export M2=${install_dir}/bin
export PATH=${install_dir}/bin:"$PATH"
EOF

source ${install_dir}/temp.sh

echo Maven installed to ${install_dir}
mvn --version
