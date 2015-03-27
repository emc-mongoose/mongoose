<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri='http://java.sun.com/jsp/jstl/core' prefix='c'%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="WEB-INF/property.tld" prefix="rt" %>
<!DOCTYPE html>
<html>
	<head>
		<meta charset="utf-8">
		<title>Mongoose-Run</title>
		<link rel='stylesheet' href='webjars/bootstrap/3.3.2-1/css/bootstrap.min.css'>
		<link href="css/styles.css" rel="stylesheet">
	</head>
	<body>
		<!-- For waiting image -->
		<div id="wait">
			<!--<img src="images/ajax-loader.gif" alt="Loading">-->
		</div>
		<!-- -->
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
					<a id="logo" href="/">
						<img width="120" height="40" src="images/mongooselogo.svg"/>
					</a>
					<a class="navbar-brand" href="/">
						Mongoose
					</a>
				</div>
				<div class="collapse navbar-collapse"
					id="main-navbar">
					<ul class="nav navbar-nav">
						<li class="active"><a href="/">Run<span class="sr-only">(current)</span></a></li>
					</ul>

					<p class="navbar-text navbar-right">v${runTimeConfig.runVersion}</p>
				</div>
			</div>
		</nav>

		<div class="content-wrapper">
			<div class="tabs-wrapper">
				<ul class="nav nav-tabs" role="presentation">
					<li class="active"><a href="#configuration" data-toggle="tab">New run...</a></li>
					<c:forEach var="mode" items="${sessionScope.runmodes}">
						<c:set var="correctMode" value="${fn:replace(mode, '.', '_')}"/>
						<li>
							<a href="#scenarioTab-${correctMode}" data-toggle="tab">
									${mode}
								<span class="glyphicon glyphicon-remove kill" value="${mode}"></span>
							</a>
						</li>
					</c:forEach>
				</ul>
			</div>

			<div class="tab-content">
				<div id="configuration" class="tab-pane active">
					<div class="container-fluid">
						<div class="row">
							<div id="menu" class="col-xs-12 col-sm-5 col-md-4 col-lg-3 no-padding">
								<div id="run-modes">
									<div>
										<label for="backup-run.mode">Run mode</label>
										<select id="backup-run.mode" data-pointer="run.mode">
											<option value="standalone">standalone</option>
											<option value="client">load client</option>
											<option value="server">load server</option>
											<option value="cinderella">cinderella</option>
										</select>
									</div>
									<div>
										<button type="button" id="start" class="btn btn-success">
											Start
										</button>
									</div>
								</div>

								<div id="config">
									<div>
										<label for="config-type">Config mode</label>
										<select id="config-type">
											<option value="base">basic</option>
											<option value="extended">extended</option>
										</select>
									</div>
									<div class="save-buttons">
										<button class="btn btn-default" id="save-config" type="button">Save</button>
										<br/>
										<a href="/save" id="save-file" class="btn btn-default">Save As...</a>
									</div>
									<br/>
									<div id="file-visibility">
										<input id="file-checkbox" type="checkbox"/>
										<label for="file-checkbox">Read config from file</label>
									</div>
									<br/>
									<input id="config-file" type="file" accept=".txt"/>
								</div>

								<ul class="folders">

								</ul>
							</div>

							<div id="main-content" class="col-xs-12 col-sm-7 col-md-8 col-lg-9 no-padding">
								<div id="base">
									<form class="form-horizontal" role="form">
										<div class="standalone client cinderella server">
											<fieldset class="no-margin">
												<legend>Run</legend>
												<div class="form-group">
													<label for="backup-run.id" class="col-sm-3 control-label">Id</label>
													<div class="col-sm-9">
														<input type="text" id="backup-run.id" class="form-control"
														       data-pointer="run.id"/>
													</div>
												</div>
											</fieldset>

											<div class="standalone client">
												<div id="time" class="form-group complex">
													<label for="backup-load.limit.time.value" class="col-sm-3 control-label">
														Time limit
													</label>
													<div class="col-sm-9">
														<input type="text" id="backup-load.limit.time.value"
														       class="form-control pre-select"
														       value="${rt:getString(runTimeConfig, 'load.limit.time.value')}"
																data-pointer="load.limit.time.value"/>
														<select class="form-select" id="backup-load.limit.time.unit"
																data-pointer="load.limit.time.unit">
															<option>
																${rt:getString(runTimeConfig, 'load.limit.time.unit')}
															</option>
															<option>days</option>
															<option>hours</option>
															<option>minutes</option>
															<option>seconds</option>
														</select>
													</div>
												</div>

												<div class="form-group">
													<label for="backup-scenario.name" class="col-sm-3 control-label">
														The scenario to run
													</label>
													<div class="col-sm-9">
														<select id="backup-scenario.name" class="form-select" data-pointer="scenario.name">
															<option value="backup-${runTimeConfig.scenarioName}">
																${runTimeConfig.scenarioName}
															</option>
															<option value="backup-single">single</option>
															<option value="backup-chain">chain</option>
															<option value="backup-rampup">rampup</option>
														</select>
														<button type="button" id="scenario-button" class="btn btn-primary"
														        data-toggle="modal"
														        data-target="#backup-${runTimeConfig.scenarioName}">
															Details...
														</button>

														<i id="scenario-load"></i>

														<div class="modal fade" id="backup-single" tabindex="-1" role="dialog" aria-labelledby="singleLabel"
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
																			<label for="backup-scenario.type.single.load"
																			       class="col-sm-6 control-label">Load type</label>
																			<div class="col-sm-6">
																				<select id="backup-scenario.type.single.load" class="form-select"
																				        data-pointer="scenario.type.single.load">
																					<option value="backup-${rt:getString(runTimeConfig,
																						'scenario.type.single.load')}">
																						${rt:getString(runTimeConfig, 'scenario.type.single.load')}
																					</option>
																					<option value="backup-create">create</option>
																					<option value="backup-read">read</option>
																					<option value="backup-update">update</option>
																					<option value="backup-delete">delete</option>
																					<option value="backup-append">append</option>
																				</select>
																			</div>
																		</div>

																		<hr/>

																		<div id="backup-create">
																			<fieldset>
																				<legend>Create</legend>
																				<div class="form-group">
																					<label class="col-sm-6 control-label" for="backup-load.type.create.threads">
																						Load threads count
																					</label>
																					<div class="col-sm-6">
																						<input type="text" id="backup-load.type.create.threads"
																						       class="form-control"
																						       data-pointer="load.type.create.threads"
																						       value="${rt:getString(runTimeConfig, 'load.type.create.threads')}"/>
																					</div>
																				</div>
																			</fieldset>
																		</div>

																		<div id="backup-read">
																			<fieldset>
																				<legend>Read</legend>
																				<div class="form-group">
																					<label class="col-sm-6 control-label" for="backup-load.type.read.threads">
																						Load threads count
																					</label>
																					<div class="col-sm-6">
																						<input type="text" id="backup-load.type.read.threads"
																						       class="form-control"
																						       data-pointer="load.type.read.threads"
																						       value="${rt:getString(runTimeConfig, 'load.type.read.threads')}"/>
																					</div>
																				</div>

																				<div class="form-group">
																					<label for="backup-load.type.read.verifyContent"
																					       class="col-sm-6 control-label">Verify content</label>
																					<div class="col-sm-6">
																						<select id="backup-load.type.read.verifyContent" class="form-select"
																						        data-pointer="load.type.read.verifyContent">
																							<option>
																								${rt:getString(runTimeConfig, 'load.type.read.verifyContent')}
																							</option>
																							<option>true</option>
																							<option>false</option>
																						</select>
																					</div>
																				</div>
																			</fieldset>
																		</div>

																		<div id="backup-update">
																			<fieldset>
																				<legend>Update</legend>
																				<div class="form-group">
																					<label class="col-sm-6 control-label" for="backup-load.type.update.threads">
																						Load threads count
																					</label>
																					<div class="col-sm-6">
																						<input type="text" id="backup-load.type.update.threads"
																						       class="form-control"
																						       value="${rt:getString(runTimeConfig, 'load.type.update.threads')}"
																						       data-pointer="load.type.update.threads"/>
																					</div>
																				</div>

																				<div class="form-group">
																					<label class="col-sm-6 control-label" for="backup-load.type.update.perItem">
																						Update per item count
																					</label>
																					<div class="col-sm-6">
																						<input type="text" id="backup-load.type.update.perItem"
																						       class="form-control"
																						       value="${rt:getString(runTimeConfig, 'load.type.update.perItem')}"
																						       data-pointer="load.type.update.perItem"/>
																					</div>
																				</div>
																			</fieldset>
																		</div>

																		<div id="backup-delete">
																			<fieldset>
																				<legend>Delete</legend>
																				<div class="form-group">
																					<label class="col-sm-6 control-label" for="backup-load.type.delete.threads">
																						Load threads count
																					</label>
																					<div class="col-sm-6">
																						<input type="text" id="backup-load.type.delete.threads"
																						       class="form-control"
																						       value="${rt:getString(runTimeConfig, 'load.type.delete.threads')}"
																						       data-pointer="load.type.delete.threads"/>
																					</div>
																				</div>
																			</fieldset>
																		</div>

																		<div id="backup-append">
																			<fieldset>
																				<legend>Append</legend>
																				<div class="form-group">
																					<label class="col-sm-6 control-label" for="backup-load.type.append.threads">
																						Load threads count
																					</label>
																					<div class="col-sm-6">
																						<input type="text" id="backup-load.type.append.threads"
																						       class="form-control"
																						       value="${rt:getString(runTimeConfig, 'load.type.append.threads')}"
																						       data-pointer="load.type.append.threads"/>
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

														<div class="modal fade" id="backup-chain" tabindex="-1" role="dialog" aria-labelledby="chainLabel"
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
																			<label for="backup-scenario.type.chain.load" class="col-sm-6 control-label">
																				Load types
																			</label>
																			<div class="col-sm-6">
																				<input type="text" class="form-control" id="backup-scenario.type.chain.load"
																				       value="${rt:getString(runTimeConfig, 'scenario.type.chain.load')}"
																				       data-pointer="scenario.type.chain.load"/>
																			</div>
																		</div>

																		<div role="tabpanel">
																			<ul class="nav nav-tabs" role="tablist">
																				<li role="presentation" class="active">
																					<a href="#backuptab-create" aria-controls="backuptab-create"
																					   role="tab" data-toggle="tab">
																						Create
																					</a>
																				</li>
																				<li role="presentation">
																					<a href="#backuptab-read" aria-controls="backuptab-read"
																					   role="tab" data-toggle="tab">
																						Read
																					</a>
																				</li>
																				<li role="presentation">
																					<a href="#backuptab-update" aria-controls="backuptab-update"
																					   role="tab" data-toggle="tab">
																						Update
																					</a>
																				</li>
																				<li role="presentation">
																					<a href="#backuptab-delete" aria-controls="backuptab-delete"
																					   role="tab" data-toggle="tab">
																						Delete
																					</a>
																				</li>
																				<li role="presentation">
																					<a href="#backuptab-append" aria-controls="backuptab-append"
																					   role="tab" data-toggle="tab">
																						Append
																					</a>
																				</li>
																			</ul>

																			<div class="tab-content modal-tabs">
																				<div role="tabpanel" class="tab-pane active" id="backuptab-create">
																					<div class="form-group">
																						<label class="col-sm-6 control-label"
																						       for="backuptab-load.type.create.threads">
																							Load threads count
																						</label>
																						<div class="col-sm-6">
																							<input type="text" id="backuptab-load.type.create.threads"
																							       class="form-control"
																							       value="${rt:getString(runTimeConfig, 'load.type.create.threads')}"
																							       data-pointer="load.type.create.threads"/>
																						</div>
																					</div>
																				</div>
																				<div role="tabpanel" class="tab-pane" id="backuptab-read">
																					<div class="form-group">
																						<label class="col-sm-6 control-label" for="backuptab-load.type.read.threads">
																							Load threads count
																						</label>
																						<div class="col-sm-6">
																							<input type="text" id="backuptab-load.type.read.threads"
																							       class="form-control"
																							       value="${rt:getString(runTimeConfig, 'load.type.read.threads')}"
																							       data-pointer="load.type.read.threads"/>
																						</div>
																					</div>

																					<div class="form-group">
																						<label for="backuptab-load.type.read.verifyContent"
																						       class="col-sm-6 control-label">
																							Verfiy content
																						</label>
																						<div class="col-sm-6">
																							<select id="backuptab-load.type.read.verifyContent" class="form-select"
																							        data-pointer="load.type.read.verifyContent">
																								<option>
																									${rt:getString(runTimeConfig, 'load.type.read.verifyContent')}
																								</option>
																								<option>true</option>
																								<option>false</option>
																							</select>
																						</div>
																					</div>
																				</div>
																				<div role="tabpanel" class="tab-pane" id="backuptab-update">
																					<div class="form-group">
																						<label class="col-sm-6 control-label"
																						       for="backuptab-load.type.update.threads">
																							Load threads count
																						</label>
																						<div class="col-sm-6">
																							<input type="text" id="backuptab-load.type.update.threads"
																							       class="form-control"
																							       value="${rt:getString(runTimeConfig, 'load.type.update.threads')}"
																							       data-pointer="load.type.update.threads"/>
																						</div>
																					</div>

																					<div class="form-group">
																						<label class="col-sm-6 control-label"
																						       for="backuptab-load.type.update.perItem">
																							Update per item count
																						</label>
																						<div class="col-sm-6">
																							<input type="text" id="backuptab-load.type.update.perItem"
																							       class="form-control"
																							       value="${rt:getString(runTimeConfig, 'load.type.update.perItem')}"
																							       data-pointer="load.type.update.perItem"/>
																						</div>
																					</div>
																				</div>
																				<div role="tabpanel" class="tab-pane" id="backuptab-delete">
																					<div class="form-group">
																						<label class="col-sm-6 control-label"
																						       for="backuptab-load.type.delete.threads">
																							Load threads count
																						</label>
																						<div class="col-sm-6">
																							<input type="text" class="form-control" id="backuptab-load.type.delete.threads"
																							       value="${rt:getString(runTimeConfig, 'load.type.delete.threads')}"
																							       data-pointer="load.type.delete.threads"/>
																						</div>
																					</div>
																				</div>
																				<div role="tabpanel" class="tab-pane" id="backuptab-append">
																					<div class="form-group">
																						<label class="col-sm-6 control-label"
																						       for="backuptab-load.type.append.threads">
																							Load threads count
																						</label>
																						<div class="col-sm-6">
																							<input type="text" id="backuptab-load.type.append.threads"
																							       class="form-control"
																							       value="${rt:getString(runTimeConfig, 'load.type.append.threads')}"
																							       data-pointer="load.type.append.threads"/>
																						</div>
																					</div>
																				</div>
																			</div>
																		</div>

																		<hr/>

																		<div class="form-group">
																			<label for="backup-scenario.type.chain.simultaneous"
																			       class="col-sm-6 control-label">
																				Simultaneous
																			</label>
																			<div class="col-sm-6">
																				<select id="backup-scenario.type.chain.simultaneous" class="form-select"
																				        data-pointer="scenario.type.chain.simultaneous">
																					<option>
																						${rt:getString(runTimeConfig, 'scenario.type.chain.simultaneous')}
																					</option>
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

														<div class="modal fade" id="backup-rampup" tabindex="-1"
														     role="dialog" aria-labelledby="rampupLabel"
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
																			<div class="col-sm-6">
																				<button type="button" class="btn btn-default" id="chain-load">
																					Chain load details
																				</button>
																			</div>
																		</div>
																		<div class="form-group">
																			<label for="backup-scenario.type.rampup.threadCounts"
																			       class="col-sm-4 control-label">
																				Thread count
																			</label>
																			<div class="col-sm-8">
																				<input type="text" id="backup-scenario.type.rampup.threadCounts"
																				       class="form-control"
																				       value="${rt:getString(runTimeConfig, 'scenario.type.rampup.threadCounts')}"
																				       data-pointer="scenario.type.rampup.threadCounts"/>
																			</div>
																		</div>

																		<div class="form-group">
																			<label for="backup-scenario.type.rampup.sizes" class="col-sm-4 control-label">
																				Objects' sizes
																			</label>
																			<div class="col-sm-8">
																				<input type="text" id="backup-scenario.type.rampup.sizes" class="form-control"
																				       value="${rt:getString(runTimeConfig, 'scenario.type.rampup.sizes')}"
																				       data-pointer="scenario.type.rampup.sizes"/>
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
											</div>
										</div>

										<div class="standalone client">
											<fieldset>
												<legend>Authentication</legend>
												<div class="form-group">
													<label for="backup-auth.id" class="col-sm-3 control-label">
														User id
													</label>
													<div class="col-sm-9">
														<input type="text" id="backup-auth.id" class="form-control"
												            data-pointer="auth.id" value="${runTimeConfig.authId}"/>
													</div>
												</div>
												<div class="form-group">
													<label for="backup-auth.secret" class="col-sm-3 control-label">
														Access secret
													</label>
													<div class="col-sm-9">
														<input type="text" id="backup-auth.secret" class="form-control"
									                        data-pointer="auth.secret" value="${runTimeConfig.authSecret}"/>
													</div>
												</div>
											</fieldset>
										</div>

										<div class="standalone client cinderella">
											<fieldset>
												<legend>Storage</legend>
												<div class="standalone client">
													<div class="form-group">
														<label for="backup-storage.addrs" class="col-sm-3 control-label">
															Addresses (comma-separated)
														</label>
														<div class="col-sm-9">
															<input type="text" id="backup-storage.addrs" class="form-control"
																data-pointer="storage.addrs"
																value="${rt:getString(runTimeConfig, 'storage.addrs')}"/>
														</div>
													</div>
												</div>
												<div class="standalone client cinderella">
													<div class="form-group">
														<label for="backup-api.name" class="col-sm-3 control-label">
															API
														</label>
														<div class="col-sm-9">
															<select id="backup-api.name" class="form-select" data-pointer="api.name">
																<option value="backup-${runTimeConfig.apiName}">${runTimeConfig.apiName}</option>
																<option value="backup-swift">swift</option>
																<option value="backup-s3">s3</option>
																<option value="backup-atmos">atmos</option>
															</select>
															<button type="button" id="api-button" class="btn btn-primary"
													            data-toggle="modal" data-target="#backup-${runTimeConfig.apiName}">
																Details...
															</button>

															<div class="modal fade" id="backup-s3" tabindex="-1" role="dialog"
													            aria-labelledby="s3Label"
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
																				<label for="backup-api.type.s3.bucket.name" class="col-sm-4 control-label">
																					Bucket
																				</label>
																				<div class="col-sm-8">
																					<input type="text" id="backup-api.type.s3.bucket.name" class="form-control"
																			            data-pointer="api.type.s3.bucket.name"
																		                value="${rt:getString(runTimeConfig, 'api.type.s3.bucket.name')}"/>
																				</div>
																			</div>
																		</div>

																		<div class="modal-footer">
																			<button type="button" class="btn btn-default" data-dismiss="modal">Ok</button>
																		</div>
																	</div>
																</div>
															</div>

															<div class="modal fade" id="backup-swift" tabindex="-1" role="dialog"
													            aria-labelledby="swiftLabel"
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

															<div class="modal fade" id="backup-atmos" tabindex="-1" role="dialog"
													            aria-labelledby="atmosLabel"
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
																			<div class="form-group">
																				<label for="backup-api.type.atmos.subtenant" class="col-sm-4 control-label">
																					Subtenant
																				</label>
																				<div class="col-sm-8">
																					<input type="text" id="backup-api.type.atmos.subtenant" class="form-control"
																					       data-pointer="api.type.atmos.subtenant"
																					       value="${rt:getString(runTimeConfig, 'api.type.atmos.subtenant')}"/>
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
												</div>
											</fieldset>
										</div>

										<div class="client">
											<fieldset>
												<legend>Controller</legend>
												<div class="form-group">
													<label for="backup-load.servers" class="col-sm-3 control-label">
														Load servers
													</label>
													<div class="col-sm-9">
														<input type="text" id="backup-load.servers" class="form-control"
												            data-pointer="load.servers"
												            value="${rt:getString(runTimeConfig, 'load.servers')}"/>
													</div>
												</div>
											</fieldset>
										</div>

										<div class="standalone client">
											<fieldset>
												<legend>Data</legend>

												<div class="form-group">
													<label for="backup-load.threads" class="col-sm-3 control-label">
														Load threads
													</label>
													<div class="col-sm-9">
														<input type="text" id="backup-load.threads" data-pointer="load.threads" class="form-control"
																value="${rt:getString(runTimeConfig, 'load.threads')}"/>
													</div>
												</div>

												<div id="objects" class="form-group">
													<label for="backup-load.limit.dataItemCount" class="col-sm-3 control-label">
														Items count limit
													</label>
													<div class="col-sm-9">
														<input type="text" id="backup-load.limit.dataItemCount" class="form-control"
									                        data-pointer="load.limit.dataItemCount"
												            value="${runTimeConfig.loadLimitDataItemCount}"/>
													</div>
												</div>

												<div class="form-group">
													<label for="backup-data.size" class="col-sm-3 control-label">
														Items size
													</label>
													<div class="col-sm-9">
														<input type="text" id="backup-data.size" data-pointer="data.size" class="form-control"
																value="${rt:getString(runTimeConfig, 'data.size')}"/>
													</div>
												</div>

												<div class="form-group">
													<label for="backup-data.src.fpath" class="col-sm-3 control-label">
														Input items list file
													</label>
													<div class="col-sm-9">
														<input type="text" id="backup-data.src.fpath" class="form-control"
												            data-pointer="data.src.fpath"
												            value="${rt:getString(runTimeConfig, 'data.src.fpath')}"
									                        placeholder="Enter path to the list of objects on remote host. Format: log/<run.mode>/<run.id>/<filename>"/>
													</div>
												</div>

												<div class="form-group">
													<label class="col-sm-3 control-label">Output directory for logs</label>
													<div class="col-sm-9">
														<input type="text" class="form-control" value="log/webui/" readonly/>
													</div>
												</div>
											</fieldset>
										</div>

									</form>
								</div>
								<div id="extended">
									<form class="form-horizontal" id="main-form" role="form">
										<c:choose>
											<c:when test = "${runTimeConfig.runMode ne 'webui'}">
												<input type="hidden" name="run.mode" id="run.mode" value="${runTimeConfig.runMode}"/>
											</c:when>
											<c:otherwise>
												<input type="hidden" name="run.mode" id="run.mode" value="standalone"/>
											</c:otherwise>
										</c:choose>
										<input type="hidden" id="data.size" name="data.size" value="${rt:getString(runTimeConfig, 'data.size')}"/>
										<input type="hidden" id="load.threads" name="load.threads" value="${rt:getString(runTimeConfig, 'load.threads')}"/>
										<!-- Input fields with labels from JS -->
										<div id="configuration-content">

										</div>
									</form>
								</div>
							</div>
						</div>
					</div>
				</div>
				<c:forEach var="mode" items="${sessionScope.runmodes}">
					<c:set var="correctMode" value="${fn:replace(mode, '.', '_')}"/>
					<div class="tab-pane" id="scenarioTab-${correctMode}">
						<ul id="scenario-tab" class="nav nav-tabs" role="presentation">
							<li class="hidden-xs">
								<c:if test="${empty sessionScope.stopped[mode]}">
									<button type="button" class="btn btn-danger stop" value="${mode}">
										Stop
									</button>
								</c:if>
							</li>
							<li class="active"><a href="#${correctMode}-logs" data-toggle="tab">Logs</a></li>
							<c:if test="${not empty chartsMap[mode]}">
								<li><a href="#${correctMode}-charts" data-toggle="tab">Charts</a></li>
							</c:if>
						</ul>
						<div class="tab-content">
							<div class="tab-pane active" id="${correctMode}-logs">
								<ul id="log-pills" class="nav nav-pills">
									<li class="hidden-sm hidden-md hidden-lg stop-block-xs">
										<c:if test="${empty sessionScope.stopped[mode]}">
											<button type="button" class="btn btn-danger stop" value="${mode}">
												Stop
											</button>
										</c:if>
									</li>
									<li class="active"><a href="#${correctMode}-messages-csv" data-toggle="pill">messages.csv</a></li>
									<li><a href="#${correctMode}-errors-log" data-toggle="pill">errors.log</a></li>
									<li><a href="#${correctMode}-perf-avg-csv" data-toggle="pill">perf.avg.csv</a></li>
									<li><a href="#${correctMode}-perf-sum-csv" data-toggle="pill">perf.sum.csv</a></li>
								</ul>
								<div id="log-wrapper" class="tab-content">
									<div class="tab-pane active" id="${correctMode}-messages-csv">
										<div class="table-content">
											<div class="table-responsive">
												<table class="table table-condenced">
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
											</div>
										</div>
									</div>
									<div class="tab-pane" id="${correctMode}-errors-log">
										<div class="table-content">
											<div class="table-responsive">
												<table class="table table-condenced">
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
											</div>
										</div>
									</div>
									<div class="tab-pane" id="${correctMode}-perf-avg-csv">
										<div class="table-content">
											<div class="table-responsive">
												<table class="table table-condenced">
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
											</div>
										</div>
									</div>
									<div class="tab-pane" id="${correctMode}-perf-sum-csv">
										<div class="table-content">
											<div class="table-responsive">
												<table class="table table-condenced">
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
											</div>
										</div>
									</div>
								</div>
							</div>
							<c:if test="${not empty chartsMap[mode]}">
								<div class="tab-pane" id="${correctMode}-charts">
									<ul id="chart-pills" class="nav nav-pills">
										<li class="hidden-sm hidden-md hidden-lg stop-block-xs">
											<c:if test="${empty sessionScope.stopped[mode]}">
												<button type="button" class="btn btn-danger stop" value="${mode}">
													Stop
												</button>
											</c:if>
										</li>
										<c:choose>
											<c:when test="${chartsMap[mode] eq 'single'}">
												<li class="active"><a href="#tp-${correctMode}" data-toggle="pill">Throughput[obj/s]</a></li>
												<li><a href="#bw-${correctMode}" data-toggle="pill">Bandwidth[mb/s]</a></li>
											</c:when>
											<c:when test="${chartsMap[mode] eq 'chain'}">
												<li class="active"><a href="#tp-${correctMode}" data-toggle="pill">Throughput[obj/s]</a></li>
												<li><a href="#bw-${correctMode}" data-toggle="pill">Bandwidth[mb/s]</a></li>
											</c:when>
											<c:when test="${chartsMap[mode] eq 'rampup'}">
												<li class="active"><a href="#tp-${correctMode}" data-toggle="pill">Throughput[obj/s]</a></li>
												<li><a href="#bw-${correctMode}" data-toggle="pill">Bandwidth[mb/s]</a></li>
											</c:when>
										</c:choose>
									</ul>
									<div class="tab-content">
										<c:choose>
											<c:when test="${chartsMap[mode] eq 'single'}">
												<div class="tab-pane active" id="tp-${correctMode}">

												</div>
												<div class="tab-pane" id="bw-${correctMode}">

												</div>
											</c:when>
											<c:when test="${chartsMap[mode] eq 'chain'}">
												<div class="tab-pane active" id="tp-${correctMode}">

												</div>
												<div class="tab-pane" id="bw-${correctMode}">

												</div>
											</c:when>
											<c:when test="${chartsMap[mode] eq 'rampup'}">
												<div class="tab-pane active" id="tp-${correctMode}">

												</div>
												<div class="tab-pane" id="bw-${correctMode}">

												</div>
											</c:when>
										</c:choose>
									</div>
								</div>
							</c:if>
						</div>
					</div>
				</c:forEach>
			</div>
		</div>
		<script type="text/javascript" src="webjars/d3js/3.5.3/d3.min.js"></script>
		<script type="text/javascript" src="webjars/jquery/2.1.0/jquery.min.js"></script>
		<script type="text/javascript" src="webjars/bootstrap/3.3.2-1/js/bootstrap.min.js"></script>
		<script type="text/javascript" src="js/script.js"></script>
		<script>
			jsonProps = ${runTimeConfig.jsonProps};
		</script>
	</body>
</html>
