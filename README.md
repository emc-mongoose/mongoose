# Mongoose

## Description
Mongoose is a tool which is initially intended to test ECS performance. It is designed to derive the best practices of its predecessors some of which used before for Centera performance testing.

In the terms of high load Mongoose is:
* A million of concurrent connections
* A million of operations per second
* A million of items which may be processed multiple times in the circular load mode
* A million of items which may be stored in the storage mock

## Features
1. Distributed Mode
2. Reporting:
  1. Item lists for reusing
  2. Statistics for the rates and timings
  3. High-resolution timings for each operation
3. Supported Load Types:
  1. Write (Update/Append are implemented as partial cases of Write)
  2. Read (Partial case is to be implemented soon)
  3. Delete
4. Abstract Load Engine supports different item types:
  1. Containers (Bucket/Directory/etc)
  2. Data Items (Object/File/etc)
  3. Tokens (Subtenant/etc - to be implemented soon)
5. Cloud storage support:
  1. Amazon S3
  2. EMC Atmos
  3. OpenStack Swift
6. Filesystem Operations Support
7. Custom Content Generation
8. Content Updating and Verification Ability
9. Circular Load Mode
10. Scenario Scripting
11. Throttling
12. Web GUI
13. Dynamic Configuration Parameters
14. Customizable Items Naming
15. SSL/TLS Support
16. Docker Integration

## Deployment

### Environment Requirements
* JRE 7 or higher
* Connectivity to the remote hosts via port numbers 1099, 1199, 9020..9025 (not filtered by firewalls, if any)
* OS open files limit is higher

### Download
The tar-file with Mongoose 2.1.1 binaries can be downloaded from EMC ASD Artifactory.

### Build from sources
1. Clone
2. Execute the command:
  ```bash
  ./gradlew dist
  ```
  The tarball is ready under build/dist/mongoose-\<VERSION\>.tar path

### Unpack
```bash
tar xvf mongoose-<VERSION>.tar
```

## Usage

### Demo Mode
You can try Mongoose without a real storage. Mongoose build contains a mock storage implementation. Start it first:
```bash
java -jar <MONGOOSE_DIR>/mongoose.jar wsmock
```
Then open another console and start Mongoose itself:
```bash
java -jar <MONGOOSE_DIR>/mongoose.jar
```
The line starts Mongoose with default scenario and the default configuration. By default Mongoose writes using Amazon S3 API to storage with IP address 127.0.0.1 (local host). Storage mock is already there to store the new objects.

Now you can switch between the two consoles to see how Mongoose regularly reports about objects created and the storage mock regularly reports about objects stored.

For detailed information please refer to the wiki.

## Contribution
Create a fork of the project into your own reposity. Make all your necessary changes and create a pull request with a description on what was added or removed and details explaining the changes in lines of code. If approved, project owners will merge it.

## Licensing
Mongoose is freely distributed under the MIT License. See LICENSE for details.

## Support
Email Mongoose.Support@emc.com to get support.

### Bug reporting
Please provide Mongoose Support team with the following information. The more information we have, the sooner fix will be available
1. Short bug description
  1. Symptoms
  2. \[if you know\] Bug trigger
2. Environment description
  1. Operating system
  2. It would be really helpful to have IP address(es)
3. Mongoose version
4. Test configuration
  1. Command line
  2. \[if modified\] Defaults configuration file
5. All the output the run produced
  1. Console output
  2. All log files
6. \[if hangs\] Thread dump
  1. Execute kill -3 <pid> on Unix
  2. Press Ctrl+Break on Windows

