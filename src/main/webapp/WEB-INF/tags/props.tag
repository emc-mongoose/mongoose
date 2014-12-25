<%@ attribute name="map" required="true" type="java.util.Map"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="myTags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:if test="${!empty map}">
	<ul>
	<c:forEach var="entry" items="${map}">
		${entry.key}
		<myTags:props map="${entry.value}"/>
	</c:forEach>
	</ul>
</c:if>