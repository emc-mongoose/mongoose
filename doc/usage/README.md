# Contents

1. Defaults
2. Configuration
2.1. Load Scaling
2.1.1. Rate
2.1.2. Concurrency
2.1.3. Distributed Mode
2.2. Parameterizing
2.2.1. Request HTTP Headers Parameterizing
2.2.2. Output Path Parameterizing
2.2.3. Multiuser Load
2.3. Storage Drivers
2.3.1. Netty Based Storage Drivers
2.3.1.1. Node Balancing
2.3.1.2. SSL/TLS
2.3.1.2. Connection Timeout
2.3.1.3. I/O Buffer Size
3. Load Generation
3.1. Items
3.1.1. Types
3.1.1.1. Data
3.1.1.1.1. Size
3.1.1.1.1.1. Fixed
3.1.1.1.1.2. Random
3.1.1.1.2. Payload
3.1.1.1.2.1. Random Using Seed
3.1.1.1.2.2. From External File
3.1.1.2. Path
3.1.1.3. Token
3.1.2. Input
3.1.2.1. File
3.1.2.2. Item Path Listing Input
3.1.2.3. New Items Input
3.1.2.3.1. Naming
3.1.2.3.1.1. Types
3.1.2.3.1.1.1. Random
3.1.2.3.1.1.2. Ascending
3.1.2.3.1.1.1. Descending
3.1.2.3.1.2. Prefix
3.1.2.3.1.3. Radix
3.1.2.3.1.4. Offset
3.1.2.3.1.5. Length
3.1.3. Output
3.1.3.1. File
3.1.3.1. Path
3.2. Load Operations
3.2.1. Types
3.2.1.1. Create
3.2.1.1.1. Basic
3.2.1.1.2. Copy Mode
3.2.1.2. Read
3.2.1.2.1. Basic
3.2.1.2.2. Content Verification
3.2.1.3. Update
3.2.1.4. Delete
3.2.1.5. Noop
3.2.2. Byte Ranges Operations
3.2.2.1. Read
3.2.2.1.1. Random Ranges
3.2.2.1.2. Fixed Ranges
3.2.2.2. Update
3.2.2.2.1. Random Ranges
3.2.2.2.2. Fixed Ranges
3.2.2.2.2.1. Append
3.2.3. Composite Operations
3.2.3.1. Storage-Side Concatenation
3.2.3.1.1. S3 Multipart Upload
3.2.3.1.2. Swift Dynamic Large Objects
3.2.4. Recycle Mode
3.3. Load Steps
3.3.1. Identification
3.3.2. Limits
3.3.2.1. Operations Count Limit
3.3.2.2. Time Limit
3.3.2.3. Transfer Size Limit
3.3.2.4. End Of Input
3.3.3. Types
3.3.3.1. Linear
3.3.3.2. Pipeline
3.3.3.2.1. Delay Between Load Operations
3.3.3.3. Weighted Load
4. Output
4.1. [General](output/general)
4.2. [Metrics](output/metrics)
5. [Scenarios](input/scenarios)
