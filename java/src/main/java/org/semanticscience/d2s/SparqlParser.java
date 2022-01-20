package org.semanticscience.d2s;

import org.eclipse.rdf4j.query.parser.sparql.SPARQLParserFactory;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.algebra.helpers.StatementPatternCollector;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import java.nio.file.Files;
import java.nio.file.Paths;
// import java.lang.ClassLoader;
import com.google.common.io.Resources;
import java.nio.charset.StandardCharsets;

public class SparqlParser {

      public static String readFile(String filename) throws Exception {
            return Files.readString(Paths.get(SparqlParser.class.getClassLoader().getResource(filename).toURI()));
      }

	public static void main(String[] args) throws Exception {
		try { 
            System.out.println("Parsing SPARQL with RDF4J:");
            
            //String sparqlQuery = "SELECT * WHERE { ?s ?p ?o . ?s a ?type.   . FILTER(?type = <http://ohoho>) . } LIMIT 14";
            // ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            // System.out.println(classLoader.getResource("query1.rq").toURI());

            // System.out.println(Paths.get(SparqlParser.class.getClassLoader().getResource("query1.rq").toURI()));
            // String sparqlQuery = Files.readString(Paths.get(SparqlParser.class.getClassLoader().getResource("query1.rq").toURI()));
            // String sparqlQuery = Files.readString(Paths.get(new ClassPathResource("query1.rq").getURI()));
            // String sparqlQuery = readFile("query1.rq");

            // URL url = Resources.getResource("query1.rq");
            String sparqlQuery = Resources.toString(Resources.getResource("query1.rq"), StandardCharsets.UTF_8);
            System.out.println(sparqlQuery);
            
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