itemOutputFile = "items_passed_through_create_read_delete_pipeline.csv"

# limit the whole pipeline step execution time by 5 minutes
# (pipeline step takes the limits configuration parameter values from the 1st configuration element)
createConfig = {
	"load": {
		"step": {
			"limit": {
				"time": "5m"
			}
		}
	}
}

readConfig = {
	"load": {
		"type": "read"
	}
}

# persist the items info into the output file after the last operation
deleteConfig = {
	"item": {
		"output": {
			"file": itemOutputFile
		}
	},
	"load": {
		"type": "delete"
	}
}

# clean up before running the pipeline load step
Command \
	.value("rm -f " + itemOutputFile) \
	.run()

PipelineLoad \
	.append(createConfig) \
	.append(readConfig) \
	.append(deleteConfig) \
	.run()
