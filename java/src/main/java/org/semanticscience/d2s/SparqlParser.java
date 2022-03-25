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
                  String fileName = "nodespatterns.txt";
                  Path filePath = Path.of(fileName);
                  // String text = "Welcome to geekforgeeks\nHappy Learning!";
                  // Files.writeString(filePath, text);
                  String file_content = Files.readString(filePath);
                  // System.out.println(file_content);
                  // Files.write(stateFile.toPath(), content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
                  System.out.println("Parsing SPARQL queries with RDF4J:");
                  //String sparqlQuery = "SELECT * WHERE { ?s ?p ?o . ?s a ?type.   . FILTER(?type = <http://ohoho>) . } LIMIT 14";

                  String sparqlQueries = Resources.toString(Resources.getResource("wholequery.rq"), StandardCharsets.UTF_8);
                  
                  SPARQLParserFactory factory = new SPARQLParserFactory();
                  QueryParser parser = factory.getParser();
                  // Path fileName1 = Path.of("patterns.json");
                  
                  
                  JSONArray allQueries = new JSONArray();
                  for (String sparqlQuery : sparqlQueries.split("##########")) {
                        // System.out.println(sparqlQuery);
                        ParsedQuery parsedQuery = parser.parseQuery(sparqlQuery, null);

                        StatementPatternCollector collector = new StatementPatternCollector();
                        TupleExpr tupleExpr = parsedQuery.getTupleExpr();
                        tupleExpr.visit(collector);

                        // JSONArray arr = new JSONArray();
                        for (StatementPattern pattern : collector.getStatementPatterns()) {
                              // System.out.println("Predicate:");
                              // System.out.println(pattern.getFil);
                              // System.out.println(pattern.getPredicateVar().getValue());
                              // System.out.println(pattern.getPredicateVar().getClass());
                              // System.out.println("");
                              // System.out.println("### NEW STATEMENT");
                              // System.out.println("Subject (name/value/class):");
                              // System.out.println(pattern.getSubjectVar().getName());
                              // System.out.println(pattern.getSubjectVar().getValue());
                              // System.out.println(pattern.getSubjectVar().getClass());
                              // System.out.println("");
                              // System.out.println("Predicate:");
                              // System.out.println(pattern.getPredicateVar().getName());
                              // System.out.println(pattern.getPredicateVar().getValue());
                              // System.out.println(pattern.getPredicateVar().getClass());
                              // System.out.println("");
                              // System.out.println("Object:");
                              // System.out.println(pattern.getObjectVar().getName());
                              // System.out.println(pattern.getObjectVar().getValue());
                              // System.out.println(pattern.getObjectVar().getClass());
                              // System.out.println("");
                              // System.out.println("Context:");
                              // System.out.println(pattern.getContextVar().getName());
                              // System.out.println(pattern.getContextVar().getValue());
                              // System.out.println(pattern.getContextVar().getClass());
                              // System.out.println("");
                              //for creating group pattern we creat json

                  //             JSONObject obj = new JSONObject();
                  //             obj.put("subjectValue", pattern.getSubjectVar().getValue());
                  //             obj.put("predicateValue", pattern.getPredicateVar().getValue());
                  //             obj.put("objectValue", pattern.getObjectVar().getValue());
                  //             arr.add(obj);
                  //       }
                  //       allQueries.add(arr);
                  // }
                  // Files.writeString(fileName1, text1);}
                  // Files.write(Paths.get(fileName), allQueries.toJSONString().getBytes(), StandardOpenOption.APPEND);
                              String b;
                              String bb;
                              String bbb;
                              String bbbb;
                              String bbbm;
                              if(pattern.getPredicateVar().getValue()!= null && pattern.getSubjectVar().getValue()!= null && pattern.getObjectVar().getValue()!= null){
                                  b= pattern.getPredicateVar().getValue()+"\r\n";
                                  bb= pattern.getSubjectVar().getValue()+"\r\n";
                                  bbb= pattern.getObjectVar().getValue()+"\r\n";
                                  bbbb=bb+bbb;
                                  Files.write(Paths.get(fileName), bbbb.getBytes(), StandardOpenOption.APPEND);}
                              else if (pattern.getPredicateVar().getValue()!= null && pattern.getSubjectVar().getValue()!= null && pattern.getObjectVar().getValue()== null){
                                    b= pattern.getPredicateVar().getValue()+"\r\n";
                                    bb= pattern.getSubjectVar().getValue()+"\r\n";
                                    bbbb=bb;
                                    Files.write(Paths.get(fileName), bbbb.getBytes(), StandardOpenOption.APPEND);}
                              else if (pattern.getPredicateVar().getValue()!= null && pattern.getSubjectVar().getValue()== null && pattern.getObjectVar().getValue()!= null){
                                    b= pattern.getPredicateVar().getValue()+"\r\n";
                                    bb= pattern.getObjectVar().getValue()+"\r\n";
                                    bbbb=bb;
                                    Files.write(Paths.get(fileName), bbbb.getBytes(), StandardOpenOption.APPEND);}
                              else if (pattern.getPredicateVar().getValue()== null && pattern.getSubjectVar().getValue()!= null && pattern.getObjectVar().getValue()!= null){
                                    b= pattern.getSubjectVar().getValue()+"\r\n";
                                    bb= pattern.getObjectVar().getValue()+"\r\n";
                                    bbbb=b+bb;
                                    Files.write(Paths.get(fileName), bbbb.getBytes(), StandardOpenOption.APPEND);}
                              else if (pattern.getPredicateVar().getValue()== null && pattern.getSubjectVar().getValue()== null && pattern.getObjectVar().getValue()!= null){
                                    bb= pattern.getObjectVar().getValue()+"\r\n";
                                    bbbb=bb;
                                    Files.write(Paths.get(fileName), bbbb.getBytes(), StandardOpenOption.APPEND);}
                              else if (pattern.getPredicateVar().getValue()== null && pattern.getSubjectVar().getValue()!= null && pattern.getObjectVar().getValue()== null){
                                    b= pattern.getSubjectVar().getValue()+"\r\n";
                                    bbbb=b;
                                    Files.write(Paths.get(fileName), bbbb.getBytes(), StandardOpenOption.APPEND);}
                              else if (pattern.getPredicateVar().getValue()!= null && pattern.getSubjectVar().getValue()== null && pattern.getObjectVar().getValue()== null){
                                    b= pattern.getPredicateVar().getValue()+"\r\n";
                                    
                                    bbbb=b;}
                                    // Files.write(Paths.get(fileName), bbbb.getBytes(), StandardOpenOption.APPEND);}
                                  }}

		} catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
		}
	}
	
}