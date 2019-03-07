var cmd_1 = new java.lang.ProcessBuilder()
	.command("/bin/sh", "-c", "rm -f weighted-load.csv")
	.inheritIO()
	.start();
cmd_1.waitFor();

PreconditionLoad
	.config({
	"load" : {
		"op" : {
			"limit" : {
				"count" : 10000
			}
		},
		"step" : {
		"limit" : {
			"size" : "1GB",
			"time" : 10
		}
		}
	},
	"item" : {
		"output" : {
		"file" : "weighted-load.csv"
		}
	}
	})
	.run();

WeightedLoad
	.config({
	"load" : {
		"step" : {
		"limit" : {
			"time" : 50
		}
		}
	}
	})
	.append({
	"load" : {
		"op" : {
		"weight" : 20
		}
	},
	"item" : {
		"output" : {
		"path" : ITEM_OUTPUT_PATH
		}
	}
	})
	.append({
	"load" : {
		"op" : {
		"recycle" : true,
		"type" : "read",
		"weight" : 80
		}
	},
	"item" : {
		"input" : {
		"file" : "weighted-load.csv"
		}
	}
	})
	.run();
