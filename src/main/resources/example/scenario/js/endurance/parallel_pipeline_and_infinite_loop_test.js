var pipelineSharedConfig = {
	"item": {
		"output": {
			"path": "endurance_0"
		}
	},
	"load": {
		"step": {
			"id": "endurance_0"
		}
	}
}

var pipelineCreateCtx = {
}

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
			"id": "endurance_1",
			"limit": {
				"time": "5s"
			}
		}
	}
}

var pipelineLoad = PipelineLoad
	.config(pipelineSharedConfig)
	.append(pipelineCreateCtx)
	.append(pipelineUpdateCtx)
	.append(pipelineReadCtx)
	.append(pipelineDeleteCtx)
	.start();

while(true) {
	Load
		.config(infiniteLoopIterationLoadConfig)
		.run();
}
