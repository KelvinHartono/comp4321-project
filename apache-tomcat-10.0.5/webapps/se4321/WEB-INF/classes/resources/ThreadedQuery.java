package resources;

import java.util.Vector;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.nio.ByteBuffer;

/**
 * This is a helper class for Query.java
 */
public class ThreadedQuery implements Runnable {
  private static final int THREADS = Runtime.getRuntime().availableProcessors() * 2; // Number of threads

  private int id;// Id of thread

  // Helper arrays
  private HashMap<String, Integer> queries;
  private HashMap<String, Integer> rawQueries;
  private Vector<String> phraseQueries;
  private Vector<HashMap<String, String>> retArr;
  private HashMap<String, Integer> dfs;

  // Length of query, saves time to not precompute in every thread
  private int queryLen;
  private Rocks rocks;
  private static final Double WEIGHT_COSIM = 0.4;
  private static final Double WEIGHT_TITLESIM = 0.5;
  private static final Double WEIGHT_PAGERANK = 0.1;
  private Porter porter;

  public ThreadedQuery(int id, HashMap<String, Integer> rawQueries, HashMap<String, Integer> queries,
      Vector<String> phraseQueries, Vector<HashMap<String, String>> retArr, HashMap<String, Integer> dfs, int queryLen,
      Rocks rocks) {
    this.id = id;
    this.queries = queries;
    this.phraseQueries = phraseQueries;
    this.retArr = retArr;
    this.dfs = dfs;
    this.queryLen = queryLen;
    this.rocks = rocks;
    this.rawQueries = rawQueries;
    this.porter = new Porter();
  }

  /**
   * byte[] to double, as there is no function like this before.
   * 
   * @param bytes
   * @return double
   */
  public static double toDouble(byte[] bytes) {
    return ByteBuffer.wrap(bytes).getDouble();
  }

