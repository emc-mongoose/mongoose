var processBuilder = new java.lang.ProcessBuilder();

var command1 = processBuilder
	.command("/bin/sh", "-c", "echo Hello world!")
	.start();

var command2 = processBuilder
	.command("/bin/sh", "-c", "ps alx | grep java")
	.start();

var command1StdOut = new java.io.BufferedReader(
	new java.io.InputStreamReader(command1.getInputStream())
);
var command1StdErr = new java.io.BufferedReader(
	new java.io.InputStreamReader(command1.getErrorStream())
);

var command2StdOut = new java.io.BufferedReader(
	new java.io.InputStreamReader(command2.getInputStream())
);
var command2StdErr = new java.io.BufferedReader(
	new java.io.InputStreamReader(command2.getErrorStream())
);

command1.waitFor();

command2.waitFor();

while(null != (nextLine = command1StdOut.readLine())) {
	print(nextLine);
}
command1StdOut.close();
while(null != (nextLine = command1StdErr.readLine())) {
	print(nextLine);
}
command1StdErr.close();

while(null != (nextLine = command2StdOut.readLine())) {
	print(nextLine);
}
command2StdOut.close();
while(null != (nextLine = command2StdErr.readLine())) {
	print(nextLine);
}
command2StdErr.close();
