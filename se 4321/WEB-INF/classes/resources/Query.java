package resources;

import java.util.Vector;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.lang.StringBuffer;
import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import org.jsoup.HttpStatusException;
import java.lang.RuntimeException;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import java.io.*;
import java.nio.ByteBuffer;

public class Query {
  public static final int THREADS = Runtime.getRuntime().availableProcessors() * 2;

  private Rocks rocks;
  private Porter porter;
  private StopWord stopW;
  HashMap<String, Integer> dfs;

  public Query() {
    // Load the database from their paths
    String[] dbpath = new String[9];
    porter = new Porter();
    stopW = new StopWord();

    for (int i = 0; i < 9; ++i) {
      dbpath[i] = "D:\\HKUST\\Spring Sem 20-21\\COMP 4321\\se 4321\\apache-tomcat-10.0.5\\webapps\\se 4321\\WEB-INF\\classes\\resources\\db\\" + Integer.toString(i);
    }
    try {
      this.rocks = new Rocks(dbpath);
    } catch (RocksDBException e) {
      System.err.println(e.toString());
    }
    dfs = new HashMap<String, Integer>();
    getAllDf(dfs);
  }

  /**
   * Return array of website links and its informations including score. This is
   * the main function to process a query
   * 
   * @param query  the query string
   * @param retArr the information result, the parameters are
   *               "score","cosim","pageRank","titleSim","child","parent","url","date","size","title"
   */
  public Vector<HashMap<String, String>> processQuery(String query) {

    System.out.println("Query: " + query);

    // Create the helper arrays
    Vector<HashMap<String, String>> retArr = new Vector<HashMap<String, String>>();
    StringBuffer curr = new StringBuffer("");
    Vector<String> queryArr = new Vector<String>();
    Vector<String> queryPArr = new Vector<String>();
    HashMap<String, Integer> rawQuery = new HashMap<String, Integer>();
    Boolean phrase = false;
    int prevSpace = 0;

    // This block of code is to process the incoming query, split it to term and
    // phrases.
    for (int i = 0; i < query.length(); i++) {
      if (phrase == false) {
        if (curr.toString().equals("")) {
          if (query.charAt(i) == ' ') {
            continue;
          } else if (query.charAt(i) == '\"') {
            phrase = true;
          } else {
            curr.append(query.charAt(i));
          }
        } else {
          if (query.charAt(i) == ' ') {
            String temp = curr.toString();
            queryArr.add(temp);
            rawQuery.put(porter.stripAffixes(temp), 1);
            curr = new StringBuffer("");
          } else {
            curr.append(query.charAt(i));
          }
        }
      } else {
        if (query.charAt(i) == '\"') {
          String latestWord = curr.toString().substring(prevSpace, curr.length());
          rawQuery.put(porter.stripAffixes(latestWord), 1);
          if (stopW.isStopWord(latestWord)) {
            curr.replace(prevSpace, curr.length(), "*");
          } else {
            curr.replace(prevSpace, curr.length(), porter.stripAffixes(latestWord));
          }
          prevSpace = curr.length();
          String temp = curr.toString();
          queryPArr.add(temp);
          curr = new StringBuffer("");
          phrase = false;
        } else if (query.charAt(i) == ' ') {
          String latestWord = curr.toString().substring(prevSpace, curr.length());
          rawQuery.put(porter.stripAffixes(latestWord), 1);
          if (stopW.isStopWord(latestWord)) {
            curr.replace(prevSpace, curr.length(), "*");
          } else {
            curr.replace(prevSpace, curr.length(), porter.stripAffixes(latestWord));
          }
          curr.append(query.charAt(i));
          prevSpace = curr.length();
        } else {
          curr.append(query.charAt(i));
        }
      }
    }
    if (!curr.toString().equals("")) {
      queryArr.add(curr.toString());
      rawQuery.put(porter.stripAffixes(curr.toString()), 1);
    }

    // Stem the term Array. This could be improved for the future
    HashMap<String, Integer> stemmedQueryArr = new HashMap<String, Integer>();
    for (String q : queryArr) {
      String stripped = porter.stripAffixes(q);
      if (stemmedQueryArr.containsKey(stripped)) {
        stemmedQueryArr.replace(stripped, stemmedQueryArr.get(stripped) + 1);
      } else {
        stemmedQueryArr.put(stripped, 1);
      }
    }

    // Call a function which handles threaded score calculation
    try {
      calculateScoresThreaded(stemmedQueryArr, queryPArr, rawQuery, retArr);
      return sortResult(retArr);
    } catch (RocksDBException e) {
      System.err.println(e.toString());
    }
    return retArr;
  }

