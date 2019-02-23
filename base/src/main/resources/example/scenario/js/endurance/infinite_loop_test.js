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

while(true) {
	Load
		.config(infiniteLoopIterationLoadConfig)
		.run();
}
