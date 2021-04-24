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
    private Porter porter;
    private StopWord stopW;

    Pagerank() //CLASS CONSTRUCTOR
        {
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
    
    public HashMap<String, Integer> calculateScores()
        {
            HashMap<String, Integer> PRScores = new HashMap<String,Integer>();
            long currentTime = System.currentTimeMillis();

            System.out.println("\nTime elapsed = " + (System.currentTimeMillis() - currentTime) + " ms");
            return PRScores;
        }
    
}
