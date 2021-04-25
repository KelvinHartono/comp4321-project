import java.util.Vector;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.lang.StringBuffer;

import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import resources.Porter;
import resources.Rocks;
import resources.StopWord;
import resources.Database;

public class Pagerank {
    private Rocks rocks;

    Pagerank() //CLASS CONSTRUCTOR
        {
            String[] dbpath = new String[9];
        
            for (int i = 0; i < 8; ++i) {
              dbpath[i] = "./db/" + Integer.toString(i);
            }
            try {
              this.rocks = new Rocks(dbpath);
            } catch (RocksDBException e) {
              System.err.println(e.toString());
            }
        }
    
    public HashMap<String, Double> calculateScores() throws RocksDBException
        {
            // Hashmap<PageID, PRscores> PRScores, initialized to 1
            // Hashmap<PageID, ChildCount> ChildCount
            HashMap<String, Double> PRScores = new HashMap<String,Double>();
            HashMap<String, Integer> ChildCount = new HashMap<String, Integer>();
            long currentTime = System.currentTimeMillis();
            final double D = 0.8;
            final double EPSILON = 0.00000001d;
            final int MAX_ITERS = 100;
            RocksIterator iter = rocks.getIterator(Database.URLtoPageID);
            //Initialize PR Scores
            for(iter.seekToFirst();iter.isValid();iter.next())
              {
                //Initialize scores to 1
                PRScores.put(new String(iter.value()),1d);
                //Initialize scores to
                byte[] testEntry = rocks.getEntry(Database.ParentToChild, iter.value());
                int childCount = 0;
                if(testEntry != null){
                  String childLinks = new String(testEntry);
                  childCount = childLinks.split("@@").length;
                }
                 
                ChildCount.put(new String(iter.value()),childCount);
              }
            //Iterate page scores
            int count = 0;
            int currDelta = 9999;
            while(count<MAX_ITERS)
              {
                //DOES 1000 iterations no matter converged/not -> probably should fix
                System.out.println("Iteration "+Integer.toString(count));
                double sumDelta = 0;
                //Do asynchronous calculation
                for(Map.Entry<String,Double> prScore : PRScores.entrySet())
                  {
                    String key = prScore.getKey();
                    double prevPr = prScore.getValue();
                    double currPr = (1-D);
                    
                    String[] parents = new String(rocks.getEntry(Database.ChildToParent, key.getBytes())).split("@@");
                    for(String p : parents)
                      {
                        currPr += D*(PRScores.get(p)/ChildCount.get(p));
                      }
                    sumDelta+=currPr-prevPr;
                  }
                  if(Math.abs(currDelta-sumDelta)<=EPSILON)
                    {
                      System.out.println("The delta is "+Double.toString(currDelta));
                      break;
                    }
                  ++count;
                  
              }
            System.out.println("Iterations until convergence "+Integer.toString(count));
            System.out.println("\nTime elapsed = " + (System.currentTimeMillis() - currentTime) + " ms");
            return PRScores;
        }

    public static void main(String[] args) {
      Pagerank test = new Pagerank();
      try
        {
          HashMap<String,Double> test1 = test.calculateScores();
          int count=0;
          for(Map.Entry<String,Double> t : test1.entrySet())
          {
            System.out.println(t.getKey()+" "+Double.toString(t.getValue()));
            if(count==100)break;
            else ++count;
          }
        }
      catch(RocksDBException e)
        {
          System.err.println(e.toString());
        }
    }
    
}
