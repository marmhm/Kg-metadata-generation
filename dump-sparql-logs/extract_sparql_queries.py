import pandas as pd
import numpy as np
import urllib
import logging as log
import os
import sys
import datetime
from pandarallel import pandarallel

# os.environ["MODIN_ENGINE"] = "dask"
# os.environ["RAY_OBJECT_STORE_ALLOW_SLOW_STORAGE"] = "1"
# import modin.pandas as pd

input_file = 'data/full_bio2rdf_sparql_logs_09072021.csv'
# input_file = 'samples_sparql_logs.csv'

output_file = 'data/bio2rdf_sparql_logs_01-2019_to_07-2021.csv'

# 4:13 runtime wihout pandarallel
# 2:18 with pandarallel use_memory_fs=False (no dshm set)

pandarallel.initialize()
# pandarallel.initialize(use_memory_fs=False)

# Extract SPARQL queries from HTTP logs retrieved from Kibana/ElasticSearch
# Bio2RDF logs starting on 01/01/2019
start_time = datetime.datetime.now()
log.basicConfig(filename='data/run.log', filemode='w', level=log.DEBUG,
    datefmt='%Y-%m-%d %H:%M:%S', format='%(asctime)s %(levelname)-8s %(message)s')
log.getLogger().addHandler(log.StreamHandler(sys.stdout))

log.info('Load the logs file')
input_df = pd.read_csv(input_file)
log.info(len(input_df))
log.info('File loaded')
# print(input_df)

df = pd.DataFrame([], columns = ['query', 'domain', 'agent'])

# Define a function to process the query column in each row (remove unecessary parts)
def process_query(query):
    if "query=" in query:
        # Remove the parts before query=
        query = query.split("query=",1)[1]
    query = urllib.parse.unquote_plus(query)
    # Make query a NaN to drop it later
    if "herbalife" in query.lower():
        # Remove some spam queries from herbalife pyramid scheme
        query=np.nan
    return query

## Pre-process the query
# With regular pandas and modin:
# df["query"] = input_df["url"].apply(lambda row: process_query(row))
# With pandarallel:
df["query"] = input_df["url"].parallel_apply(lambda row: process_query(row))
# Without pre-processing:
# df["query"] = input_df["url"]

log.info('Query extracted')

df["agent"] = input_df["agent"]
df["domain"] = input_df["domain"]
df["timestamp"] = input_df["@timestamp"]

# Drop NaN queries (for pre-process)
df = df.dropna(axis=0, subset=['query'])

log.info('Saving the file...')
log.info(len(df))

df.to_csv(output_file, index=False)

run_time = datetime.datetime.now() - start_time
log.info('Ran for ' + str(run_time))
