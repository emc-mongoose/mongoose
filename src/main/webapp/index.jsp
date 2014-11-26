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
	</head>
	<body>
		<nav class="navbar navbar-default" role="navigation">
			<div class="container-fluid">
				<div class="navbar-header">
					<a id="logo-image" href="/"><img src="images/logo.jpg" width="50" height="50"></a>
					<a class="navbar-brand" href="/">Mongoose</a>
				</div>
				<div class="collapse navbar-collapse" id="bx-example-navbar-collapse-1">
					<ul class="nav navbar-nav">
						<li class="active"><a href="/">Run</a></li>
					</ul>

					<ul class="nav navbar-nav navbar-right">
						<li><a href="about.html">About</a></li>
					</ul>
				</div>
			</div>
		</nav>

		<div class="content-wrapper">
			<div class="tabs-wrapper">
				<ul class="nav nav-tabs tabs header-tabs" role="tablist">
					<li class="active"><a href="#configuration" data-toggle="tab">Configuration</a></li>
					<c:forEach var="mode" items="${sessionScope.runmodes}">
						<c:set var="correctMode" value="${fn:replace(mode, '.', '_')}"/>
						<li><a href="#${correctMode}" data-toggle="tab">${mode}<span class="glyphicon glyphicon-remove" value="${correctMode}"></span></a></li>
					</c:forEach>
				</ul>
			</div>

			<div class="tab-content header-tab-content">
				<div class="tab-pane active" id="configuration">
					<form id="mainForm">

						<select name="run.mode">
							<option>standalone</option>
							<option>distributed</option>
							<option>server</option>
							<option>wsmock</option>
						</select>

						<button id="start" type="submit" class="button default">Start</button>

						<div class="fixed-block resize">
							<fieldset class="scheduler-border-top">
								<legend class="scheduler-border">Auth</legend>
								<label for="auth.id">auth.id:</label>
								<input id="auth.id" name="authId" type="text" class="form-control" value="${runTimeConfig.authId}">
								<br>
								<label for="auth.secret">auth.secret:</label>
								<input id="auth.secret" name="auth.secret" type="text" class="form-control" value="${runTimeConfig.authSecret}">
							</fieldset>
						</div>

						<div class="storages-block resize">
							<fieldset class="scheduler-border">
								<legend class="scheduler-border">Storage</legend>
								<label>storage.api:</label>
								<select name="storage.api" id="storage.api">
									<option selected="selected">${runTimeConfig.storageApi}</option>
									<option>s3</option>
									<option>atmos</option>
									<option>swift</option>
								</select>
								<label>scheme:</label>
								<input id="storage.scheme" name="storage.scheme" type="text" class="form-control counter" value="${runTimeConfig.storageProto}">
								<fieldset class="scheduler-border">
									<legend class="scheduler-border">Data nodes</legend>
									<button type="button" class="default add-node">Add</button>
									<button type="button" class="default remove remove-node">Remove</button>
									<div class="input-group data-node">
										<input id="data-node-text" type="text" class="form-control" placeholder="Enter data node">
										<span class="input-group-btn">
											<button id="save" type="button" class="btn btn-default">Save</button>
										</span>
									</div>

									<c:forEach var="addr" items="${runTimeConfig.storageAddrs}">
										<div class="storages">
											<div class="input-group">
												<span class="input-group-addon">
													<input class="storage.addrs" type="checkbox">
												</span>
												<label class="form-control">
													<input type="hidden" name="storage.addrs" value="${addr}">
													${addr}
												</label>
											</div>
										</div>
									</c:forEach>
								</fieldset>
							</fieldset>
						</div>

						<div class="fixed-block run-limits resize">
							<fieldset class="scheduler-border-top">
								<legend class="scheduler-border">Run Limits</legend>
								<div class="tabs-wrapper">
									<ul id="run-parameters" class="nav nav-tabs" role="tablist">
										<li class="active"><a href="#runTimeTab" data-toggle="tab">run.time</a></li>
										<li><a href="#dataCountTab" data-toggle="tab">data.count</a></li>
									</ul>
								</div>
								<div class="tab-content">
									<div class="tab-pane active" id="runTimeTab">
										<label for="run.time">run.time:</label>
										<c:set var="runTime" value="${fn:split(runTimeConfig.runTime, '.')}" />
										<input type="text" name="run.time" id="run.time" class="form-control counter" value="${runTime[0]}">
										<select id="runTimeSelect" name="run.time">
											<option selected="selected">${runTime[1]}</option>
											<option>seconds</option>
											<option>minutes</option>
											<option>hours</option>
											<option>days</option>
										</select>
									</div>
									<div class="tab-pane" id="dataCountTab">
										<label for="count">data.count:</label>
										<input name="data.count" id="data.count" type="text" class="form-control counter" value="${runTimeConfig.dataCount}">
									</div>
								</div>
								<label for="run.id">run.id:</label>
								<input id="run.id" name="run.id" type="text" class="form-control" placeholder="${runTimeConfig.runId}">
								<br>
								<label for="run.metrics.period.sec">run.metrics.period.sec:</label>
								<input id="run.metrics.period.sec" name="run.metrics.period.sec" type="text" class="form-control counter" value="${runTimeConfig.runMetricsPeriodSec}">
							</fieldset>
						</div>

						<div class="operations">
							<fieldset class="scheduler-border">
								<legend class="scheduler-border">Data</legend>
								<label>size.min</label>
								<input name="data.size.min" type="text" class="form-control sizes" placeholder="min" value="${rt:getString(runTimeConfig, 'data.size.min')}">
								-
								<label>size.max</label>
								<input name="data.size.max" type="text" class="form-control sizes" placeholder="max" value="${rt:getString(runTimeConfig, 'data.size.max')}">
								<br>

								<fieldset class="scheduler-border">
									<legend class="scheduler-border">Scenario</legend>
									<label>run.scenario.name</label>
									<select name="run.scenario.name" id="run.scenario.name">
										<option selected="selected">${rt:getString(runTimeConfig, 'run.scenario.name')}</option>
										<option>chain</option>
										<option>rampup</option>
										<option>single</option>
									</select>
									<div class="tabs-wrapper">
										<ul id="scenarioTab" class="nav nav-tabs" role="tablist">
											<li class="active"><a href="#single" data-toggle="tab">Single</a></li>
											<li><a href="#chain" data-toggle="tab">Chain</a></li>
											<li><a href="#rampup" data-toggle="tab">Rampup</a></li>
										</ul>
									</div>

									<div class="tab-content">
										<div class="tab-pane active" id="single">
											<label>scenario.single.load</label>
											<select name="scenario.single.load" id="scenario.single.load">
												<option selected="selected">${rt:getString(runTimeConfig, 'scenario.single.load')}</option>
												<option>create</option>
												<option>read</option>
												<option>delete</option>
												<option>update</option>
												<option>append</option>
											</select>
										</div>
										<div class="tab-pane" id="chain">
											<div class="labels">
												<label>scenario.chain.load</label>
												<label>scenario.chain.simultaneous</label>
											</div>
											<div class="properties resize">
												<input name="scenario.chain.load" id="scenario.chain.load" type="text" class="form-control" value="${rt:getString(runTimeConfig, 'scenario.chain.load')}">
												<select name="scenario.chain.simultaneous" id="scenario.chain.simultaneous">
													<option selected="selected">${rt:getString(runTimeConfig, 'scenario.chain.simultaneous')}</option>
													<option>false</option>
													<option>true</option>
												</select>
											</div>
										</div>
										<div class="tab-pane" id="rampup">
											<div class="labels">
												<label>scenario.rampup.thread.counts</label>
												<label>scenario.rampup.sizes</label>
												<label>operations(scenario.chain.load)</label>
											</div>
											<div class="properties resize">
												<input id="scenario.rampup.thread.counts" name="scenario.rampup.thread.counts" type="text" class="form-control" value="${rt:getString(runTimeConfig, 'scenario.rampup.thread.counts')}">
                                                <input id="scenario.rampup.sizes" name="scenario.rampup.sizes" type="text" class="form-control" value="${rt:getString(runTimeConfig, 'scenario.rampup.sizes')}">
                                                <input id="scenario.chain.load.duplicate" type="text" class="form-control" value="${rt:getString(runTimeConfig, 'scenario.chain.load')}">
											</div>
										</div>
									</div>
								</fieldset>

								<fieldset class="scheduler-border">
									<legend class="scheduler-border">Load</legend>
									<div class="labels">
										<label>data.src.fpath</label>
									</div>
									<div class="properties resize">
										<input id="data.src.fpath" name="data.src.fpath" class="form-control" type="text" placeholder="log/<run.mode>/<run.id>/<filename>" value="${rt:getString(runTimeConfig, 'data.src.fpath')}">
									</div>
									<div class="tabs-wrapper">
										<ul id="loadTab" class="nav nav-tabs" role="tablist">
											<li class="active"><a href="#create" data-toggle="tab">Create</a></li>
											<li><a href="#read" data-toggle="tab">Read</a></li>
											<li><a href="#update" data-toggle="tab">Update</a></li>
											<li><a href="#delete" data-toggle="tab">Delete</a></li>
											<li><a href="#append" data-toggle="tab">Append</a></li>
										</ul>
									</div>

									<div class="tab-content">
										<div class="tab-pane active" id="create">
											<label>load.create.threads</label>
											<input id="load.create.threads" name="load.create.threads" type="text" class="form-control sizes" value="${rt:getString(runTimeConfig, 'load.create.threads')}">
										</div>
										<div class="tab-pane" id="read">
											<div class="labels">
												<label>load.read.threads</label>
												<label>load.read.verify.content</label>
											</div>
											<div class="properties">
												<input id="load.read.threads" name="load.read.threads" type="text" class="form-control sizes" value="${rt:getString(runTimeConfig, 'load.read.threads')}">
												<br>
												<input id="load.read.verify.content" name="load.read.verify.content" type="text" class="form-control sizes" value="${rt:getString(runTimeConfig, 'load.read.verify.content')}">
											</div>
										</div>
										<div class="tab-pane" id="update">
											<div class="labels">
												<label>load.update.threads</label>
												<label>load.update.per.item</label>
											</div>
											<div class="properties">
												<input id="load.update.threads" name="load.update.threads" type="text" class="form-control sizes" value="${rt:getString(runTimeConfig, 'load.update.threads')}">
												<br>
												<input id="load.update.per.item" name="load.update.per.item" type="text" class="form-control sizes" value="${rt:getString(runTimeConfig, 'load.update.per.item')}">
											</div>


										</div>
										<div class="tab-pane" id="delete">
											<label>load.delete.threads</label>
											<input id="load.delete.threads" name="load.delete.threads" type="text" class="form-control sizes" value="${rt:getString(runTimeConfig, 'load.delete.threads')}">
										</div>
										<div class="tab-pane" id="append">
											<label>load.append.threads</label>
											<input id="load.append.threads" name="load.append.threads" type="text" class="form-control sizes" value="${rt:getString(runTimeConfig, 'load.append.threads')}">
										</div>
									</div>
								</fieldset>
							</fieldset>

						</div>

						<div class="interfaces">
							<fieldset class="scheduler-border">
								<legend class="scheduler-border">API</legend>
								<div class="tabs-wrapper">
									<ul id="apiTab" class="nav nav-tabs crud-tabs" role="tablist">
										<li class="active"><a href="#s3" data-toggle="tab">S3</a></li>
										<li><a href="#atmos" data-toggle="tab">Atmos</a></li>
										<li><a href="#swift" data-toggle="tab">Swift</a></li>
									</ul>
								</div>

								<div class="tab-content">
									<div class="tab-pane active" id="s3">
										<div class="labels">
											<label>api.s3.port</label>
											<br>
											<label>api.s3.auth.prefix</label>
											<br>
											<label>api.s3.bucket</label>
										</div>
										<div class="properties resize">
											<input id="api.s3.port" name="api.s3.port" type="text" class="form-control" value="${rt:getString(runTimeConfig, 'api.s3.port')}">
											<input id="api.s3.auth.prefix" name="api.s3.auth.prefix" type="text" class="form-control" value="${rt:getString(runTimeConfig, 'api.s3.auth.prefix')}">
											<input id="api.s3.bucket" name="api.s3.bucket" type="text" class="form-control" value="${rt:getString(runTimeConfig, 'api.s3.bucket')}">
										</div>
									</div>
									<div class="tab-pane" id="atmos">
										<div class="labels">
											<label>api.atmos.port</label>
											<br>
											<label>api.atmos.subtenant</label>
											<br>
											<label>api.atmos.path.rest</label>
											<br>
											<label>api.atmos.interface</label>
										</div>
										<div class="properties resize">
											<input id="api.atmos.port" name="api.atmos.port" type="text" class="form-control" value="${rt:getString(runTimeConfig, 'api.atmos.port')}">
											<input id="api.atmos.subtenant" name="api.atmos.subtenant" type="text" class="form-control" value="${rt:getString(runTimeConfig, 'api.atmos.subtenant')}">
											<input id="api.atmos.path.rest" name="api.atmos.path.rest" type="text" class="form-control" value="${rt:getString(runTimeConfig, 'api.atmos.path.rest')}">
											<input id="api.atmos.interface" name="api.atmos.interface" type="text" class="form-control" value="${rt:getString(runTimeConfig, 'api.atmos.interface')}">
										</div>
									</div>
									<div class="tab-pane" id="swift">
										<label>api.swift.port</label>
										<input id="api.swift.port" name="api.swift.port" type="text" class="form-control counter" value="${rt:getString(runTimeConfig, 'api.swift.port')}">
									</div>
								</div>
							</fieldset>

							<div class="drivers-block resize">
								<fieldset class="scheduler-border">
									<legend class="scheduler-border">Remote</legend>
									<label>remote.import.port:</label>
									<input id="remote.import.port" name="remote.import.port" type="text" class="form-control counter" value="${runTimeConfig.remoteImportPort}">
									<fieldset class="scheduler-border">
										<legend class="scheduler-border">Drivers</legend>
										<button type="button" class="default add-driver">Add</button>
										<button type="button" class="default remove remove-driver">Remove</button>
										<div class="input-group driver">
											<input id="driver-text" type="text" class="form-control" placeholder="Enter driver">
											<span class="input-group-btn">
												<button id="save-driver" type="button" class="btn btn-default">Save</button>
											</span>
										</div>

										<c:forEach var="server" items="${runTimeConfig.remoteServers}">
											<div class="drivers">
												<div class="input-group">
													<span class="input-group-addon">
														<input class="remote.servers" type="checkbox">
													</span>
													<label class="form-control">
														<input type="hidden" name="remote.servers" value="${server}">
														${server}
													</label>
												</div>
											</div>
										</c:forEach>

									</fieldset>
									<br>
									<label>JMX Port(remote.export.port):</label>
									<input id="remote.export.port" name="remote.export.port" type="text" class="form-control counter" value="${runTimeConfig.remoteExportPort}">
								</fieldset>
							</div>
						</div>
					</form>
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
		<script type="text/javascript" src="js/jquery-2.1.0.min.js"></script>
		<script type="text/javascript" src="js/script.js"></script>
		<script type="text/javascript" src="js/bootstrap.min.js"></script>
		<script type="text/javascript" src="js/jquery.cookie.js"></script>
	</body>
</html>
