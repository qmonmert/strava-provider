<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<template:addResources type="javascript" resources="jquery.min.js"/>
<c:url value="${url.base}${currentNode.path}.tmdbtoken.do" var="url" />
<script type="text/javascript">
    function tokenRequest() {
        $.post('${url}', null, function(r) {
            window.open(r['url'])
        },'json');
    }
</script>

<a href="#" onclick="tokenRequest()">Request token</a>