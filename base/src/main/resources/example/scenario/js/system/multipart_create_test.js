var cmd = new java.lang.ProcessBuilder()
	.command("sh", "-c", "rm -f ${ITEM_OUTPUT_FILE}")
	.inheritIO()
	.start();
cmd.waitFor();

Load
	.config({
	"item" : {
		"data" : {
		"ranges" : {
			"threshold" : PART_SIZE
		}
		},
		"output" : {
		"file" : "" + ITEM_OUTPUT_FILE + ""
		}
	},
	"storage" : {
		"namespace" : "ns1"
	},
	"load" : {
		"batch" : {
		"size" : 1
		},
		"step" : {
		"limit" : {
			"size" : SIZE_LIMIT
		}
		}
	}
	})
	.run();
