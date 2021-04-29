/*
 * Generated by the Jasper component of Apache Tomcat
 * Version: Apache Tomcat/10.0.5
 * Generated at: 2021-04-29 14:37:34 UTC
 * Note: The last modified time of this file was set to
 *       the last modified time of the source file after
 *       generation to assist with modification tracking.
 */
package org.apache.jsp;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.jsp.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.text.*;
import java.lang.*;
import org.rocksdb.*;
import org.rocksdb.util.*;
import resources.*;

public final class search_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent,
                 org.apache.jasper.runtime.JspSourceImports {


		private Query query;
		public void jspInit() {
			query = new Query();
			System.out.println("Query object created [search.jsp]");
		}
	
  private static final jakarta.servlet.jsp.JspFactory _jspxFactory =
          jakarta.servlet.jsp.JspFactory.getDefaultFactory();

  private static java.util.Map<java.lang.String,java.lang.Long> _jspx_dependants;

  private static final java.util.Set<java.lang.String> _jspx_imports_packages;

  private static final java.util.Set<java.lang.String> _jspx_imports_classes;

  static {
    _jspx_imports_packages = new java.util.HashSet<>();
    _jspx_imports_packages.add("org.rocksdb");
    _jspx_imports_packages.add("java.util");
    _jspx_imports_packages.add("java.text");
    _jspx_imports_packages.add("java.lang");
    _jspx_imports_packages.add("java.net");
    _jspx_imports_packages.add("jakarta.servlet");
    _jspx_imports_packages.add("java.io");
    _jspx_imports_packages.add("jakarta.servlet.http");
    _jspx_imports_packages.add("jakarta.servlet.jsp");
    _jspx_imports_packages.add("resources");
    _jspx_imports_packages.add("org.rocksdb.util");
    _jspx_imports_classes = null;
  }

  private volatile jakarta.el.ExpressionFactory _el_expressionfactory;
  private volatile org.apache.tomcat.InstanceManager _jsp_instancemanager;

  public java.util.Map<java.lang.String,java.lang.Long> getDependants() {
    return _jspx_dependants;
  }

  public java.util.Set<java.lang.String> getPackageImports() {
    return _jspx_imports_packages;
  }

  public java.util.Set<java.lang.String> getClassImports() {
    return _jspx_imports_classes;
  }

  public jakarta.el.ExpressionFactory _jsp_getExpressionFactory() {
    if (_el_expressionfactory == null) {
      synchronized (this) {
        if (_el_expressionfactory == null) {
          _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
        }
      }
    }
    return _el_expressionfactory;
  }

  public org.apache.tomcat.InstanceManager _jsp_getInstanceManager() {
    if (_jsp_instancemanager == null) {
      synchronized (this) {
        if (_jsp_instancemanager == null) {
          _jsp_instancemanager = org.apache.jasper.runtime.InstanceManagerFactory.getInstanceManager(getServletConfig());
        }
      }
    }
    return _jsp_instancemanager;
  }

  public void _jspInit() {
  }

  public void _jspDestroy() {
  }

  public void _jspService(final jakarta.servlet.http.HttpServletRequest request, final jakarta.servlet.http.HttpServletResponse response)
      throws java.io.IOException, jakarta.servlet.ServletException {

    if (!jakarta.servlet.DispatcherType.ERROR.equals(request.getDispatcherType())) {
      final java.lang.String _jspx_method = request.getMethod();
      if ("OPTIONS".equals(_jspx_method)) {
        response.setHeader("Allow","GET, HEAD, POST, OPTIONS");
        return;
      }
      if (!"GET".equals(_jspx_method) && !"POST".equals(_jspx_method) && !"HEAD".equals(_jspx_method)) {
        response.setHeader("Allow","GET, HEAD, POST, OPTIONS");
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "JSPs only permit GET, POST or HEAD. Jasper also permits OPTIONS");
        return;
      }
    }

    final jakarta.servlet.jsp.PageContext pageContext;
    jakarta.servlet.http.HttpSession session = null;
    final jakarta.servlet.ServletContext application;
    final jakarta.servlet.ServletConfig config;
    jakarta.servlet.jsp.JspWriter out = null;
    final java.lang.Object page = this;
    jakarta.servlet.jsp.JspWriter _jspx_out = null;
    jakarta.servlet.jsp.PageContext _jspx_page_context = null;


    try {
      response.setContentType("text/html; charset=ISO-8859-1");
      pageContext = _jspxFactory.getPageContext(this, request, response,
      			null, true, 8192, true);
      _jspx_page_context = pageContext;
      application = pageContext.getServletContext();
      config = pageContext.getServletConfig();
      session = pageContext.getSession();
      out = pageContext.getOut();
      _jspx_out = out;

      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("<!DOCTYPE html>\r\n");
      out.write("<html lang=\"en\">\r\n");
      out.write("  <head>\r\n");
      out.write("    <meta charset=\"UTF-8\" />\r\n");
      out.write("    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\" />\r\n");
      out.write("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\r\n");
      out.write("    <link\r\n");
      out.write("      href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-beta3/dist/css/bootstrap.min.css\"\r\n");
      out.write("      rel=\"stylesheet\"\r\n");
      out.write("      integrity=\"sha384-eOJMYsd53ii+scO/bJGFsiCZc+5NDVN2yr8+0RDqr0Ql0h+rP48ckxlpbzKgwra6\"\r\n");
      out.write("      crossorigin=\"anonymous\"\r\n");
      out.write("    />\r\n");
      out.write("    <link\r\n");
      out.write("      rel=\"stylesheet\"\r\n");
      out.write("      href=\"https://cdn.jsdelivr.net/npm/bootstrap-icons@1.4.1/font/bootstrap-icons.css\"\r\n");
      out.write("    />\r\n");
      out.write("    <link rel=\"stylesheet\" href=\"style.css\" />\r\n");
      out.write("    <title>4321 Search Engine</title>\r\n");
      out.write("  </head>\r\n");
      out.write("  <body>\r\n");
      out.write("    <nav class=\"navbar sticky-top navbar-light bg-light\">\r\n");
      out.write("      <div class=\"container\">\r\n");
      out.write("        <div class=\"col-3\">\r\n");
      out.write("          <img src=\"gocari.png\" alt=\"\" height=\"25\" />\r\n");
      out.write("        </div>\r\n");
      out.write("        <div class=\"col-9\">\r\n");
      out.write("          <form class=\"d-flex\">\r\n");
      out.write("            <input\r\n");
      out.write("              class=\"form-control me-2\"\r\n");
      out.write("              type=\"text\"\r\n");
      out.write("              placeholder=\"Keywords\"\r\n");
      out.write("              aria-label=\"Search\"\r\n");
      out.write("              action=\"search.jsp\"\r\n");
      out.write("              method=\"get\"\r\n");
      out.write("              name=\"input\"\r\n");
      out.write("            />\r\n");
      out.write("            <button class=\"btn\" type=\"submit\">Search</button>\r\n");
      out.write("          </form>\r\n");
      out.write("        </div>\r\n");
      out.write("      </div>\r\n");
      out.write("    </nav>\r\n");
      out.write("\r\n");
      out.write("\t");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\t");

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
			out.print("<div class='container'><div class='row' style='margin-top: 40px'><p><small>Search Results of \"" + s + "\" (" + (System.currentTimeMillis()-currentTime) + ")</small></p></div>");

			
			for(HashMap<String, String> ret: retArr) {

        //out.print(ret);

        //print website title
        out.print("<div class='row'><div class='content'><div class='row'><div class='col-12'><h6><a href='"+ ret.get("url") +"' target='_blank' style='color: rgb(26, 10, 213); text-decoration: none'>" + ret.get("title") +"</a></h6>");

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
				if(counter == 1) {
					break;
				}
			}
			

      
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
      out.write("\r\n");
      out.write("      </div>\r\n");
      out.write("    </div>\r\n");
      out.write("  </body>\r\n");
      out.write("  <script>\r\n");
      out.write("    function myFunction(num) {\r\n");
      out.write("      var dot = num + 1;\r\n");
      out.write("      var more = num + 2;\r\n");
      out.write("      var btnText = document.getElementById(num);\r\n");
      out.write("      var dots = document.getElementById(dot);\r\n");
      out.write("      var moreText = document.getElementById(more);\r\n");
      out.write("\r\n");
      out.write("      if (dots.classList.contains('hidden')) {\r\n");
      out.write("        dots.classList.toggle('hidden');\r\n");
      out.write("        btnText.innerHTML = '<i class=\"bi bi-caret-down-fill\"></i> More';\r\n");
      out.write("        moreText.classList.toggle('hidden');\r\n");
      out.write("      } else {\r\n");
      out.write("        dots.classList.toggle('hidden');\r\n");
      out.write("        btnText.innerHTML = '<i class=\"bi bi-caret-up-fill\"></i> Less';\r\n");
      out.write("        moreText.classList.toggle('hidden');\r\n");
      out.write("      }\r\n");
      out.write("    }\r\n");
      out.write("  </script>\r\n");
      out.write("</html>");
    } catch (java.lang.Throwable t) {
      if (!(t instanceof jakarta.servlet.jsp.SkipPageException)){
        out = _jspx_out;
        if (out != null && out.getBufferSize() != 0)
          try {
            if (response.isCommitted()) {
              out.flush();
            } else {
              out.clearBuffer();
            }
          } catch (java.io.IOException e) {}
        if (_jspx_page_context != null) _jspx_page_context.handlePageException(t);
        else throw new ServletException(t);
      }
    } finally {
      _jspxFactory.releasePageContext(_jspx_page_context);
    }
  }
}