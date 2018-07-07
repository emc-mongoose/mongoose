var itemOutputFile = "items_passed_through_create_read_delete_pipeline.csv"

// limit the whole pipeline step execution time by 5 minutes
// (pipeline step takes the limits configuration parameter values from the 1st configuration element)
var sharedConfig = {
	"load": {
		"step": {
			"limit": {
				"time": "5m"
			}
		}
	}
};

var readConfig = {
	"load": {
		"type": "read"
	}
};

// persist the items info into the output file after the last operation
var deleteConfig = {
	"item": {
		"output": {
			"file": itemOutputFile
		}
	},
	"load": {
		"type": "delete"
	}
};

// clean up before running the pipeline load step
var cmd = new java.lang.ProcessBuilder("/bin/sh", "-c", "rm -f " + itemOutputFile).start();
cmd.waitFor();

PipelineLoad
	.config(sharedConfig)
	.append({})
	.append(readConfig)
	.append(deleteConfig)
	.run();
