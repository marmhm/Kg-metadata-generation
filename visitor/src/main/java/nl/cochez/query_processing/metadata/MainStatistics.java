package nl.cochez.query_processing.metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.ext.com.google.common.collect.Iterables;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.apache.jena.sparql.exec.http.QuerySendMode;

import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Multiset.Entry;

public class MainStatistics {

	private static class StatVisitor extends AllBGPOpVisitor {

		Multiset<String> subjects = HashMultiset.create();
		Multiset<String> predicates = HashMultiset.create();
		Multiset<String> objects = HashMultiset.create();
		Multiset<String> literal_values = HashMultiset.create();
		Multiset<String> literal_labels = HashMultiset.create();
		Multiset<String> languages = HashMultiset.create();
		Multiset<String> types = HashMultiset.create();
		Multiset<String> rdf_types = HashMultiset.create();

		@Override
		public void visit(OpBGP opBGP) {
			for (Triple triple : opBGP.getPattern().getList()) {
				Node s = triple.getSubject();
				if (s.isURI()) {
					subjects.add(s.getURI());
				} else if (s.isVariable()) {
					// TODO
				} else {
					// blank nodes ingored
				}
				Node p = triple.getPredicate();
				if (p.isURI()) {
					predicates.add(p.getURI());
				} else if (p.isVariable()) {
					// TODO
				} else {
					throw new AssertionError("This should never happen");
				}
				Node o = triple.getObject();
				if (o.isURI()) {
					objects.add(o.getURI());
				} else if (o.isVariable()) {
					// TODO
				} else if (o.isLiteral()) {
					LiteralLabel l = o.getLiteral();
					literal_values.add(l.getLexicalForm());
					String type = l.getDatatypeURI();
					if (type == null) {
						type = "no-type";
					}
					String language = l.language();
					if (language.equals("")) {
						language = "no-language-tag";
					}

					types.add(type);
					languages.add(language);
					literal_labels.add(type + "---" + language);

				} else {
					// blank nodes ingored
				}
				
				// find all RDF types
				
				if (p.isURI() && p.getURI().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
					if (o.isURI()) {
						String rdf_type = o.getURI();
						rdf_types.add(rdf_type);
					}
				}
			}

		}
	};

