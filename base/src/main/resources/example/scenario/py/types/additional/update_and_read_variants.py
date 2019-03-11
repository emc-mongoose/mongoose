PreconditionLoad \
	.config(
		{
			"item": {
				"output": {
					"file": ITEMS_FILE_0
				}
			},
			"load": {
				"op": {
					"limit": {
						"count": FIRST_STEP_COUNT_LIMIT
					}
				}
			}
		}
	) \
	.run()

UpdateRandomRangeLoad \
	.config(
		{
			"item": {
				"input": {
					"file": ITEMS_FILE_0
				},
				"output": {
					"file": ITEMS_FILE_1
				}
			}
		}
	) \
	.run()

# should cause verification failures as far as ITEMS_FILE_0 input file is used instead of
# ITEMS_FILE_1
ReadVerifyLoad \
	.config(
		{
			"item": {
				"input": {
					"file": ITEMS_FILE_0
				}
			}
		}
	) \
	.run()

# should not cause verification failures as far as the verification is disabled by default
ReadRandomRangeLoad \
	.config(
		{
			"item": {
				"input": {
					"file": ITEMS_FILE_0
				}
			}
		}
	) \
	.run()

# should not cause verification failures as far as the correct items input file is used
ReadVerifyRandomRangeLoad \
	.config(
		{
			"item": {
				"input": {
					"file": ITEMS_FILE_1
				}
			}
		}
	) \
	.run()
