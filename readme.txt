# Instruction

The database explanation is in DBdoc.txt

## Version control

- javac = javac 11.0.10
- java = openjdk version "11.0.10" 2021-01-19
- jsoup = jsoup-1.13.1.jar
- rocksdb = rocksdbjni-6.15.5.jar
- sudo update-alternatives --config java

## Set up the working directories

- Firstly, make sure that java is installed and its on PATH.
- Download this repo to your local file (choose one, ssh or https)
  ```
  git clone https://github.com/KelvinHartono/comp4321-project.git
  git clone git@github.com:KelvinHartono/comp4321-project.git
  ```
- Go inside the directory
  ```
  cd comp4321-project/apache-tomcat-10.0.5
  ```
- This is the main working space, initialize all aliases
  ```
  source .aliases
  ```
- Compile every class (simply type "compile_all" in the terminal)
  ```
  compile_all
  ```

### Turning on the server

- To turn on the server,
  ```
  on_server
  ```
- Access the url of the website, for example when the VM's ip is (143.89.130.225), so the url is
  ```
  http://143.89.130.225:8080/se4321/search.jsp
  ```
- To turn off the server,
  ```
  off_server
  ```

### Some other useful commands

- Crawl the website (Remember to also calculate the pagerank AFTER finish crawling everything)
  ```
  crawl
  calculate_pagerank
  ```
- purge the database files
  ```
  purge_db
  ```
- test out the query outputs
  ```
  test_query
  ```
