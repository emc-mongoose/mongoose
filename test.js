swift_item_list = "swift_item_list.csv"

config = {
	"item": {
		"data" : {
			"size" : "100",
//			"size" : "10MB",
//			"input" : {
//				"file" : "unicode_content_2.txt"
////				"file" : ""
//			}
		}
	},
	"storage" : {
		"driver" : {
			"type" : "swift"
		},
		"namespace" : "AUTH_test",
		"net": {
			"node" : {
				"port" : 8080
			}
		},
		"auth" : {
			"uid" : "test:tester",
			"secret" : "anavooshaush"
		}
	},
	"load": {
		"op": {
			"limit": {
				"count": 1
			}
		}
	}
}

read_config = {
		"item": {
			"input": {
				"file": swift_item_list
			},
			"data" : {
				"ranges" : {
					"fixed" : new java.util.ArrayList([ "0-10","15-35" ])
//					"random" : 2
				},
				"verify" : true
			}
		}
	}

create_config = {
		"item": {
			"output": {
				"file": swift_item_list
			}
		}
	}

var cmd = new java.lang.ProcessBuilder()
	.command("sh", "-c", "rm -R " + swift_item_list)
	.inheritIO()
	.start();

cmd.waitFor();

PreconditionLoad
	.config(config)
	.config(create_config)
	.run();

ReadLoad
	.config(config)
	.config(read_config)
	.run()
