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

<div style="background:url('${currentNode.properties['backdrop_path'].string}');height:250px;width:780px;">
    <div style="position:absolute; margin:30px 50px;background-color:#ffffff;border:1px solid black;opacity:0.7;filter:alpha(opacity=70);height:80px; ">
        <p style="margin:30px 40px;font-weight:bold;color:#000000;">
          <a href="<c:url value="${url.base}${currentNode.path}.html"/>">${currentNode.properties['jcr:title'].string}</a>
        </p>
    </div>
</div>


<%--<a href="${currentNode.properties['poster_path'].string}"><img id="upload_poster" class="shadow" src="${currentNode.properties['poster_path'].string}" width="185" height="278" itemprop="image"></a>--%>

