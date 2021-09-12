import urllib

link = "https://umids-download.137.120.31.102.nip.io/bio2rdf/logs/bio2rdf_sparql_logs_processed_01-2019_to_07-2021.csv"
f = urllib.urlopen(link)
myfile = f.read()
#print(myfile)