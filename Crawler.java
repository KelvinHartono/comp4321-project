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

import resources.Porter;
import resources.StopWord;
import resources.ThreadedCrawler;
import resources.Database;
import resources.Link;
import resources.Rocks;
import resources.Pagerank;

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
  private static final int THREADS = Runtime.getRuntime().availableProcessors() * 2;
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
      dbpath[i] = "./db/" + Integer.toString(i);
    }
    try {
      this.rocks = new Rocks(dbpath);
    } catch (RocksDBException e) {
      System.err.println(e.toString());
    }
  }

  /**
   * Send an HTTP request and analyze the response.
   * 
   * @return {Response} res
   * @throws HttpStatusException for non-existing pages
   * @throws IOException
   */
  public Response getResponse(String url) throws HttpStatusException, IOException {
    if (this.urls.containsKey(url)) {
      throw new RevisitException(); // if the page has been visited, break the function
    }

    Connection conn = Jsoup.connect(url).followRedirects(true);

    Response res;
    try {
      /* establish the connection and retrieve the response */
      res = conn.execute();
      /* if the link redirects to other place... */
      if (res.hasHeader("location")) {
        String actual_url = res.header("location");
        if (this.urls.containsKey(actual_url)) {
          throw new RevisitException();
        } else {
          this.urls.put(actual_url, 1);
        }
      } else {
        this.urls.put(url, 1);
      }
    } catch (HttpStatusException e) {
      throw e;
    }
    return res;
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
    // ADD YOUR CODES HERE
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
    int counter = 0;
    Stack<String> scrapedLinks = new Stack<String>();
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
              Thread.sleep(10000);
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
    Vector<Thread> pool = new Vector<Thread>();
    for (int i = 0; i < THREADS; i++) {
      ThreadedCrawler tq = new ThreadedCrawler(i, this.todos, this.rocks, this.urls, scrapedLinks, this.paths);
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

    // Debugging outputs

    // try {
    // rocks.printHead(Database.URLtoPageID, 3);
    // System.out.println("\n\n\n\n");
    // rocks.printHead(Database.PageIDtoURLInfo, 3);
    // System.out.println("\n\n\n\n");
    // rocks.printHead(Database.ParentToChild, 3);
    // System.out.println("\n\n\n\n");
    // rocks.printHead(Database.ChildToParent, 3);
    // System.out.println("\n\n\n\n");
    // rocks.printHead(Database.ForwardIndex, 100);
    // System.out.println("\n\n\n\n");
    // rocks.printHead(Database.WordToPage, 3);
    // System.out.println("\n\n\n\n");
    // rocks.printHead(Database.HTMLtoPage, 3);
    // System.out.println("\n\n\n\n");
    // rocks.printHead(Database.InvertedIndex, 3);
    // } catch (RocksDBException e) {
    // System.err.println(e.toString());
    // }
  }

  public static void main(String[] args) {
    long currentTime = System.currentTimeMillis();
    String url = "https://cse.ust.hk/";
    Crawler crawler = new Crawler(url);

    Stack<String> scrapedLinks = new Stack<String>();
    crawler.crawlLoop();
    // System.out.println("Calculating Pagerank Scores");
    // Pagerank pr = new Pagerank();
    // try
    // {
    // pr.calculateScores();
    // }
    // catch(RocksDBException e)
    // {
    // System.err.println(e.toString());
    // }
    System.out.println("\nSuccessfully Returned");
    System.out.println("\nTime elapsed = " + (System.currentTimeMillis() - currentTime) + " ms");
  }
}