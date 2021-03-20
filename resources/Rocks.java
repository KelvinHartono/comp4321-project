package resources;

import org.rocksdb.RocksDB;
import org.rocksdb.Options;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import resources.Database;
import java.util.Vector;

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

  public void addEntry(Database choice, byte[] key, byte[] values) throws RocksDBException {
    db[choice.ordinal()].put(key, values);
  }

  public void delEntry(Database choice, byte[] key) throws RocksDBException {
    db[choice.ordinal()].delete(key);
  }

  public void print(Database choice, byte[] key) throws RocksDBException {
    System.out.println(key + "=" + new String(db[choice.ordinal()].get(key)));
  }

  public void printHead(Database choice, int size) throws RocksDBException {
    RocksIterator iter = db[choice.ordinal()].newIterator();
    int count = 0;
    for (iter.seekToFirst(); iter.isValid() && count < size; iter.next()) {
      System.out.println(Integer.toString(count) + ". " + new String(iter.key()) + " = " + new String(iter.value()));
      ++count;
    }
  }

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

  public byte[] getEntry(Database choice, byte[] key) throws RocksDBException {
    return db[choice.ordinal()].get(key);
  }
}