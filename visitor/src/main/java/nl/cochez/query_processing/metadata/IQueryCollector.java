package nl.cochez.query_processing.metadata;

import java.util.ArrayList;

import org.apache.jena.query.Query;

public interface IQueryCollector {
	
	void add(Query q);

	void reportFailure(String input);

	void stats();

	ArrayList<Query> getQueryList();

}
