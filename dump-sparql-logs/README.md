# ✨ Dump SPARQL logs

### Dump logs from ElasticSearch

This step will depends on the infrastructure you use to store the logs.

In this example the logs are stored in a ElasticSearch instance deployed using docker-compose

To dump the logs fromour ElasticSearch we used https://github.com/elasticsearch-dump/elasticsearch-dump

We added an entry to the docker-compose file to connect to ElasticSearch and dumps the logs:

```yaml
version: "3"

services: 
  dump-elasticsearch:
    image: elasticdump/elasticsearch-dump
    container_name: dump-elasticsearch
    command:
      - --input=http://elasticsearch:9200/_all
      - --output=csv:///data/dump_sparql_logs.csv
      - "--searchBody=@/data/searchbody.json"
      - "--overwrite"
    networks: 
      - net
    environment: 
      - NODE_TLS_REJECT_UNAUTHORIZED=0
      - LOG4J_FORMAT_MSG_NO_LOOKUPS=true
    volumes: 
      - ./dumps/searchbody.json:/data/searchbody.json
```

It uses the query defined in `searchbody.json`, this query has been currently written to extract logs only from bio2rdf.org, you will need to change it to adapt it to your logs.

Run it with:

```bash
docker-compose run dump-elasticsearch
```

### Prepare logs as CSV

We need to preprocess the dump to make sure it uses the expected CSV format, and remove any identifiable informations

1. Install dependencies and download data:

```bash
# With conda
conda install --yes --file requirements.txt
# Or pip
pip install -r requirements.txt
```

2. Run script to extract SPARQL queries and remove personal informations:

```bash
python3 extract_sparql_queries.py
```

Output goes in the `data` folder

If the logs are really big you can easily generate a sample for quicker processing during the development process: 

```bash
head -n 10000 sparql_logs.csv > sample_sparql_logs.csv
```

