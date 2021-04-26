package resources;

import java.util.Map;
import java.util.HashMap;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import java.nio.ByteBuffer;

public class Pagerank {
  private Rocks rocks;

  public Pagerank() // CLASS CONSTRUCTOR
  {
    String[] dbpath = new String[9];

    for (int i = 0; i < 9; ++i) {
      dbpath[i] = "./db/" + Integer.toString(i);
    }
    try {
      this.rocks = new Rocks(dbpath);
    } catch (RocksDBException e) {
      System.err.println(e.toString());
    }
  }

  public static byte[] toByteArray(double value) {
    byte[] bytes = new byte[8];
    ByteBuffer.wrap(bytes).putDouble(value);
    return bytes;
  }

  public void calculateScores() throws RocksDBException {
    // Hashmap<PageID, PRscores> PRScores, initialized to 1
    // Hashmap<PageID, ChildCount> ChildCount
    HashMap<String, Double> PRScores = new HashMap<String, Double>();
    HashMap<String, Integer> ChildCount = new HashMap<String, Integer>();
    long currentTime = System.currentTimeMillis();
    final double D = 0.8;
    final double EPSILON = 0.00000001d;
    final int MAX_ITERS = 100;
    RocksIterator iter = rocks.getIterator(Database.URLtoPageID);
    // Initialize PR Scores
    for (iter.seekToFirst(); iter.isValid(); iter.next()) {
      // Initialize scores to 1
      PRScores.put(new String(iter.value()), 1d);
      // Initialize scores to
      byte[] testEntry = rocks.getEntry(Database.ParentToChild, iter.value());
      int childCount = 0;
      if (testEntry != null) {
        String childLinks = new String(testEntry);
        childCount = childLinks.split("@@").length;
      }

      ChildCount.put(new String(iter.value()), childCount);
    }
    // Iterate page scores
    int count = 0;
    int currDelta = 9999;
    while (count < MAX_ITERS) {
      // DOES 1000 iterations no matter converged/not -> probably should fix
      System.out.println("Iteration " + Integer.toString(count));
      double sumDelta = 0;
      double sumScores = 0;
      // Do asynchronous calculation
      for (Map.Entry<String, Double> prScore : PRScores.entrySet()) {
        String key = prScore.getKey();
        double prevPr = prScore.getValue();
        double currPr = (1 - D);

        String[] parents = new String(rocks.getEntry(Database.ChildToParent, key.getBytes())).split("@@");
        for (String p : parents) {
          currPr += D * (PRScores.get(p) / ChildCount.get(p));
        }
        PRScores.replace(key, currPr);
        sumDelta += currPr - prevPr;
        sumScores += currPr;
      }
      if (Math.abs(currDelta - sumDelta) <= EPSILON) {
        System.out.println("The delta is " + Double.toString(currDelta));
        break;
      }
      ++count;
      for (Map.Entry<String, Double> prScore : PRScores.entrySet()) {
        String key = prScore.getKey();
        double currPr = prScore.getValue();
        PRScores.replace(key, currPr / sumScores);
      }
      // Normalize the scores
    }
    System.out.println("Iterations until convergence " + Integer.toString(count));
    System.out.println("\nTime elapsed = " + (System.currentTimeMillis() - currentTime) + " ms");
    // Input into database
    for (Map.Entry<String, Double> t : PRScores.entrySet()) {
      rocks.addEntry(Database.PageRank, t.getKey().getBytes(), toByteArray(t.getValue()));
    }

  }

  public static void main(String[] args) {
    Pagerank test = new Pagerank();
    try {
      test.calculateScores();
    } catch (RocksDBException e) {
      System.err.println(e.toString());
    }
  }

}