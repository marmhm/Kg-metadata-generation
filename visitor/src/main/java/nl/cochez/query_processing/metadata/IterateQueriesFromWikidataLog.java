package nl.cochez.query_processing.metadata;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpSlice;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import nl.cochez.query_processing.metadata.IsomorpismClusteringQueryCollector.Node;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.DescribeBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.handlers.HandlerBlock;

public class IterateQueriesFromWikidataLog {
	public static void processFromFile(InputStream in, IQueryCollector collector) throws IOException {
		System.out.println("1 file processing");
		try (BufferedReader l = new BufferedReader(new BufferedReader(new InputStreamReader(in)))) {
			String line;
			// take headerline off
			l.readLine();
			ArrayList<Query> queryList = new ArrayList<Query>();
			while ((line = l.readLine()) != null) {
				String queryString = URLDecoder.decode(line.split("\t")[0].replaceAll("%(?![0-9a-fA-F]{2})", "%25").replaceAll("\\+", "%2B"), StandardCharsets.UTF_8);
				Query q;
				try {
					q = QueryFactory.create(queryString);

				} catch (Exception e) {
					collector.reportFailure(queryString);
					continue;
				}
				collector.add(q);
				try {
					queryList.add(q);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			// rankQuery(queryList, 100);//input is (queryList, top number of display)
			rankPattern(queryList, 100);//input is (queryList, top number of display)
		}
	}

	// public static void rankQuery(ArrayList<Query> queryList, int top) {
	// 	List<Map.Entry<Query, Integer>> result = sortByValue(findFrequentNumber(queryList));
	// 	for (int i = 0; i < Math.min(top, result.size()); i++) {
	// 		BufferedWriter bw = null;
	// 		try {
	// 			bw = new BufferedWriter(new FileWriter("top"+Integer.toString(top)+"_query.json",true));
	// 		} catch (IOException e) {
	// 			e.printStackTrace();
	// 			System.exit(1);
	// 		}
	// 		JsonObject jo = new JsonObject();
	// 		jo.put("Top Number", i+1);
	// 		jo.put("SPARQL query", result.get(i).getKey().serialize());
	// 		jo.put("frequency", result.get(i).getValue());
	// 		try {
	// 			bw.write(jo.toString());
	// 			bw.newLine();
	// 			bw.flush();
	// 		} catch (IOException e) {
	// 			e.printStackTrace();
	// 			System.exit(1);
	// 		}
	// 	}
	// }

	// public static List<Map.Entry<Query, Integer>> sortByValue(HashMap<Query, Integer> hm) {
	// 	List<Map.Entry<Query, Integer>> list = new LinkedList<Map.Entry<Query, Integer>>(hm.entrySet());
	// 	Collections.sort(list, new Comparator<Map.Entry<Query, Integer>>() {
	// 		public int compare(Map.Entry<Query, Integer> o1,
	// 				Map.Entry<Query, Integer> o2) {
	// 			return (o2.getValue()).compareTo(o1.getValue());
	// 		}
	// 	});
	// 	HashMap<Query, Integer> temp = new LinkedHashMap<Query, Integer>();
	// 	for (Map.Entry<Query, Integer> aa : list) {
	// 		temp.put(aa.getKey(), aa.getValue());
	// 	}
	// 	return list;
	// }

	// private static HashMap<Query, Integer> findFrequentNumber(ArrayList<Query> inputArr) {
	// 	HashMap<Query, Integer> numberMap = new HashMap<Query, Integer>();
	// 	Query result = null;
	// 	int frequency = -1;

	// 	int value;
	// 	for (int i = 0; i < inputArr.size(); i++) {

	// 		value = -1;
	// 		if (numberMap.containsKey(inputArr.get(i))) {
	// 			value = numberMap.get(inputArr.get(i));
	// 		}
	// 		if (value != -1) {

	// 			value += 1;
	// 			if (value > frequency) {

	// 				frequency = value;
	// 				result = inputArr.get(i);
	// 			}

	// 			numberMap.put(inputArr.get(i), value);
	// 		} else {

	// 			numberMap.put(inputArr.get(i), 1);
	// 		}

	// 	}
	// 	return numberMap;
	// }

	public static void rankPattern(ArrayList<Query> queryList, int top) {
		List<Query> pattern_query = new ArrayList<Query>();
		Map<Query, Query> pattern_instance_pair = new HashMap<Query,Query>();
		br1: for (Query q : queryList) {
			// System.out.println(q.queryType().name());
			Map<String, String> replace_map = new HashMap<String, String>();
			Op ope = Algebra.compile(q);
			Set<String> var_set = new HashSet<String>();
			Set<String> entity_set = new HashSet<String>();
			Set<String> literal_set = new HashSet<String>();
			Set<Long> number_set = new HashSet<Long>();
			AllOpVisitor allbgp = new AllOpVisitor() {
				@Override
				public void visit(OpBGP opBGP) {
					for (Triple t : opBGP.getPattern()) {
						if (t.getSubject().isVariable() || t.getSubject().toString().startsWith("?")) {
							var_set.add(t.getSubject().toString());
						}
						if (t.getObject().isVariable() || t.getObject().toString().startsWith("?")) {
							var_set.add(t.getObject().toString());
						}
						if (t.getSubject().isURI()) {
							entity_set.add(t.getSubject().toString());
						}
						if (t.getObject().isURI()) {
							entity_set.add(t.getObject().toString());
						}
						if (t.getSubject().isLiteral()){
							literal_set.add(t.getSubject().getLiteralLexicalForm());
						}
						if (t.getObject().isLiteral()){
							literal_set.add(t.getObject().getLiteralLexicalForm());
						}
					}
				}

				@Override
				public void visit(OpSlice opSlice) {
					// TODO Auto-generated method stub
					number_set.add(opSlice.getStart());
					number_set.add(opSlice.getLength());
					opSlice.getSubOp().visit(this);
				}
			};
			ope.visit(allbgp);
			String replace_query_string = "";
			try {
				replace_query_string = q.serialize();
			} catch (Exception e) {
				continue;
			}

			List<String> ent_vars = new ArrayList<String>();
			List<String> lit_vars = new ArrayList<String>();
			int count = 1;
			for (String var : var_set) {
				replace_map.put(var, "?variable" + Integer.toString(count++));
			}
			count = 1;
			for (String ent : entity_set){
				replace_map.put("<"+ent+">", "?ent" + Integer.toString(count));
				ent_vars.add("?ent" + Integer.toString(count++));
			}
			count = 1;
			for (String literal : literal_set){
				replace_map.put("\""+literal+"\"", "?str" + Integer.toString(count));
				replace_map.put(literal, "?str" + Integer.toString(count));
				lit_vars.add("?str" + Integer.toString(count++));
			}
			count = 1;
			for (Long num : number_set)
				replace_query_string = replace_query_string.replace(" " + Long.toString(num), " 1");
			for (String var : replace_map.keySet()) {
				replace_query_string = replace_query_string.replace(var + " ", replace_map.get(var) + " ")
						.replace(var + "\n", replace_map.get(var) + "\n").replace(var + ")", replace_map.get(var) + ")")
						.replace(var + "\r", replace_map.get(var) + "\r").replace(var + ",", replace_map.get(var) + ",")
						.replace("<" + var + ">", "<" + replace_map.get(var) + ">");
			}

			if(q.isSelectType()){
				SelectBuilder selectBuilder = new SelectBuilder();
				try {
					HandlerBlock handlerBlock = new HandlerBlock(QueryFactory.create(replace_query_string));
					selectBuilder.getHandlerBlock().addAll(handlerBlock);
					selectBuilder.setBase(null);
					for(String ent : ent_vars){
						try {
							selectBuilder.addFilter("isIRI("+ent+")");
						} catch (ParseException e) {
						}
					}
					for (String literal : lit_vars){
						try {
							selectBuilder.addFilter("isLiteral("+literal+")");
						} catch (ParseException e) {
						}
					}
					Query pattern_q = selectBuilder.build();
					pattern_query.add(pattern_q);
					if(!pattern_instance_pair.containsKey(pattern_q))
						pattern_instance_pair.put(pattern_q, q);
				} catch (Exception e) {
					//TODO: handle exception
					continue br1;
				}
			}
			else if (q.isConstructType()){
				ConstructBuilder selectBuilder = new ConstructBuilder();
				try {
					HandlerBlock handlerBlock = new HandlerBlock(QueryFactory.create(replace_query_string));
					selectBuilder.getHandlerBlock().addAll(handlerBlock);
					selectBuilder.setBase(null);
					for(String ent : ent_vars){
						try {
							selectBuilder.addFilter("isIRI("+ent+")");
						} catch (ParseException e) {
						}
					}
					for (String literal : lit_vars){
						try {
							selectBuilder.addFilter("isLiteral("+literal+")");
						} catch (ParseException e) {
						}
					}
					Query pattern_q = selectBuilder.build();
					pattern_query.add(pattern_q);
					if(!pattern_instance_pair.containsKey(pattern_q))
						pattern_instance_pair.put(pattern_q, q);
				} catch (Exception e) {
					//TODO: handle exception
					continue br1;
				}
			}
			else if (q.isAskType()){
				AskBuilder selectBuilder = new AskBuilder();
				try {
					HandlerBlock handlerBlock = new HandlerBlock(QueryFactory.create(replace_query_string));
					selectBuilder.getHandlerBlock().addAll(handlerBlock);
					selectBuilder.setBase(null);
					for(String ent : ent_vars){
						try {
							selectBuilder.addFilter("isIRI("+ent+")");
						} catch (ParseException e) {
						}
					}
					for (String literal : lit_vars){
						try {
							selectBuilder.addFilter("isLiteral("+literal+")");
						} catch (ParseException e) {
						}
					}
					Query pattern_q = selectBuilder.build();
					pattern_query.add(pattern_q);
					if(!pattern_instance_pair.containsKey(pattern_q))
						pattern_instance_pair.put(pattern_q, q);
				} catch (Exception e) {
					continue br1;
				}
			}
			else if (q.isDescribeType()){
				DescribeBuilder selectBuilder = new DescribeBuilder();
				try {
					HandlerBlock handlerBlock = new HandlerBlock(QueryFactory.create(replace_query_string));
					selectBuilder.getHandlerBlock().addAll(handlerBlock);
					selectBuilder.setBase(null);
					for(String ent : ent_vars){
						try {
							selectBuilder.addFilter("isIRI("+ent+")");
						} catch (ParseException e) {
						}
					}
					for (String literal : lit_vars){
						try {
							selectBuilder.addFilter("isLiteral("+literal+")");
						} catch (ParseException e) {
						}
					}
					Query pattern_q = selectBuilder.build();
					pattern_query.add(pattern_q);
					if(!pattern_instance_pair.containsKey(pattern_q))
						pattern_instance_pair.put(pattern_q, q);
				} catch (Exception e) {
					continue br1;
				}
			}
			else{
				System.out.println("Query is not any type of SELECT or CONSTRUCT or ASK:");
				System.out.println(q.serialize());
				continue;
			}

			
			// try {
			// 	pattern_query.add(QueryFactory.create(replace_query_string));
			// } catch (Exception e) {
			// 	// System.out.println(replace_map);
			// 	// System.out.println(replace_query_string);
			// 	// e.printStackTrace();
			// 	// System.exit(1);
			// }
		}
		List<Map.Entry<Query, Integer>> result = sortPatternByValue(findFrequentPattern(pattern_query));
		for (int i = 0; i < Math.min(top, result.size()); ) {
			// if(!check_with_endpoint(result.get(i).getKey()) || !check_with_endpoint(pattern_instance_pair.get(result.get(i).getKey())))
			// 	continue;
			BufferedWriter bw = null;
			BufferedWriter bw_all = null;
			try {
				bw = new BufferedWriter(new FileWriter("top"+Integer.toString(top)+"_pattern.json",true));
				bw_all = new BufferedWriter(new FileWriter("top"+Integer.toString(top)+"_pattern_with_frequency.json",true));
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			JsonObject jo = new JsonObject();
			jo.put("Title", "");
			jo.put("Pattern Rank Number", i+1);
			jo.put("SPARQL Query Pattern", result.get(i).getKey().serialize());
			jo.put("Instance Query", pattern_instance_pair.get(result.get(i).getKey()).serialize());
			
			try {
				bw.write(jo.toString());
				bw.newLine();
				bw.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(1);
			}

			jo.put("Frequency", result.get(i).getValue());
			try {
				bw_all.write(jo.toString());
				bw_all.newLine();
				bw_all.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(1);
			}
			i++;
			// System.out.println("----------------\n" + "Top" + (i + 1) + " pattern is\n****************\n"
			// 		+ result.get(i).getKey().serialize() + "\n****************\nThe frequency of above query is "
			// 		+ result.get(i).getValue() + "\n----------------\n");
		}
	}

	private static HashMap<Query, Integer> findFrequentPattern(List<Query> inputArr) {
		HashMap<Query, Integer> numberMap = new HashMap<Query, Integer>();
		int frequency = -1;

		int value;
		for (int i = 0; i < inputArr.size(); i++) {

			value = -1;
			if (numberMap.containsKey(inputArr.get(i)))
				if (listOflistContains(inputArr.get(i), numberMap.keySet())) {
					value = numberMap.get(inputArr.get(i));
				}
			if (value != -1) {

				value += 1;
				if (value > frequency) {

					frequency = value;
				}

				numberMap.put(inputArr.get(i), value);
			} else {

				numberMap.put(inputArr.get(i), 1);
			}

		}
		return numberMap;
	}

	private static boolean listOflistContains(Query list, Set<Query> listlist) {
		if (listlist.contains(list))
			return true;
		List<Triple> list_pattern = new ArrayList<Triple>();
		AllOpVisitor list_visit = new AllOpVisitor() {
			@Override
			public void visit(OpBGP opBGP) {
				BasicPattern bp = simplify(opBGP.getPattern());
				list_pattern.addAll(bp.getList());
			}

			@Override
			public void visit(OpSlice opSlice) {
				// TODO Auto-generated method stub
				opSlice.getSubOp().visit(this);

			}
		};
		try{
		Algebra.compile(list).visit(list_visit);
		} catch (Exception e){
			return false;
		}
		for (Query temp : listlist) {
			List<Triple> temp_pattern = new ArrayList<Triple>();
			AllOpVisitor temp_visit = new AllOpVisitor() {
				@Override
				public void visit(OpBGP opBGP) {
					BasicPattern bp = simplify(opBGP.getPattern());
					temp_pattern.addAll(bp.getList());
				}

				@Override
				public void visit(OpSlice opSlice) {
					// TODO Auto-generated method stub
					opSlice.getSubOp().visit(this);

				}
			};
			try{
			Algebra.compile(temp).visit(temp_visit);
			} catch(Exception e){
				return false;
			}
			if (temp_pattern.containsAll(list_pattern) && list_pattern.containsAll(temp_pattern)) {
				return true;
			}
		}
		return false;
	}

	private static BasicPattern simplify(BasicPattern bgp) {
		Model model = ModelFactory.createDefaultModel();
		BasicPattern bp = new BasicPattern();
		for (Triple triple : bgp) {
			org.apache.jena.graph.Node subject;
			org.apache.jena.graph.Node object;

			if (triple.getSubject().isVariable()) {
				subject = model.createLiteral("variable").asNode();
			} else if (triple.getSubject().isBlank()) {
				subject = model.createLiteral(EquivalenceClasses.ENTITY_GROUP).asNode();
			} else if (triple.getSubject().isLiteral()) {
				subject = model.createLiteral("literal").asNode();
			} else {
				subject = model
						.createLiteral(EquivalenceClasses.getEquivalentOrDefault(
								triple.getSubject().getIndexingValue().toString(), EquivalenceClasses.ENTITY_GROUP))
						.asNode();
			}
			if (triple.getObject().isVariable()) {
				object = model.createLiteral("variable").asNode();
			} else if (triple.getObject().isBlank()) {
				object = model.createLiteral(EquivalenceClasses.ENTITY_GROUP).asNode();
			} else if (triple.getObject().isLiteral()) {
				object = model.createLiteral("literal").asNode();
			} else {
				object = model
						.createLiteral(EquivalenceClasses.getEquivalentOrDefault(
								triple.getSubject().getIndexingValue().toString(), EquivalenceClasses.ENTITY_GROUP))
						.asNode();
			}

			bp.add(new Triple(subject, triple.getPredicate(), object));
		}

		return bp;

	}

	public static List<Map.Entry<Query, Integer>> sortPatternByValue(HashMap<Query, Integer> hm) {
		List<Map.Entry<Query, Integer>> list = new LinkedList<Map.Entry<Query, Integer>>(hm.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<Query, Integer>>() {
			public int compare(Map.Entry<Query, Integer> o1, Map.Entry<Query, Integer> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});
		HashMap<Query, Integer> temp = new LinkedHashMap<Query, Integer>();
		for (Map.Entry<Query, Integer> aa : list) {
			temp.put(aa.getKey(), aa.getValue());
		}
		return list;
	}

	private static boolean check_with_endpoint(Query query) { // check if query will return results via bio2rdf SPARQL endpoint
		SelectBuilder selectBuilder = new SelectBuilder();
        HandlerBlock handlerBlock = new HandlerBlock(query);
        selectBuilder.getHandlerBlock().addAll(handlerBlock);
		selectBuilder.setLimit(1);
		selectBuilder.setBase(null);
        QueryExecution qexec = QueryExecutionFactory.sparqlService("https://bio2rdf.org/sparql", selectBuilder.build());
        ResultSet results = null;
        try {
        	results = qexec.execSelect();
        }catch (Exception e) {
        	return false;
        }
        if (results.hasNext()) {
        	return true;
        }
        return false;
	}
	private static Graph toGraph(Query pattern){
		Graph<Node, RelationshipEdge> graph = new DefaultDirectedGraph<Node, RelationshipEdge>(RelationshipEdge.class);
		AllBGPOpVisitor visitor = new AllBGPOpVisitor() {

			@Override
			public void visit(OpBGP opBGP) {
				for (Triple tp : opBGP.getPattern()){
					org.apache.jena.graph.Node s = tp.getSubject();
					org.apache.jena.graph.Node o = tp.getObject();
					org.apache.jena.graph.Node p = tp.getPredicate();

					RelationshipEdge edge = new RelationshipEdge(p.toString());
					Node sNode = null;
					if (s.isVariable() || s.toString().startsWith("?")){
						if(s.toString().startsWith("?variable")){
							sNode = new Node(s.toString().replace("?",""), "variable");
						}
						else if (s.toString().startsWith("?ent")){
							sNode = new Node(s.toString().replace("?", ""), "entity");
						}
						else if (s.toString().startsWith("?str")){
							sNode = new Node(s.toString().replace("?", ""), "literal");
						}
					}
					else {
						sNode = new Node(s.toString(), s.toString());
					}

					Node oNode = null;
					if (o.isVariable() || o.toString().startsWith("?")){
						if(o.toString().startsWith("?variable")){
							oNode = new Node(o.toString().replace("?",""), "variable");
						}
						else if (o.toString().startsWith("?ent")){
							oNode = new Node(o.toString().replace("?", ""), "entity");
						}
						else if (o.toString().startsWith("?str")){
							oNode = new Node(o.toString().replace("?", ""), "literal");
						}
					}
					else {
						oNode = new Node(o.toString(),o.toString());
					}
					graph.addVertex(sNode);
					graph.addVertex(oNode);
					graph.addEdge(sNode, oNode, edge);
				}
			}
		};

		Op op = Algebra.compile(pattern);
		op.visit(visitor);
		
		return graph;
	}
}

	

class RelationshipEdge
    extends
    DefaultEdge
{
    private String label;

    public RelationshipEdge(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return label;
    }

    @Override
    public String toString()
    {
        return "( "+ ((Node)getSource()).value + " "+ label+" " + ((Node)getTarget()).value+" )";
    }

	public String toString_group(){
		return "( "+ ((Node)getSource()).getGroup() + " "+ label+" " + ((Node)getTarget()).getGroup()+" )";
	}
}