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

<div id="leftCol" class="well">

    <h3 style="margin-top: 2px;">Posters </h3>
    <a href="${currentNode.properties['poster_path'].string}"><img id="upload_poster" class="shadow" src="${currentNode.properties['poster_path'].string}" width="185" height="278" itemprop="image"></a>

    <h3>Original Title</h3>
    <p>${currentNode.properties['original_title'].string}</p>

    <h3>Movie Facts</h3>

    <p><strong>Status:</strong> <span id="status">${currentNode.properties['status'].string}</span></p>
    <p><strong>Runtime:</strong> <span id="runtime"><meta itemprop="duration" content="PT1M57S">${currentNode.properties['runtime'].string}</span></p>
    <p><strong>Budget:</strong> <span id="budget">${currentNode.properties['budget'].string}</span></p>
    <p><strong>Revenue:</strong> <span id="revenue">${currentNode.properties['revenue'].string}</span></p>
    <p><strong>Webpage:</strong> <span id="homepage"><a href="${currentNode.properties['homepage'].string}">${currentNode.properties['homepage'].string}</a></span></p>

</div>
