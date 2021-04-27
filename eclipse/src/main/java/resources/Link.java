package resources;

/**
 * The data structure for the crawling queue.
 */
public class Link {
  public String url;
  public int level;

  public Link(String url, int level) {
    this.url = url;
    this.level = level;
  }
}
