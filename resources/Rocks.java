package resources;

import org.rocksdb.RocksDB;
import org.rocksdb.Options;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import resources.Database;
import java.util.Vector;
import java.util.HashMap;

//RocksDB class, stores 8 different databases corresponding to each type in Database.Java
public class Rocks {
  private RocksDB[] db;
  private Options options;

  public Rocks(String[] dbpath) throws RocksDBException {
    this.db = new RocksDB[8];
    this.options = new Options();
    this.options.setCreateIfMissing(true);
    for (int i = 0; i < 8; ++i) {
      this.db[i] = RocksDB.open(this.options, dbpath[i]);
    }
  }

  // Adds an entry to the chosen database, with byte[] key and byte[] values as
  public void addEntry(Database choice, byte[] key, byte[] values) throws RocksDBException {
    db[choice.ordinal()].put(key, values);
  }

  // Deletes an entry to the chosen database with the same byte[] key
  public void delEntry(Database choice, byte[] key) throws RocksDBException {
    db[choice.ordinal()].delete(key);
  }

  // Prints the Database entry of the chosen database with the same byte[] key
  public void print(Database choice, byte[] key) throws RocksDBException {
    System.out.println(key + "=" + new String(db[choice.ordinal()].get(key)));
  }

  // Prints the first n entries in the chosen database, where n is the size
  // argument
  public void printHead(Database choice, int size) throws RocksDBException {
    RocksIterator iter = db[choice.ordinal()].newIterator();
    int count = 0;
    for (iter.seekToFirst(); iter.isValid() && count < size; iter.next()) {
      System.out.println(Integer.toString(count) + ". " + new String(iter.key()) + " = " + new String(iter.value()));
      ++count;
    }
  }

  // Returns the first n keys of the chosen database, where n is the size
  public Vector<String> allKeys(Database choice, double size) throws RocksDBException {
    if (size == -1.)
      size = Double.POSITIVE_INFINITY;
    RocksIterator iter = db[choice.ordinal()].newIterator();
    int count = 0;
    Vector<String> retArr = new Vector<String>();
    // String[] retArr = new String[size * 2];
    for (iter.seekToFirst(); iter.isValid() && count < size; iter.next()) {
      retArr.add(new String(iter.key()));
      retArr.add(new String(iter.value()));
      count++;
    }
    return retArr;
  }

  public RocksIterator getIterator(Database choice){
    return db[choice.ordinal()].newIterator();
  }
  public long getSize(Database choice){
    return db[choice.ordinal()].getLongProperty("rocksdb.estimate-num-keys");

  }

  public Vector<HashMap<String, String>> calculateScores(HashMap<String, Integer> queries, Vector<String> phraseQueries)
      throws RocksDBException {
    RocksIterator iter = db[Database.ForwardIndex.ordinal()].newIterator(); // Iterzator
    Vector<HashMap<String, String>> retArr = new Vector<HashMap<String, String>>(); // Final Array

    /*
     * Prepare phrase queries helper arrays I DONT THINK WE NEED THIS phraseQuery =
     * ["Hong Kong","american eagle"] phraseDicArray = [{"Hong"=1,
     * "Kong"=1},{"american"=1, "eagle"=1}] phraseDics = {"Hong"=[0],
     * "Kong"=[0],"american"=[1], "eagle"=[1]}
     **/
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

    long N = db[Database.ForwardIndex.ordinal()].getLongProperty("rocksdb.estimate-num-keys");
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
          int df = db[Database.HTMLtoPage.ordinal()].get(strFreq[0].getBytes()).toString().split("@@").length;
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
          byte[] infos = db[Database.WordToPage.ordinal()].get(key);
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
        // if (freq > 0)
        // df += 1;
        // phrasalFreqs.add(freq);
      }
      // if (df > 0) {
      // // Problems here, idf is wrong!
      // double idf = Math.log(N / df) / Math.log(2);
      // System.out.println(df + " " + idf);
      // for (Integer freq : phrasalFreqs) {
      // innerProduct += freq * idf;
      // }
      // }
      cosSim = innerProduct / (Math.sqrt(docLen) * Math.sqrt(queryLen));
      HashMap<String, String> ret = new HashMap<String, String>();
      ret.put("score", cosSim.toString());

      String link = new String(db[Database.PageIDtoURLInfo.ordinal()].get(iter.key())).split("@@")[0];
      ret.put("url", link);
      retArr.add(ret);
    }

    return retArr;
  }

  // private int getFreqPhrase(String[] phraseSplit, int[] checkpoints,
  // Vector<String[]> posInfos, int curr) {
  // if (curr >= phraseSplit.length) {
  // return 1;
  // } else {

  // }
  // }

  // Returns the entry from the chosen database with the given key
  public byte[] getEntry(Database choice, byte[] key) throws RocksDBException {
    return db[choice.ordinal()].get(key);
  }
}