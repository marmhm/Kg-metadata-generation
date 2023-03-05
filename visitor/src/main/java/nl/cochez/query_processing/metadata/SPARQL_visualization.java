package nl.cochez.query_processing.metadata;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.DescribeBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.handlers.HandlerBlock;
import org.apache.jena.graph.NodeFactory;
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


import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Font;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.Node;
import static guru.nidi.graphviz.model.Link.to;
import static guru.nidi.graphviz.engine.Format.*;

import static guru.nidi.graphviz.model.Factory.graph;
import static guru.nidi.graphviz.model.Factory.node;

public class SPARQL_visualization {
    public static void main(String[] args) {
        String endpoint = "";
        String queryString = "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> \n SELECT DISTINCT ?uri WHERE { ?uri a <http://dbpedia.org/ontology/Song> .\n ?uri <http://dbpedia.org/ontology/artist> <http://dbpedia.org/resource/Bruce_Springsteen> . ?uri <http://dbpedia.org/ontology/releaseDate> ?date . FILTER (?date >= '1980-01-01'^^xsd:date && ?date <= '1990-12-31'^^xsd:date) }";
        List<String> queries = new ArrayList<String>();
        queries.add(queryString);
        queries.add("SELECT DISTINCT  ?p ?o WHERE   { <http://bio2rdf.org/drugbank_vocabulary:target>               ?p  ?o     FILTER ( ?p != <http://www.w3.org/2002/07/owl#sameAs> )     FILTER ( ( ?p != <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ) || ( ?o != <http://www.w3.org/2002/07/owl#Class> ) )     FILTER ( ( ?p != <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ) || ( ?o != <http://www.w3.org/1999/02/22-rdf-syntax-ns#Property> ) )     FILTER ( ( ?p != <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ) || ( ?o != <http://www.w3.org/2002/07/owl#ObjectProperty> ) )     FILTER ( ( ?p != <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ) || ( ?o != <http://www.w3.org/2002/07/owl#AnnotationProperty> ) )     FILTER ( ( ?p != <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ) || ( ?o != <http://www.w3.org/2002/07/owl#DatatypeProperty> ) )   }");
        List<Node> nodes = display_multi_SPARQL(queries);
        // add query into String list "queries"
        Graph g = graph().directed().with(nodes);
        try {
            Graphviz.fromGraph(g).render(Format.PNG).toFile(new File("SPARQL_graph.png")); // output as png graph
            Graphviz.fromGraph(g).render(Format.DOT).toFile(new File("SPARQL_graph.dot")); // output as .dot file which you analyse this graph with this file
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println(g.toString());
        // System.out.println(getShortURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
    }

    private static List<Node> display_multi_SPARQL(List<String> queries){
        List<Node> nodes = new ArrayList<Node>();

        
        List<Integer> count = new ArrayList<Integer>();
        count.add(1);
        for(String query :queries){
            HashMap<String, org.apache.jena.graph.Node> dict = new HashMap<String, org.apache.jena.graph.Node>();
            for (Triple t : getALLtriples(query,count,dict)){
                nodes.add(Triple2Node(t));
            }
        }

        return nodes;
    }

    private static List<Triple> getALLtriples(String inputString){

        Query q = QueryFactory.create(inputString);
        Op ope = Algebra.compile(q);
        List<Triple> triples = new ArrayList<Triple>();

        AllOpVisitor allbgp = new AllOpVisitor() {
            @Override
            public void visit(OpBGP opBGP) {
                for (Triple t : opBGP.getPattern()) {
                    triples.add(t);
                    // System.out.println(t.getSubject().toString());
                    // System.out.println(t.getPredicate().getLocalName());
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

        return triples;
    }

    private static List<Triple> getALLtriples(String inputString, List<Integer> count, HashMap<String, org.apache.jena.graph.Node> dict){

        Query q = QueryFactory.create(inputString);
        Op ope = Algebra.compile(q);
        List<Triple> triples = new ArrayList<Triple>();

        AllOpVisitor allbgp = new AllOpVisitor() {
            @Override
            public void visit(OpBGP opBGP) {
                for (Triple t : opBGP.getPattern()) {
                    NodeFactory model = new NodeFactory();
                    org.apache.jena.graph.Node s = t.getSubject();
                    org.apache.jena.graph.Node p = t.getPredicate();
                    org.apache.jena.graph.Node o = t.getObject();
                    if(t.getSubject().toString().startsWith("?")){
                        if(dict.containsKey(t.getSubject().toString())){
                            s = dict.get(t.getSubject().toString());
                        }
                        else{
                            s = model.createVariable("v"+Integer.toString(count.size()));
                            count.add(1);
                            dict.put(t.getSubject().toString(), s);
                        }
                    }
                    if(t.getPredicate().toString().startsWith("?")){
                        if(dict.containsKey(t.getPredicate().toString())){
                            p = dict.get(t.getPredicate().toString());
                        }
                        else{
                            p = model.createVariable("v"+Integer.toString(count.size()));
                            count.add(1);
                            dict.put(t.getPredicate().toString(), p);
                        }
                    }
                    if(t.getObject().toString().startsWith("?")){
                        if(dict.containsKey(t.getObject().toString())){
                            o = dict.get(t.getObject().toString());
                        }
                        else{
                            o = model.createVariable("v"+Integer.toString(count.size()));
                            count.add(1);
                            dict.put(t.getObject().toString(), o);
                        }
                    }
                    triples.add(new Triple(s, p, o));
                    // System.out.println(t.getSubject().toString());
                    // System.out.println(t.getPredicate().getLocalName());
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

        return triples;
    }

    private static Node Triple2Node(Triple t){
        return node(getShortURI(t.getSubject().toString())) // source node
        .link(to(node(getShortURI(t.getObject().toString()))) // target node
        .with(Label.of(getShortURI(t.getPredicate().toString().replace("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "a"))))); // give relation label
    }

    private static String getShortURI(String fullURI){
        String shortURI = fullURI;

        if (fullURI == "http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
            return "a";

        String link = "https://prefix.cc/?q="+fullURI;
        try {
            // URLConnection con = new URL( link ).openConnection();
            // System.out.println( "orignal url: " + con.getURL() );
            
            // con.connect();
            // System.out.println( "connected url: " + con.getURL() );
            shortURI = getRedirectedUrl(link).replace("https://prefix.cc/", "");
        } catch (Exception e) {
            // TODO: handle exception
        }
        

        return shortURI;
    }

    public static String getRedirectedUrl(String url) throws MalformedURLException, IOException {
        HttpURLConnection connection;
        String finalUrl = url;//from   w  w  w .  ja va2  s  .  co m
        do {
            connection = (HttpURLConnection) new URL(finalUrl).openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setRequestMethod("GET");
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode >= 300 && responseCode < 400) {
                String redirectedUrl = connection.getHeaderField("Location");
                if (redirectedUrl == null)
                    break;
                finalUrl = redirectedUrl;
            } else
                break;
        } while (connection.getResponseCode() != HttpURLConnection.HTTP_OK);
        connection.disconnect();
        return finalUrl.replaceAll(" ", "%20");
    }
}
