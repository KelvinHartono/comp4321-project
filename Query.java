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

public class Query {
  private Rocks rocks;
  private Porter porter;
  private StopWord stopW;

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
  }

  // Return array of website links and its score
  public Vector<HashMap<String, String>> processQuery(String query) {
    // Non term e.g. "Hong Kong"
    // String[] queryArr = query.split("\\s+");

    System.out.println("Query: " + query);

    StringBuffer curr = new StringBuffer("");
    Vector<String> queryArr = new Vector<String>();
    Vector<String> queryPArr = new Vector<String>();
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
            queryArr.add(curr.toString());
            curr = new StringBuffer("");
          } else {
            curr.append(query.charAt(i));
          }
        }
      } else {
        if (query.charAt(i) == '\"') {
          String latestWord = curr.toString().substring(prevSpace, curr.length());
          if (stopW.isStopWord(latestWord)) {
            curr.replace(prevSpace, curr.length(), "*");
          } else {
            curr.replace(prevSpace, curr.length(), porter.stripAffixes(latestWord));
          }
          prevSpace = curr.length();
          queryPArr.add(curr.toString());
          curr = new StringBuffer("");
          phrase = false;
        } else if (query.charAt(i) == ' ') {
          String latestWord = curr.toString().substring(prevSpace, curr.length());
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
    Vector<HashMap<String, String>> retval;

    try {
      retval = calculateScores(stemmedQueryArr, queryPArr);
      for (HashMap<String, String> hm : retval) {
        if (Double.parseDouble(hm.get("score")) > 0.0)
          System.out.println(hm.get("url") + " = " + hm.get("score"));
      }
      return retval;
    } catch (RocksDBException e) {
      System.err.println(e.toString());
    }

    return new Vector<HashMap<String, String>>();
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

  public Vector<HashMap<String, String>> calculateScores(HashMap<String, Integer> queries, Vector<String> phraseQueries)
      throws RocksDBException {
    RocksIterator iter = rocks.getIterator(Database.ForwardIndex); // Iterzator
    Vector<HashMap<String, String>> retArr = new Vector<HashMap<String, String>>(); // Final Array

    /*
     * Prepare phrase queries helper array phraseQueries =
     * ["Hong Kong","american eagle"] phraseDics =
     * {"Hong"=[0],"Kong"=[0],"american"=[1], "eagle"=[1]}
     **/
    HashMap<String, Vector<Integer>> phraseDics = getPhraseDics(phraseQueries);

    long N = rocks.getSize(Database.ForwardIndex);
    int queryLen = 0;
    for (int query : queries.values())
      queryLen += Math.pow(query, 2);
    for (Vector<Integer> q : phraseDics.values())
      queryLen += Math.pow(q.size(), 2);

    for (iter.seekToFirst(); iter.isValid(); iter.next()) {
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
          int df = rocks.getEntry(Database.HTMLtoPage, strFreq[0].getBytes()).toString().split("@@").length;
          if (df == 0) {
            df = 1;
          }
          double idf = Math.log(N / df) / Math.log(2);
          innerProduct += Double.parseDouble(strFreq[1]) * tf * idf;
        }
      }
      // Phrasal scoring
      Vector<Integer> phrasalFreqs = new Vector<Integer>();
      Integer df = 0;
      for (String phrase : phraseQueries) {
        boolean valid = true;
        String[] phraseSplit = phrase.split(" ");
        int[] checkpoints = new int[phraseSplit.length];
        Vector<String[]> posInfos = new Vector<String[]>();
        int freq = 0; // frequency of the term
        for (int i = 0; i < phraseSplit.length; i++) {
          String currWord = phraseSplit[i];
          byte[] key = (currWord + "@" + new String(iter.key())).getBytes();
          byte[] infos = rocks.getEntry(Database.WordToPage, key);
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
      String link = new String(rocks.getEntry(Database.PageIDtoURLInfo, iter.key())).split("@@")[0];
      ret.put("url", link);
      retArr.add(ret);
    }

    return retArr;
  }

  public static void main(String[] args) {
    Query test = new Query();
    test.processQuery("\"hong in kong\"");
  }
}
