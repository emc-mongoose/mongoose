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
	* Item lists for reusing
	* Statistics for the rates and timings
	* High-resolution timings for each operation
3. Supported Load Types:
	* Write (Update/Append are implemented as partial cases of Write)
	* Read (Partial case is to be implemented soon)
	* Delete
4. Abstract Load Engine supports different item types:
	* Containers (Bucket/Directory/etc)
	* Data Items (Object/File/etc)
	* Tokens (Subtenant/etc - to be implemented soon)
5. Cloud storage support:
	* Amazon S3
	* EMC Atmos
	* OpenStack Swift
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