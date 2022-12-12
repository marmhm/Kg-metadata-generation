package nl.cochez.query_processing.metadata;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpSlice;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.DescribeBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.handlers.HandlerBlock;
import java.util.ArrayList;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;

import nl.cochez.query_processing.metadata.IsomorpismClusteringQueryCollector.Node;
import java.io.IOException;

public class PatternDisplay {
    public static void rankPattern(ArrayList<Query> queryList, int top,int offset, int tripleNumber, boolean checkEndpoint) {
		List<Query> pattern_query = new ArrayList<Query>();
		List<Query> invalid_pattern_query = new ArrayList<Query>();
		Map<Query,Boolean> dict_query = getDict();
		Map<Query, Query> pattern_instance_pair = new HashMap<Query,Query>();
		Map<Query, Integer> patter_length_map = new HashMap<Query,Integer>();
		Map<Integer, Integer> pattern_numbers = new HashMap<Integer, Integer>();
		Map<Integer, Integer> instance_numbers = new HashMap<Integer, Integer>();
		br1: for (Query q : queryList) {
			// System.out.println(q.queryType().name());
			List<Triple> triples = new ArrayList<Triple>();
			Map<String, String> replace_map = new HashMap<String, String>();
			Op ope = Algebra.compile(q);
			Set<String> var_set = new HashSet<String>();
			Set<String> entity_set = new HashSet<String>();
			Set<String> literal_set = new HashSet<String>();
			Set<String> predicate_set = new HashSet<String>();
			Set<Long> number_set = new HashSet<Long>();
			AllOpVisitor allbgp = new AllOpVisitor() {
				@Override
				public void visit(OpBGP opBGP) {
					for (Triple t : opBGP.getPattern()) {
						triples.add(t);
						if (t.getSubject().isVariable() || t.getSubject().toString().startsWith("?")) {
							var_set.add(t.getSubject().toString());
						}
						if (t.getObject().isVariable() || t.getObject().toString().startsWith("?")) {
							var_set.add(t.getObject().toString());
						}
						if (t.getPredicate().isVariable() || t.getPredicate().toString().startsWith("?")) {
							predicate_set.add(t.getPredicate().toString());
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
			for (String predicate : predicate_set){
				replace_map.put(predicate, "?predicate" + Integer.toString(count));
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
					for (Entry<String,String> prefix :q.getPrefixMapping().getNsPrefixMap().entrySet()){
						selectBuilder.addPrefix(prefix.getKey(), prefix.getValue());
					}
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
					invalid_pattern_query.add(pattern_q);
					if(!pattern_instance_pair.containsKey(pattern_q)){
						if(checkEndpoint){
							if(StoreOrRead(q,dict_query)){
								pattern_query.add(pattern_q);
								pattern_instance_pair.put(pattern_q, q);
								patter_length_map.put(pattern_q, triples.size());
							}
						}
						else{
							pattern_instance_pair.put(pattern_q, q);
							patter_length_map.put(pattern_q, triples.size());
						}
					}
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
					for (Entry<String,String> prefix :q.getPrefixMapping().getNsPrefixMap().entrySet()){
						selectBuilder.addPrefix(prefix.getKey(), prefix.getValue());
					}
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
					invalid_pattern_query.add(pattern_q);
					if(!pattern_instance_pair.containsKey(pattern_q)){
						if(checkEndpoint){
							if(StoreOrRead(q,dict_query)){
								pattern_query.add(pattern_q);
								pattern_instance_pair.put(pattern_q, q);
								patter_length_map.put(pattern_q, triples.size());
							}
						}
						else{
							pattern_instance_pair.put(pattern_q, q);
							patter_length_map.put(pattern_q, triples.size());
						}
					}
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
					for (Entry<String,String> prefix :q.getPrefixMapping().getNsPrefixMap().entrySet()){
						selectBuilder.addPrefix(prefix.getKey(), prefix.getValue());
					}
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
					invalid_pattern_query.add(pattern_q);
					if(!pattern_instance_pair.containsKey(pattern_q)){
						if(checkEndpoint){
							if(StoreOrRead(q,dict_query)){
								pattern_query.add(pattern_q);
								pattern_instance_pair.put(pattern_q, q);
								patter_length_map.put(pattern_q, triples.size());
							}
						}
						else{
							pattern_instance_pair.put(pattern_q, q);
							patter_length_map.put(pattern_q, triples.size());
						}
					}
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
					for (Entry<String,String> prefix :q.getPrefixMapping().getNsPrefixMap().entrySet()){
						selectBuilder.addPrefix(prefix.getKey(), prefix.getValue());
					}
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
					invalid_pattern_query.add(pattern_q);
					if(!pattern_instance_pair.containsKey(pattern_q)){
						if(checkEndpoint){
							if(StoreOrRead(q,dict_query)){
								pattern_query.add(pattern_q);
								pattern_instance_pair.put(pattern_q, q);
								patter_length_map.put(pattern_q, triples.size());
							}
						}
						else{
							pattern_instance_pair.put(pattern_q, q);
							patter_length_map.put(pattern_q, triples.size());
						}
					}
				} catch (Exception e) {
					continue br1;
				}
			}
			else{
				System.out.println("Query is not any type of SELECT or CONSTRUCT or ASK or DESCRIBE:");
				System.out.println(q.serialize());
				continue;
			}

			int length = triples.size();
			if (instance_numbers.containsKey(length)) {
				instance_numbers.put(length, instance_numbers.get(length) + 1);
			} else {
				instance_numbers.put(length, 1);
			}
		}

		// System.out.println("Statistics of number of pattern in each length:"+pattern_numbers);
		System.out.println("Statistics of number of instance in each length:"+instance_numbers);
		List<Map.Entry<Query, Integer>> result = sortPatternByValue(findFrequentPattern(invalid_pattern_query));
		for (Entry<Query, Integer> res : result) {
			int length = patter_length_map.get(res.getKey());
			if (pattern_numbers.containsKey(length)) {
				pattern_numbers.put(length, pattern_numbers.get(length) + 1);
			} else {
				pattern_numbers.put(length, 1);
			}
			// Algebra.compile(res.getKey()).visit(get_pattern_visitor);
		}
		System.out.println("Statistics of number of pattern in each length:"+pattern_numbers);
		Map<Integer,Integer> count_map = new HashMap<Integer,Integer>();
		for (int i =1;i<=10;i++){
			count_map.put(i, 0);
		}
		result = sortPatternByValue(findFrequentPattern(pattern_query));
		br1: for (int i = 0; !(check_count_all(count_map,top,offset,tripleNumber)) && i < result.size();i++){
			if (checkEndpoint)
				if (!result.get(i).getKey().isSelectType())
					if (!StoreOrRead(result.get(i).getKey(),dict_query))
						continue br1;
			if (checkEndpoint)
				if (StoreOrRead(result.get(i).getKey(),dict_query)) {
					continue br1;
				}
			
			int num = getBGPtripleNumber(result.get(i).getKey());
			if (num < offset || num > tripleNumber)
				continue br1;
			if (check_count(count_map, num, top)) {
				continue br1;
			}
			BufferedWriter bw = null;
			BufferedWriter bw_all = null;
			try {
				bw = new BufferedWriter(new FileWriter(
						"top" + Integer.toString(top) + "_pattern" + "_length" + Integer.toString(num) + ".json",
						true));
				bw_all = new BufferedWriter(
						new FileWriter("top" + Integer.toString(top) + "_pattern_with_frequency" + "_length"
								+ Integer.toString(num) + ".json", true));
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			JsonObject jo = new JsonObject();
			jo.put("Title", "");
			// jo.put("Pattern Rank Number",
			// Integer.toString(count+1)+"("+Integer.toString(i+1)+")");
			jo.put("SPARQL Query Pattern", result.get(i).getKey().serialize());
			jo.put("Instance Query", pattern_instance_pair.get(result.get(i).getKey()).serialize());
			jo.put("Contained Triple's Number", num);

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
			count_map.put(num, count_map.get(num) + 1);
		}
		System.out.print("Statistics of each length: ");
		System.out.println(count_map);
		storeDict(dict_query);
	}

	private static boolean check_count(Map<Integer,Integer> count_map,int num, int top){
		if (count_map.get(num) >= top) {
			return true;
		}
		return false;
	}

	private static boolean check_count_all(Map<Integer,Integer> count_map, int top, int offset, int tripleNumber){
		for (int i =offset;i<=tripleNumber;i++){
			if(count_map.get(i)<top){
				return false;
			}
		}
		return true;
	}

	private static int getBGPtripleNumber(Query q){
		List<Integer> num = new ArrayList<Integer>();
		AllBGPOpVisitor visitor = new AllBGPOpVisitor() {
			@Override
			public void visit(OpBGP opBGP) {
				for (Triple tp : opBGP.getPattern())
					num.add(1);
			}
		};
		try {
			Algebra.compile(q).visit(visitor);
		} catch (Exception e) {
			//TODO: handle exception
			return 0;
		}
		
		return num.size();
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
			if (temp_pattern.containsAll(list_pattern) && list_pattern.containsAll(temp_pattern) && temp_pattern.size() == list_pattern.size()) {
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
		boolean check = false;
		if(!query.isSelectType()){
			return check_ASK_CONSTRUCT_DESCRIBE(query);
		}
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
			qexec.close();
        	return false;
        }
        if (results.hasNext()) {
        	check = true;
        }
		qexec.close();
        return check;
	}

	private static boolean check_ASK_CONSTRUCT_DESCRIBE(Query query){
		boolean check = false;
		if (query.isAskType()) {
			AskBuilder selectBuilder = new AskBuilder();
			HandlerBlock handlerBlock = new HandlerBlock(query);
			selectBuilder.getHandlerBlock().addAll(handlerBlock);
			selectBuilder.setLimit(1);
			selectBuilder.setBase(null);
			// selectBuilder.addVar("*");
			QueryExecution qexec = QueryExecutionFactory.sparqlService("https://bio2rdf.org/sparql",
					selectBuilder.build());
			ResultSet results = null;
			try {
				check = qexec.execAsk();
			} catch (Exception e) {
			}
		} else if (query.isConstructType()) {
			ConstructBuilder selectBuilder = new ConstructBuilder();
			HandlerBlock handlerBlock = new HandlerBlock(query);
			selectBuilder.getHandlerBlock().addAll(handlerBlock);
			selectBuilder.setLimit(1);
			selectBuilder.setBase(null);
			// selectBuilder.addVar("*");
			QueryExecution qexec = QueryExecutionFactory.sparqlService("https://bio2rdf.org/sparql",
					selectBuilder.build());
			Model results = null;
			try {
				results = qexec.execConstruct();
			} catch (Exception e) {
			}
			if (results == null){
				qexec.close();
				return false;
			}
			if (results.isEmpty()){
				qexec.close();
				return false;
			}
				
			check = true;
			qexec.close();
		} else if (query.isDescribeType()) {
			DescribeBuilder selectBuilder = new DescribeBuilder();
			HandlerBlock handlerBlock = new HandlerBlock(query);
			selectBuilder.getHandlerBlock().addAll(handlerBlock);
			selectBuilder.setLimit(1);
			selectBuilder.setBase(null);
			// selectBuilder.addVar("*");
			QueryExecution qexec = QueryExecutionFactory.sparqlService("https://bio2rdf.org/sparql",
					selectBuilder.build());
			Model results = null;
			try {
				results = qexec.execDescribe();
			} catch (Exception e) {
			}
			if (results == null){
				qexec.close();
				return false;
			}
			if (results.isEmpty()){
				qexec.close();
				return false;
			}
			check = true;
			qexec.close();
		}
		return check;
	}

	public static Map<Var,org.apache.jena.graph.Node> get_result_of_vars(Query query){ // get var-value pairs for pattern query via endpoint 
		Map<Var,org.apache.jena.graph.Node> var_results = new HashMap<Var,org.apache.jena.graph.Node>();
		if (query.isSelectType()){
			SelectBuilder selectBuilder = new SelectBuilder();
        	HandlerBlock handlerBlock = new HandlerBlock(query);
			selectBuilder.getHandlerBlock().addAll(handlerBlock);
			selectBuilder.setLimit(1);
			selectBuilder.setBase(null);
			// selectBuilder.addVar("*");
			QueryExecution qexec = QueryExecutionFactory.sparqlService("https://bio2rdf.org/sparql",
					selectBuilder.build());
			ResultSet results = null;
			try {
				results = qexec.execSelect();
			} catch (Exception e) {
				qexec.close();
				return var_results;
			}
			if (results.hasNext()) {
				QuerySolution qs = results.next();
				Iterator<String> it_var = qs.varNames();
				while (it_var.hasNext()) {
					String var = it_var.next();
					var_results.put(Var.alloc(var), new NodeFactory().createURI(qs.get(var).toString()));
				}
			}
			qexec.close();
		}
		
        return var_results;
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

	private static boolean StoreOrRead(Query query,Map<Query, Boolean> dict_query){
		if(dict_query.containsKey(query)){
			return dict_query.get(query);
		}
		boolean bl = check_with_endpoint(query);
		dict_query.put(query, bl);
		return bl;
	}

	private static Map<Query, Boolean> getDict() {
		Map<Query, Boolean> dict_query = new HashMap<Query, Boolean>();
		if (!new File("query_dict.index").exists()) {
			return dict_query;
		} else {
			try {
				BufferedReader br = new BufferedReader(new FileReader("query_dict.index"));
				String line = null;
				while ((line = br.readLine()) != null) {
					String[] splitline = line.split(" & ");
					dict_query.put(QueryFactory.create(splitline[0]), Boolean.parseBoolean(splitline[1]));
				}
				br.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return dict_query;
	}

	private static void storeDict(Map<Query, Boolean> dict_query){
		try {
			new File("query_dict.index").delete();
			BufferedWriter bw = new BufferedWriter(new FileWriter("query_dict.index",true));
			for(Entry<Query,Boolean> dq:dict_query.entrySet()){
				bw.write(dq.getKey().serialize().replace("\n", "\\n").replace("\r", "\\r")+" & "+Boolean.toString(dq.getValue()));
				bw.newLine();
				bw.flush();
			}
			bw.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
}
