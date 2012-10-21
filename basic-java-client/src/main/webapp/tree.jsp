<%--

    Cloud Foundry 2012.02.03 Beta
    Copyright (c) [2009-2012] VMware, Inc. All Rights Reserved.

    This product is licensed to you under the Apache License, Version 2.0 (the "License").
    You may not use this product except in compliance with the License.

    This product includes a number of subcomponents with
    separate copyright notices and license terms. Your use of these
    subcomponents is subject to the terms and conditions of the
    subcomponent's license, as noted in the LICENSE file.

--%>
<%@ page session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<html>
<head>
	<title>Your Cloud Foundry Applications</title>
</head>
<body>

<h1>${title}</h1>

	<ul>
		<li><a href="<c:url value="/"/>">Home</a></li>
	</ul>

Your ${name}:

    <ul id="tree" class="treeview">
      <c:forEach var="item" items="${items}">
        <li>${item.name}
        	<ul><c:forEach var="entry" items="${item}">
        		<li>${entry.key}: ${entry.value}</li>
		      </c:forEach>
        	</ul>
        </li>
      </c:forEach>
    </ul>

</body>
</html>
