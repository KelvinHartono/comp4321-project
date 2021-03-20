import java.util.Vector;
import java.util.HashSet;
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

import resources.Porter;
import resources.StopWord;
import resources.Database;
import resources.Link;
import resources.Rocks;

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
  private HashSet<String> urls; // the set of urls that have been visited before
  public Vector<Link> todos; // the queue of URLs to be crawled
  private int max_crawl_depth = 1; // feel free to change the depth limit of the spider.
  private StopWord stopW;
  private Porter porter;
  private Rocks rocks;

  Crawler(String _url) {
    this.todos = new Vector<Link>();
    this.todos.add(new Link(_url, 1));
    this.urls = new HashSet<String>();

    this.stopW = new StopWord();
    this.porter = new Porter();
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
    if (this.urls.contains(url)) {
      throw new RevisitException(); // if the page has been visited, break the function
    }

    Connection conn = Jsoup.connect(url).followRedirects(false);
    // the default body size is 2Mb, to attain unlimited page, use the following.
    // Connection conn =
    // Jsoup.connect(this.url).maxBodySize(0).followRedirects(false);
    Response res;
    try {
      /* establish the connection and retrieve the response */
      res = conn.execute();
      /* if the link redirects to other place... */
      if (res.hasHeader("location")) {
        String actual_url = res.header("location");
        if (this.urls.contains(actual_url)) {
          throw new RevisitException();
        } else {
          this.urls.add(actual_url);
        }
      } else {
        this.urls.add(url);
      }
    } catch (HttpStatusException e) {
      throw e;
    }
    /* Get the metadata from the result */
    // String lastModified = res.header("last-modified");
    // int size = res.bodyAsBytes().length;
    // String htmlLang = res.parse().select("html").first().attr("lang");
    // String bodyLang = res.parse().select("body").first().attr("lang");
    // String lang = htmlLang + bodyLang;
    // System.out.printf("Last Modified: %s\n", lastModified);
    // System.out.printf("Size: %d Bytes\n", size);
    // System.out.printf("Language: %s\n", lang);
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

  public String stem(String str) {
    return porter.stripAffixes(str);
  }

  /**
   * Use a queue to manage crawl tasks.
   */
  public void crawlLoop() {
    int counter = 0;
    while (!this.todos.isEmpty() && counter < 30) {
      Link focus = this.todos.remove(0);
      if (focus.level > this.max_crawl_depth)
        break; // stop criteria
      if (this.urls.contains(focus.url))
        continue; // ignore pages that has been visited
      /* start to crawl on the page */
      try {
        Response res = this.getResponse(focus.url);

        byte[] pageID = Integer.toString(focus.url.hashCode()).getBytes();
        rocks.addEntry(Database.URLtoPageID, focus.url.getBytes(), pageID);
        // rocks.printHead(Database.URLtoPageID, 100);

        String lastModified = res.header("last-modified");
        int size = res.bodyAsBytes().length;
        String title = res.parse().title();
        String infos = focus.url + "@@" + lastModified + "@@" + size + "@@" + title;
        rocks.addEntry(Database.PageIDtoURLInfo, pageID, infos.getBytes());
        rocks.printHead(Database.PageIDtoURLInfo, 100);

        Document doc = res.parse();

        Vector<String> words = this.extractWords(doc);

        // System.out.println("\nWords:");
        for (String word : words) {
          if (word.length() > 0) {
            if (stopW.isStopWord(word))
              continue;
            else
              word = stem(word);
          }
          // System.out.print(word + ", ");
        }
        Vector<String> links = this.extractLinks(doc);
        ///////////////////////////////// KELVIN WORKSPACE /////////////////////////
        for (String link : links) {
          try {
            if (link.charAt(0) == '/')
              link = focus.url + link.substring(1);
            else if (link.charAt(0) == '#')
              continue;
          } catch (StringIndexOutOfBoundsException e) {

          }
          // System.out.println(link);
          this.todos.add(new Link(link, focus.level + 1)); // add links
        }
        ///////////////////////////////// KELVIN WORKSPACE /////////////////////////
        ///////////////////////////////// MAXI WORKSPACE /////////////////////////
        for (String word : words) {
          if (word.length() > 0) {
            if (stopW.isStopWord(word))
              continue;
            else
              word = stem(word);
              byte[] temp = rocks.getEntry(Database.ForwardIndex,word.getBytes());
              System.out.println()
          }
        String WordFreq = "";
        // rocks.addEntry(Database.ForwardIndex, pageID, WordFreq);
        ///////////////////////////////// MAXI WORKSPACE /////////////////////////
        counter++;
      } catch (HttpStatusException e) {
        // e.printStackTrace ();
        System.out.printf("\nLink Error: %s\n", focus.url);
      } catch (IOException e) {
        e.printStackTrace();
      } catch (RevisitException e) {
      } catch (RocksDBException e) {
        System.err.println(e.toString());
      }
    }

  }

  public static void main(String[] args) {
    String url = "https://www.cse.ust.hk/";
    Crawler crawler = new Crawler(url);

    crawler.crawlLoop();

    System.out.println("\nSuccessfully Returned");
  }
}