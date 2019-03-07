var parentConfig_1 = {
"storage" : {
	"namespace" : "ns1"
}
};

var fileName = "SingleRandomUpdateAndMultipleRandomRead";

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
			"count" : COUNT_LIMIT
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
		"step" : {
			"id" : STEP_ID_UPDATE
		},
		"op" : {
		"type" : "update"
		}
	},
	"item" : {
		"data" : {
		"ranges" : {
			"random" : 1
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
		"step" : {
			"id" : STEP_ID_READ
		},
		"op" : {
		"type" : "read"
		}
	},
	"item" : {
		"data" : {
		"ranges" : {
			"random" : 12
		},
		"verify" : true
		},
		"input" : {
		"file" : fileName + "-1.csv"
		}
	}
	})
	.run();
