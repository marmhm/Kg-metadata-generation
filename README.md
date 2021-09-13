## Run the scripts

Install the packages:

```
pip install -r requirements.txt
```

Run the script.

```
python3 parse_sparql.py 
```



## Graph summarization Algorithm

Rule learner AnyBURL (Anytime Bottom Up Rule Learning):

https://web.informatik.uni-mannheim.de/AnyBURL/

## Use case and data sets

Bio2RDF Drug Bank data set:

* https://download.bio2rdf.org/#/release/4/drugbank/

Logs of Bio2RDF user's SPARQL queries: 

* https://umids-download.137.120.31.102.nip.io/bio2rdf/logs/

## Other tools

### Representing SPARQL as RDF: LSQ 

http://lsq.aksw.org/

Skip tests and build:

```bash
mvn -P bundle clean install -DskipTests
```

Run the built jar:

```bash
java -jar .\lsq-cli\target\lsq-cli-*-jar-with-dependencies.jar
```




