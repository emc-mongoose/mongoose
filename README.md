Mongoose
========
What Mongoose is and what it is not
-----------------------------------
Mongoose is a tool which is initially intended to test ECS performance. It is designed to derive the best practices of its predecessors some of which used before for Centera performance testing.

In the terms of high load Mongoose is:

* A million of concurrent connections
* A million of operations per second
* A million of items which may be processed multiple times in the circular load mode
* A million of items which may be stored in the storage mock

Features
--------
1. Distributed Mode
2. Reporting:
	2.1. Item lists for reusing
	2.2. Statistics for the rates and timings
	2.3. High-resolution timings for each operation
3. Supported Load Types:
	3.1. Write (Update/Append are implemented as partial cases of Write)
	3.2. Read (Partial case is to be implemented soon)
	3.3. Delete
4. Abstract Load Engine supports different item types:
	4.1. Containers (Bucket/Directory/etc)
	4.2. Data Items (Object/File/etc)
	4.3. Tokens (Subtenant/etc - to be implemented soon)
5. Cloud storage support:
	5.1. Amazon S3
	5.2. EMC Atmos
	5.3. OpenStack Swift
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