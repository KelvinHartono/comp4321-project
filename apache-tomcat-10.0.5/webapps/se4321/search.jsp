<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ page import="java.net.*, java.io.*, java.util.*, java.text.*, java.lang.*, org.rocksdb.*, org.rocksdb.util.*" %>
<%@ page import="resources.*" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <link
      href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-beta3/dist/css/bootstrap.min.css"
      rel="stylesheet"
      integrity="sha384-eOJMYsd53ii+scO/bJGFsiCZc+5NDVN2yr8+0RDqr0Ql0h+rP48ckxlpbzKgwra6"
      crossorigin="anonymous"
    />
    <link
      rel="stylesheet"
      href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.4.1/font/bootstrap-icons.css"
    />
    <link rel="stylesheet" href="style.css" />
    <title>4321 Search Engine</title>
  </head>
  <body>
    <nav class="navbar sticky-top navbar-light bg-light">
      <div class="container">
        <div class="col-3">
          <img src="gocari.png" alt="" height="25" />
        </div>
        <div class="col-9">
          <form class="d-flex">
            <input
              class="form-control me-2"
              type="text"
              placeholder="Keywords"
              aria-label="Search"
              action="search.jsp"
              method="get"
              name="input"
            />
            <button class="btn" type="submit">Search</button>
          </form>
        </div>
      </div>
    </nav>

	<%!
		private Query query;
		public void jspInit() {
			query = new Query();
			System.out.println("Query object created [search.jsp]");
		}
	%>

	<%
		if(query == null) {
			jspInit();
		}
		String s = request.getParameter("input");
		
		Vector<HashMap<String, String>> retArr = new Vector<HashMap<String, String>>();
		int counter = 0;
		try {
			
			if(s == null) {
				return;
			}
			long currentTime = System.currentTimeMillis();
			
			retArr = query.processQuery(s);
			

      int links_counter = 1;
			
			//print Search result term and time
			out.print("<div class='container'><div class='row' style='margin-top: 40px'><p><small>Search Results of \"" + s + "\" (" + (System.currentTimeMillis()-currentTime) + " ms)</small></p></div>");
      out.print("<div class='row'>");
			
			for(HashMap<String, String> ret: retArr) {

        //out.print(ret);

        //print website title
        out.print("<div class='content'><div class='row'><div class='col-12'><h6><a href='"+ ret.get("url") +"' target='_blank' style='color: rgb(26, 10, 213); text-decoration: none'>" + ret.get("title") +"</a></h6>");

        //print url
        out.print("<p><a href='"+ ret.get("url") +"' target='_blank' style='color: green; text-decoration: none'>" + ret.get("url") + "</a></p>");

        //print metadata
        out.print("<p><strong>Last modified: </strong>" + ret.get("date") + ", <strong>Size:</strong> " + ret.get("size") + " Bytes</p><p><strong>Score:</strong> " + ret.get("score") + "</p><p><strong>PageRank:</strong> " + ret.get("pageRank") + "</p><p><strong>Title Similarity:</strong> " + ret.get("titleSim") + "</p><p><strong>Cosine Similarity:</strong> " + ret.get("cosim") + "</p><p><strong>Most frequent stemmed keywords: </strong>" + ret.get("keyword") + "</p>");


        //print parent links
        out.print("<div class='row'><div class='col-12'><p><strong>Parent Links:<button onclick='myFunction(" + links_counter + ")' id='" + links_counter + "' class='show-button'><i class='bi bi-caret-down-fill'></i> More</button></strong></p></div></div><p style='line-height: 100%'>");

        String parent[] = ret.get("parent").split("@@");

        for(int i = 0; i < parent.length; i++) {
          out.print(parent[i] + "<br />");
          if(i == 1) {
            links_counter++;
            out.print("<span id='" + links_counter + "'>...</span>");
            links_counter++;
            out.print("<span id='" + links_counter + "' class='hidden'>");
          }
        }

        out.print("</span></p>");

        links_counter++;


        //print child links
        out.print("<div class='row'><div class='col-12'><p><strong>Child Links:<button onclick='myFunction(" + links_counter + ")' id='" + links_counter + "' class='show-button'><i class='bi bi-caret-down-fill'></i> More</button></strong></p></div></div><p style='line-height: 100%'>");

        String child[] = ret.get("child").split("@@");

        for(int i = 0; i < child.length; i++) {
          out.print(child[i] + "<br />");
          if(i == 1) {
            links_counter++;
            out.print("<span id='" + links_counter + "'>...</span>");
            links_counter++;
            out.print("<span id='" + links_counter + "' class='hidden'>");
          }
        }

        out.print("</span></p>");

        links_counter++;


        out.print("</div></div></div><hr />");

        
				counter++;
				if(counter == 50) {
					break;
				}
			}
			

      
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	%>
      </div>
    </div>
  </body>
  <script>
    function myFunction(num) {
      var dot = num + 1;
      var more = num + 2;
      var btnText = document.getElementById(num);
      var dots = document.getElementById(dot);
      var moreText = document.getElementById(more);

      if (dots.classList.contains('hidden')) {
        dots.classList.toggle('hidden');
        btnText.innerHTML = '<i class="bi bi-caret-down-fill"></i> More';
        moreText.classList.toggle('hidden');
      } else {
        dots.classList.toggle('hidden');
        btnText.innerHTML = '<i class="bi bi-caret-up-fill"></i> Less';
        moreText.classList.toggle('hidden');
      }
    }
  </script>
</html>