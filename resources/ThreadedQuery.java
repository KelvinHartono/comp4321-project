package resources;

import java.util.Vector;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.lang.RuntimeException;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import resources.Porter;
import resources.Rocks;
import resources.StopWord;
import resources.Database;

public class ThreadedQuery implements Runnable {
  private static final int THREADS = Runtime.getRuntime().availableProcessors();
  private int id;
  private HashMap<String, Integer> queries;
  private Vector<String> phraseQueries;
  private Vector<HashMap<String, String>> retArr;
  private HashMap<String, Integer> dfs;
  private int queryLen;
  private Rocks rocks;
  private Porter porter;
  private StopWord stopW;

  public ThreadedQuery(int id, HashMap<String, Integer> queries, Vector<String> phraseQueries,
      Vector<HashMap<String, String>> retArr, HashMap<String, Integer> dfs, int queryLen, Rocks rocks) {
    this.id = id;
    this.queries = queries;
    this.phraseQueries = phraseQueries;
    this.retArr = retArr;
    this.dfs = dfs;
    this.queryLen = queryLen;
    this.rocks = rocks;
    this.porter = new Porter();
    this.stopW = new StopWord();
  }

  public void run() {
    long currentTime = System.currentTimeMillis();

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
      Double cosSim;
      double innerProduct = 0;
      double docLen = 0;

      // We do the non-phrase terms
      for (String str : wordFreqs.split(", ")) {
        String[] strFreq = str.split("=");
        docLen += Math.pow(Double.parseDouble(strFreq[1]), 2);
        if (queries.containsKey(strFreq[0])) {
          int tf = queries.get(strFreq[0]);
          int df = dfs.get(strFreq[0]);
          if (df == 0) {
            df = 1;
          }
          double idf = Math.log(N / df) / Math.log(2);
          innerProduct += Double.parseDouble(strFreq[1]) * tf * idf;
        }
      }

      // Phrasal scoring
      // Vector<Integer> phrasalFreqs = new Vector<Integer>();
      // Integer df = 0;
      for (String phrase : phraseQueries) {
        boolean valid = true;
        String[] phraseSplit = phrase.split(" ");
        int[] checkpoints = new int[phraseSplit.length];
        Vector<String[]> posInfos = new Vector<String[]>();
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
          // System.out.println(currWord + "@" + new String(iter.key()));
          if (infos != null) {
            posInfos.add((new String(infos)).split("@"));
            // System.out.println(new String(infos));
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
              // System.out.println("firstloc = " + firstLoc + ", currLoc =" + currLoc);
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
        innerProduct += Math.pow(freq, 1);
      }
      cosSim = innerProduct / (Math.sqrt(docLen) * Math.sqrt(queryLen));
      HashMap<String, String> ret = new HashMap<String, String>();
      ret.put("score", cosSim.toString());
      String link = "";
      try {
        byte[] linkInBytes = rocks.getEntry(Database.PageIDtoURLInfo, iter.key());
        link = new String(linkInBytes);
        byte[] ptc = rocks.getEntry(Database.ParentToChild, linkInBytes);
        if (ptc != null)
          ret.put("child", new String(ptc));
        byte[] ctp = rocks.getEntry(Database.ChildToParent, linkInBytes);
        if (ctp != null)
          ret.put("parent", new String(ctp));
      } catch (RocksDBException e) {
        System.err.println(e.toString());
      }
      String infos[] = link.split("@");
      ret.put("url", infos[0]);
      ret.put("date", infos[1]);
      ret.put("size", infos[2]);
      ret.put("title", infos[3]);
      retArr.add(ret);
    }
  }
}
