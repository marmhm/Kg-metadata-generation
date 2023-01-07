package nl.cochez.query_processing.metadata;

import java.util.List;
import java.util.Map.Entry;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.handlers.HandlerBlock;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.core.Var;

public class test {
    public static void main(String[] args) {
        // String queryString = "SELECT ?o Where {?s ?p ?o}";
        // SelectBuilder selectBuilder = new SelectBuilder();
        // List<Var> vars = selectBuilder.getVars();
        // HandlerBlock handlerBlock = new HandlerBlock(new QueryFactory().create(queryString));
        // selectBuilder.getHandlerBlock().addAll(handlerBlock);
        // System.out.println(selectBuilder.buildString());
        // selectBuilder.addVar("*");
        // selectBuilder.setLimit(1);
		// Query query = selectBuilder.build();
        
        // System.out.println(query.serialize());
        // System.out.println(PatternDisplay.get_result_of_vars(query));
        // for(Entry<Var,org.apache.jena.graph.Node> pair: PatternDisplay.get_result_of_vars(query).entrySet()){
        //     System.out.println(pair);
        //     selectBuilder.setVar(pair.getKey(),pair.getValue());
        // }
        // for(Var var : vars)
        // selectBuilder.addVar(var);
        // selectBuilder.setLimit(0);
        // System.out.println(selectBuilder.buildString());
    }
}
