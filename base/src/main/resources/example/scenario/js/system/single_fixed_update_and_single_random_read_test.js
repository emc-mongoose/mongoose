var parentConfig_1 = {
"storage" : {
	"namespace" : "ns1"
}
};

var fileName = "SingleFixedUpdateAndSingleRandomRead";

var cmd_1 = new java.lang.ProcessBuilder()
	.command("/bin/sh", "-c", "rm -f " + fileName + "-0.csv " + fileName + "-1.csv")
	.inheritIO()
	.start();
cmd_1.waitFor();

PreconditionLoad
	.config(parentConfig_1)
	.config({
	"load" : {
		"op" : {
		"limit" : {
			"count" : 1000
		}
		}
	},
	"item" : {
		"output" : {
		"file" : fileName + "-0.csv"
		}
	}
	})
	.run();

var cmd_2 = new java.lang.ProcessBuilder()
	.command("/bin/sh", "-c", "sleep 5")
	.inheritIO()
	.start();
cmd_2.waitFor();

UpdateLoad
	.config(parentConfig_1)
	.config({
	"load" : {
		"op" : {
		"type" : "update"
		},
		"step" : {
			"id" : STEP_ID_UPDATE
		}
	},
	"item" : {
		"data" : {
		"ranges" : {
			"fixed" : new java.util.ArrayList([ "2KB-5KB" ])
		}
		},
		"output" : {
		"file" : fileName + "-1.csv"
		},
		"input" : {
		"file" : fileName + "-0.csv"
		}
	}
	})
	.run();

var cmd_3 = new java.lang.ProcessBuilder()
	.command("/bin/sh", "-c", "sleep 5")
	.inheritIO()
	.start();
cmd_3.waitFor();

ReadLoad
	.config(parentConfig_1)
	.config({
	"load" : {
		"op" : {
		"type" : "read"
		},
		"step" : {
			"id" : STEP_ID_READ
		}
	},
	"item" : {
		"data" : {
		"ranges" : {
			"random" : 1
		},
		"verify" : true
		},
		"input" : {
		"file" : fileName + "-1.csv"
		}
	}
	})
	.run();
