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

<div id="mainCol">
    <div class="title">
        <h2 id="title"><span itemprop="name">${currentNode.properties['jcr:title'].string}</span>
            (${currentNode.properties['release_date'].date.time.year + 1900})</h2>
    </div>

    <div class="rating">
        <div id="updateRating" itemprop="aggregateRating" itemscope="" itemtype="http://schema.org/AggregateRating">
            <p class="average"><span id="rating_hint"
                                     itemprop="ratingValue">${currentNode.properties['vote_average'].string}</span>/<span
                    itemprop="bestRating">10</span> (<span
                    itemprop="ratingCount">${currentNode.properties['vote_count'].string}</span> votes)</p>
        </div>
    </div>


    <div style="border-radius: 4px;border: 1px solid #e1e1e8;background-color: #f7f7f9;margin-bottom: 14px;padding: 9px 14px;">

        <h3>Overview </h3>

        <p id="overview" class="lead" itemprop="description">${currentNode.properties['overview'].string}</p>
    </div>
    <h3>Tagline</h3>

    <p id="tagline">${currentNode.properties['tagline'].string}</p>

    <h3>Crew</h3>
    <table class="table table-striped">
        <tbody>
        <c:forEach items="${jcr:getChildrenOfType(currentNode,'jnt:crew')}" var="sub">
            <tr>
              <td>${sub.properties['job'].string}</td>
              <td><a href="<c:url value="${url.base}${sub.properties['person'].node.path}.html"/>">${sub.properties['name'].string}</a></td>
                <td>
                    <c:if test="${not empty sub.properties['profile'].string}">
                        <img width="50" src="${sub.properties['profile'].string}"/>
                    </c:if>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>

    <h3>Cast</h3>
    <table class="table table-striped">
        <tbody>
        <c:forEach items="${jcr:getChildrenOfType(currentNode,'jnt:cast')}" var="sub">
            <tr>
              <td><a href="<c:url value="${url.base}${sub.properties['person'].node.path}.html"/>">${sub.properties['name'].string}</a></td>
                <td>${sub.properties['character'].string}</td>
                <td><img width="50" src="${sub.properties['profile'].string}"/></td>
            </tr>
        </c:forEach>


        </tbody>
    </table>


    <div style="clear: both;"></div>
</div>

