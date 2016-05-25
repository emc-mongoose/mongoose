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
# Distributed Mode
# Reporting:
	## Item lists for reusing
	## Statistics for the rates and timings
	## High-resolution timings for each operation
# Supported Load Types:
	## Write (Update/Append are implemented as partial cases of Write)
	## Read (Partial case is to be implemented soon)
	## Delete
# Abstract Load Engine supports different item types:
	## Containers (Bucket/Directory/etc)
	## Data Items (Object/File/etc)
	## Tokens (Subtenant/etc - to be implemented soon)
# Cloud storage support:
	## Amazon S3
	## EMC Atmos
	## OpenStack Swift
# Filesystem Operations Support
# Custom Content Generation
# Content Updating and Verification Ability
# Circular Load Mode
# Scenario Scripting
# Throttling
# Web GUI
# Dynamic Configuration Parameters
# Customizable Items Naming
# SSL/TLS Support
# Docker Integration