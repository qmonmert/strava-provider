<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="ui" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="s" uri="http://www.jahia.org/tags/search" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<!-- SQL-2 query -->
<jcr:sql var="res" sql="select * from [jnt:stravaAccount]"/>

<h3>Strava account (SQL-2 query)</h3>
<ul>
    <c:forEach items="${res.nodes}" var="stravaAccount">
        <li>Lastname : ${stravaAccount.properties['lastname'].string}</li>
        <li>Firstname : ${stravaAccount.properties['firstname'].string}</li>
        <li>City : ${stravaAccount.properties['city'].string}</li>
        <li>State : ${stravaAccount.properties['state'].string}</li>
        <li>Country : ${stravaAccount.properties['country'].string}</li>
        <li>Sex : ${stravaAccount.properties['sex'].string}</li>
        <li>Email : ${stravaAccount.properties['email'].string}</li>
        <li>Weight : ${stravaAccount.properties['weight'].string}</li>
    </c:forEach>
</ul>

<!-- JQOM (Java Query Object Model) -->

<jcr:jqom var="sitemaps">
    <query:selector nodeTypeName="jnt:stravaAccount" selectorName="stmp"/>
</jcr:jqom>

<h3>Strava account (JQOM)</h3>
<ul>
    <c:forEach items="${sitemaps.nodes}" varStatus="status" var="stravaAccount">
        <li>Lastname : ${stravaAccount.properties['lastname'].string}</li>
        <li>Firstname : ${stravaAccount.properties['firstname'].string}</li>
        <li>City : ${stravaAccount.properties['city'].string}</li>
        <li>State : ${stravaAccount.properties['state'].string}</li>
        <li>Country : ${stravaAccount.properties['country'].string}</li>
        <li>Sex : ${stravaAccount.properties['sex'].string}</li>
        <li>Email : ${stravaAccount.properties['email'].string}</li>
        <li>Weight : ${stravaAccount.properties['weight'].string}</li>
    </c:forEach>
</ul>

<!-- XPATH : /jcr:root/sites/ACMESPACE/contents//element(*, jnt:stravaAccount)  -->