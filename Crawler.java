import java.util.Vector;
import java.util.HashSet;
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

import java.net.URL;
import java.net.MalformedURLException;

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
  private int max_crawl_depth = 10; // feel free to change the depth limit of the spider.
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

    Connection conn = Jsoup.connect(url).followRedirects(true);

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
    Stack<String> scrapedLinks = new Stack<String>();
    Thread progressBar = new Thread(new Runnable() {
      @Override
      public void run() {
        // code goes here.
        int counter = 0;
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          System.err.println(e.toString());
        }
        while (counter < 6) {
          if (scrapedLinks.empty()) {
            counter++;
            try {
              Thread.sleep(10000);
            } catch (InterruptedException e) {
              System.err.println(e.toString());
            }
          } else {
            counter=0;
            System.out.println("Scraped " + scrapedLinks.pop());
          }
        }
      }
    });
    progressBar.start();
    System.out.println("---Progress Spider---");
    while (!this.todos.isEmpty()) {
      Link focus = this.todos.remove(0);
      if (focus.level > this.max_crawl_depth)
        break; // stop criteria
      if (this.urls.contains(focus.url))
        continue; // ignore pages that has been visited
      if (!focus.url.contains("cse.ust.hk"))
        continue;
      /* start to crawl on the page */
      try {
        Response res = this.getResponse(focus.url);

        // Getting metadatas
        int size = res.bodyAsBytes().length;
        String lastModified = res.header("last-modified");

        Document doc = res.parse();
        // Handle only english pages
        String htmlLang = doc.select("html").first().attr("lang");
        if (htmlLang != "" && !htmlLang.substring(0, 2).equals("en"))
          continue;

        // FOR
        // URLtoPageID-----------------------------------------------------------------
        byte[] pageID = Integer.toString(focus.url.hashCode()).getBytes();

        rocks.addEntry(Database.URLtoPageID, focus.url.getBytes(), pageID);
        // rocks.printHead(Database.URLtoPageID, 100);
        // ------------------------------------------------------------------------------

        // FOR
        // PageIDtoURLInfo-------------------------------------------------------------------
        String title = doc.title();
        String infos = focus.url + "@@" + lastModified + "@@" + size + "@@" + title;
        rocks.addEntry(Database.PageIDtoURLInfo, pageID, infos.getBytes());
        // rocks.printHead(Database.PageIDtoURLInfo, 100);
        Vector<String> words;
        try {
          words = this.extractWords(doc);
        } catch (Exception e) {
          words = new Vector<String>();
          System.out.println(e.toString());
        }

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
        // ------------------------------------------------------------------------------

        // Add child links and parent links to the
        // database-----------------------------------------------
        String childLinks = "";
        Vector<String> currLinks = new Vector<String>();
        for (String link : links) {
          try {
            if (link.length() == 0 || link.charAt(0) == '#')
              continue;
            else if (link.charAt(0) == '/') {
              try {
                URL url = new URL(focus.url);
                String baseUrl = url.getProtocol() + "://" + url.getHost();
                link = baseUrl + link;
              } catch (MalformedURLException e) {
                System.out.println(e.toString());
                continue;
              }
            } else if (link.charAt(0) == '?' || (link.length() > 4 && !link.substring(0, 4).equals("http"))) {
              link = focus.url + link;
            } else if (link.length() > 4 && !link.substring(0, 4).equals("http")) {
              String temp = focus.url;
              if (temp.contains("?")) {
                int remove = temp.indexOf("?");
                temp = temp.substring(0, remove);
              }
              link = temp + link;
            } else if (link.charAt(0) == '.' && link.charAt(1) == '/')
              link = focus.url + link.substring(2);
            link = link.replaceAll("(?<!(http:|https:))//", "/");
          } catch (StringIndexOutOfBoundsException e) {
            System.err.println(e.toString());
          }
          if (currLinks.contains(link))
            continue;
          else if (!link.contains("cse.ust.hk"))
            continue;
          byte[] pageIDlink = Integer.toString(link.hashCode()).getBytes();
          rocks.addEntry(Database.URLtoPageID, link.getBytes(), pageIDlink);
          String dummyinfos = link + "@@null@@null@@null";
          if (rocks.getEntry(Database.PageIDtoURLInfo, pageIDlink) == null) {
            rocks.addEntry(Database.PageIDtoURLInfo, pageIDlink, dummyinfos.getBytes());
          }

          this.todos.add(new Link(link, focus.level + 1)); // add link

          if (this.rocks.getEntry(Database.ParentToChild, pageID) == null) {
            childLinks = childLinks + "@@" + new String(pageIDlink);
            currLinks.add(link);

            byte[] tempParentLinks = this.rocks.getEntry(Database.ChildToParent, pageIDlink);
            if (tempParentLinks != null) {
              this.rocks.addEntry(Database.ChildToParent, pageIDlink,
                  (new String(tempParentLinks) + "@@" + new String(pageID)).getBytes());
            } else {
              this.rocks.addEntry(Database.ChildToParent, pageIDlink, pageID);
            }
          }
        }
        if (this.rocks.getEntry(Database.ParentToChild, pageID) == null) {
          if (childLinks != "") {
            this.rocks.addEntry(Database.ParentToChild, pageID, childLinks.substring(2).getBytes());
          } else {
            this.rocks.addEntry(Database.ParentToChild, pageID, childLinks.getBytes());
          }
        }
        // ------------------------------------------------------------------------------

        // Making Forward Index---------------------------------------------------------
        HashMap<String, Integer> wordFreqs = new HashMap<String, Integer>();
        HashMap<String, String> WordPage = new HashMap<String, String>();
        Integer ctr = 0;
        int docLength = 0;
        for (String word : words) {
          word = word.replaceAll("[^\\x00-\\x7F]", "");
          if (word.length() > 0) {
            if (stopW.isStopWord(word))
              continue;
            else
              word = stem(word);
            if (word.equals(""))
              continue;
            ++ctr;
            if (wordFreqs.containsKey(word))
              wordFreqs.put(word, wordFreqs.get(word) + 1);
            else
              wordFreqs.put(word, 1);
            if (WordPage.containsKey(word)) {
              String temp = WordPage.get(word).substring(WordPage.get(word).indexOf('@') + 1,
                  WordPage.get(word).length());
              WordPage.put(word, wordFreqs.get(word) + "@" + temp + "@" + Integer.toString(ctr));
            } else
              WordPage.put(word, wordFreqs.get(word) + "@" + Integer.toString(ctr));
          }
        }
        wordFreqs.put("document-length", ctr);
        rocks.addEntry(Database.ForwardIndex, pageID, wordFreqs.toString().getBytes());
        // ------------------------------------------------------------------------------

        // FOR WordToPage---------------------------------------------------------------
        for (HashMap.Entry<String, String> entry : WordPage.entrySet()) {
          String key = entry.getKey() + "@" + new String(pageID);
          String val = entry.getValue();
          rocks.addEntry(Database.WordToPage, key.getBytes(), val.getBytes());
        }
        // ------------------------------------------------------------------------------

        // FOR
        // HTMLtoPage----------------------------------------------------------------
        for (HashMap.Entry<String, Integer> entry : wordFreqs.entrySet()) {
          byte[] temp = rocks.getEntry(Database.HTMLtoPage, entry.getKey().getBytes());
          if (temp == null)
            rocks.addEntry(Database.HTMLtoPage, entry.getKey().getBytes(), pageID);
          else {
            String newVal = new String(temp) + "@@" + new String(pageID);
            rocks.addEntry(Database.HTMLtoPage, entry.getKey().getBytes(), newVal.getBytes());
          }
        }
        // --------------------------------------------------------------------------------

        // For InvertedIndex DB---------------------------------------------------------
        Vector<String> titleWords = new Vector<String>();
        StringTokenizer stTitle = new StringTokenizer(title);
        while (stTitle.hasMoreTokens()) {
          titleWords.add(stTitle.nextToken());
        }
        for (String titleWord : titleWords) {
          if (titleWord.length() > 0) {
            if (stopW.isStopWord(titleWord))
              continue;
            else
              titleWord = stem(titleWord);
            if (titleWord.equals(""))
              continue;
            else {
              byte[] temp = rocks.getEntry(Database.InvertedIndex, titleWord.getBytes());
              if (temp == null) {
                rocks.addEntry(Database.InvertedIndex, titleWord.getBytes(), pageID);
              } else {
                String newVal = new String(temp) + "@@" + new String(pageID);
                rocks.addEntry(Database.InvertedIndex, titleWord.getBytes(), newVal.getBytes());
              }
            }
          }
        }
        // ------------------------------------------------------------------------------
        counter++;
      } catch (HttpStatusException e) {
        // e.printStackTrace ();
        System.out.println("Link Error: " + focus.url);
      } catch (IOException e) {
        e.printStackTrace();
      } catch (RevisitException e) {
      } catch (RocksDBException e) {
        System.err.println(e.toString());
      }
      scrapedLinks.push(focus.url);
      // System.out.println("Scraped " + focus.url);
    }
    try {
      progressBar.join();
    } catch (InterruptedException e) {
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

    // Stack<String> scrapedLinks = new Stack<String>();
    crawler.crawlLoop();
    System.out.println("\nSuccessfully Returned");
    System.out.println("\nTime elapsed = " + (System.currentTimeMillis() - currentTime) + " ms");
  }
}