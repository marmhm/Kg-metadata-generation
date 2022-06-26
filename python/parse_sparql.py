import os
from rdflib.plugins.sparql.parser import Query, UpdateUnit
from rdflib.plugins.sparql.processor import translateQuery

os.system

query_string = 'SELECT * WHERE { ?s ?p ?o . ?s a ?type .} LIMIT 14'

parsed_query = translateQuery(Query.parseString(query_string, parseAll=True))

# print(Query.parseString(query_string, parseAll=True))
print(parsed_query.algebra)