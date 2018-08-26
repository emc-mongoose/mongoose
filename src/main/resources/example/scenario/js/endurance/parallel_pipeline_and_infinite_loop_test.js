var sharedConfig = {
	"storage": {
		"net": {
			"http": {
				"namespace": "ns1"
			}
		}
	}
}

var pipelineSharedConfig = {
	"item": {
		"output": {
			"path": "endurance_0"
		}
	}
}

var pipelineCreateCtx = {}

var pipelineUpdateCtx = {
	"item": {
		"data": {
			"ranges": {
				"random": 4
			}
		}
	},
	"load": {
		"op": {
			"type": "update"
		}
	}
}

var pipelineReadCtx = {
	"item": {
		"data": {
			"verify": true
		}
	},
	"load": {
		"op": {
			"type": "read"
		}
	}
}

var pipelineDeleteCtx = {
	"load": {
		"op": {
			"type": "delete"
		}
	}
}

var infiniteLoopIterationLoadConfig = {
	"item": {
		"data": {
			"size": "1KB"
		},
		"output": {
			"path": "endurance_1"
		}
	},
	"load": {
		"step": {
			"limit": {
				"time": "5s"
			}
		}
	}
}

var pipelineLoad = Pipeline
	.config(sharedConfig)
	.config(pipelineSharedConfig)
	.append(pipelineCreateCtx)
	.append(pipelineUpdateCtx)
	.append(pipelineReadCtx)
	.append(pipelineDeleteCtx)
	.start();

while(true) {
	Load
		.config(sharedConfig)
		.config(infiniteLoopIterationLoadConfig)
		.run();
}
