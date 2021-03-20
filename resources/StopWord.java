package resources;

import java.util.HashSet;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class StopWord {
  private HashSet<String> stopWords;

  public StopWord() {
    this("./resources/stopwords-en.txt");
  }

  public StopWord(String str) {
    stopWords = new HashSet<String>();

    try {
      BufferedReader reader = new BufferedReader(new FileReader(str));
      String line;
      while ((line = reader.readLine()) != null) {
        stopWords.add(line);
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public boolean isStopWord(String str) {
    return stopWords.contains(str);
  }
}