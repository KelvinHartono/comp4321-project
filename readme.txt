# instruction

The database explanation is in DBdoc.txt

## Version control

- javac = javac 11.0.10
- java = openjdk version "11.0.10" 2021-01-19
- jsoup = jsoup-1.13.1.jar
- rocksdb = rocksdbjni-6.15.5.jar
- sudo update-alternatives --config java

## How to run

- Firstly, make sure that java is installed and its on PATH, so that our working directory can access it
- Make sure to empty the content of each directory inside `/db/`
  ```
  chmod a+x ./purgeDB.sh
  ./purgeDB.sh
  ```
- To run the Spider
  - Build the main file for crawling, which is crawler.java
    ```
    javac -cp lib/jsoup-1.13.1.jar:lib/rocksdbjni-6.15.5.jar:. Crawler.java
    ```
  - Run the crawler
    ```
    java -cp lib/jsoup-1.13.1.jar:lib/rocksdbjni-6.15.5.jar:. Crawler
    ```
- To get spider_result.txt file
  - Build the output file, which is Output.java
    ```
    javac -cp lib/jsoup-1.13.1.jar:lib/rocksdbjni-6.15.5.jar:. Output.java
    ```
  - Run the output file
    ```
    java -cp lib/jsoup-1.13.1.jar:lib/rocksdbjni-6.15.5.jar:. Output
    ```
  - spider_result.txt will appear!
- To try query
  - Build the query file, which is Query.java
    ```
    javac -cp lib/jsoup-1.13.1.jar:lib/rocksdbjni-6.15.5.jar:. Query.java
    ```
  - Run the query file
    ```
    java -cp lib/jsoup-1.13.1.jar:lib/rocksdbjni-6.15.5.jar:. Query
    ```