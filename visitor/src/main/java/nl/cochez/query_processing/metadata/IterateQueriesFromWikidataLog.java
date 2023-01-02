package nl.cochez.query_processing.metadata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.jgrapht.graph.DefaultEdge;
import nl.cochez.query_processing.metadata.IsomorpismClusteringQueryCollector.Node;

public class IterateQueriesFromWikidataLog {
	public static void processFromFile(InputStream in, IQueryCollector collector) throws IOException {
		System.out.println("1 file processing");
		try (BufferedReader l = new BufferedReader(new BufferedReader(new InputStreamReader(in)))) {
			String line;
			// take headerline off
			l.readLine();
			ArrayList<Query> queryList = new ArrayList<Query>();
			while ((line = l.readLine()) != null) {
				String queryString = URLDecoder.decode(line.split("\t")[0].replace("+", " ").replaceAll("%(?![0-9a-fA-F]{2})", "%25").replaceAll("\\+", "%2B"), StandardCharsets.UTF_8);
				// System.out.println(queryString);
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
			// rankPattern(queryList, 10, 5,5);//input is (queryList, top number of display, max number of triples in pattern query)
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