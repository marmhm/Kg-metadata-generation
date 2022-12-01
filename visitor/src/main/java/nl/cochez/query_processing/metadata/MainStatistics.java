package nl.cochez.query_processing.metadata;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.apache.jena.ext.com.google.common.collect.Iterables;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;

import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

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
		Stopwatch watch = Stopwatch.createStarted();
		String filename = "/home/coder/project/visitor/src/main/java/nl/cochez/query_processing/metadata/reallall-bio2rdf-processed.tsv.gz";
		if (args.length > 0) {
			filename = args[0];
		}
		
		InputStream in = new FileInputStream(filename);
        
		final StatVisitor visitor = new StatVisitor();
		
		IQueryCollector collector = new IQueryCollector() {
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
				System.out.println("subjects " +  Iterables.limit(Multisets.copyHighestCountFirst(visitor.subjects).entrySet(), 100));
				System.out.println("predicates " + Iterables.limit(Multisets.copyHighestCountFirst(visitor.predicates).entrySet(), 100));
				System.out.println("objects " + Iterables.limit(Multisets.copyHighestCountFirst(visitor.objects).entrySet(), 100));
				System.out.println("literals" + Iterables.limit(Multisets.copyHighestCountFirst(visitor.literal_values).entrySet(), 100));
				System.out.println("languages" + Iterables.limit(Multisets.copyHighestCountFirst(visitor.languages).entrySet(), 100));
				System.out.println("types" + Iterables.limit(Multisets.copyHighestCountFirst(visitor.types).entrySet(), 100));
				System.out.println("literal_labels" + Iterables.limit(Multisets.copyHighestCountFirst(visitor.literal_labels).entrySet(), 100));
				System.out.println("rdf_types" + Iterables.limit(Multisets.copyHighestCountFirst(visitor.rdf_types).entrySet(), 100));
				
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
				Op op = Algebra.compile(q);
				// System.out.println("NEXT QUERY");
				op.visit(visitor);
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
	}
}
