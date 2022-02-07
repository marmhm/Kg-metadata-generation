package org.semanticscience.d2s;

import org.eclipse.rdf4j.query.parser.sparql.SPARQLParserFactory;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.algebra.helpers.StatementPatternCollector;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
// import org.json.simple.parser.JSONParser;
// import org.json.simple.parser.ParseException;
// import java.nio.file.File;
// import java.lang.ClassLoader;
import com.google.common.io.Resources;
import java.nio.charset.StandardCharsets;

public class SparqlParser {

      public static String readFile(String filename) throws Exception {
            return Files.readString(Paths.get(SparqlParser.class.getClassLoader().getResource(filename).toURI()));
      }

	public static void main(String[] args) throws Exception {
		try { 
                  String fileName = "pattern.json";
                  Path filePath = Path.of(fileName);
                  // String text = "Welcome to geekforgeeks\nHappy Learning!";
                  // Files.writeString(filePath, text);
                  String file_content = Files.readString(filePath);
                  System.out.println(file_content);
                  // Files.write(stateFile.toPath(), content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
                  System.out.println("Parsing SPARQL queries with RDF4J:");
                  //String sparqlQuery = "SELECT * WHERE { ?s ?p ?o . ?s a ?type.   . FILTER(?type = <http://ohoho>) . } LIMIT 14";

                  String sparqlQueries = Resources.toString(Resources.getResource("query1.rq"), StandardCharsets.UTF_8);
                  
                  SPARQLParserFactory factory = new SPARQLParserFactory();
                  QueryParser parser = factory.getParser();
                  // Path fileName1 = Path.of("patterns.json");
                  for (String sparqlQuery : sparqlQueries.split("##########")) {
                        // System.out.println(sparqlQuery);
                        ParsedQuery parsedQuery = parser.parseQuery(sparqlQuery, null);

                        StatementPatternCollector collector = new StatementPatternCollector();
                        TupleExpr tupleExpr = parsedQuery.getTupleExpr();
                        tupleExpr.visit(collector);

                        JSONArray arr = new JSONArray();
                        for (StatementPattern pattern : collector.getStatementPatterns()) {
                              System.out.println("### NEW STATEMENT");
                              System.out.println("Subject (name/value/class):");
                              System.out.println(pattern.getSubjectVar().getName());
                              System.out.println(pattern.getSubjectVar().getValue());
                              System.out.println(pattern.getSubjectVar().getClass());
                              System.out.println("");
                              System.out.println("Predicate:");
                              System.out.println(pattern.getPredicateVar().getName());
                              System.out.println(pattern.getPredicateVar().getValue());
                              System.out.println(pattern.getPredicateVar().getClass());
                              System.out.println("");
                              System.out.println("Object:");
                              System.out.println(pattern.getObjectVar().getName());
                              System.out.println(pattern.getObjectVar().getValue());
                              System.out.println(pattern.getObjectVar().getClass());
                              System.out.println("");
                              // System.out.println("Context:");
                              // System.out.println(pattern.getContextVar().getName());
                              // System.out.println(pattern.getContextVar().getValue());
                              // System.out.println(pattern.getContextVar().getClass());
                              // System.out.println("");

                              JSONObject obj = new JSONObject();
                              obj.put("subjectName", pattern.getSubjectVar().getName().toString());
                              obj.put("predicateName", pattern.getPredicateVar().getName().toString());
                              obj.put("objectName", pattern.getObjectVar().getName().toString());
                              arr.add(obj);
                        
                        }
                        // Files.writeString(fileName1, text1);}
                        Files.write(Paths.get(fileName), arr.toJSONString().getBytes(), StandardOpenOption.APPEND);
                  }

		} catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
		}
	}
	
}