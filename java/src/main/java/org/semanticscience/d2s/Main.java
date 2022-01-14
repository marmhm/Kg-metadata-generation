package org.semanticscience.d2s;

import org.eclipse.rdf4j.query.parser.sparql.SPARQLParserFactory;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.algebra.helpers.StatementPatternCollector;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.StatementPattern;

public class Main {

	public static void main(String[] args) throws Exception {
		try { 
            System.out.println("Parsing SPARQL with RDF4J:");

            String sparqlQuery = "SELECT * WHERE { ?s ?p ?o . ?s a ?type. ?s ?p ?o  . FILTER(?type = <http://ohoho>) . } LIMIT 14";

            SPARQLParserFactory factory = new SPARQLParserFactory();
            QueryParser parser = factory.getParser();
            ParsedQuery parsedQuery = parser.parseQuery(sparqlQuery, null);

            StatementPatternCollector collector = new StatementPatternCollector();
            TupleExpr tupleExpr = parsedQuery.getTupleExpr();
            tupleExpr.visit(collector);

            for (StatementPattern pattern : collector.getStatementPatterns()) {
                System.out.println(pattern);
            }

		} catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
		}
	}
	
}