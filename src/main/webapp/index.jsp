<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri='http://java.sun.com/jsp/jstl/core' prefix='c'%>
<!DOCTYPE html>
<html>
    <head>
		<meta charset="utf-8">
		<link href="css/bootstrap/bootstrap.min.css" rel="stylesheet">
		<link href="css/styles.css" rel="stylesheet">
		<link href="css/bootstrap-vertical-tabs-1.1.0/bootstrap.vertical-tabs.css" rel="stylesheet">
		<script type="text/javascript" src="http://code.jquery.com/jquery-latest.min.js"></script>
		<script type="text/javascript" src="js/script.js"></script>
		<script type="text/javascript" src="js/bootstrap/bootstrap.min.js"></script>
	</head>
	<body>
		<nav class="navbar navbar-default" role="navigation">
  			<div class="container-fluid">
  				<div class="navbar-header">
  					<a id="logo-image" href="#"><img src="images/vipr.png" width="50" height="50"></a>
      				<a class="navbar-brand" href="#">Mongoose</a>
  				</div>
    			<div class="collapse navbar-collapse" id="bx-example-navbar-collapse-1">
    				<ul class="nav navbar-nav">
    					<li class="active"><a href="#">Run</a></li>
    					<li><a href="/home/gusakk/driver.html">Driver</a></li>
    				</ul>

    				<ul class="nav navbar-nav navbar-right">
    					<li><a href="#">About</a></li>
    				</ul>
    			</div>
    		</div>
		</nav>

        ${runTimeConfig.authId}
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
					<form>

						<div class="fixed-block">
							<fieldset class="scheduler-border-top">
								<legend class="scheduler-border">Run</legend>
								<label for="run-time">run.time:</label>
								<input type="text" id="run-time" class="form-control counter" value="1000">
								<select>
									<option>seconds</option>
									<option>minutes</option>
									<option>hours</option>
									<option>days</option>
								</select>
								<br>
								<label for="run-metrics-period-sec">run.metrics.period.sec:</label>
								<input type="text" class="form-control counter" value="10">
							</fieldset>
						</div>

						<div class="fixed-block">
							<fieldset class="scheduler-border-top">
								<legend class="scheduler-border">Auth</legend>
								<label for="auth-id">auth.id:</label>
								<input type="text" class="form-control" value="wuser1@sanity.local">
								<br>
								<label for="auth-secret">auth.secret:</label>
								<input type="text" class="form-control" value="Ug5wGLnRYgtEEdIIxDRhUwDruQl2lRGSO7uV/5A4">
							</fieldset>
						</div>
						<br>

						<div class="storages-block">
							<fieldset class="scheduler-border">
								<legend class="scheduler-border">Storage</legend>
								<label>storage.api:</label>
								<input type="text" class="form-control counter" value="s3">
								<label>scheme:</label>
								<input type="text" class="form-control counter" value="http">
								<fieldset class="scheduler-border">
									<legend class="scheduler-border">Data nodes</legend>
									<button type="button" class="default add-node">Add</button>
									<div class="input-group data-node">
										<input id="data-node-text" type="text" class="form-control" placeholder="Enter data node">
										<span class="input-group-btn">
											<button id="save" type="button" class="btn btn-default">Save</button>
										</span>
									</div>

									<div class="storages">
										<div class="input-group">
                    						<span class="input-group-addon">
                        						<input type="checkbox">
                    						</span>
                    						<label class="form-control">
                    							127.0.0.1
                    						</label>
                    						<span class="input-group-btn">
                    							<button type="button" class="btn btn-default remove">Remove</button>
                    						</span>
                    					</div>
									</div>
								</fieldset>
							</fieldset>

						</div>

						<!-- Fix it -->
						<!-- <div class="distributed"> -->
							<div class="drivers-block">
								<fieldset class="scheduler-border">
									<legend class="scheduler-border">Remote</legend>
									<label>remote.monitor.port:</label>
									<input type="text" class="form-control counter" value="1199">
									<fieldset class="scheduler-border">
										<legend class="scheduler-border">Drivers</legend>
										<button type="button" class="default add-driver">Add</button>
										<div class="input-group driver">
											<input id="driver-text" type="text" class="form-control" placeholder="Enter driver">
											<span class="input-group-btn">
												<button id="save-driver" type="button" class="btn btn-default">Save</button>
											</span>
										</div>

										<div class="drivers">
											<div class="input-group">
                    							<span class="input-group-addon">
                        							<input type="checkbox">
                    							</span>
                    							<label class="form-control">
                    								127.0.0.1
                    							</label>
                    							<span class="input-group-btn">
                    								<button type="button" class="btn btn-default remove">Remove</button>
                    							</span>
                    						</div>
										</div>

									</fieldset>
								</fieldset>
							</div>
						<!-- </div> -->


						<div class="operations">
							<fieldset class="scheduler-border">
								<legend class="scheduler-border">Data</legend>
								<label for="count">data.count:</label>
								<input id="count" type="text" class="form-control counter" value="0">
								<select>
									<option>objects</option>
									<option>seconds</option>
									<option>minutes</option>
									<option>hours</option>
									<option>days</option>
								</select>
								<label>data.size</label>
								<input type="text" class="form-control counter" placeholder="min" value="1MB">
								-
								<input type="text" class="form-control counter" placeholder="max" value="1MB">


								<!-- -->
								<fieldset class="scheduler-border">
									<legend class="scheduler-border">Load</legend>
									<div class="tabs-wrapper">
										<ul class="nav nav-tabs" role="tablist">
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
											<input type="text" class="form-control counter" value="4">
										</div>
										<div class="tab-pane" id="read">
											<label>load.thread</label>
											<input type="text" class="form-control counter" value="4">
											<label>verify.content</label>
											<input type="text" class="form-control counter" value="true">
										</div>
										<div class="tab-pane" id="update">
											<label>load.thread</label>
											<input type="text" class="form-control counter" value="4">
											<label>load.per.item</label>
											<input type="text" class="form-control counter" value="1">
										</div>
										<div class="tab-pane" id="delete">
											<label>load.thread</label>
											<input type="text" class="form-control counter" value="4">
										</div>
										<div class="tab-pane" id="append">
											<label>load.thread</label>
											<input type="text" class="form-control counter" value="4">
										</div>
									</div>
								</fieldset>
							</fieldset>

						</div>

						<div class="distributed">
							<div class="interfaces">
								<fieldset class="scheduler-border">
									<legend class="scheduler-border">API</legend>
									<div class="tabs-wrapper">
										<ul class="nav nav-tabs crud-tabs" role="tablist">
							  				<li class="active"><a href="#s3" data-toggle="tab">S3</a></li>
							  				<li><a href="#atmos" data-toggle="tab">Atmos</a></li>
							  				<li><a href="#swift" data-toggle="tab">Swift</a></li>
										</ul>
									</div>

									<div class="tab-content">
										<div class="tab-pane active" id="s3">
											<label>api.port</label>
											<input type="text" class="form-control counter" value="9020">
											<label>api.auth.prefix</label>
											<input type="text" class="form-control counter" value="AWS">
											<label>api.bucket</label>
											<input type="text" class="form-control counter">
										</div>
										<div class="tab-pane" id="atmos">
											<label>api.port</label>
											<input type="text" class="form-control counter" value="9020">
											<label>api.subtenant</label>
											<input type="text" class="form-control counter" value="511afdbfc6a3432fac65f2a5a1bc85c7">
											<label>api.path.rest</label>
											<input type="text" class="form-control counter" value="rest">
											<label>api.interface</label>
											<input type="text" class="form-control counter" value="objects">
										</div>
										<div class="tab-pane" id="swift">
											<label>api.port</label>
											<input type="text" class="form-control counter" value="9024">
										</div>
									</div>
								</fieldset>
							</div>
						</div>

						<div class="start-wrapper">
							<button id="start" type="button" class="default"><span>Start</span></button>
						</div>
					</form>
				</div>

				<div class="tab-pane" id="monitor">
					<div class="left-side">
						<div class="menu-wrapper">
							<div class="col-xs-8">
    							<ul class="nav nav-tabs tabs-left">
							      	<li class="active"><a href="#data-items-csv" data-toggle="tab">data.items.csv</a></li>
							      	<li><a href="#errors-log" data-toggle="tab">errors.log</a></li>
							      	<li><a href="#messages-csv" data-toggle="tab">messages.csv</a></li>
							      	<li><a href="#perf-avg-csv" data-toggle="tab">perf.avg.csv</a></li>
							    	<li><a href="#perf-sum-csv" data-toggle="tab">perf.sum.csv</a></li>
							    	<li><a href="#perf-trace-csv" data-toggle="tab">perf.trace.csv</a></li>
    							</ul>
							</div>
						</div>
					</div>

					<div class="right-side">
						<div class="log-wrapper">
							<div class="tab-content">
								<div class="tab-pane active" id="data-items-csv">
									data.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csvcsvdata.items.csv
								</div>
								<div class="tab-pane" id="errors-log">
									errors.log
								</div>
								<div class="tab-pane" id="messages-csv">
								   	messages.csv
								</div>
								<div class="tab-pane" id="perf-avg-csv">
									perf.avg.csv
								</div>
								<div class="tab-pane" id="perf-sum-csv">
									perf.sum.csv
								</div>
								<div class="tab-pane" id="perf-trace-csv">
								   	perf.trace.csv
								</div>
	    					</div>
    					</div>
					</div>
					<button id="stop" type="button" class="default"><span>Stop</span></button>
				</div>
			</div>
		</div>
	</body>
</html>
