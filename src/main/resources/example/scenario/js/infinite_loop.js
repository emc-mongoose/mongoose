while(true) {
	Load
		.config(
			{
				"load": {
					"step": {
						"limit": {
							"time": 5
						}
					}
				}
			}
		)
		.run();

	print("Sleep 5s...");
	java.lang.Thread.sleep(5000);
}
