package resources;

import java.util.Vector;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Stack;

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

import jdk.jfr.ContentType;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import org.jsoup.UnsupportedMimeTypeException;

class RevisitException extends RuntimeException {
  public RevisitException() {
    super();
  }
}

public class ThreadedCrawler implements Runnable {
  public Vector<Link> todos;
  private Rocks rocks;
  private HashMap<String, Integer> urls;
  private HashMap<String, Integer> paths;
  private StopWord stopW;
  private Porter porter;
  private Stack<String> scrapedLinks;
  private int max_crawl_depth = 2;
  private int id;

  public ThreadedCrawler(int id, Vector<Link> todos, Rocks rocks, HashMap<String, Integer> urls,
      Stack<String> scrapedLinks, HashMap<String, Integer> paths) {
    this.todos = todos;
    this.rocks = rocks;
    this.urls = urls;
    this.scrapedLinks = scrapedLinks;
    this.stopW = new StopWord();
    this.porter = new Porter();
    this.paths = paths;
    this.id = id;
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
      // if (res.hasHeader("location")) {
      // String actual_url = res.header("location");
      // } else {
      // this.urls.put(url, 1);
      // }
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
    if (doc.body() == null) {
      return result;
    }
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

  public void run() {
    int counter = 0;
    while (counter < 10) {
      if (this.todos.isEmpty()) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          System.err.println(e.toString());
        }
        counter++;
      } else if (!this.todos.isEmpty()) {
        counter = 0;
        Link focus = this.todos.remove(0);
        Response res;
        try {
          res = this.getResponse(focus.url);
          if (res.hasHeader("location"))
            focus.url = res.header("location");
        } catch (Exception e) {
          // System.out.println(e.toString());
          continue;
        }

        try {
          URL url = new URL(focus.url);
          String str = url.getProtocol() + "://" + url.getHost() + url.getPath();
          if (paths.containsKey(str)) {
            if (paths.get(str) > 30) {
              continue;
            }
            paths.replace(str, paths.get(str) + 1);
          } else
            paths.put(str, 1);
        } catch (MalformedURLException e) {
          // e.printStackTrace();
          continue;
        }

        if (this.urls.containsKey(focus.url))
          continue; // ignore pages that has been visited
        else
          this.urls.put(focus.url, 1);
        if (!focus.url.contains("cse.ust.hk"))
          continue;

        /* start to crawl on the page */
        try {
          // Getting metadatas
          String size = Integer.toString(res.bodyAsBytes().length);
          String lastModified = res.header("last-modified");
          String ct = res.contentType().split(";")[0].trim();
          // System.out.println(ct);
          if (!ct.equals("text/html") && !ct.equals("text/plain") && !ct.equals("text/htm")) {
            continue;
          }
          Document doc = res.parse();
          // if(doc.)
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

          if (lastModified == null || lastModified.equals(""))
            lastModified = "null";
          if (title == null || title.equals(""))
            title = "null";
          if (size == null || size.equals(""))
            size = "null";
          String infos = focus.url + "@@" + lastModified + "@@" + size + "@@" + title;
          rocks.addEntry(Database.PageIDtoURLInfo, pageID, infos.getBytes());
          // rocks.printHead(Database.PageIDtoURLInfo, 100);
          Vector<String> words;
          try {
            words = this.extractWords(doc);
          } catch (Exception e) {
            words = new Vector<String>();
            // System.out.println(focus.url);
            // e.printStackTrace();
          }

          Vector<String> links = this.extractLinks(doc);
          // ------------------------------------------------------------------------------

          // Add child links and parent links to the
          // database-----------------------------------------------
          String childLinks = "";
          Vector<String> currLinks = new Vector<String>();
          for (String link : links) {
            if (link.length() > 0 && link.contains("#")) {
              // System.out.println(link + " - " + link.indexOf('#'));
              link = link.substring(0, link.indexOf('#'));
            }
            if (link.length() > 4 && (link.substring(0, 4).equals("http") || link.substring(0, 3).equals("www"))) {
              // link = link;
            } else {
              try {
                URL baseUrl = new URL(focus.url);
                URL newUrl = new URL(baseUrl, link);
                link = newUrl.toString();
              } catch (StringIndexOutOfBoundsException e) {
                // System.err.println(e.toString());
              } catch (MalformedURLException e) {
                // System.out.println(e.toString());
                continue;
              }
            }
            String checkUrl = link;
            try {
              URL url = new URL(focus.url);
              checkUrl = url.getHost();
            } catch (MalformedURLException e) {
              // e.printStackTrace();
            }
            if (currLinks.contains(link))
              continue;
            else if (!checkUrl.contains("cse.ust.hk"))
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
          scrapedLinks.push(focus.url);
        } catch (Exception e) {
          e.printStackTrace();
          // System.out.println("Link Error: " + focus.url);
        }
      }
    }
    System.out.println("Thread Crawler Gracefully Exited");
  }
}