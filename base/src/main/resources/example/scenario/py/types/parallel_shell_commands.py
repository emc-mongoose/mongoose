noOutputColorConfig = {
	"output": {
		"color": False
	}
}

command1 = Command \
	.config(noOutputColorConfig) \
	.value("echo Hello world!") \

command2 = Command \
	.value("ps alx | grep java")

Parallel \
	.step(command1) \
	.step(command2) \
	.run()
