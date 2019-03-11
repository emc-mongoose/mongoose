// to run this scenario please define ITEM_INPUT_FILE either ITEM_INPUT_PATH environment variable
// and ITEM_OUTPUT_PATH environment variable
var CopyLoadUsingEnvVars = CreateLoad
	.config(
		{
			"item": {
				"input": {
					"file": ITEM_INPUT_FILE,
					"path": ITEM_INPUT_PATH
				},
				"output": {
					"path": ITEM_OUTPUT_PATH
				}
			}
		}
	);

CopyLoadUsingEnvVars.run();
