import pandas as pd
import numpy as np
import urllib
import logging as log
import sys
import datetime
# from logging import log
import dask.dataframe as dd

# from dask.distributed import Client, progress
# client = Client(n_workers=1, threads_per_worker=10, memory_limit='10GB')
# client

# 4:13 runtime without Dask

# Extract SPARQL queries from HTTP logs retrieved from Kibana/ElasticSearch
# Bio2RDF logs starting on 01/01/2019
start_time = datetime.datetime.now()
log.basicConfig(filename='run.log', filemode='w', level=log.DEBUG,
    datefmt='%Y-%m-%d %H:%M:%S', format='%(asctime)s %(levelname)-8s %(message)s')
log.getLogger().addHandler(log.StreamHandler(sys.stdout))

input_file = 'samples_sparql_logs.csv'
# input_file = 'original_bio2rdf_sparql_logs_09072021.csv'
# input_file = 'Get all SPARQL queries ran on Bio2RDF the last year.csv'

log.info('Load the logs file')
# input_df = pd.read_csv(input_file)
input_df = dd.read_csv(input_file)
log.info(len(input_df))
log.info('File loaded')
# print(input_df)

pandas_df = pd.DataFrame([], columns=['query', 'domain', 'agent', 'timestamp'])
# pandas_df = dd.DataFrame([], columns=['query', 'domain', 'agent'])
df = dd.from_pandas(pandas_df, npartitions=2, sort=False)
df = df.set_index('timestamp')

# Define a function to process the query column in each row (remove unecessary parts)
def process_query(query):
    if "query=" in query:
        # Remove the parts before query=
        query = query.split("query=",1)[1]
    query = urllib.parse.unquote_plus(query)
    # Make query a NaN to drop it later
    if "herbalife" in query.lower():
        query=np.nan
    return query

# Or use the url column
# /sparql?query=SELECT++%3Fs%0AWHERE%0A++%7B+%3Fs+%3Fp+%3Fo%7D%0ALIMIT+++1%0A
# df["query"] = input_df["url"].apply(lambda row: process_query(row), meta=('str'))
# https://stackoverflow.com/questions/45545110/make-pandas-dataframe-apply-use-all-cores
# .map_partitions().compute(get=get)
df["query"] = input_df.map_partitions(input_df["url"].apply(lambda row: process_query(row), meta=('str'))).compute(get=get)

log.info('Query extracted')

df["agent"] = input_df["agent"]
df["domain"] = input_df["domain"]
df["timestamp"] = input_df["@timestamp"]

# Drop NaN queries
df = df.dropna(axis=0, subset=['query'])
# df = df[~df.query.str.contains("Herbalife")]

log.info('Saving the file...')
log.info(len(df))

df.to_csv('bio2rdf_sparql_logs_01-2019_to_07-2021.csv')

run_time = datetime.datetime.now() - start_time
log.info('Ran for ' + str(run_time))