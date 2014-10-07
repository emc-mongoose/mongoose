<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri='http://java.sun.com/jsp/jstl/core' prefix='c'%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <script src="http://code.jquery.com/jquery-latest.min.js"></script>
    <script>
		$(document).ready(function() {
			$("#start_btn").click(function(){
            	$.post("/start",
                	{
                    	runmode: $("#runmode").val()
                    },
                    function(data,status){
                        alert("Data: " + data + "\nStatus: " + status);
                    });
            });

            $("#stop_btn").click(function(){
                $.post("/stop",
                    {
                        runmode: $("#runmode").val()
                    },
                    function(data,status){
                        alert("Data: " + data + "\nStatus: " + status);
                    });
            });

        });
        </script>
</head>
<body>
    <button id="start_btn">Start</button>
    <button id="stop_btn">Stop</button>
    <br>
    <select id="runmode">
        <c:forEach var="mode" items="${runmodes}">
            <option value="${mode}">${mode.value}</option>
        </c:forEach>
    </select>
</body>
</html>
