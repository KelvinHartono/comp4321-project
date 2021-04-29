package resources;

import java.util.Vector;
import java.util.HashMap;
import java.util.Stack;
import java.util.StringTokenizer;
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
import java.io.PrintWriter;
import java.io.FileNotFoundException;

import java.net.URL;
import java.net.MalformedURLException;

@SuppressWarnings("serial")
/**
 * This is customized exception for those pages that have been visited before.
 */
class RevisitException extends RuntimeException {
  public RevisitException() {
    super();
  }
}

public class Crawler {
  private static final int THREADS = Runtime.getRuntime().availableProcessors() * 4;
  private HashMap<String, Integer> urls; // the set of urls that have been visited before
  private HashMap<String, Integer> paths; // the set of urls that have been visited before
  public Vector<Link> todos; // the queue of URLs to be crawled
  private Rocks rocks;

  Crawler(String _url) {
    this.todos = new Vector<Link>();
    this.todos.add(new Link(_url, 1));
    this.urls = new HashMap<String, Integer>();
    this.paths = new HashMap<String, Integer>();

    String[] dbpath = new String[9];
    for (int i = 0; i < 9; ++i) {
      dbpath[i] = "./resources/db/" + Integer.toString(i);
    }
    try {
      this.rocks = new Rocks(dbpath);
    } catch (RocksDBException e) {
      System.err.println(e.toString());
    }
  }

  /**
   * Extract words in the web page content. note: use StringTokenizer to tokenize
   * the result
   * 
   * @param {Document} doc
   * @return {Vector<String>} a list of words in the web page body
   */
  public Vector<String> extractWords(Document doc) {
    Vector<String> result = new Vector<String>();
    // ADD YOUR CODES HERE
    String contents = doc.body().text();
    StringTokenizer st = new StringTokenizer(contents);
    while (st.hasMoreTokens()) {
      result.add(st.nextToken());
    }
    return result;
  }

  /**
   * Extract useful external urls on the web page. note: filter out images,
   * emails, etc.
   * 
   * @param {Document} doc
   * @return {Vector<String>} a list of external links on the web page
   */
  public Vector<String> extractLinks(Document doc) {
    Vector<String> result = new Vector<String>();
    Elements links = doc.select("a[href]");
    for (Element link : links) {
      String linkString = link.attr("href");
      // filter out emails
      if (linkString.contains("mailto:")) {
        continue;
      }
      result.add(link.attr("href"));
    }
    return result;
  }

  /**
   * Use a queue to manage crawl tasks.
   */
  public void crawlLoop() {
    Stack<String> scrapedLinks = new Stack<String>();
    HashMap<String, String> redirect = new HashMap<String, String>();

    // Create a progress updater so we know our progress
    Thread progressBar = new Thread(new Runnable() {
      @Override
      public void run() {
        // code goes here.
        int counter = 0;
        int ctr = 0;
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          System.err.println(e.toString());
        }
        while (counter < 20) {
          if (scrapedLinks.empty()) {
            counter++;
            try {
              Thread.sleep(3000);
            } catch (InterruptedException e) {
              System.err.println(e.toString());
            }
          } else {
            counter = 0;
            ctr++;
            System.out.println(ctr + ". Scraped " + scrapedLinks.pop());
          }
        }
      }
    });
    progressBar.start();

    System.out.println("---Progress Spider---");

    // Use threading to handle the crawling
    Vector<Thread> pool = new Vector<Thread>();
    for (int i = 0; i < THREADS; i++) {
      ThreadedCrawler tq = new ThreadedCrawler(i, this.todos, this.rocks, this.urls, scrapedLinks, this.paths,
          redirect);
      pool.add(new Thread(tq));
      pool.get(i).start();
    }
    for (int i = 0; i < THREADS; i++) {
      try {
        pool.get(i).join();
      } catch (InterruptedException e) {
        System.err.println(e.toString());
      }
    }
    try {
      progressBar.join();
    } catch (Exception e) {
      System.err.println(e.toString());
    }
  }

  public static void main(String[] args) {
    long currentTime = System.currentTimeMillis();
    String url = "https://cse.ust.hk/";
    Crawler crawler = new Crawler(url);

    // Do crawling and handle their pagerank immediately
    crawler.crawlLoop();
    Pagerank pr = new Pagerank();
    try {
      pr.calculateScores();
    } catch (RocksDBException e) {
      System.err.println(e.toString());
    }
    System.out.println("\nSuccessfully Returned");
    System.out.println("\nTime elapsed = " + (System.currentTimeMillis() - currentTime) + " ms");
  }
}