import urllib

link = "https://download.dumontierlab.com/bio2rdf/logs/bio2rdf_sparql_logs_processed_01-2019_to_07-2021.csv"
f = urllib.urlopen(link)
myfile = f.read()
#print(myfile)