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

import org.apache.jena.atlas.io.IndentedLineBuffer;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Node_Variable;
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
import org.apache.jena.sparql.algebra.op.OpExtend;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.algebra.op.OpSlice;
import org.apache.jena.sparql.algebra.op.OpTable;
import org.apache.jena.sparql.algebra.table.TableData;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.sparql.serializer.FormatterElement;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementBind;
import org.apache.jena.sparql.syntax.ElementData;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

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
import nl.cochez.query_processing.metadata.OpAsQuery.Converter;

import java.io.IOException;

public class PatternDisplay {
    public static void rankPattern(ArrayList<Query> queryList, int top,int offset, int tripleNumber, boolean checkEndpoint, String sparqlendpoint, String dict_name) {
		// List<Query> pattern_query = new ArrayList<Query>();
		List<Query> invalid_pattern_query = new ArrayList<Query>();
		Map<Query,Boolean> dict_query = getDict(dict_name);
		// Map<Query, Query> pattern_instance_pair = new HashMap<Query,Query>();
		HashMap<Query, HashMultiset<Query>> pattern_instance = new HashMap<Query, HashMultiset<Query>>(); // hashmap for pattern and a set of unique queries for this pattern
		Map<Query, Integer> patter_length_map = new HashMap<Query,Integer>();
		Map<Integer, Integer> pattern_numbers = new HashMap<Integer, Integer>();
		Map<Integer, Integer> instance_numbers = new HashMap<Integer, Integer>();
		HashMap<Query, Integer> instance_freq = sortInstanceByValue(findFrequentQuery(queryList)); // get unique query list and the frequency of each unique query
		try {
			BufferedWriter bw1 = new BufferedWriter(new FileWriter("unique_query_frequency.csv",true));
			for(Entry<Query,Integer> uqf:instance_freq.entrySet()){
				bw1.write(uqf.getKey().serialize().replace("\r", "\\r").replace("\n", "\\n")+" & "+Integer.toString(uqf.getValue()));
				bw1.newLine();
				bw1.flush();
			}
			bw1.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		br1: for (Query q : instance_freq.keySet()) {
			// System.out.println(q.queryType().name());
			List<Triple> triples = new ArrayList<Triple>();
			Map<String, String> replace_map = new HashMap<String, String>();
			Set<String> var_set = new HashSet<String>();
			Set<String> entity_set = new HashSet<String>();
			Set<String> literal_set = new HashSet<String>();
			Set<String> predicate_set = new HashSet<String>();
			Set<Long> number_set = new HashSet<Long>();
			Set<String> bind_set = get_bind_vars(q);
			Set<String> values_set = new HashSet<String>();
			// Set<String> count_set = new HashSet<String>();
			Map<String, Integer> count_count = new HashMap<String, Integer>();
			HashMap<Var, Var> extend_dict = new HashMap<Var, Var>();
			Op ope = Algebra.compile(q);
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
						if (t.getSubject().isURI() || t.getSubject().isBlank()) {
							entity_set.add(t.getSubject().toString());
						}
						if (t.getObject().isURI() || t.getObject().isBlank()) {
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

				public void visit(OpExtend opExtend){
					for(Var var : opExtend.getVarExprList().getExprs().keySet()){
						try {
							extend_dict.put(opExtend.getVarExprList().getExprs().get(var).asVar(), var);
						} catch (Exception e) {
							//TODO: handle exception
						}
						
					}
					opExtend.getSubOp().visit(this);
				}
	
				@Override
				public void visit(OpGroup opGroup){
					for(ExprAggregator exp : opGroup.getAggregators()){
						// System.out.println(extend_dict.get(exp.getVar()).getVarName()+" "+exp.getAggregator().getName().toLowerCase()); 
						try {
							if(count_count.containsKey(exp.getAggregator().getName().toLowerCase())){
								count_count.put(exp.getAggregator().getName().toLowerCase(), count_count.get(exp.getAggregator().getName().toLowerCase())+1);
								replace_map.put("?"+extend_dict.get(exp.getVar()).getVarName(), "?"+exp.getAggregator().getName().toLowerCase()+Integer.toString(count_count.get(exp.getAggregator().getName().toLowerCase())));
							}
							else{
								count_count.put(exp.getAggregator().getName().toLowerCase(), 1);
								replace_map.put("?"+extend_dict.get(exp.getVar()).getVarName(), "?"+exp.getAggregator().getName().toLowerCase()+Integer.toString(count_count.get(exp.getAggregator().getName().toLowerCase())));
							}
						} catch (Exception e) {
							//TODO: handle exception
						}
					}
					opGroup.getSubOp().visit(this);
				}
	
				@Override
				public void visit(OpTable opTable){
					// Iterator<Binding> rows = opTable.getTable().rows();
					// for(;rows.hasNext();){
					// 	Binding row = rows.next();
					// System.out.println(row.vars().next()+" "+row.get(row.vars().next()).toString());
					// }
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
				replace_map.put(predicate, "?predicate" + Integer.toString(count++));
			}
			count = 1;
			for (Long num : number_set)
				replace_query_string = replace_query_string.replace(" " + Long.toString(num), " 1");
			count = 1;
			for(String bind: bind_set){
				replace_map.put("?"+bind, "?bind" + Integer.toString(count++));
			}
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
					pattern_q = generalize_VALUES(pattern_q);
					if(pattern_instance.containsKey(pattern_q)){
						pattern_instance.get(pattern_q).add(q);
					}
					else{
						pattern_instance.put(pattern_q, HashMultiset.create());
						pattern_instance.get(pattern_q).add(q);
					}
					invalid_pattern_query.add(pattern_q);
					patter_length_map.put(pattern_q, triples.size());
					// if(!pattern_instance_pair.containsKey(pattern_q)){
					// 	if(checkEndpoint){
					// 		if(StoreOrRead(q,dict_query)){
					// 			pattern_query.add(pattern_q);
					// 			pattern_instance_pair.put(pattern_q, q);
					// 			// patter_length_map.put(pattern_q, triples.size());
					// 		}
					// 	}
					// 	else{
					// 		pattern_instance_pair.put(pattern_q, q);
					// 		// patter_length_map.put(pattern_q, triples.size());
					// 	}
					// }
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
					pattern_q = generalize_VALUES(pattern_q);
					if(pattern_instance.containsKey(pattern_q)){
						pattern_instance.get(pattern_q).add(q);
					}
					else{
						pattern_instance.put(pattern_q, HashMultiset.create());
						pattern_instance.get(pattern_q).add(q);
					}
					invalid_pattern_query.add(pattern_q);
					patter_length_map.put(pattern_q, triples.size());
					// if(!pattern_instance_pair.containsKey(pattern_q)){
					// 	if(checkEndpoint){
					// 		if(StoreOrRead(q,dict_query)){
					// 			pattern_query.add(pattern_q);
					// 			pattern_instance_pair.put(pattern_q, q);
					// 			// patter_length_map.put(pattern_q, triples.size());
					// 		}
					// 	}
					// 	else{
					// 		pattern_instance_pair.put(pattern_q, q);
					// 		// patter_length_map.put(pattern_q, triples.size());
					// 	}
					// }
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
					pattern_q = generalize_VALUES(pattern_q);
					if(pattern_instance.containsKey(pattern_q)){
						pattern_instance.get(pattern_q).add(q);
					}
					else{
						pattern_instance.put(pattern_q, HashMultiset.create());
						pattern_instance.get(pattern_q).add(q);
					}
					invalid_pattern_query.add(pattern_q);
					patter_length_map.put(pattern_q, triples.size());
					// if(!pattern_instance_pair.containsKey(pattern_q)){
					// 	if(checkEndpoint){
					// 		if(StoreOrRead(q,dict_query)){
					// 			pattern_query.add(pattern_q);
					// 			pattern_instance_pair.put(pattern_q, q);
					// 			// patter_length_map.put(pattern_q, triples.size());
					// 		}
					// 	}
					// 	else{
					// 		pattern_instance_pair.put(pattern_q, q);
					// 		// patter_length_map.put(pattern_q, triples.size());
					// 	}
					// }
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
					pattern_q = generalize_VALUES(pattern_q);
					if(pattern_instance.containsKey(pattern_q)){
						pattern_instance.get(pattern_q).add(q);
					}
					else{
						pattern_instance.put(pattern_q, HashMultiset.create());
						pattern_instance.get(pattern_q).add(q);
					}
					invalid_pattern_query.add(pattern_q);
					patter_length_map.put(pattern_q, triples.size());
					// if(!pattern_instance_pair.containsKey(pattern_q)){
					// 	if(checkEndpoint){
					// 		if(StoreOrRead(q,dict_query)){
					// 			pattern_query.add(pattern_q);
					// 			pattern_instance_pair.put(pattern_q, q);
					// 			// patter_length_map.put(pattern_q, triples.size());
					// 		}
					// 	}
					// 	else{
					// 		pattern_instance_pair.put(pattern_q, q);
					// 		// patter_length_map.put(pattern_q, triples.size());
					// 	}
					// }
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
				instance_numbers.put(length, instance_numbers.get(length) + instance_freq.get(q));
			} else {
				instance_numbers.put(length, instance_freq.get(q));
			}
		}

		// System.out.println("Statistics of number of pattern in each length:"+pattern_numbers);
		System.out.println("Statistics of number of instance in each length:"+instance_numbers);
		List<Map.Entry<Query, Integer>> result = sortPatternByValue(findFrequentPattern(invalid_pattern_query));
		try {
			BufferedWriter bw2 = new BufferedWriter(new FileWriter("allPattern_frequency.csv",true));
			for(Entry<Query,Integer> uqf:result){
				bw2.write(uqf.getKey().serialize().replace("\r", "\\r").replace("\n", "\\n")+" & "+Integer.toString(uqf.getValue()));
				bw2.newLine();
				bw2.flush();
			}
			bw2.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		try {
			BufferedWriter bw_pattern_instance = new BufferedWriter(new FileWriter("pattern_statistics.csv", true));
			for (Query pattern_q : pattern_instance.keySet()) {
				int count=0;
				for(Query unique : pattern_instance.get(pattern_q)){
					count += instance_freq.get(unique);
				}
				bw_pattern_instance.write(pattern_q.serialize().replace("\r", "\\r").replace("\n", "\\n")+" & "+Integer.toString(pattern_instance.get(pattern_q).size())+" & "+Integer.toString(count));
				bw_pattern_instance.newLine();
				bw_pattern_instance.flush();
			}
			bw_pattern_instance.close();
		} catch (Exception e) {
			//TODO: handle exception
		}
		

		try {
			BufferedWriter bw1 = new BufferedWriter(new FileWriter("patter_allQuery.csv",true));
			for(Entry<Query, HashMultiset<Query>> uqf:pattern_instance.entrySet()){
				bw1.write(uqf.getKey().serialize().replace("\r", "\\r").replace("\n", "\\n")+" & "+uqf.getValue().toString().replace("\r", "\\r").replace("\n", "\\n"));
				bw1.newLine();
				bw1.flush();
			}
			bw1.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
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
		List<Integer> triple_number_list = new ArrayList<Integer>(pattern_numbers.keySet());
		Collections.sort(triple_number_list);
        Collections.reverse(triple_number_list);
		tripleNumber = triple_number_list.get(0);
		Map<Integer,Integer> count_map = new HashMap<Integer,Integer>();
		for (int i =1;i<=tripleNumber;i++){
			count_map.put(i, 0);
		}
		result = sortPatternByValue(findFrequentPattern(new ArrayList<Query>(pattern_instance.keySet())));
		br2: for (int i = 0; !(check_count_all(count_map,top,offset,tripleNumber)) && i < result.size();i++){
			Query pattern_query = result.get(i).getKey();
			int num = getBGPtripleNumber(pattern_query);
			if (num < offset || num > tripleNumber)
				continue br2;
			if (check_count(count_map, num, top)) {
				continue br2;
			}
			if (checkEndpoint)
				if (!pattern_query.isSelectType())
					if (!StoreOrRead(pattern_query,dict_query, sparqlendpoint, dict_name))
						continue br2;
			if (checkEndpoint)
				if (!StoreOrRead(pattern_query,dict_query, sparqlendpoint, dict_name)) {
					continue br2;
				}
			Query query = null;
			br3: for (Query q : pattern_instance.get(pattern_query)) {
				if (checkEndpoint)
					if (StoreOrRead(q, dict_query, sparqlendpoint, dict_name)) {
						query = q;
						break br3;
					}
			}
			if(query == null)
				continue br2;
			
			
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
			jo.put("SPARQL Query Pattern", pattern_query.serialize());
			jo.put("Instance Query", query.serialize());
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
		// storeDict(dict_query);
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

	private static HashMap<Query, Integer> findFrequentQuery(List<Query> inputArr) {
		HashMap<Query, Integer> numberMap = new HashMap<Query, Integer>();
		int frequency = -1;

		int value;
		for (int i = 0; i < inputArr.size(); i++) {

			value = -1;
			if (numberMap.containsKey(inputArr.get(i)))
				// if (numberMap.keySet().contains(inputArr.get(i))) {
				value = numberMap.get(inputArr.get(i));
				// }
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

	public static HashMap<Query, Integer> sortInstanceByValue(HashMap<Query, Integer> hm) {
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
		return temp;
	}

	private static boolean check_with_endpoint(Query query, String sparqlendpoint) { // check if query will return results via bio2rdf SPARQL endpoint
		boolean check = false;
		if(!query.isSelectType()){
			return check_ASK_CONSTRUCT_DESCRIBE(query, sparqlendpoint);
		}
		SelectBuilder selectBuilder = new SelectBuilder();
        HandlerBlock handlerBlock = new HandlerBlock(query);
        selectBuilder.getHandlerBlock().addAll(handlerBlock);
		selectBuilder.setLimit(1);
		selectBuilder.setBase(null);
        QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlendpoint, selectBuilder.build());
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

	private static boolean check_ASK_CONSTRUCT_DESCRIBE(Query query, String sparqlendpint){ // "https://bio2rdf.org/sparql"
		boolean check = false;
		if (query.isAskType()) {
			AskBuilder selectBuilder = new AskBuilder();
			HandlerBlock handlerBlock = new HandlerBlock(query);
			selectBuilder.getHandlerBlock().addAll(handlerBlock);
			selectBuilder.setLimit(1);
			selectBuilder.setBase(null);
			// selectBuilder.addVar("*");
			QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlendpint,
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
			QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlendpint,
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
			QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlendpint,
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

	public static Map<Var,org.apache.jena.graph.Node> get_result_of_vars(Query query, String sparqlendpoint){ // get var-value pairs for pattern query via endpoint 
		Map<Var,org.apache.jena.graph.Node> var_results = new HashMap<Var,org.apache.jena.graph.Node>();
		if (query.isSelectType()){
			SelectBuilder selectBuilder = new SelectBuilder();
        	HandlerBlock handlerBlock = new HandlerBlock(query);
			selectBuilder.getHandlerBlock().addAll(handlerBlock);
			selectBuilder.setLimit(1);
			selectBuilder.setBase(null);
			// selectBuilder.addVar("*");
			QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlendpoint,
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

	private static boolean StoreOrRead(Query query,Map<Query, Boolean> dict_query, String sparqlendpoint, String dict_name){
		if(dict_query.keySet().contains(query)){
			return dict_query.get(query);
		}
		boolean bl = check_with_endpoint(query, sparqlendpoint);
		dict_query.put(query, bl);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(dict_name, true));
			bw.write(query.serialize().replace("\n", "\\n").replace("\r", "\\r") + " & " + Boolean.toString(bl));
			bw.newLine();
			bw.flush();
			bw.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return bl;
	}

	private static Map<Query, Boolean> getDict(String dict_name) {
		Map<Query, Boolean> dict_query = new HashMap<Query, Boolean>();
		if (!new File(dict_name).exists()) {
			return dict_query;
		} else {
			try {
				BufferedReader br = new BufferedReader(new FileReader(dict_name));
				String line = null;
				while ((line = br.readLine()) != null) {
					String[] splitline = line.split(" & ");
					try {
						dict_query.put(construcQuery(splitline[0].replace("\\n", "\n").replace("\\r", "\r")), Boolean.parseBoolean(splitline[1]));
					} catch (Exception e) {
						// TODO: handle exception
						// System.out.println(splitline[0]);
						// e.printStackTrace();
					}
				}
				br.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
		}
		return dict_query;
	}

	private static Query construcQuery(String queryString){
		Query query = QueryFactory.create(queryString);
		if(query.isSelectType()){
			SelectBuilder builder = new SelectBuilder();
			HandlerBlock handlerBlock = new HandlerBlock(query);
			builder.getHandlerBlock().addAll(handlerBlock);
			builder.setBase(null);
			query = builder.build();
		}
		else if(query.isAskType()){
			AskBuilder builder = new AskBuilder();
			HandlerBlock handlerBlock = new HandlerBlock(query);
			builder.getHandlerBlock().addAll(handlerBlock);
			builder.setBase(null);
			query = builder.build();
		}
		else if (query.isConstructType()){
			ConstructBuilder builder = new ConstructBuilder();
			HandlerBlock handlerBlock = new HandlerBlock(query);
			builder.getHandlerBlock().addAll(handlerBlock);
			builder.setBase(null);
			query = builder.build();
		}
		else if (query.isDescribeType()){
			DescribeBuilder builder = new DescribeBuilder();
			HandlerBlock handlerBlock = new HandlerBlock(query);
			builder.getHandlerBlock().addAll(handlerBlock);
			builder.setBase(null);
			query = builder.build();
		}
		return query;
	}

	private static Set<String> get_bind_vars(Query query){
		HashSet<String> bindvars = new HashSet<String>();
		SerializationContext cxt = new SerializationContext();
        IndentedLineBuffer b = new IndentedLineBuffer();
        FormatterElement visitor = new FormatterElement(b, cxt){
            @Override
            public void visit(ElementBind el) {
                bindvars.add(el.getVar().getVarName());
            }
        };
		try {
			query.getQueryPattern().visit(visitor);
		} catch (Exception e) {
			//TODO: handle exception
		}
		return bindvars;
	}

	private static Query generalize_VALUES(Query q){
		Map<String,String> replace_map = new HashMap<String, String>();
		List<Integer> rowCount = new ArrayList<Integer>();
			List<Var> bindVars = new ArrayList<Var>();
			List<Binding> rowlist = new ArrayList<>();
			try {
				List<Element> elements = new ArrayList<Element>();
				if (q.getQueryPattern() instanceof ElementGroup)
					elements = ((ElementGroup) q.getQueryPattern()).getElements();
				else if (q.getQueryPattern() instanceof Element) {
					elements.add(q.getQueryPattern());
				} else {
					return q;
				}
				Element queryel = q.cloneQuery().getQueryPattern();
				GroupElementVisitor elvisitor = new GroupElementVisitor(){

					@Override
					public void visit(ElementData ele) {
						// TODO Auto-generated method stub
						if(ele.toString().strip().startsWith("VALUES") || ele.toString().strip().startsWith("{ VALUES")) {
							Op op = Algebra.compile(ele);
							AllOpVisitor visitorBind = new AllOpVisitor() {
								@Override
								public void visit(OpBGP opBGP) {
									// Do nothing
								}
	
								@Override
								public void visit(OpSlice opSlice) {
									opSlice.getSubOp().visit(this);
								}
	
								@Override
								public void visit(OpTable opTable) {
									Iterator<Binding> rows = opTable.getTable().rows();
									for (; rows.hasNext();) {
										Binding row = rows.next();
										// BindingBuilder.create().add(null, null)
										// System.out.println(row.vars().next() + " " +
										// row.get(row.vars().next()).toString());
										Iterator<Var> varIte = row.vars();
										BindingBuilder bindingBuilder = BindingBuilder.create();
										for (; varIte.hasNext();) {
											Var var = varIte.next();
											if (!bindVars.contains(var)) {
												bindVars.add(var);
												// Binding varRow = BindingBuilder.create().add(null, null);
												// varRow
												// rowlist.add(BindingBuilder.create().add(row.vars().next(), new
												// Node_Variable("ValuesVar")).build());
											}
											bindingBuilder.add(var,NodeFactory.createURI("https://example.org/ValuesVar" + Integer.toString(rowCount.size() + 1)));
											rowCount.add(1);
										}
										Binding newBinding = bindingBuilder.build();
										if (!rowlist.contains(newBinding)) {
											rowlist.add(newBinding);
										}
									}
								}
	
							};
							op.visit(visitorBind);
							op = OpTable.create(new TableData(bindVars, rowlist));
							Converter converter = new Converter(op);
							// ((ElementGroup) q.getQueryPattern()).getElements().remove(ele);
							// ((ElementGroup) q.getQueryPattern()).getElements().add(converter.asElement(op));
							// System.out.println(ele.toString());
							replace_map.put(ele.toString().strip(), "");
					}
				}
				};
				queryel.visit(elvisitor);
				String queryString = q.serialize();
				for(Entry<String,String> replace_ele : replace_map.entrySet()){
					queryString = queryString.replace(replace_ele.getKey(), replace_ele.getValue());
				}
				// System.out.println(queryString);
				return construcQuery(queryString);
				// System.out.println(q.serialize());
			} catch (Exception e) {
				//TODO: handle exception
				e.printStackTrace();
			}
			return q;
	}
}
