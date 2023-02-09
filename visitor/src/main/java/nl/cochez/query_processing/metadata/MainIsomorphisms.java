package nl.cochez.query_processing.metadata;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.apache.jena.query.QueryException;

import com.google.common.base.Stopwatch;

/**
 * 
 * 
 * 
 * @author cochez
 *
 */
public class MainIsomorphisms {

	public static void main(String[] args) throws IOException, QueryException {
		// String sparqlendpoint = "https://bio2rdf.org/sparql";
		String sparqlendpoint = "https://query.wikidata.org/sparql";
		String dict_name = "query_dict_wiki.index";
		// String dict_name = "query_dict.index";
		Stopwatch watch = Stopwatch.createStarted();
		org.apache.jena.query.ARQ.init();

		InputStream in = new FileInputStream("/home/jovyan/work/persistent/visitor/src/main/java/nl/cochez/query_processing/2017-06-12_2017-07-09_organic.tsv.gz");
		IsomorpismClusteringQueryCollector collector = new IsomorpismClusteringQueryCollector();

		IterateQueriesFromWikidataLog.processFromFile(new GZIPInputStream(in), collector);

		watch.stop();
		System.out.println("Elapsed" + watch.elapsed(TimeUnit.SECONDS));

		collector.stats();
		
		PatternDisplay.rankPattern(collector.queryList,10,5,5,true, sparqlendpoint, dict_name,null,null,null,null);
	}

}
