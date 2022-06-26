
rm -rf target
rm -rf **/.ipynb_checkpoints

mvn clean package
# mvn package 
java -jar target/metadata-0.0.1-SNAPSHOT.jar
java -jar target/sparql-parser-0.0.1-jar-with-dependencies.jar