	public static void main(String[] args) throws IOException {
		// String sparqlendpoint = "https://bio2rdf.org/sparql";
		String sparqlendpoint = "https://query.wikidata.org/sparql";
		String dict_name = "query_dict_wiki.index";
		// String dict_name = "query_dict.index";
		Stopwatch watch = Stopwatch.createStarted();
		String filename = "/home/coder/project/2017-06-12_2017-07-09_organic.tsv.gz";
		// String filename = "drugbank_test.tsv.gz";
		if (args.length > 0) {
			filename = args[0];
		}

		List<String> stop_list = new ArrayList<String>();
		List<String> ptop_list = new ArrayList<String>();
		List<String> otop_list = new ArrayList<String>();
		List<String> typetop_list = new ArrayList<String>();
		
		InputStream in = new FileInputStream(filename);
        
		final StatVisitor visitor = new StatVisitor();
		
		IQueryCollector collector = new IQueryCollector() {
			ArrayList<Query> queryList = new ArrayList<Query>();
			int failures = 0;

//			@Override
//			public void stats() {
//				System.out.println("subjects " +  Iterables.limit(Multisets.copyHighestCountFirst(visitor.subjects).entrySet(), 100));
//				System.out.println("predicates " + visitor.predicates);
//				System.out.println("objects " + visitor.objects);
//				System.out.println("literals" + visitor.literal_values);
//				System.out.println("languages" + visitor.languages);
//				System.out.println("types" + visitor.types);
//				System.out.println("literal_labels" + visitor.literal_labels);
//				System.out.println("rdf_types" + Multisets.copyHighestCountFirst(visitor.rdf_types));
//				
//				System.out.println("Number of failures is : " + failures);
//			}
			@Override
			public void stats() {
				PrintStream ps_console = System.out;
				File file = new File("statistics.txt");
				FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(file, true);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				PrintStream ps = new PrintStream(fos);
				System.setOut(ps);
				System.out.println("subject,label,frequency");
				System.out.println(Multisets.copyHighestCountFirst(visitor.subjects).entrySet().size());
				Iterable<Entry<String>> subjets = Iterables.limit(Multisets.copyHighestCountFirst(visitor.subjects).entrySet(), Multisets.copyHighestCountFirst(visitor.subjects).entrySet().size());
				stop_list.addAll(get_top(subjets, 10, sparqlendpoint));
				print_with_label(subjets,"s",100,sparqlendpoint);
				System.out.println("predicate,label,frequency");
				Iterable<Entry<String>> predicates = Iterables.limit(Multisets.copyHighestCountFirst(visitor.predicates).entrySet(), Multisets.copyHighestCountFirst(visitor.subjects).entrySet().size());
				ptop_list.addAll(get_top(predicates, 10, sparqlendpoint));
				System.out.println(Multisets.copyHighestCountFirst(visitor.predicates).entrySet().size());
				print_with_label(predicates,"p",100,sparqlendpoint);
				System.out.println("object,label,frequency");
				System.out.println(Multisets.copyHighestCountFirst(visitor.objects).entrySet().size());
				Iterable<Entry<String>> objects = Iterables.limit(Multisets.copyHighestCountFirst(visitor.objects).entrySet(), Multisets.copyHighestCountFirst(visitor.objects).entrySet().size());
				otop_list.addAll(get_top_type(objects, 10, sparqlendpoint));
				print_with_label(objects,"o",100,sparqlendpoint);
				System.out.println("literal,label,frequency");
				System.out.println(Multisets.copyHighestCountFirst(visitor.literal_values).entrySet().size());
				print_without_label(Iterables.limit(Multisets.copyHighestCountFirst(visitor.literal_values).entrySet(), 100));
				System.out.println("languages,label,frequency");
				System.out.println(Multisets.copyHighestCountFirst(visitor.languages).entrySet().size());
				print_without_label(Iterables.limit(Multisets.copyHighestCountFirst(visitor.languages).entrySet(), 100));
				System.out.println("types,label,frequency");
				System.out.println(Multisets.copyHighestCountFirst(visitor.types).entrySet().size());
				print_without_label(Iterables.limit(Multisets.copyHighestCountFirst(visitor.types).entrySet(), 100));
				System.out.println("literal_labels,label,frequency");
				System.out.println(Multisets.copyHighestCountFirst(visitor.literal_labels).entrySet().size());
				print_without_label(Iterables.limit(Multisets.copyHighestCountFirst(visitor.literal_labels).entrySet(), 100));
				System.out.println("rdftype,label,frequency");
				System.out.println(Multisets.copyHighestCountFirst(visitor.rdf_types).entrySet().size());
				Iterable<Entry<String>> types = Iterables.limit(Multisets.copyHighestCountFirst(visitor.rdf_types).entrySet(), Multisets.copyHighestCountFirst(visitor.rdf_types).entrySet().size());
				typetop_list.addAll(get_top_type(types, 10, sparqlendpoint));
				print_with_label(types,"o",100, sparqlendpoint);
				System.setOut(ps_console);
				System.out.println("Number of failures is : " + failures);
			}

			@Override
			public void reportFailure(String input) {
//				throw new RuntimeException("");
				//System.err.println("ignoring " + input);
				failures++;
			}

			@Override
			public void add(Query q) {
				this.queryList.add(q);
				Op op = Algebra.compile(q);
				// System.out.println(q.serialize());
				// System.out.println("NEXT QUERY");
				op.visit(visitor);
			}

			@Override
			public ArrayList<Query> getQueryList(){
				return this.queryList;
			}
		};

		IterateQueriesFromWikidataLog.processFromFile(new GZIPInputStream(in),collector);

//		List<String> queries = Lists.newArrayList("SELECT * WHERE { <http://test.com/subject> a <https://www.wikidata.org/wiki/Q5> }");
//				//, "SELECT * WHERE { <http://test.com/subject> ?p 5 }",
////			"SELECT * WHERE { <http://test.com/subject> ?p \"Hello\"@en }");
//		for (String query : queries) {
//			Query q = QueryFactory.create(query);
//			System.out.println(q);
//			Op op = Algebra.compile(q);
//			op.visit(visitor);
//		}
		watch.stop();
		System.out.println("Elapsed" + watch.elapsed(TimeUnit.SECONDS));

		collector.stats();
		PatternDisplay.rankPattern(collector.getQueryList(), 10, 1,25,true, sparqlendpoint, dict_name,stop_list,ptop_list,otop_list,typetop_list);//input is (queryList, top number of display, max number of triples in pattern query)
		System.out.println("Finished!" + watch.elapsed(TimeUnit.SECONDS));
	}

