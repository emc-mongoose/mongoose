var fileName = "ReadVerificationFailTest.csv";

var cmd_1 = new java.lang.ProcessBuilder()
	.command("/bin/sh", "-c", "rm -f " + fileName)
	.inheritIO()
	.start();
cmd_1.waitFor();

PreconditionLoad
	.config({
	"load" : {
		"op" : {
			"limit": {
				"count" : 10000
			}
		},
		"step" : {
		"limit" : {
			"size" : "1GB"
		}
		}
	},
	"item" : {
		"output" : {
		"file" : fileName
		}
	}
	})
	.run();

PreconditionLoad
	.config({
	"load" : {
		"op" : {
		"type" : "update"
		}
	},
	"item" : {
		"data" : {
		"ranges" : {
			"random" : 4
		}
		},
		"input" : {
		"file" : fileName
		}
	}
	})
	.run();

ReadLoad
	.config({
	"load" : {
		"op" : {
		"type" : "read"
		}
	},
	"item" : {
		"data" : {
		"verify" : true
		},
		"input" : {
		"file" : fileName
		}
	}
	})
	.run();
