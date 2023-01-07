package nl.cochez.query_processing.metadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpSlice;
import org.apache.jena.sparql.core.BasicPattern;

public class queryQquavalence_test {
    public static void main(String[] args) {
        String query1 = "";
		String query2 = "";
		String endpoint = "";

		System.out.println(isomorphism(QueryFactory.create(query1), QueryFactory.create(query2), endpoint));
    }

	private static boolean isomorphism(Query q1, Query q2, String endpoint){
		Set<Query> temp_set = new HashSet<Query>();
		temp_set.add(q2);
		return listOflistContains_checkQueryEquavalence(q1,temp_set,endpoint);
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

    private static boolean listOflistContains_checkQueryEquavalence(Query list, Set<Query> listlist, String endpoint) {
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
				return check_query_equavalence(list,temp,endpoint);
			}
		}
		return false;
	}

	private static boolean check_query_equavalence(Query q1, Query q2, String endpoint){
		boolean bl = true;
		List<String> result1 = get_query_top_results_and_result_count(q1, endpoint);
		List<String> result2 = get_query_top_results_and_result_count(q2, endpoint);
		if(result1.size() == result2.size()){
			for(int i =0;i<result1.size();i++){
				if(!result1.get(i).equals(result2.get(i))){
					bl = false;
				}
			}
		} else {
			bl = false;
		}
		return bl;
	}

	private static List<String> get_query_top_results_and_result_count(Query q, String endpoint){
		List<String> outputs = new ArrayList<String>();
		QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, q.serialize());
        ResultSet rs = null;
		try {
        	rs = qexec.execSelect();
			outputs.add(Integer.toString(rs.getRowNumber()));
			outputs.add(Integer.toString(rs.getResultVars().size()));
			int count = 0;
            if (rs.hasNext() && count < 5) {
				String result = "";
				QuerySolution rb = rs.nextSolution();
				Iterator<String> varIte = rb.varNames();
				while(varIte.hasNext()){
					String var = varIte.next();
					result+= var+" "+rb.get(var).toString()+",";
				}
				outputs.add(result);
				count++;
			}
        }catch (Exception e) {
        	
        }
		qexec.close();
		return outputs;
	}
}
