
import java.util.*;
import java.io.*;
import org.rocksdb.RocksDB;
import org.rocksdb.Options;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import org.jsoup.HttpStatusException;
import java.lang.RuntimeException;

import resources.Porter;
import resources.StopWord;
import resources.Database;
import resources.Link;
import resources.Rocks;

public class Output {
  private Rocks rocks;

  Output() {
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

  // If the file doesn't exists, create and write to it
  // If the file exists, truncate (remove all content) and write to it
  private String getTitle(int pageID) {
    try {
      System.out
          .println(new String(this.rocks.getEntry(Database.PageIDtoURLInfo, Integer.toString(pageID).getBytes())));
    } catch (RocksDBException e) {
      System.err.println(e.toString());
    }
    return "";
  }

  // private String getURL() {
  // // RocksIterator it = db[1].newIterator();

  // // for(it.seekToFirst(); it.isValid(); it.next()) {
  // // System.out.println(new String(it.key()) + "=" + new String(it.value()));
  // // }
  // }

  // private String getDate() {

  // }

  // private String getSize() {

  // }

  // private String getKeywordWithFreq(int pageID) {
  // // String res = new String(getEntry(4, pageID.getBytes()));
  // // return res;
  // }

  // private String getChildURL() {
  // // String res = new String(getEntry(2, pageID.getBytes()));

  // // Vector<String> result = new Vector<String>();
  // // StringTokenizer st = new StringTokenizer(res);
  // // while(st.hasMoreTokens()) {
  // // result.add(st.nextToken());
  // // }
  // }

  // private String getData(int pageID) {
  // // String title = getTitle(pageID);

  // }

  // // try {

  // // // FileWriter writer = new FileWriter("spider_result.txt");
  // // // BufferedWriter bw = new BufferedWriter(writer);
  // // // bw.write(content);

  // // } catch (IOException e) {
  // // e.printStackTrace();
  // // }

  public static void main(String[] args) {
    Output output = new Output();
    try {
      byte[][] res = toutput.rocks.allKeys(Database.PageIDtoURLInfo, 5);
      for() {
        
      }
    } catch (RocksDBException e) {
      System.err.println(e.toString());
    }
  }
}
