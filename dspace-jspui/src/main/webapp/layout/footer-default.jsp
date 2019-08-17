<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Footer for home page
  --%>

<%@page import="org.dspace.core.ConfigurationManager"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="org.dspace.eperson.EPerson"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.core.NewsManager" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.app.webui.util.LocaleUIHelper" %>

<%
	String footerNews = NewsManager.readNewsFile(LocaleSupport.getLocalizedMessage(pageContext, "news-footer.html"));
    String sidebar = (String) request.getAttribute("dspace.layout.sidebar");
	String[] mlinks = new String[0];
	String mlinksConf = ConfigurationManager.getProperty("cris","navbar.cris-entities");
	if (StringUtils.isNotBlank(mlinksConf)) {
		mlinks = StringUtils.split(mlinksConf, ",");
	}

	boolean showCommList = ConfigurationManager.getBooleanProperty("community-list.show.all",true);
	boolean isRtl = StringUtils.isNotBlank(LocaleUIHelper.ifLtr(request, "","rtl"));
%>

            <%-- Right-hand side bar if appropriate --%>
<%
    if (sidebar != null)
    {
%>
	</div>
	<div class="col-md-3">
                    <%= sidebar %>
    </div>
    </div>
<%
    }
%>
</div>
<br/>
</main>
            <%-- Page footer --%>
            <footer class="container navbar navbar-inverse navbar-bottom">
             <div class="row">

                <div class="col-md-3 col-sm-6">
                    <a href="http://www.wku.edu.cn/en/library/" target="_blank">Wenzhou-Kean University Library</a>
                    <div><fmt:message key="jsp.layout.footer-default.library-mission-description"/>
                            <a href="http://www.wku.edu.cn/en/library/about/library-mission-statement/" target="_blank">[read more <i class="fa fa-external-link"></i>]</a>
                    </div>
                </div>

                <div class="col-md-3 col-sm-6">
                    <div class="panel panel-default">
                        <div class="panel-heading">
                            <h6 class="panel-title"><fmt:message key="jsp.layout.footer-default.explore"/></h6>
                        </div>
                        <div class="panel-body">
                        <ul>
                <% 	if(showCommList){ %>
                <li><a href="<%= request.getContextPath() %>/community-list"><fmt:message key="jsp.layout.navbar-default.communities-collections"/></a></li>
                <%	}
                    for (String mlink : mlinks) {
                %>
                <c:set var="fmtkey">
                jsp.layout.navbar-default.cris.<%= mlink.trim() %>
                </c:set>
                <li><a href="<%= request.getContextPath() %>/cris/explore/<%= mlink.trim() %>"><fmt:message key="${fmtkey}"/></a></li>
                    <% } %>
                        </ul>
                    </div>
                </div>
                </div>

                <div class="col-md-3 col-sm-6">
                    <div class="panel panel-default">
                        <div class="panel-heading">
                            <h6 class="panel-title">Useful Links</h6>
                        </div>
                        <div class="panel-body">
                        <ul>
                                <li><a href="http://www.wku.edu.cn/en/library/service/wire/">About WIRE</a></li>
                                <li><a target="_blank" href="http://www.wku.edu.cn/en/library/about/news-events/">Library News & Events</a></li>
                                <li><a href="http://www.wku.edu.cn/en/library/service/digital-learning">Library Digital Learning</a></li>
                                <li><a href="http://www.wku.edu.cn/en/library/service/digital-research">Library Digital Research</a></li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div class="col-md-3 col-sm-6">
                    <div class="panel panel-default">
                        <div class="panel-heading">
                            <h6 class="panel-title">Submit an Item</h6>
                        </div>
                        <div class="panel-body">
                            <div>Wenzhou-Kean University faculty, staff, and students can request to submit a digital item to WIRE. Please contact your library liaisons via <a target="_blank" href="http://www.wku.edu.cn/en/library/service/liaison/">Library Liaisons</a></div><br/>
                        </div>
                    </div>
                </div>
            </div>
            <%-- <div class="col-md-9 col-sm-6">
                <%= footerNews %>
            </div> --%>

            <div class="extra-footer row">
                <div id="footer_feedback" class="col-md-12 col-sm-12 text-center">
                    <a href="mailto:wire@wku.edu.cn">Contact us <i class="fa fa-envelope-o"></i></a> |
                    <a target="_blank" href="http://www.wku.edu.cn/en/library/service/wire/terms-of-use/">Terms of Use <i class="fa fa-external-link"></i></a> |
                    <span><fmt:message key="jsp.layout.footer-default.copy-right"/></span>
                </div>
			</div>
            <div class="extra-footer design-bar row">
                <div id="designedby" class="col-md-12 col-sm-12 col-md-offset-8">
                    <fmt:message key="jsp.layout.footer-default.text"/>
                    <fmt:message key="jsp.layout.footer-default.theme"/>
                    <a href="#">
                        <fmt:message key="jsp.layout.footer-default.zhangmeng"/>
                    </a>
                </div>
			</div>
	    </footer>
    </body>
</html>