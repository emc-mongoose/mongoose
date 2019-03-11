var loadSteps = [CONCURRENCY_LEVEL];

var loadStepConfig = {
	"item": {
		"input": {
			"path": ITEM_INPUT_PATH
		}
	},
	"load": {
		"op": {
			"type": "noop"
		},
		"step": {
			"id": LOAD_STEP_ID
		}
	}
}

for(var i = 0; i < CONCURRENCY_LEVEL; i ++) {
	loadSteps[i] = Load
		.config(loadStepConfig)
		.start();
}

try {
	for(var i = 0; i < CONCURRENCY_LEVEL; i ++) {
		loadSteps[i].await();
	}
} finally {
	for(var i = 0; i < CONCURRENCY_LEVEL; i ++) {
		loadSteps[i].close();
	}
}
