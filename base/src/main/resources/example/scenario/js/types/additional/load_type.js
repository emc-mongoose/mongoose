CreateLoad
	.config(
		{
			"item": {
				"output": {
					"file": ITEMS_FILE_0
				}
			},
			"load": {
				"step": {
					"limit": {
						"time": FIRST_STEP_DURATION_LIMIT
					}
				}
			}
		}
	)
	.run();

UpdateLoad
	.config(
		{
			"item": {
				"data": {
					"ranges": {
						"random": UPDATE_RANDOM_RANGES_COUNT
					}
				},
				"input": {
					"file": ITEMS_FILE_0
				},
				"output": {
					"file": ITEMS_FILE_1
				}
			}
		}
	)
	.run();

ReadLoad
	.config(
		{
			"item": {
				"data": {
					"verify": true
				},
				"input": {
					"file": ITEMS_FILE_1
				}
			}
		}
	)
	.run();

DeleteLoad
	.config(
		{
			"item": {
				"input": {
					"file": ITEMS_FILE_0
				},
				"output": {
					"file": ITEMS_FILE_2
				}
			}
		}
	)
	.run()

NoopLoad
	.config(
		{
			"item": {
				"input": {
					"file": ITEMS_FILE_2
				}
			}
		}
	)
	.run();