  /**
   * helper array for Query.java
   */
  public void run() {
    RocksIterator iter = rocks.getIterator(Database.ForwardIndex); // Iterzator
    long N = rocks.getSize(Database.ForwardIndex);
    /*
     * Prepare phrase queries helper array phraseQueries =
     * ["Hong Kong","american eagle"] phraseDics =
     * {"Hong"=[0],"Kong"=[0],"american"=[1], "eagle"=[1]}
     **/
    int counter = 0;
    for (iter.seekToFirst(); iter.isValid(); iter.next()) {
      if (counter % THREADS != id) {
        counter++;
        continue;
      }
      counter++;
      String wordFreqs = new String(iter.value());
      wordFreqs = wordFreqs.substring(1, wordFreqs.length() - 1);

      // initialize the scores
      double cosSim;
      Double titleSim = 0.0;
      Double pageRank = 0.0;

      double innerProduct = 0;
      double docLen = 0;

      // We do the non-phrase terms
      ArrayList<HashMap<String, String>> top5 = new ArrayList<HashMap<String, String>>();
      // We do the non-phrase terms
      for (String str : wordFreqs.split(", ")) {
        String[] strFreq = str.split("=");
        int freq = Integer.parseInt(strFreq[1]);
        docLen += Math.pow(freq, 2);
        if (queries.containsKey(strFreq[0])) {
          int tf = queries.get(strFreq[0]);
          int df = dfs.get(strFreq[0]);
          if (df == 0) {
            df = 1;
          }
          double idf = Math.log(N / df) / Math.log(2);
          innerProduct += Double.parseDouble(strFreq[1]) * tf * idf;
        }
        int putAt = 0;
        for (putAt = top5.size() > 6 ? 6 : top5.size(); putAt > 0; putAt--) {
          if (freq <= Integer.parseInt(top5.get(putAt - 1).get("freq"))) {
            break;
          }
        }
        HashMap<String, String> temp = new HashMap<String, String>();
        temp.put("key", strFreq[0]);
        temp.put("freq", Integer.toString(freq));
        top5.add(putAt, temp);
      }

      // Phrasal scoring
      for (String phrase : phraseQueries) {
        boolean valid = true;
        String[] phraseSplit = phrase.split(" ");
        int[] checkpoints = new int[phraseSplit.length];
        Vector<String[]> posInfos = new Vector<String[]>();// position infos
        int freq = 0; // frequency of the term
        for (int i = 0; i < phraseSplit.length; i++) {
          String currWord = phraseSplit[i];
          byte[] key = (currWord + "@" + new String(iter.key())).getBytes();
          byte[] infos = null;
          try {
            infos = rocks.getEntry(Database.WordToPage, key);
          } catch (RocksDBException e) {
            System.err.println(e.toString());
          }
          if (infos != null) {
            posInfos.add((new String(infos)).split("@"));
          } else {
            valid = false;
            break;
          }
        }
        if (!valid)
          continue;

        for (int i = 1; i < posInfos.get(0).length; i++) {
          int firstLoc = Integer.parseInt(posInfos.get(0)[i]);
          int distance = 1;
          for (int j = 1; j < phraseSplit.length; j++) {
            boolean isPhrase = false;
            if (phraseSplit[j].equals("*")) {
              distance += 1;
              continue;
            }
            for (int k = checkpoints[j] + 1; k < posInfos.get(j).length; k++) {
              int currLoc = Integer.parseInt(posInfos.get(j)[k]);
              if (currLoc <= firstLoc) {
                continue;
              } else if (firstLoc < currLoc && currLoc <= firstLoc + distance) {
                checkpoints[j] = k;
                isPhrase = true;
                break;
              } else {
                checkpoints[j] = k;
                break;
              }
            }
            if (j == phraseSplit.length - 1 && isPhrase) {
              freq += 1;
            }
          }
        }
        innerProduct += freq;
      }
      // Handling 0 divisor
      if (docLen == 0.0) {
        docLen = 1.0;
      } else if (queryLen == 0.0) {
        queryLen = 1;
      }
      cosSim = innerProduct / (Math.sqrt(docLen) * Math.sqrt(queryLen));
      HashMap<String, String> ret = new HashMap<String, String>();

      // Get the page informations and put it as the output
      String title = "";
      try {
        byte[] linkInBytes = rocks.getEntry(Database.PageIDtoURLInfo, iter.key());
        String link = new String(linkInBytes);
        byte[] ptc = rocks.getEntry(Database.ParentToChild, iter.key());
        String ptcs = new String(ptc);
        String ret_ptc = "@@";
        if (ptcs.contains("@@")) {
          ret_ptc = "";
          String ptcsa[] = ptcs.split("@@");
          for (String p : ptcsa) {
            byte[] out = rocks.getEntry(Database.PageIDtoURLInfo, p.getBytes());
            String infos[] = new String(out).split("@@");
            if (ret_ptc == null) {
              ret_ptc = new String(infos[0].substring(2, infos[0].length()));
            } else {
              ret_ptc = ret_ptc + "@@" + new String(infos[0]);
            }
          }
        }
        ret.put("child", new String(ret_ptc));
        byte[] ctp = rocks.getEntry(Database.ChildToParent, iter.key());
        String ctps = new String(ptc);
        String ret_ctp = "@@";
        if (ctps.contains("@@")) {
          ret_ctp = "";
          String ctpsa[] = ctps.split("@@");
          for (String p : ctpsa) {
            byte[] out = rocks.getEntry(Database.PageIDtoURLInfo, p.getBytes());
            String infos[] = new String(out).split("@@");
            if (ret_ctp == null) {
              ret_ctp = new String(infos[0].substring(2, infos[0].length()));
            } else {
              ret_ctp = ret_ctp + "@@" + new String(infos[0]);
            }
          }
        }
        ret.put("parent", new String(ret_ctp));
        String infos[] = link.split("@@");
        ret.put("url", infos[0]);
        ret.put("date", infos[1]);
        ret.put("size", infos[2]);
        title = infos[3];
        ret.put("title", infos[3]);
        int tempSize = (top5.size() < 6) ? top5.size() : 6;
        // int tempSize = top5.size();
        String top5Str = "";
        for (int i = 1; i < tempSize; i++) {
          HashMap<String, String> keyword = top5.get(i);
          top5Str += keyword.get("key") + " " + keyword.get("freq") + "; ";
        }
        ret.put("keyword", top5Str);
      } catch (Exception e) {
        e.printStackTrace();
        // System.err.println(e.toString());
      }

      // handle title similarity calculation
      if (!title.equals("")) {
        HashMap<String, Integer> titleMap = new HashMap<String, Integer>();
        StringTokenizer st = new StringTokenizer(title);
        while (st.hasMoreTokens()) {
          String str = porter.stripAffixes(st.nextToken());
          if (titleMap.containsKey(str)) {
            titleMap.replace(str, titleMap.get(str) + 1);
          } else {
            titleMap.put(str, 1);
          }
        }
        Double tf = 0.0;
        for (String j : rawQueries.keySet()) {
          if (titleMap.containsKey(j)) {
            tf += 1.0;
          }
        }
        titleSim = Math.pow(tf / ((queryLen + titleMap.size()) / 2), 1);
      }

      // handle pagerank calculation
      try {
        byte[] temp = rocks.getEntry(Database.PageRank, iter.key());
        String tempQ = "max";
        double max = toDouble(rocks.getEntry(Database.PageRank, tempQ.getBytes()));
        tempQ = "min";
        double min = toDouble(rocks.getEntry(Database.PageRank, tempQ.getBytes()));
        if (temp != null) {
          pageRank = (toDouble(temp) - min) / (max - min);
        }
      } catch (Exception e) {
        e.printStackTrace();
        // System.err.println(e.toString());
      }

      // put values on the returned array
      ret.put("cosim", Double.toString(cosSim));
      ret.put("titleSim", Double.toString(titleSim));
      ret.put("pageRank", Double.toString(pageRank));
      ret.put("score",
          Double.toString(cosSim * WEIGHT_COSIM + titleSim * WEIGHT_TITLESIM + pageRank * WEIGHT_PAGERANK));
      retArr.add(ret);
    }
  }
}