  /**
   * To get all the words in the dictionary, will be deprecated if there is any
   * performance update
   * 
   * @param phraseQueries
   * @return [{"Hong"=1,"Kong"=1},{"american"=1, "eagle"=1}]
   */
  private HashMap<String, Vector<Integer>> getPhraseDics(Vector<String> phraseQueries) {
    HashMap<String, Vector<Integer>> phraseDics = new HashMap<String, Vector<Integer>>();
    for (int i = 0; i < phraseQueries.size(); i++) {
      System.out.println(phraseQueries.get(i));
      HashMap<String, Integer> curPhrase = new HashMap<String, Integer>();
      for (String word : phraseQueries.get(i).split(" ")) {
        curPhrase.put(word, 1);
        if (phraseDics.containsKey(word)) {
          Vector<Integer> locs = phraseDics.get(word);
          locs.add(i);
          phraseDics.replace(word, locs);
        } else {
          Vector<Integer> temp = new Vector<Integer>();
          temp.add(i);
          phraseDics.put(word, temp);
        }
      }
    }
    return phraseDics;
  }

  /**
   * To calculate the document frequency of every term
   * 
   * @param df input the df hashmap, we will return the result here
   */
  public void getAllDf(HashMap<String, Integer> df) {
    RocksIterator iter = rocks.getIterator(Database.HTMLtoPage); // Iterzator
    for (iter.seekToFirst(); iter.isValid(); iter.next()) {
      df.put(new String(iter.key()), new String(iter.value()).split("@@").length);
    }
  }

  /**
   * Calculate the scores by threading. This is a helper function!
   * 
   * @param queries
   * @param phraseQueries
   * @param rawQuery
   * @param retArr        We will return the result here
   * @throws RocksDBException
   */
  public void calculateScoresThreaded(HashMap<String, Integer> queries, Vector<String> phraseQueries,
      HashMap<String, Integer> rawQuery, Vector<HashMap<String, String>> retArr) throws RocksDBException {
    long currentTime = System.currentTimeMillis();
    /*
     * Prepare phrase queries helper array phraseQueries =
     * ["Hong Kong","american eagle"] phraseDics =
     * {"Hong"=[0],"Kong"=[0],"american"=[1], "eagle"=[1]}
     **/
    HashMap<String, Vector<Integer>> phraseDics = getPhraseDics(phraseQueries);

    long N = rocks.getSize(Database.ForwardIndex);// approximate size of the database

    // print informations
    System.out.println("There are " + N + " documents in total.");
    System.out.println("There are " + THREADS + " threads in total.");
    int queryLen = 0;
    for (int query : queries.values())
      queryLen += Math.pow(query, 2);
    for (Vector<Integer> q : phraseDics.values())
      queryLen += Math.pow(q.size(), 2);

    // Do the calculation
    Vector<Thread> pool = new Vector<Thread>();
    for (int i = 0; i < THREADS; i++) {
      ThreadedQuery tq = new ThreadedQuery(i, rawQuery, queries, phraseQueries, retArr, dfs, queryLen, rocks);
      pool.add(new Thread(tq));
      pool.get(i).start();
    }
    for (int i = 0; i < THREADS; i++) {
      try {
        pool.get(i).join();
      } catch (Exception e) {
        System.err.println(e.toString());
      }
    }

    // return retArr;
  }

  /**
   * Sort the result. This is a helper function!
   * 
   * @param input
   * @return
   */
  public static Vector<HashMap<String, String>> sortResult(Vector<HashMap<String, String>> input) {
    Vector<HashMap<String, String>> ret = new Vector<HashMap<String, String>>();
    // int size = input.size();
    for (HashMap<String, String> curr : input) {
      Double score = Double.parseDouble(curr.get("score"));
      int l = 0;
      int r = ret.size();
      while (r > l) {
        int m = l + (r - l) / 2;
        Double cs = Double.parseDouble(ret.elementAt(m).get("score"));
        if (cs > score)
          l = m + 1;
        else
          r = m;
      }
      ret.insertElementAt(curr, l + (r - l) / 2);
    }
    return ret;
  }

//  public static void main(String[] args) {
//    long currentTime = System.currentTimeMillis();
//    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//    String str = "";
//    Query test = new Query();
//    System.out.println("\nTime elapsed = " + (System.currentTimeMillis() - currentTime) + " ms");
//    while (!str.equals("x")) {
//      try {
//        System.out.println("Enter query:");
//        str = br.readLine();
//        currentTime = System.currentTimeMillis();
//        Vector<HashMap<String, String>> retVal = test.processQuery(str);
//        System.out.println("\nTime elapsed = " + (System.currentTimeMillis() - currentTime) + " ms");
//        for (int i = 0; i < 5; i++) {
//          System.out.print((i + 1) + ". " + retVal.elementAt(i).get("url"));
//          System.out.print(" " + retVal.elementAt(i).get("score"));
//          System.out.print(" " + retVal.elementAt(i).get("cosim"));
//          System.out.print(" " + retVal.elementAt(i).get("titleSim"));
//          System.out.println(" " + retVal.elementAt(i).get("pageRank"));
//        }
//      } catch (IOException ioe) {
//        System.out.println(ioe);
//      }
//    }
//  }
}
