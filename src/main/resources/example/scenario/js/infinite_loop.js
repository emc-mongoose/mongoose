while(true) {
	Load
		.config(
			{
				"load": {
					"step": {
						"limit": {
							"time": 10
						}
					}
				}
			}
		)
		.run();

	print("Sleep 10s...");
	java.lang.Thread.sleep(10000);
}