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
    this.db = new RocksDB[9];
    this.options = new Options();
    this.options.setCreateIfMissing(true);
    for (int i = 0; i < 9; ++i) {
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

  public RocksIterator getIterator(Database choice) {
    return db[choice.ordinal()].newIterator();
  }

  public long getSize(Database choice) {
    long x = 0;
    try {
      return db[choice.ordinal()].getLongProperty("rocksdb.estimate-num-keys");
    } catch (RocksDBException e) {
      System.err.println(e.toString());
    }
    return x;
  }

  // Returns the entry from the chosen database with the given key
  public byte[] getEntry(Database choice, byte[] key) throws RocksDBException {
    return db[choice.ordinal()].get(key);
  }

  //DB closer
  public void closeDatabases() throws RocksDBException
    {
      for(int i=0;i<9;++i)
        this.db[i].close();
    }
  
  protected void finalize()
    {
      try
      {
        closeDatabases();
      }
      catch(RocksDBException e)
        {
          System.err.println(e.toString());
        }
    }
}