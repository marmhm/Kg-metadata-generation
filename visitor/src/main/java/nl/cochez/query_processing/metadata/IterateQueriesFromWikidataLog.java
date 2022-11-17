package nl.cochez.query_processing.metadata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;

public class IterateQueriesFromWikidataLog {
	public static void processFromFile(InputStream in, IQueryCollector collector) throws IOException {
		try (BufferedReader l = new BufferedReader(new BufferedReader(new InputStreamReader(in)))) {
			String line;
			// take headerline off
			l.readLine();
			ArrayList<Query> queryList = new ArrayList<Query>();
			while ((line = l.readLine()) != null) {
				String queryString = URLDecoder.decode(line.split("\t")[0], StandardCharsets.UTF_8);
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
			rankQuery(queryList);
		}
	}

	public static void rankQuery(ArrayList<Query> queryList) {
		List<Map.Entry<Query, Integer>> result = sortByValue(findFrequentNumber(queryList));
		for (int i = 0; i < 10; i++) {
			System.out.println(
					"----------------\n" + "Top" + (i + 1) + " query is\n****************\n" + result.get(i).getKey().toString()
							+ "\n****************\nThe frequency of above query is " + result.get(i).getValue() + "\n----------------\n");
		}
	}

	public static List<Map.Entry<Query, Integer>> sortByValue(HashMap<Query, Integer> hm) {
		List<Map.Entry<Query, Integer>> list = new LinkedList<Map.Entry<Query, Integer>>(hm.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<Query, Integer>>() {
			public int compare(Map.Entry<Query, Integer> o1,
					Map.Entry<Query, Integer> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});
		HashMap<Query, Integer> temp = new LinkedHashMap<Query, Integer>();
		for (Map.Entry<Query, Integer> aa : list) {
			temp.put(aa.getKey(), aa.getValue());
		}
		return list;
	}

	private static HashMap<Query, Integer> findFrequentNumber(ArrayList<Query> inputArr) {
		HashMap<Query, Integer> numberMap = new HashMap<Query, Integer>();
		Query result = null;
		int frequency = -1;

		int value;
		for (int i = 0; i < inputArr.size(); i++) {

			value = -1;
			if (numberMap.containsKey(inputArr.get(i))) {
				value = numberMap.get(inputArr.get(i));
			}
			if (value != -1) {

				value += 1;
				if (value > frequency) {

					frequency = value;
					result = inputArr.get(i);
				}

				numberMap.put(inputArr.get(i), value);
			} else {

				numberMap.put(inputArr.get(i), 1);
			}

		}
		return numberMap;
	}
}
