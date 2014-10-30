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
		<script type="text/javascript" src="js/jquery.cookie.js"></script>
	</head>
	<body>
		<nav class="navbar navbar-default" role="navigation">
			<div class="container-fluid">
				<div class="navbar-header">
					<a id="logo-image" href="/"><img src="images/vipr.png" width="50" height="50"></a>
					<a class="navbar-brand" href="/">Mongoose</a>
				</div>
				<div class="collapse navbar-collapse" id="bx-example-navbar-collapse-1">
					<ul class="nav navbar-nav">
						<li class="active"><a href="/">Run</a></li>
						<li><a href="driver.html">Driver</a></li>
						<li><a href="wsmock.html">WSMock</a></li>
					</ul>

					<ul class="nav navbar-nav navbar-right">
						<li><a href="about.html">About</a></li>
					</ul>
				</div>
			</div>
		</nav>

		<div class="content-wrapper">
			<div class="tabs-wrapper">
				<ul class="nav nav-tabs tabs" role="tablist">
					<li class="active"><a href="#configuration" data-toggle="tab">Configuration</a></li>
					<li><a href="#monitor" data-toggle="tab">Monitor</a></li>
				</ul>
			</div>

			<div class="tab-content">
				<div class="tab-pane active" id="configuration">
					<div class="runmodes">
						<button id="standalone" type="button" class="default">Standalone</button>
						<button id="distributed" type="button" class="default">Distributed</button>
					</div>
					<form id="mainForm">
						<input type="hidden" id="runmode" name="runmode" value="VALUE_RUN_MODE_STANDALONE">

						<div class="fixed-block">
							<fieldset class="scheduler-border-top">
								<legend class="scheduler-border">Run</legend>
								<label for="run-time">run.time:</label>
								<c:set var="runTime" value="${fn:split(runTimeConfig.runTime, '.')}" />
								<input type="text" name="runTime" id="runTime" class="form-control counter" value="${runTime[0]}">
								<select name="runTimeSelect">
									<option>seconds</option>
									<option>minutes</option>
									<option>hours</option>
									<option>days</option>
								</select>
								<br>
								<label for="run-metrics-period-sec">run.metrics.period.sec:</label>
								<input id="runMetricsPeriodSec" name="runMetricsPeriodSec" type="text" class="form-control counter" value="${runTimeConfig.runMetricsPeriodSec}">
							</fieldset>
						</div>

						<div class="fixed-block">
							<fieldset class="scheduler-border-top">
								<legend class="scheduler-border">Auth</legend>
								<label for="auth-id">auth.id:</label>
								<input id="authId" name="authId" type="text" class="form-control" value="${runTimeConfig.authId}">
								<br>
								<label for="auth-secret">auth.secret:</label>
								<input id="authSecret" name="authSecret" type="text" class="form-control" value="${runTimeConfig.authSecret}">
							</fieldset>
						</div>
						<br>

						<div class="storages-block">
							<fieldset class="scheduler-border">
								<legend class="scheduler-border">Storage</legend>
								<label>storage.api:</label>
								<select name="storageApi" id="storageApi">
									<option value="s3">S3</option>
									<option value="atmos">Atmos</option>
									<option value="swift">Swift</option>
								</select>
								<label>scheme:</label>
								<input id="scheme" name="scheme" type="text" class="form-control counter" value="${runTimeConfig.storageProto}">
								<fieldset class="scheduler-border">
									<legend class="scheduler-border">Data nodes</legend>
									<button type="button" class="default add-node">Add</button>
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
													<input id="dataNodes" name="dataNodes" type="checkbox" value="${addr}">
												</span>
												<label class="form-control">
													${addr}
												</label>
												<span class="input-group-btn">
													<button type="button" class="btn btn-default remove">Remove</button>
												</span>
											</div>
										</div>
									</c:forEach>
								</fieldset>
							</fieldset>

						</div>

						<div class="drivers-block">
							<fieldset class="scheduler-border">
								<legend class="scheduler-border">Remote</legend>
								<label>remote.monitor.port:</label>
								<input id="remoteMonitorPort" name="remoteMonitorPort" type="text" class="form-control counter" value="${runTimeConfig.remoteMonitorPort}">
								<fieldset class="scheduler-border">
									<legend class="scheduler-border">Drivers</legend>
									<button type="button" class="default add-driver">Add</button>
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
													<input id="drivers" name="drivers" type="checkbox" value="${server}">
												</span>
												<label class="form-control">
													${server}
												</label>
												<span class="input-group-btn">
													<button type="button" class="btn btn-default remove">Remove</button>
												</span>
											</div>
										</div>
									</c:forEach>

								</fieldset>
							</fieldset>
						</div>


						<div class="operations">
							<fieldset class="scheduler-border">
								<legend class="scheduler-border">Data</legend>
								<label for="count">data.count:</label>
								<input name="dataCount" id="dataCount" type="text" class="form-control counter" value="${runTimeConfig.dataCount}">
								<label>size.min</label>
								<input name="dataSizeMin" type="text" class="form-control counter" placeholder="min" value="${rt:getString(runTimeConfig, 'data.size.min')}">
								-
								<label>size.max</label>
								<input name="dataSizeMax" type="text" class="form-control counter" placeholder="max" value="${rt:getString(runTimeConfig, 'data.size.max')}">
								<br>

								<fieldset class="scheduler-border">
									<legend class="scheduler-border">Load</legend>
									<label>scenario.single.load</label>
									<select name="scenarioSingleLoad" id="scenarioSingleLoad">
										<option value="create">Create</option>
										<option value="read">Read</option>
										<option value="delete">Delete</option>
										<option value="update">Update</option>
										<option value="append">Append</option>
									</select>
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
											<label>load.thread</label>
											<input name="loadCreateThreads" type="text" class="form-control counter" value="${rt:getString(runTimeConfig, 'load.create.threads')}">
										</div>
										<div class="tab-pane" id="read">
											<label>load.thread</label>
											<input name="loadReadThreads" type="text" class="form-control counter" value="${rt:getString(runTimeConfig, 'load.read.threads')}">
											<label>verify.content</label>
											<input name="loadReadVerifyContent" type="text" class="form-control counter" value="${rt:getString(runTimeConfig, 'load.read.verify.content')}">
										</div>
										<div class="tab-pane" id="update">
											<label>load.thread</label>
											<input name="loadUpdateThreads" type="text" class="form-control counter" value="${rt:getString(runTimeConfig, 'load.update.threads')}">
											<label>load.per.item</label>
											<input name="loadUpdatePerItem" type="text" class="form-control counter" value="${rt:getString(runTimeConfig, 'load.update.per.item')}">
										</div>
										<div class="tab-pane" id="delete">
											<label>load.thread</label>
											<input name="loadDeleteThreads" type="text" class="form-control counter" value="${rt:getString(runTimeConfig, 'load.delete.threads')}">
										</div>
										<div class="tab-pane" id="append">
											<label>load.thread</label>
											<input name="loadAppendThreads" type="text" class="form-control counter" value="${rt:getString(runTimeConfig, 'load.append.threads')}">
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
										<label>api.port</label>
										<input name="apiS3Port" type="text" class="form-control counter" value="${rt:getString(runTimeConfig, 'api.s3.port')}">
										<label>api.auth.prefix</label>
										<input name="apiS3AuthPrefix" type="text" class="form-control counter" value="${rt:getString(runTimeConfig, 'api.s3.auth.prefix')}">
										<label>api.bucket</label>
										<input name="apiS3Bucket" type="text" class="form-control length-input" value="${rt:getString(runTimeConfig, 'api.s3.bucket')}">
									</div>
									<div class="tab-pane" id="atmos">
										<label>api.port</label>
										<input name="apiAtmosPort" type="text" class="form-control counter" value="${rt:getString(runTimeConfig, 'api.atmos.port')}">
										<label>api.subtenant</label>
										<input name="apiAtmosSubtenant" type="text" class="form-control length-input" value="${rt:getString(runTimeConfig, 'api.atmos.subtenant')}">
										<label>api.path.rest</label>
										<input name="apiAtmosPathRest" type="text" class="form-control counter" value="${rt:getString(runTimeConfig, 'api.atmos.path.rest')}">
										<label>api.interface</label>
										<input name="apiAtmosInterface" type="text" class="form-control counter" value="${rt:getString(runTimeConfig, 'api.atmos.interface')}">
									</div>
									<div class="tab-pane" id="swift">
										<label>api.port</label>
										<input name="apiSwiftPort" type="text" class="form-control counter" value="${rt:getString(runTimeConfig, 'api.swift.port')}">
									</div>
								</div>
							</fieldset>
						</div>

						<div class="start-wrapper">
							<button id="start" type="submit" class="default"><span>Start</span></button>
						</div>
					</form>
				</div>

				<div class="tab-pane" id="monitor">
					<div class="left-side">
						<div class="menu-wrapper">
							<div class="col-xs-8">
								<ul class="nav nav-tabs tabs-left">
									<li class="active"><a href="#messages-csv" data-toggle="tab">messages.csv</a></li>
									<li><a href="#errors-log" data-toggle="tab">errors.log</a></li>
									<li><a href="#perf-avg-csv" data-toggle="tab">perf.avg.csv</a></li>
									<li><a href="#perf-sum-csv" data-toggle="tab">perf.sum.csv</a></li>
								</ul>
							</div>
						</div>
					</div>

					<div class="right-side">
						<button id="stop" type="button" class="default"><span>Stop</span></button>
						<div class="log-wrapper">
							<div class="tab-content">
								<div class="tab-pane active" id="messages-csv">
									<table class="table">
										<thead>
											<tr>
												<th>Level</th>
												<th>LoggerName</th>
												<th>Marker</th>
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
								<div class="tab-pane" id="errors-log">
									<table class="table">
										<thead>
											<tr>
												<th>Level</th>
												<th>LoggerName</th>
												<th>Marker</th>
												<th>Message</th>
												<th>ThreadName</th>
												<th>TimeMillis</th>
											</tr>
										</thead>
										<tbody>

										</tbody>
									</table>
									<button type="button" class="default clear">Clear</button>
								</div>
								<div class="tab-pane" id="perf-avg-csv">
									<table class="table">
										<thead>
											<tr>
												<th>Level</th>
												<th>LoggerName</th>
												<th>Marker</th>
												<th>Message</th>
												<th>ThreadName</th>
												<th>TimeMillis</th>
											</tr>
										</thead>
										<tbody>

										</tbody>
									</table>
									<button type="button" class="default clear">Clear</button>
								</div>
								<div class="tab-pane" id="perf-sum-csv">
									<table class="table">
										<thead>
											<tr>
												<th>Level</th>
												<th>LoggerName</th>
												<th>Marker</th>
												<th>Message</th>
												<th>ThreadName</th>
												<th>TimeMillis</th>
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
			</div>
		</div>
	</body>
</html>