	private static String get_labels(String str, String sparqlendpoint){
		String label_str= null;
		String queryString = "PREFIX dct: <http://purl.org/dc/terms/>\n SELECT * WHERE {\n   ?s dct:title ?o .\n  Values ?s {"
				+ str + "}}  LIMIT 1";
		Map<String, String> map = new HashMap<String, String>();
		QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlendpoint, queryString);
        ResultSet rs = null;
		try {
        	rs = qexec.execSelect();
            if (rs.hasNext()) {
				QuerySolution rb = rs.nextSolution();
				RDFNode label = rb.get("o");
				label_str = label.asLiteral().getString();
			}
			else{
				label_str = get_labels_rdfs(str,sparqlendpoint);
			}
        }catch (Exception e) {
        	
        }
		qexec.close();
		return label_str;
	}

	private static String get_labels_rdfs(String str, String sparqlendpoint){
		String label_str= null;
		String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n SELECT * WHERE {\n   ?s rdfs:label ?o .\n  Values ?s {"
				+ str + "}}  LIMIT 1";
		Map<String, String> map = new HashMap<String, String>();
		QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlendpoint, queryString);
        ResultSet rs = null;
		try {
        	rs = qexec.execSelect();
            if (rs.hasNext()) {
				QuerySolution rb = rs.nextSolution();
				RDFNode label = rb.get("o");
				label_str = label.asLiteral().getString();
			}
        }catch (Exception e) {
        	
        }
		qexec.close();
		return label_str;
	}

	private static boolean check_exist_subject(String input, String sparqlendpoint){
		boolean exist = false;
		String queryString = "PREFIX dct: <http://purl.org/dc/terms/>\n SELECT * WHERE {\n   ?s ?p ?o .\n  Values ?s {<"
		+ input + ">}}  LIMIT 1";
				QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlendpoint, queryString);
				ResultSet rs = null;
		try {
        	rs = qexec.execSelect();
            if (rs.hasNext()) {
				exist = true;
			}
        }catch (Exception e) {
        	
        }
		qexec.close();
		return exist;
	}

	private static boolean check_exist_predicate(String input, String sparqlendpoint){
		boolean exist = false;
		String queryString = "PREFIX dct: <http://purl.org/dc/terms/>\n SELECT * WHERE {\n   ?s ?p ?o .\n  Values ?p {<"
				+ input + ">}}  LIMIT 1";
				QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlendpoint, queryString);
				ResultSet rs = null;
		try {
        	rs = qexec.execSelect();
            if (rs.hasNext()) {
				exist = true;
			}
        }catch (Exception e) {
        	
        }
		qexec.close();
		return exist;
	}

	private static boolean check_exist_object(String input, String sparqlendpoint){
		boolean exist = false;
		String queryString = "PREFIX dct: <http://purl.org/dc/terms/>\n SELECT * WHERE {\n   ?s ?p ?o .\n  Values ?o {<"
		+ input + ">}} LIMIT 1";
				QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlendpoint, queryString);
				ResultSet rs = null;
		try {
        	rs = qexec.execSelect();
            if (rs.hasNext()) {
				exist = true;
			}
        }catch (Exception e) {
        	
        }
		qexec.close();
		return exist;
	}

	private static void print_with_label(Iterable<Entry<String>> input, String type, int limit, String spaqlendpoint){
		int count = 0;
		for (Entry<String> str : input) {
			if (count >=limit)
			break;
			String label = get_labels("<" + str.getElement() + ">", spaqlendpoint);
			if (label != null) {
				System.out.println("<" + str.getElement() + ">" + " & " + get_labels("<" + str.getElement() + ">",spaqlendpoint)
						+ " & " + str.getCount());
				count ++;
			} else if (type == "s") {
				if (check_exist_subject(str.getElement(),spaqlendpoint)) {
					System.out.println("<" + str.getElement() + ">" + " & & " + str.getCount());
					count ++;
				}
			} else if (type == "p") {
				if (check_exist_predicate(str.getElement(), spaqlendpoint)) {
					System.out.println("<" + str.getElement() + ">" + " & & " + str.getCount());
					count ++;
				}
			} else if (type == "o") {
				if (check_exist_object(str.getElement(),spaqlendpoint)) {
					System.out.println("<" + str.getElement() + ">" + " & & " + str.getCount());
					count ++;
				}
			}
		}
	}

	private static List<String> get_top(Iterable<Entry<String>> input, int limit, String sparqlendpoint){
		List<String> top_list = new ArrayList<String>();
		int count = 0;
		for (Entry<String> str : input) {
			if (count >= limit)
				break;
			String label = get_labels("<" + str.getElement() + ">", sparqlendpoint);
			if (label != null) {
				top_list.add(str.getElement());
				count++;
			}
		}
		return top_list;
	}

	private static List<String> get_top_type(Iterable<Entry<String>> input, int limit, String sparqlendpoint){
		List<String> top_list = new ArrayList<String>();
		int count = 0;
		for (Entry<String> str : input) {
			if (count >= limit)
				break;
			String label = get_labels("<" + str.getElement() + ">", sparqlendpoint);
			if (label != null && !str.getElement().contains(":Resource")) {
				top_list.add(str.getElement());
				count++;
			}
		}
		return top_list;
	}

	private static void print_without_label(Iterable<Entry<String>> input){
		int count = 0;
		for (Entry<String> str : input){
			System.out.println("<"+str.getElement()+">"+"& &"+str.getCount());
		}
	}

	private static void print_without_and_with_label(Iterable<Entry<String>> input, String sparqlendpoint){
		for (Entry<String> str : input){
			String label = get_labels("<"+str.getElement()+">",sparqlendpoint);
			if(label!=null)
				System.out.println("<"+str.getElement()+">"+" & "+get_labels("<"+str.getElement()+">", sparqlendpoint)+" & "+str.getCount());
			else
				System.out.println("<"+str.getElement()+">"+" & & "+str.getCount());
		}
	}
}
