<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri='http://java.sun.com/jsp/jstl/core' prefix='c'%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="WEB-INF/property.tld" prefix="rt" %>
<html>
	<head>
		<meta charset="utf-8">
		<title>Run</title>
		<link href="css/bootstrap.min.css" rel="stylesheet">
		<link href="css/styles.css" rel="stylesheet">
		<link href="css/bootstrap.vertical-tabs.min.css" rel="stylesheet">
	</head>
	<body>
		<nav class="navbar navbar-default" role="navigation">
			<div class="container-fluid">
				<div class="navbar-header">
					<button type="button" class="navbar-toggle collapsed" data-toggle="collapse"
						data-target="#main-navbar">
						<span class="sr-only">Toggle navigation</span>
						<span class="icon-bar"></span>
						<span class="icon-bar"></span>
						<span class="icon-bar"></span>
					</button>
					<a id="logo" href="/"><img src="images/logo.jpg"></a>
					<a class="navbar-brand" href="/">Mongoose</a>
				</div>
				<div class="collapse navbar-collapse"
					id="main-navbar">
					<ul class="nav navbar-nav">
						<li class="active"><a href="/">Run<span class="sr-only">(current)</span></a></li>
						<li><a href="/charts">Charts</a></li>
					</ul>

					<ul class="nav navbar-nav navbar-right">
						<li><a href="about.html">About</a></li>
					</ul>
				</div>
			</div>
		</nav>

		<div class="content-wrapper">
			<div class="tabs-wrapper">
				<ul class="nav nav-tabs" role="presentation">
					<li class="active"><a href="#configuration" data-toggle="tab">Configuration</a></li>
					<c:forEach var="mode" items="${sessionScope.runmodes}">
						<c:set var="correctMode" value="${fn:replace(mode, '.', '_')}"/>
						<li><a href="#${correctMode}" data-toggle="tab">
							${mode}
							<span class="glyphicon glyphicon-remove" value="${correctMode}"></span>
						</a></li>
					</c:forEach>
				</ul>
			</div>

			<div class="tab-content">
				<div class="tab-pane active" id="configuration">
					<div id="menu">
						<div id="runmodes">
							<select>
								<option>standalone</option>
								<option>client</option>
								<option>server</option>
								<option>wsmock</option>
							</select>
							<button id="start" type="button">
								Start
							</button>
						</div>
						<ul class="folders">
							<li>
								<label for="properties">properties</label>
								<input type="checkbox" id="properties">
								<ul>
									<li>
										<label for="api">api</label>
										<input type="checkbox" id="api">
										<ul>
											<li class="file"><a href="#atmos">atmos</a></li>
											<li class="file"><a href="#s3">s3</a></li>
											<li class="file"><a href="#swift">swift</a></li>
										</ul>
									</li>
									<li>
										<label for="load">load</label>
										<input type="checkbox" id="load">
										<ul>
											<li class="file"><a href="#append">append</a></li>
											<li class="file"><a href="#create">create</a></li>
											<li class="file"><a href="#read">read</a></li>
											<li class="file"><a href="#update">update</a></li>
											<li class="file"><a href="#delete">delete</a></li>
										</ul>
									</li>
									<li>
										<label for="scenario">scenario</label>
										<input type="checkbox" id="scenario">
										<ul>
											<li class="file"><a href="#single">single</a></li>
											<li class="file"><a href="#chain">chain</a></li>
											<li class="file"><a href="#rampup">rampup</a></li>
											<li class="file"><a href="#rampup-create">rampup-create</a></li>
										</ul>
									</li>
									<li class="file"><a href="#auth">auth</a></li>
									<li class="file"><a href="#data">data</a></li>
									<li class="file"><a href="#http">http</a></li>
									<li class="file"><a href="#remote">remote</a></li>
									<li class="file"><a href="#run">run</a></li>
									<li class="file"><a href="#storage">storage</a></li>
								</ul>
							</li>
						</ul>
					</div>

					<div id="right-side">
						<ol class="breadcrumb">
						</ol>

						<form id="main-form">
                            <input type="hidden" name="run.mode" id="run-mode" value="standalone">
							<div id="configuration-content">
								<div id="atmos">
									<div class="property-labels">
										<label for="atmos-port">port:</label>
										<label for="atmos-subtenant">subtenant:</label>
										<label for="atmos-path-rest">path.rest:</label>
										<label for="atmos-interface">interface:</label>
									</div>
									<div class="property-inputs">
										<input type="text" id="atmos-port" name="api.atmos.port"><br/>
										<input type="text" id="atmos-subtenant" name="api.atmos.subtenant"><br/>
										<input type="text" id="atmos-path-rest" name="api.atmos.path.rest"><br/>
										<input type="text" id="atmos-interface" name="api.atmos.interface">
									</div>
								</div>

								<div id="s3">
									<div class="property-labels">
										<label for="s3-port">port:</label>
										<label for="s3-auth-prefix">auth.prefix:</label>
										<label for="s3-bucket">bucket:</label>
									</div>
									<div class="property-inputs">
										<input type="text" id="s3-port" name="api.s3.port"><br/>
										<input type="text" id="s3-auth-prefix" name="api.s3.auth.prefix"><br/>
										<input type="text" id="s3-bucket" name="api.s3.bucket">
									</div>
								</div>

								<div id="swift">
									<div class="property-labels">
										<label for="swift-port">port:</label>
									</div>
									<div class="property-inputs">
										<input type="text" id="swift-port" name="api.swift.port">
									</div>
								</div>

								<div id="append">
									<div class="property-labels">
										<label for="append-threads">threads:</label>
									</div>
									<div class="property-inputs">
										<input type="text" id="append-threads" name="load.append.threads">
									</div>
								</div>

								<div id="create">
									<div class="property-labels">
										<label for="create-threads">threads:</label>
									</div>
									<div class="property-inputs">
										<input type="text" id="create-threads" name="load.create.threads">
									</div>
								</div>

								<div id="delete">
									<div class="property-labels">
										<label for="delete-threads">threads:</label>
									</div>
									<div class="property-inputs">
										<input type="text" id="delete-threads" name="load.delete.threads">
									</div>
								</div>

								<div id="read">
									<div class="property-labels">
										<label for="read-threads">threads:</label>
										<label for="read-verify-content">verify.content:</label>
									</div>
									<div class="property-inputs">
										<input type="text" id="read-threads" name="load.read.threads"><br/>
										<input type="text" id="read-verify-content" name="load.read.verify.content">
									</div>
								</div>

								<div id="update">
									<div class="property-labels">
										<label for="update-threads">threads:</label>
										<label for="update-per-item">per.item:</label>
									</div>
									<div class="property-inputs">
										<input type="text" id="update-threads" name="load.update.threads"><br/>
										<input type="text" id="update-per-item" name="load.update.per.item">
									</div>
								</div>

								<div id="chain">
									<div class="property-labels">
										<label for="chain-load">load:</label>
										<label for="chain-simultaneous">simultaneous:</label>
									</div>
									<div class="property-inputs">
										<input type="text" id="chain-load" name="scenario.chain.load"><br/>
										<input type="text" id="chain-simultaneous" name="scenario.chain.simultaneous">
									</div>
								</div>

								<div id="rampup">
									<div class="property-labels">
										<label for="rampup-thread-counts">thread.counts:</label>
										<label for="rampup-sizes">sizes:</label>
									</div>
									<div class="property-inputs">
										<input type="text" id="rampup-thread-counts" name="scenario.rampup.thread.counts"><br/>
										<input type="text" id="rampup-sizes" name="scenario.rampup.sizes">
									</div>
								</div>

								<div id="rampup-create">
									<div class="property-labels">
										<label for="rampup-create-load">load:</label>
										<label for="rampup-create-threads">threads:</label>
										<label for="rampup-create-objectsizes">objectsizes:</label>
									</div>
									<div class="property-inputs">
										<input type="text" id="rampup-create-load" name="scenario.rampup-create.load"><br/>
										<input type="text" id="rampup-create-threads" name="scenario.rampup-create.threads"><br/>
										<input type="text" id="rampup-create-objectsizes" name="scenario.rampup-create.objectsizes">
									</div>
								</div>

								<div id="single">
									<div class="property-labels">
										<label for="single-load">load:</label>
									</div>
									<div class="property-inputs">
										<input type="text" id="single-load" name="scenario.single.load">
									</div>
								</div>

								<div id="auth">
									<div class="property-labels">
										<label for="auth-id">id:</label>
										<label for="auth-secret">secret:</label>
									</div>
									<div class="property-inputs">
										<input type="text" id="auth-id" name="auth.id"><br/>
										<input type="text" id="auth-secret" name="auth.secret">
									</div>
								</div>

								<div id="data">
									<div class="property-labels">
										<label for="data-count">count:</label>
										<label for="data-size-min">size.min:</label>
										<label for="data-size-max">size.max:</label>
										<label for="data-src-fpath">src.fpath:</label>
										<label for="data-src-separator">src.separator:</label>
										<label for="data-page-size">page.size:</label>
										<label for="data-ring-seed">ring.seed:</label>
										<label for="data-ring-size">ring.size:</label>
									</div>
									<div class="property-inputs">
										<input type="text" id="data-count" name="data.count"><br/>
										<input type="text" id="data-size-min" name="data.size.min"><br/>
										<input type="text" id="data-size-max" name="data.size.max"><br/>
										<input type="text" id="data-src-fpath" name="data.src.fpath"><br/>
										<input type="text" id="data-src-separator" name="data.src.separator">
										<br/>
										<input type="text" id="data-page-size" name="data.page.size"><br/>
										<input type="text" id="data-ring-seed" name="data.ring.seed"><br/>
										<input type="text" id="data-ring-size" name="data.ring.size">
									</div>
								</div>

								<div id="run">
									<div class="property-labels">
										<label for="run-id">run.id</label>
									</div>
									<div class="property-inputs">
										<input type="text" id="run-id" name="run.id">
									</div>
								</div>
							</div>
						</form>
					</div>
				</div>
				<c:forEach var="mode" items="${sessionScope.runmodes}">
					<c:set var="correctMode" value="${fn:replace(mode, '.', '_')}"/>
					<div class="tab-pane" id="${correctMode}">
						<div class="left-side">
							<div class="menu-wrapper">
								<div class="col-xs-8">
									<ul class="nav nav-tabs tabs-left">
										<li class="active"><a href="#${correctMode}messages-csv" data-toggle="tab">messages.csv</a></li>
										<li><a href="#${correctMode}errors-log" data-toggle="tab">errors.log</a></li>
										<li><a href="#${correctMode}perf-avg-csv" data-toggle="tab">perf.avg.csv</a></li>
										<li><a href="#${correctMode}perf-sum-csv" data-toggle="tab">perf.sum.csv</a></li>
									</ul>
								</div>
							</div>
						</div>
						<div class="right-side">
							<c:if test="${empty sessionScope.stopped[mode]}">
								<button type="button" class="btn-default stop"><span>Stop</span></button>
							</c:if>
							<div class="log-wrapper">
								<div class="tab-content">
									<div class="tab-pane active" id="${correctMode}messages-csv">
										<table class="table">
											<thead>
												<tr>
													<th>Level</th>
													<th>LoggerName</th>
													<th>ThreadName</th>
													<th>TimeMillis</th>
													<th>Message</th>
												</tr>
											</thead>
											<tbody>
											</tbody>
										</table>
										<button type="button" class="default clear">Clear</button>
									</div>
									<div class="tab-pane" id="${correctMode}errors-log">
										<table class="table">
											<thead>
												<tr>
													<th>Level</th>
													<th>LoggerName</th>
													<th>ThreadName</th>
													<th>TimeMillis</th>
													<th>Message</th>
												</tr>
											</thead>
											<tbody>
											</tbody>
										</table>
										<button type="button" class="default clear">Clear</button>
									</div>
									<div class="tab-pane" id="${correctMode}perf-avg-csv">
										<table class="table">
											<thead>
												<tr>
													<th>Level</th>
													<th>LoggerName</th>
													<th>ThreadName</th>
													<th>TimeMillis</th>
													<th>Message</th>
												</tr>
											</thead>
											<tbody>
											</tbody>
										</table>
										<button type="button" class="default clear">Clear</button>
									</div>
									<div class="tab-pane" id="${correctMode}perf-sum-csv">
										<table class="table">
											<thead>
												<tr>
													<th>Level</th>
													<th>LoggerName</th>
													<th>ThreadName</th>
													<th>TimeMillis</th>
													<th>Message</th>
												</tr>
											</thead>
											<tbody>
											</tbody>
										</table>
										<button type="button" class="default clear">Clear</button>
									</div>
								</div>
							</div>
						</div>
					</div>
				</c:forEach>
			</div>
		</div>
		<script type="text/javascript" src="js/jquery-2.1.0.min.js"></script>
		<script type="text/javascript" src="js/script.js"></script>
		<script type="text/javascript" src="js/bootstrap.min.js"></script>
		<script type="text/javascript" src="js/jquery.cookie.js"></script>
	</body>
</html>