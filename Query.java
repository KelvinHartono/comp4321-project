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
import resources.Porter;
import resources.Rocks;
import resources.StopWord;
import resources.Database;
import resources.ThreadedQuery;
import java.io.*;

public class Query {
  public static final int THREADS = Runtime.getRuntime().availableProcessors() * 2;

  private Rocks rocks;
  private Porter porter;
  private StopWord stopW;
  HashMap<String, Integer> dfs;
  HashMap<String, Integer> titleDfs;

  Query() {
    // Load the database from their paths
    String[] dbpath = new String[9];
    porter = new Porter();
    stopW = new StopWord();

    for (int i = 0; i < 9; ++i) {
      dbpath[i] = "./db/" + Integer.toString(i);
    }
    try {
      this.rocks = new Rocks(dbpath);
    } catch (RocksDBException e) {
      System.err.println(e.toString());
    }
    dfs = new HashMap<String, Integer>();
    titleDfs = new HashMap<String, Integer>();
    getAllDf(dfs, titleDfs);
  }

  // Return array of website links and its score
  public void processQuery(String query, Vector<HashMap<String, String>> retArr) {
    long currentTime = System.currentTimeMillis();
    // Non term e.g. "Hong Kong"
    // String[] queryArr = query.split("\\s+");

    System.out.println("Query: " + query);
    // return;
    StringBuffer curr = new StringBuffer("");
    Vector<String> queryArr = new Vector<String>();
    Vector<String> queryPArr = new Vector<String>();
    HashMap<String, Integer> rawQuery = new HashMap<String, Integer>();
    Boolean phrase = false;
    int prevSpace = 0;
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
    }

    HashMap<String, Integer> stemmedQueryArr = new HashMap<String, Integer>();

    for (String q : queryArr) {
      String stripped = porter.stripAffixes(q);
      if (stemmedQueryArr.containsKey(stripped)) {
        stemmedQueryArr.replace(stripped, stemmedQueryArr.get(stripped) + 1);
      } else {
        stemmedQueryArr.put(stripped, 1);
      }
    }
    try {
      calculateScoresThreaded(stemmedQueryArr, queryPArr, rawQuery, retArr);
      for (HashMap<String, String> hm : retval) {
        if (Double.parseDouble(hm.get("score")) > 0.0)
          System.out.println(hm.get("url") + " = " + hm.get("score"));
        break;
      }
      // System.out.println(retArr.size());
      // return retval;
    } catch (RocksDBException e) {
      System.err.println(e.toString());
    }
    System.out.println("\nTime elapsed = " + (System.currentTimeMillis() - currentTime) + " ms");
    return;
    // return new Vector<HashMap<String, String>>();
  }

  private HashMap<String, Vector<Integer>> getPhraseDics(Vector<String> phraseQueries) {
    // phraseDicArray = [{"Hong"=1,"Kong"=1},{"american"=1, "eagle"=1}]
    Vector<HashMap<String, Integer>> phraseDicArray = new Vector<HashMap<String, Integer>>();
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
      phraseDicArray.add(curPhrase);
    }
    return phraseDics;
  }

  public void getAllDf(HashMap<String, Integer> df, HashMap<String, Integer> titleDf) {
    RocksIterator iter = rocks.getIterator(Database.HTMLtoPage); // Iterzator
    for (iter.seekToFirst(); iter.isValid(); iter.next()) {
      df.put(new String(iter.key()), new String(iter.value()).split("@@").length);
    }
    iter = rocks.getIterator(Database.InvertedIndex); // Iterzator
    for (iter.seekToFirst(); iter.isValid(); iter.next()) {
      titleDf.put(new String(iter.key()), new String(iter.value()).split("@@").length);
    }
  }

  public void calculateScoresThreaded(HashMap<String, Integer> queries, Vector<String> phraseQueries,
      HashMap<String, Integer> rawQuery, Vector<HashMap<String, String>> retArr) throws RocksDBException {
    long currentTime = System.currentTimeMillis();
    /*
     * Prepare phrase queries helper array phraseQueries =
     * ["Hong Kong","american eagle"] phraseDics =
     * {"Hong"=[0],"Kong"=[0],"american"=[1], "eagle"=[1]}
     **/
    HashMap<String, Vector<Integer>> phraseDics = getPhraseDics(phraseQueries);
    long N = rocks.getSize(Database.ForwardIndex);
    System.out.println("There are " + N + " documents in total.");
    System.out.println("There are " + THREADS + " threads in total.");
    int queryLen = 0;
    for (int query : queries.values())
      queryLen += Math.pow(query, 2);
    for (Vector<Integer> q : phraseDics.values())
      queryLen += Math.pow(q.size(), 2);

    Vector<Thread> pool = new Vector<Thread>();
    for (int i = 0; i < THREADS; i++) {
      ThreadedQuery tq = new ThreadedQuery(i, rawQuery, queries, phraseQueries, retArr, dfs, titleDfs, queryLen, rocks);
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

  public static void main(String[] args) {
    long currentTime = System.currentTimeMillis();
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String str = "";
    Query test = new Query();
    System.out.println("\nTime elapsed = " + (System.currentTimeMillis() - currentTime) + " ms");
    while (!str.equals("x")) {
      try {
        System.out.println("Enter query:");
        str = br.readLine();
        currentTime = System.currentTimeMillis();
        Vector<HashMap<String, String>> retVal = new Vector<HashMap<String, String>>();
        test.processQuery(str, retVal);
        System.out.println("\nTime elapsed = " + (System.currentTimeMillis() - currentTime) + " ms");
      } catch (IOException ioe) {
        System.out.println(ioe);
      }
    }
  }
}
