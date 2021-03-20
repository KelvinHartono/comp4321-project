
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

  public String getChildString(String pageID) {
    String str = "";
    try {
      str = new String(this.rocks.getEntry(Database.ParentToChild, pageID.getBytes()));
    } catch (RocksDBException e) {
      System.err.println(e.toString());
    }
    return str;
  }

  // public String getParentString(String input) {

  // String seg[] = input.split("@@");
  // System.out.println(Arrays.toString(seg));

  // return "";
  // }

  public String getForwardIndex(String input) {
    String str = "";
    try {
      byte[] content = rocks.getEntry(Database.ForwardIndex, input.getBytes());
      str = new String(content);
    } catch (RocksDBException e) {
      System.err.println(e.toString());
    }
    // HashMap<String, Integer> test = (HashMap<String, Integer>)
    // Arrays.asList(str.split(",")).stream().map(s ->
    // s.split("=")).collect(Collectors.toMap(e -> e[0], e ->
    // Integer.parseInt(e[1])));
    // Vector<String> buf = new Vector<String>();
    // StringTokenizer st = new StringTokenizer(content);
    // while (st.hasMoreTokens()) {
    // buf.add(st.nextToken());
    // }
    // while(!buf.isEmpty()) {
    // res += buf.firstElement();

    // }
    return str;
  }

  public String getMetaData(String input) {

    String seg[] = input.split("@@");
    // System.out.println(Arrays.toString(seg));
    String pageTitle = seg[seg.length - 1];
    String URL = seg[0];
    String lastModificationDate = seg[1];
    String pageSize = seg[2];

    String ret = "Page Title: " + pageTitle + "\nURL: " + URL + "\nLast Modification Date: " + lastModificationDate
        + ", Page Size: " + pageSize + " Bytes\n";

    return ret;
  }

  public Boolean isIndexed(String input) {
    try {
      byte[] content = rocks.getEntry(Database.ForwardIndex, input.getBytes());
      if (content == null) {
        return false;
      }
    } catch (RocksDBException e) {
      System.err.println(e.toString());
    }

    return true;
  }

  public static void main(String[] args) {
    Output output = new Output();
    try {
      Vector<String> res = new Vector<String>();
      int num = 10;
      res = output.rocks.allKeys(Database.PageIDtoURLInfo, -1);
      int count = 0;
      // System.out.println(res.size());
      for (int i = 0; i < res.size(); i += 2) {
        if (!output.isIndexed(res.elementAt(i))) {
          continue;
        } else if (count >= num) {
          break;
        }
        String metaData = output.getMetaData(res.elementAt(i + 1));
        String forwardIndex = output.getForwardIndex(res.elementAt(i));
        forwardIndex = forwardIndex.substring(1, forwardIndex.length() - 1);
        String child[] = output.getChildString(res.elementAt(i)).split("@@");
        System.out.println(metaData);
        System.out.println("Keywords:\n");
        System.out.println(forwardIndex);
        System.out.println("\nChildren Links (including the non-indexed ones) :");
        for (int j = 0; j < child.length; j++) {
          System.out.print("\t");
          byte[] li = output.rocks.getEntry(Database.PageIDtoURLInfo, child[j].getBytes());
          if (li == null)
            continue;
          String link = new String(li).split("@@")[0];
          System.out.println(link);
        }
        // count++;
      }

    } catch (RocksDBException e) {
      System.err.println(e.toString());
    }
  }
}