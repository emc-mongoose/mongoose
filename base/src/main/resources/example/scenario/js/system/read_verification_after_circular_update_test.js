var fileName = "ReadVerificationAfterCircularUpdateTest";

var cmd_1 = new java.lang.ProcessBuilder()
	.command("/bin/sh", "-c", "rm -f " + fileName + "0.csv " + fileName + "1.csv")
	.inheritIO()
	.start();
cmd_1.waitFor();

PreconditionLoad
	.config({
	"load" : {
		"op" : {
		"limit" : {
			"count" : 1000
		}
		},
		"step" : {
		"id": STEP_ID_CREATE
		}
	},
	"item" : {
		"output" : {
		"file" : fileName + "0.csv"
		}
	}
	})
	.run();

PreconditionLoad
	.config({
	"load" : {
		"op" : {
		"recycle" : true,
		"type" : "update",
		"limit" : {
			"count": 10000
		}
		},
		"step" : {
			"id": STEP_ID_UPDATE
		}
	},
	"item" : {
		"data" : {
		"ranges" : {
			"random" : 10
		}
		},
		"output" : {
		"file" : fileName + "1.csv"
		},
		"input" : {
		"file" : fileName + "0.csv"
		}
	}
	})
	.run();

var cmd_2 = new java.lang.ProcessBuilder()
	.command("/bin/sh", "-c", "sleep 5")
	.inheritIO()
	.start();
cmd_2.waitFor();

ReadLoad
	.config({
	"load" : {
		"op" : {
		"type" : "read"
		},
		"step" : {
			"id": STEP_ID_READ
		}
	},
	"item" : {
		"data" : {
		"verify" : true
		},
		"input" : {
		"file" : fileName + "1.csv"
		}
	}
	})
	.run();
