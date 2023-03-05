package nl.cochez.query_processing.metadata;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.DescribeBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.handlers.HandlerBlock;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpExtend;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.algebra.op.OpSlice;
import org.apache.jena.sparql.algebra.op.OpTable;

import guru.nidi.graphviz.model.Graph;

import static guru.nidi.graphviz.model.Factory.graph;
import static guru.nidi.graphviz.model.Factory.node;

public class FormatSPARQLQuery {
    public static void main(String[] args) {
        String endpoint = "";
        String queryString = "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> \n SELECT DISTINCT ?uri WHERE { ?uri <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Song> .\n ?uri <http://dbpedia.org/ontology/artist> <http://dbpedia.org/resource/Bruce_Springsteen> . ?uri <http://dbpedia.org/ontology/releaseDate> ?date . FILTER (?date >= '1980-01-01'^^xsd:date && ?date <= '1990-12-31'^^xsd:date) }";

        System.out.println(formatSPARQL(queryString));

        Graph g= graph();
        g.directed();
        g.with(node("a"));
        g.with(node("b"));
        g.with();
    }

    public static String formatSPARQL(String inputString){
        String outputString = inputString;

        Query q = QueryFactory.create(outputString);
        Op ope = Algebra.compile(q);
        List<Triple> triples = new ArrayList<Triple>();

        AllOpVisitor allbgp = new AllOpVisitor() {
            @Override
            public void visit(OpBGP opBGP) {
                for (Triple t : opBGP.getPattern()) {
                    triples.add(t);
                }
            }

            @Override
            public void visit(OpSlice opSlice) {
                opSlice.getSubOp().visit(this);
            }

            public void visit(OpExtend opExtend){
                opExtend.getSubOp().visit(this);
            }

            @Override
            public void visit(OpGroup opGroup){
                opGroup.getSubOp().visit(this);
            }

            @Override
            public void visit(OpTable opTable){
            }
        };
        ope.visit(allbgp);
        outputString=q.serialize();

        return outputString;
    }



    private static Query construcQuery(String queryString, List<Triple> triples){
		Query query = QueryFactory.create(queryString);
		// Op op = Algebra.compile(query);
		// query = OpAsQuery.asQuery(op);
		if(query.isSelectType()){
			SelectBuilder builder = new SelectBuilder();
			HandlerBlock handlerBlock = new HandlerBlock(query);
			builder.getHandlerBlock().addAll(handlerBlock);
			builder.setBase(null);
            builder.clearWhereValues();
            for(Triple t : triples){
                builder.addWhere(t);
            }
			query = builder.build();
		}
		else if(query.isAskType()){
			AskBuilder builder = new AskBuilder();
			HandlerBlock handlerBlock = new HandlerBlock(query);
			builder.getHandlerBlock().addAll(handlerBlock);
			builder.setBase(null);
            builder.clearWhereValues();
            for(Triple t : triples){
                builder.addWhere(t);
            }
			query = builder.build();
		}
		else if (query.isConstructType()){
			ConstructBuilder builder = new ConstructBuilder();
			HandlerBlock handlerBlock = new HandlerBlock(query);
			builder.getHandlerBlock().addAll(handlerBlock);
			builder.setBase(null);
            builder.clearWhereValues();
            for(Triple t : triples){
                builder.addWhere(t);
            }
			query = builder.build();
		}
		else if (query.isDescribeType()){
			DescribeBuilder builder = new DescribeBuilder();
			HandlerBlock handlerBlock = new HandlerBlock(query);
			builder.getHandlerBlock().addAll(handlerBlock);
			builder.setBase(null);
            builder.clearWhereValues();
            for(Triple t : triples){
                builder.addWhere(t);
            }
			query = builder.build();
		}
		return query;
	}
}
