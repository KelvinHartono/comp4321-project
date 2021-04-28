<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ page import="java.net.*, java.io.*, java.util.*, java.text.*, org.rocksdb.*, org.rocksdb.util.*" %>
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
    <div class="container">
      <div class="row">
        <h3>COMP 4321 Search Engine</h3>
      </div>
      <br />
      <div class="row">
        <div class="search-box">
          <form class="search-form" action="search.jsp" method="get">
            <input
              class="form-control"
              placeholder="Keywords"
              type="text"
              name="input"
            />
            <button class="btn btn-link search-btn">
              <i class="bi bi-search"></i>
            </button>
          </form>
        </div>
      </div>



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
		
		/*
		if(s != null) {
			out.print(s);
		}
		*/
		
		Vector<HashMap<String, String>> retArr = new Vector<HashMap<String, String>>();
		int counter = 0;
		try {
			
			if(s == null) {
				return;
			}
			
			
			retArr = query.processQuery(s);
			
			
			//print search results header
			out.print("<div class='row'><h3>Search Results: \"" + s + "\"</h3></div><br />");
			
			for(HashMap<String, String> ret: retArr) {
				
				out.print(ret);
									
				out.print("<div class='row'>");

				    out.print("<div class='content'>");
				    out.print("<div class='row'>");
				    out.print("<div class='col-1'><p>" + (counter + 1) + "</p></div>");
				    out.print("<div class='col-1'><p>Score</p><p>" + ret.get("score") + "</p><p>PageRank</p><p>" + ret.get("pageRank") + "</p><p>Title Sim</p><p>" + ret.get("titleSim") + "</p><p>Cos Sim</p><p>" + ret.get("cosim") + "</p></div>");
				    out.print(" <div class='col-10'>");
				    out.print("<p>Title: <a href='" + ret.get("url") + "' target='_blank'>" + ret.get("title") + "</a></p>");
				    out.print("<p>URL: <a href='" + ret.get("url") + "' target='_blank'>" + ret.get("url") + "</a></p>");
					out.print("<p>Last modified date: " + ret.get("date") + ", Size: " + ret.get("size") + "</p>");
		            //out.print("<p>keyword 1 freq 1; keyword 2 freq 2; keyword 3 freq 3; keyword 4 freq 4; keyword 5 freq 5;</p>");
		            //out.print("<p>Parent Links</p>");
		            //out.print("<p>Child Links</p>");
		            out.print("</div></div></div><hr />");

				out.print("</div>");

				
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
  </body>
</html>
