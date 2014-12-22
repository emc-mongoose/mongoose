<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri='http://java.sun.com/jsp/jstl/core' prefix='c'%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="WEB-INF/property.tld" prefix="rt" %>
<!DOCTYPE html>
<html>
	<head>
		<meta charset="utf-8">
		<title>Mongoose-Run</title>
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
						<li><a href="charts.html">Charts</a></li>
					</ul>

					<p class="navbar-text navbar-right">v.${runTimeConfig.runVersion}</p>
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
								<option value="standalone">standalone</option>
								<option value="client">controller</option>
								<option value="server">driver</option>
								<option value="wsmock">wsmock</option>
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
						<div id="base">
							<form class="form-horizontal" role="form">
								<fieldset>
									<div class="standalone client">
										<legend>Auth</legend>
										<div class="form-group">
											<label for="fake-auth.id" class="col-sm-2 control-label">auth.id</label>
											<div class="col-sm-10">
												<input type="text" class="form-control" pointer="auth.id" id="fake-auth.id"
													value="${runTimeConfig.authId}" placeholder="Enter 'auth.id' property">
											</div>
										</div>

										<div class="form-group">
											<label for="fake-auth.secret" class="col-sm-2 control-label">auth.secret</label>
											<div class="col-sm-10">
												<input type="text" class="form-control" pointer="auth.secret" id="fake-auth.secret"
													value="${runTimeConfig.authSecret}" placeholder="Enter 'auth.secret' property">
											</div>
										</div>
									</div>
								</fieldset>

								<fieldset>
									<div class="standalone client wsmock">
										<legend>Storage</legend>
									</div>
									<div class="standalone client">
										<div class="form-group">
											<label for="fake-storage.addrs" class="col-sm-2 control-label">storage.addrs(data nodes)</label>
											<div class="col-sm-10">
												<input type="text" class="form-control" id="fake-storage.addrs"
													pointer="storage.addrs"
														value="${rt:getString(runTimeConfig, 'storage.addrs')}" placeholder="Enter 'storage.addrs' property">
											</div>
										</div>
									</div>

									<div class="standalone client wsmock">
										<div class="form-group">
											<label for="fake-storage.api" class="col-sm-2 control-label">storage.api</label>
											<div class="col-sm-10">
												<select class="form-select" id="fake-storage.api" pointer="storage.api">
													<option value="fake-${runTimeConfig.storageApi}">${runTimeConfig.storageApi}</option>
													<option value="fake-swift">swift</option>
													<option value="fake-s3">s3</option>
													<option value="fake-atmos">atmos</option>
												</select>
												<br/>
												<button id="api-button" type="button" class="btn btn-primary" data-toggle="modal" data-target="#fake-${runTimeConfig.storageApi}">
													More...
												</button>

												<div class="modal fade" id="fake-s3" tabindex="-1" role="dialog" aria-labelledby="s3Label"
													 aria-hidden="true">
													<div class="modal-dialog">
														<div class="modal-content">
															<div class="modal-header">
																<button type="button" class="close" data-dismiss="modal">
																	<span aria-hidden="true">&times;</span>
																	<span class="sr-only">Close</span>
																</button>
																<h4 class="modal-title" id="s3Label">S3</h4>
															</div>

															<div class="modal-body">
																<div class="form-group">
																	<label for="fake-api.s3.bucket" class="col-sm-4 control-label">api.s3.bucket</label>
																	<div class="col-sm-8">
																		<input type="text" class="form-control" id="fake-api.s3.bucket"
																			pointer="api.s3.bucket"
																				value="${rt:getString(runTimeConfig, 'api.s3.bucket')}"
																					placeholder="Enter 'api.s3.bucket' property">
																	</div>
																</div>
															</div>

															<div class="modal-footer">
																<button type="button" class="btn btn-default" data-dismiss="modal">Ok</button>
															</div>
														</div>
													</div>
												</div>

												<div class="modal fade" id="fake-swift" tabindex="-1" role="dialog" aria-labelledby="swiftLabel"
													aria-hidden="true">
													<div class="modal-dialog">
														<div class="modal-content">
															<div class="modal-header">
																<button type="button" class="close" data-dismiss="modal">
																	<span aria-hidden="true">&times;</span>
																	<span class="sr-only">Close</span>
																</button>
																<h4 class="modal-title" id="swiftLabel">Swift</h4>
															</div>

															<div class="modal-body">

															</div>

															<div class="modal-footer">
																<button type="button" class="btn btn-default" data-dismiss="modal">Ok</button>
															</div>
														</div>
													</div>
												</div>
											</div>

											<div class="modal fade" id="fake-atmos" tabindex="-1" role="dialog" aria-labelledby="atmosLabel"
												aria-hidden="true">
												<div class="modal-dialog">
													<div class="modal-content">
														<div class="modal-header">
															<button type="button" class="close" data-dismiss="modal">
																<span aria-hidden="true">&times;</span>
																<span class="sr-only">Close</span>
															</button>
															<h4 class="modal-title" id="atmosLabel">Atmos</h4>
														</div>

														<div class="modal-body">

														</div>

														<div class="modal-footer">
															<button type="button" class="btn btn-default" data-dismiss="modal">Ok</button>
														</div>
													</div>
												</div>
											</div>
										</div>
									</div>
								</fieldset>

								<fieldset>
									<div class="client">
										<legend>Controller</legend>
										<div class="form-group">
											<label for="fake-remote.servers" class="col-sm-2 control-label">remote.servers(drivers)</label>
											<div class="col-sm-10">
												<input type="text" class="form-control" id="fake-remote.servers" pointer="remote.servers"
													value="${rt:getString(runTimeConfig, 'remote.servers')}" placeholder="Enter 'remote.servers' property">
											</div>
										</div>
									</div>
								</fieldset>

								<fieldset>
									<div class="standalone client">
										<legend>Data</legend>
										<div class="form-group">
											<label for="fake-data" class="col-sm-2 control-label">data</label>
											<div class="col-sm-10">
												<select id="fake-data" class="form-select">
													<option>time</option>
													<option>objects</option>
												</select>
											</div>
										</div>

										<div id="objects" class="form-group">
											<label for="fake-data.count" class="col-sm-2 control-label">data.count</label>
											<div class="col-sm-10">
												<input type="text" class="form-control" id="fake-data.count" pointer="data.count"
													value="${runTimeConfig.dataCount}" placeholder="Enter 'data.count' property">
											</div>
										</div>

										<div id="time" class="form-group complex">
											<c:set var="runTimeArray" value="${fn:split(runTimeConfig.runTime, '.')}"/>
											<label for="fake-run.time" class="col-sm-2 control-label">run.time</label>
											<div class="col-sm-10">
												<input id="fake-run.time.input" type="text" class="form-control pre-select" value="${runTimeArray[0]}">
												<select class="form-select" id="fake-run.time.select">
													<option>${runTimeArray[1]}</option>
													<option>days</option>
													<option>hours</option>
													<option>minutes</option>
													<option>seconds</option>
												</select>
											</div>
											<input id="fake-run.time" type="hidden" class="form-control" pointer="run.time" value="${runTimeArray[0]}.${runTimeArray[1]}">
										</div>

										<div class="form-group">
											<label for="fake-data.size.min" class="col-sm-2 control-label">data.size.min</label>
											<div class="col-sm-10">
												<input type="text" class="form-control" id="fake-data.size.min" pointer="data.size.min"
													value="${rt:getString(runTimeConfig, 'data.size.min')}" placeholder="Enter 'data.size.min' property">
											</div>
										</div>

										<div class="form-group">
											<label for="fake-data.size.max" class="col-sm-2 control-label">data.size.max</label>
											<div class="col-sm-10">
												<input type="text" class="form-control" id="fake-data.size.max" pointer="data.size.max"
													value="${rt:getString(runTimeConfig, 'data.size.max')}" placeholder="Enter 'data.size.max' property">
											</div>
										</div>

										<div class="form-group">
											<label for="fake-data.src.fpath" class="col-sm-2 control-label">data.src.fpath</label>
											<div class="col-sm-10">
												<input type="text" class="form-control" id="fake-data.src.fpath" pointer="data.src.fpath"
													value="${rt:getString(runTimeConfig, 'data.src.fpath')}"
													placeholder="Enter relative path to the list of objects on remote host. Format: log/<run.mode>/<run.id>/<filename>">
											</div>
										</div>
									</div>
								</fieldset>

								<fieldset>
									<div class="standalone client wsmock server">
										<legend>Run</legend>
										<div class="form-group">
											<label for="fake-run.id" class="col-sm-2 control-label">run.id</label>
											<div class="col-sm-10">
												<input type="text" class="form-control" id="fake-run.id" pointer="run.id"
													placeholder="Enter 'run.id' property. For example, ${runTimeConfig.runId}">
											</div>
										</div>
									</div>

									<div class="standalone client">
										<div class="form-group">
											<label for="fake-run.scenario.name" class="col-sm-2 control-label">run.scenario.name</label>
											<div class="col-sm-10">
												<select id="fake-run.scenario.name" class="form-select" pointer="run.scenario.name">
													<option value="fake-${runTimeConfig.runScenarioName}">${runTimeConfig.runScenarioName}</option>
													<option value="fake-single">single</option>
													<option value="fake-chain">chain</option>
													<option value="fake-rampup">rampup</option>
												</select>
												<br/>

												<button id="scenario-button" type="button" class="btn btn-primary" data-toggle="modal"
													data-target="#fake-${runTimeConfig.runScenarioName}">
														More...
												</button>

												<div class="modal fade" id="fake-single" tabindex="-1" role="dialog" aria-labelledby="singleLabel"
													 aria-hidden="true">
													<div class="modal-dialog">
														<div class="modal-content">
															<div class="modal-header">
																<button type="button" class="close" data-dismiss="modal">
																	<span aria-hidden="true">&times;</span>
																	<span class="sr-only">Close</span>
																</button>
																<h4 class="modal-title" id="singleLabel">Single</h4>
															</div>

															<div class="modal-body">
																<div class="form-group">
																	<label for="fake-scenario.single.load"
																		   class="col-sm-6 control-label">scenario.single.load</label>
																	<div class="col-sm-6">
																		<select id="fake-scenario.single.load" class="form-select" pointer="scenario.single.load">
																			<option value="fake-${rt:getString(runTimeConfig, 'scenario.single.load')}">
																				${rt:getString(runTimeConfig, 'scenario.single.load')}
																			</option>
																			<option value="fake-create">create</option>
																			<option value="fake-read">read</option>
																			<option value="fake-update">update</option>
																			<option value="fake-delete">delete</option>
																			<option value="fake-append">append</option>
																		</select>
																	</div>
																</div>

																<hr/>

																<div id="fake-create">
																	<fieldset>
																		<legend>Create</legend>
																		<div class="form-group">
																			<label class="col-sm-6 control-label" for="fake-load.create.threads">
																				load.create.threads
																			</label>
																			<div class="col-sm-6">
																				<input type="text" class="form-control" pointer="load.create.threads"
																					id="fake-load.create.threads" value="${rt:getString(runTimeConfig, 'load.create.threads')}"
																						placeholder="Enter 'load.create.threads' property">
																			</div>
																		</div>
																	</fieldset>
																</div>

																<div id="fake-read">
																	<fieldset>
																		<legend>Read</legend>
																		<div class="form-group">
																			<label class="col-sm-6 control-label" for="fake-load.read.threads">
																				load.read.threads
																			</label>
																			<div class="col-sm-6">
																				<input type="text" class="form-control" pointer="load.read.threads"
																					id="fake-load.read.threads" value="${rt:getString(runTimeConfig, 'load.read.threads')}"
																						placeholder="Enter 'load.read.threads' property">
																			</div>
																		</div>

																		<div class="form-group">
																			<label for="fake-load.read.verify.content"
																				   class="col-sm-6 control-label">load.read.verify.content</label>
																			<div class="col-sm-6">
																				<select id="fake-load.read.verify.content" class="form-select" pointer="load.read.verify.content">
																					<option>${rt:getString(runTimeConfig, 'load.read.verify.content')}</option>
																					<option>true</option>
																					<option>false</option>
																				</select>
																			</div>
																		</div>
																	</fieldset>
																</div>

																<div id="fake-update">
																	<fieldset>
																		<legend>Update</legend>
																		<div class="form-group">
																			<label class="col-sm-6 control-label" for="fake-load.update.threads">
																				load.update.threads
																			</label>
																			<div class="col-sm-6">
																				<input type="text" class="form-control"
																					id="fake-load.update.threads" value="${rt:getString(runTimeConfig, 'load.update.threads')}"
																						pointer="load.update.threads"
																							placeholder="Enter 'load.update.threads' property">
																			</div>
																		</div>

																		<div class="form-group">
																			<label class="col-sm-6 control-label" for="fake-load.update.per.item">
																				load.update.per.item
																			</label>
																			<div class="col-sm-6">
																				<input type="text" class="form-control"
																					id="fake-load.update.per.item" value="${rt:getString(runTimeConfig, 'load.update.per.item')}"
																						pointer="load.update.per.item"
																							placeholder="Enter 'load.update.per.item' property">
																			</div>
																		</div>
																	</fieldset>
																</div>

																<div id="fake-delete">
																	<fieldset>
																		<legend>Delete</legend>
																		<div class="form-group">
																			<label class="col-sm-6 control-label" for="fake-load.delete.threads">load.delete.threads</label>
																			<div class="col-sm-6">
																				<input type="text" class="form-control"
																					id="fake-load.delete.threads" value="${rt:getString(runTimeConfig, 'load.delete.threads')}"
																						pointer="load.delete.threads"
																							placeholder="Enter 'load.delete.threads' property">
																			</div>
																		</div>
																	</fieldset>
																</div>

																<div id="fake-append">
																	<fieldset id="fake-append">
																		<legend>Append</legend>
																		<div class="form-group">
																			<label class="col-sm-6 control-label" for="fake-load.append.threads">load.append.threads</label>
																			<div class="col-sm-6">
																				<input type="text" class="form-control"
																					id="fake-load.append.threads" value="${rt:getString(runTimeConfig, 'load.append.threads')}"
																						pointer="load.append.threads"
																							placeholder="Enter 'load.append.threads' property">
																			</div>
																		</div>
																	</fieldset>
																</div>
															</div>

															<div class="modal-footer">
																<button type="button" class="btn btn-default" data-dismiss="modal">Ok</button>
															</div>
														</div>
													</div>
												</div>

												<div class="modal fade" id="fake-chain" tabindex="-1" role="dialog" aria-labelledby="chainLabel"
													 aria-hidden="true">
													<div class="modal-dialog">
														<div class="modal-content">
															<div class="modal-header">
																<button type="button" class="close" data-dismiss="modal">
																	<span aria-hidden="true">&times;</span>
																	<span class="sr-only">Close</span>
																</button>
																<h4 class="modal-title" id="chainLabel">Chain</h4>
															</div>

															<div class="modal-body">
																<div class="form-group">
																	<label for="fake-scenario.chain.load" class="col-sm-6 control-label">
																		scenario.chain.load
																	</label>
																	<div class="col-sm-6">
																		<input type="text" class="form-control" id="fake-scenario.chain.load"
																			value="${rt:getString(runTimeConfig, 'scenario.chain.load')}"
																				pointer="scenario.chain.load"
																					placeholder="Enter 'scenario.chain.load' property">
																	</div>
																</div>

																<div role="tabpanel">
																	<ul class="nav nav-tabs" role="tablist">
																		<li role="presentation" class="active">
																			<a href="#faketab-create" aria-controls="faketab-create" role="tab" data-toggle="tab">
																				Create
																			</a>
																		</li>
																		<li role="presentation">
																			<a href="#faketab-read" aria-controls="faketab-read" role="tab" data-toggle="tab">
																				Read
																			</a>
																		</li>
																		<li role="presentation">
																			<a href="#faketab-update" aria-controls="faketab-update" role="tab" data-toggle="tab">
																				Update
																			</a>
																		</li>
																		<li role="presentation">
																			<a href="#faketab-delete" aria-controls="faketab-delete" role="tab" data-toggle="tab">
																				Delete
																			</a>
																		</li>
																		<li role="presentation">
																			<a href="#faketab-append" aria-controls="faketab-append" role="tab" data-toggle="tab">
																				Append
																			</a>
																		</li>
																	</ul>

																	<div class="tab-content modal-tabs">
																		<div role="tabpanel" class="tab-pane active" id="faketab-create">
																			<div class="form-group">
																				<label class="col-sm-6 control-label" for="faketab-load.create.threads">load.create.threads</label>
																				<div class="col-sm-6">
																					<input type="text" class="form-control" id="faketab-load.create.threads"
																						value="${rt:getString(runTimeConfig, 'load.create.threads')}"
																							pointer="load.create.threads"
																								placeholder="Enter 'load.create.threads' property">
																				</div>
																			</div>
																		</div>
																		<div role="tabpanel" class="tab-pane" id="faketab-read">
																			<div class="form-group">
																				<label class="col-sm-6 control-label" for="faketab-load.read.threads">load.read.threads</label>
																				<div class="col-sm-6">
																					<input type="text" class="form-control" id="faketab-load.read.threads"
																						value="${rt:getString(runTimeConfig, 'load.read.threads')}"
																							pointer="load.read.threads"
																								placeholder="Enter 'load.read.threads' property">
																				</div>
																			</div>

																			<div class="form-group">
																				<label for="faketab-load.read.verify.content"
																					   class="col-sm-6 control-label">load.read.verify.content</label>
																				<div class="col-sm-6">
																					<select id="faketab-load.read.verify.content" class="form-select" pointer="load.read.verify.content">
																						<option>${rt:getString(runTimeConfig, 'load.read.verify.content')}</option>
																						<option>true</option>
																						<option>false</option>
																					</select>
																				</div>
																			</div>
																		</div>
																		<div role="tabpanel" class="tab-pane" id="faketab-update">
																			<div class="form-group">
																				<label class="col-sm-6 control-label" for="faketab-load.update.threads">load.update.threads</label>
																				<div class="col-sm-6">
																					<input type="text" class="form-control" id="faketab-load.update.threads"
																						value="${rt:getString(runTimeConfig, 'load.update.threads')}"
																							pointer="load.update.threads"
																								placeholder="Enter 'load.update.threads' property">
																				</div>
																			</div>

																			<div class="form-group">
																				<label class="col-sm-6 control-label" for="faketab-load.update.per.item">load.update.per.item</label>
																				<div class="col-sm-6">
																					<input type="text" class="form-control" id="faketab-load.update.per.item"
																						value="${rt:getString(runTimeConfig, 'load.update.per.item')}"
																							pointer="load.update.per.item"
																								placeholder="Enter 'load.update.per.item' property">
																				</div>
																			</div>
																		</div>
																		<div role="tabpanel" class="tab-pane" id="faketab-delete">
																			<div class="form-group">
																				<label class="col-sm-6 control-label" for="faketab-load.delete.threads">load.delete.threads</label>
																				<div class="col-sm-6">
																					<input type="text" class="form-control" id="faketab-load.delete.threads"
																						value="${rt:getString(runTimeConfig, 'load.delete.threads')}"
																							pointer="load.delete.threads"
																								placeholder="Enter 'load.delete.threads' property">
																				</div>
																			</div>
																		</div>
																		<div role="tabpanel" class="tab-pane" id="faketab-append">
																			<div class="form-group">
																				<label class="col-sm-6 control-label" for="fake-load.append.threads">load.append.threads</label>
																				<div class="col-sm-6">
																					<input type="text" class="form-control" id="fake-load.append.threads"
																						value="${rt:getString(runTimeConfig, 'load.append.threads')}"
																							pointer="load.append.threads"
																								placeholder="Enter 'load.append.threads' property">
																				</div>
																			</div>
																		</div>
																	</div>
																</div>

																<hr/>

																<div class="form-group">
																	<label for="fake-scenario.chain.simultaneous" class="col-sm-6 control-label">
																		scenario.chain.simultaneous
																	</label>
																	<div class="col-sm-6">
																		<select class='form-select' pointer="scenario.chain.simultaneous">
																			<option>${rt:getString(runTimeConfig, 'scenario.chain.simultaneous')}</option>
																			<option>true</option>
																			<option>false</option>
																		</select>
																	</div>
																</div>
															</div>

															<div class="modal-footer">
																<button type="button" class="btn btn-default" data-dismiss="modal">Ok</button>
															</div>
														</div>
													</div>
												</div>

												<div class="modal fade" id="fake-rampup" tabindex="-1" role="dialog" aria-labelledby="rampupLabel"
													 aria-hidden="true">
													<div class="modal-dialog">
														<div class="modal-content">
															<div class="modal-header">
																<button type="button" class="close" data-dismiss="modal">
																	<span aria-hidden="true">&times;</span>
																	<span class="sr-only">Close</span>
																</button>
																<h4 class="modal-title" id="rampupLabel">Rampup</h4>
															</div>

															<div class="modal-body">

																<div class="form-group">
																	<label class="col-sm-6 control-label" for="chain-button">
																		You need to configure chain.load
																	</label>
																	<div class="col-sm-6">
																		<button type="button" class="btn btn-default" id="chain-load">
																			Configure
																		</button>
																	</div>
																</div>
																<div class="form-group">
																	<label for="fake-scenario.rampup.thread.counts" class="col-sm-4 control-label">
																		thread.counts
																	</label>
																	<div class="col-sm-8">
																		<input type="text" class="form-control" id="fake-scenario.rampup.thread.counts"
																			value="${rt:getString(runTimeConfig, 'scenario.rampup.thread.counts')}"
																				pointer="scenario.rampup.thread.counts"
																					placeholder="Enter 'scenario.rampup.thread.counts' property">
																	</div>
																</div>

																<div class="form-group">
																	<label for="fake-scenario.rampup.sizes" class="col-sm-4 control-label">
																		load.rampup.sizes
																	</label>
																	<div class="col-sm-8">
																		<input type="text" class="form-control" id="fake-scenario.rampup.sizes"
																			value="${rt:getString(runTimeConfig, 'scenario.rampup.sizes')}"
																				pointer="scenario.rampup.sizes"
																					placeholder="Enter 'scenario.rampup.sizes' property">
																	</div>
																</div>
															</div>

															<div class="modal-footer">
																<button type="button" class="btn btn-default" data-dismiss="modal">Ok</button>
															</div>
														</div>
													</div>
												</div>
											</div>
										</div>

										<div class="form-group">
											<label for="fake-run.request.retries" class="col-sm-2 control-label">run.request.retries</label>
											<div class="col-sm-10">
												<select id="fake-run.request.retries" class="form-select" pointer="run.request.retries">
													<option>${rt:getString(runTimeConfig, "run.request.retries")}</option>
													<option>true</option>
													<option>false</option>
												</select>
											</div>
										</div>
									</div>
								</fieldset>
							</form>
						</div>
						<div id="extended">
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