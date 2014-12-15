<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri='http://java.sun.com/jsp/jstl/core' prefix='c'%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="WEB-INF/property.tld" prefix="rt" %>
<!DOCTYPE html>
<html>
	<head>
		<meta charset="utf-8">
		<title>Run</title>
		<link href="css/bootstrap.min.css" rel="stylesheet">
		<link href="css/styles.css" rel="stylesheet">
		<link href="css/bootstrap.vertical-tabs.min.css" rel="stylesheet">
		<script type="text/javascript" src="js/jquery-2.1.0.min.js"></script>
		<script type="text/javascript" src="js/script.js"></script>
		<script type="text/javascript" src="js/bootstrap.min.js"></script>
		<script>
			propertiesMap = ${runTimeConfig.propertiesMap};
		</script>
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
						<div id="run-modes">
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

						<div id="config">
							<label for="config-type">Config type</label>
							<select id="config-type">
								<option value="base">base</option>
								<option value="extended">extended</option>
							</select>
						</div>

						<!-- List of folders from JS-->
						<ul class="folders">

						</ul>
					</div>

					<div id="main-content">
						<div id="base-config">
							<form class="form-horizontal" role="form">
								<div>
									<div class="form-group">
										<label for="auth.id" class="col-sm-2 control-label">auth.id</label>
										<div class="col-sm-10">
											<input type="text" class="form-control" id="auth.id" name="auth.id">
										</div>
									</div>

									<div class="form-group">
										<label for="auth.secret" class="col-sm-2 control-label">auth.secret</label>
										<div class="col-sm-10">
											<input type="text" class="form-control" id="auth.secret" name="auth.secret">
										</div>
									</div>
								</div>

								<div>
									<div class="form-group">
										<label for="data" class="col-sm-2 control-label">data</label>
										<div class="col-sm-10">
											<select id="data" class="form-select">
												<option>time</option>
												<option>objects</option>
											</select>
										</div>
									</div>
									<div class="form-group">
										<label for="data.count" class="col-sm-2 control-label">data.count</label>
										<div class="col-sm-10">
											<input type="text" class="form-control" id="data.count" name="data.count">
										</div>
									</div>

									<div class="form-group">
										<label for="run.time" class="col-sm-2 control-label">run.time</label>
										<div class="col-sm-10">
											<input type="text" class="form-control pre-select" id="run.time" name="run.time">
											<select class="form-select">
												<option>hours</option>
											</select>
										</div>
									</div>
									<div class="form-group">
										<label for="data.size.min" class="col-sm-2 control-label">data.size.min</label>
										<div class="col-sm-10">
											<input type="text" class="form-control" id="data.size.min" name="data.size.min">
										</div>
									</div>
									<div class="form-group">
										<label for="data.size.max" class="col-sm-2 control-label">data.size.max</label>
										<div class="col-sm-10">
											<input type="text" class="form-control" id="data.size.max" name="data.size.max">
										</div>
									</div>
									<div class="form-group">
										<label for="data.src.fpath" class="col-sm-2 control-label">data.src.fpath</label>
										<div class="col-sm-10">
											<input type="text" class="form-control" id="data.src.fpath" name="data.src.fpath">
										</div>
									</div>
									<div class="form-group">
										<label for="remote.servers" class="col-sm-2 control-label">remote.servers</label>
										<div class="col-sm-10">
											<input type="text" class="form-control" id="remote.servers" name="remote.servers">
										</div>
									</div>
									<div class="form-group">
										<label for="run.id" class="col-sm-2 control-label">run.id</label>
										<div class="col-sm-10">
											<input type="text" class="form-control" id="run.id" name="run.id">
										</div>
									</div>
									<div class="form-group">
										<label for="remote.servers" class="col-sm-2 control-label">remote.servers</label>
										<div class="col-sm-10">
											<input type="text" class="form-control" id="remote.servers" name="remote.servers">
										</div>
									</div>
									<div class="form-group">
										<label for="run.scenario.name" class="col-sm-2 control-label">run.scenario.name</label>
										<div class="col-sm-10">
											<select class="form-select" id="fake-scenario">
												<option value="fake-single">single</option>
												<option value="fake-rampup">rampup</option>
												<option value="fake-chain">chain</option>
											</select>
											<div class="submenu">
												<div id="fake-single">
													<label for="scenario.single.load">load</label>
													<select id="scenario.single.load" name="scenario.single.load">
														<option>create</option>
														<option>read</option>
														<option>update</option>
														<option>delete</option>
														<option>append</option>
													</select>
												</div>
												<div id="fake-rampup">
													<label for="scenario.chain.load">chain.load</label>
													<input type="text" class="form-control" id="scenario.chain.load" name="scenario.chain.load">
													<label for="scenario.rampup.thread.counts">thread.counts</label>
													<input type="text" class="form-control" id="scenario.rampup.thread.counts" name="scenario.rampup.thread.counts">
													<label for="scenario.rampup.sizes">sizes</label>
													<input type="text" class="form-control" id="scenario.rampup.sizes" name="scenario.rampup.sizes">
												</div>
												<div id="fake-chain">
													<label for="scenario.chain.load">load</label>
													<input type="text" class="form-control" id="scenario.chain.load" name="scenario.chain.load">
													<label for="scenario.chain.simultaneous">simultaneous</label>
													<select id="scenario.chain.simultaneous" name="scenario.chain.simultaneous">
														<option>true</option>
														<option>false</option>
													</select>
												</div>
											</div>
										</div>
									</div>

									<div class="form-group">
										<label for="run.request.retires" class="col-sm-2 control-label">run.request.retires</label>
										<div class="col-sm-10">
											<select class="form-select">
												<option>true</option>
												<option>false</option>
											</select>
										</div>
									</div>

									<div class="form-group">
										<label for="storage.addrs" class="col-sm-2 control-label">storage.addrs</label>
										<div class="col-sm-10">
											<input type="text" class="form-control" id="storage.addrs" name="storage.addrs">
										</div>
									</div>

									<div class="form-group">
										<label for="storage.api" class="col-sm-2 control-label">storage.api</label>
										<div class="col-sm-10">
											<select class="form-select">
												<option>s3</option>
												<option>atmos</option>
												<option>swift</option>
											</select>
											<div class="submenu">
												<div id="s3">
													<label for="api.s3.bucket">bucket</label>
													<input type="text" class="form-control" id="api.s3.bucket" name="api.s3.bucket">
												</div>
												<div id="atmos">
													<label for="api.atmos.subtenant">subtenant</label>
													<input type="text" class="form-control" id="api.atmos.subtenant" name="api.atmos.subtenant">
												</div>
												<div id="swift">

												</div>
											</div>
										</div>
									</div>
								</div>
							</form>
						</div>
						<div id="extended-config">
							<!-- List of breadcrumbs from JS -->
							<ol class="breadcrumb">

							</ol>

							<form class="form-horizontal" id="main-form" role="form">
								<input type="hidden" name="run.mode" id="run-mode" value="standalone">

								<!-- Input fields with labels from JS -->
								<div id="configuration-content">

								</div>
							</form>
						</div>
					</div>
				</div>
				<c:forEach var="mode" items="${sessionScope.runmodes}">
					<c:set var="correctMode" value="${fn:replace(mode, '.', '_')}"/>
					<div class="tab-pane table-pane" id="${correctMode}">
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
								<button type="button" class="default stop"><span>Stop</span></button>
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
	</body>
</html>