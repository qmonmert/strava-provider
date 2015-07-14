<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<h2 id="title"><span itemprop="name">${currentNode.properties['name'].string}</span></h2>

${currentNode.properties['biography'].string}


<hr>
<jcr:jqom var="res" statement="SELECT * FROM [jnt:cast] where id='${currentNode.name}'" limit="20"/>
<ul>
    <c:forEach items="${res.nodes}" var="cast">
        <li>
                ${cast.properties['character'].string}
          <a href="<c:url value="${url.base}${cast.parent.path}.html"/>">${cast.parent.properties['jcr:title'].string}</a>
        </li>
    </c:forEach>
</ul>
<hr>

<jcr:jqom var="res" statement="SELECT * FROM [jnt:crew] where id='${currentNode.name}'" limit="20"/>
<ul>
    <c:forEach items="${res.nodes}" var="crew">
        <li>
                ${crew.properties['job'].string}
          <a href="<c:url value="${url.base}${crew.parent.path}.html"/>">${crew.parent.properties['jcr:title'].string}</a>
        </li>
    </c:forEach>
</ul>


