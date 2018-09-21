# Contents

1. [Defaults](defaults)<br/>
2. Input<br/>
2.1. [Defaults](defaults)<br/>
2.2. Configuration
2.2.1. Parameterizing<br/>
2.2.1.1 Request HTTP Headers Parameterizing<br/>
2.2.1.2. Output Path Parameterizing<br/>
2.2.1.3. Multiuser Load<br/>
2.2.2. Storage Drivers<br/>
2.2.2.1. [Netty Based](../../storage/driver/coop/netty)<br/>
2.2.2.1.1. Node Balancing<br/>
2.2.2.1.2. SSL/TLS<br/>
2.2.2.1.2. Connection Timeout<br/>
2.2.2.1.3. I/O Buffer Size<br/>
2.3. [Scenarios](input/scenarios)<br/>
3. Output<br/>
3.1. [General](output/general)<br/>
3.2. [Metrics](output/metrics)<br/>
4. Load Generation<br/>
4.1. Items<br/>
4.1.1. Types<br/>
4.1.1.1. Data<br/>
4.1.1.1.1. Size<br/>
4.1.1.1.1.1. Fixed<br/>
4.1.1.1.1.2. Random<br/>
4.1.1.1.2. Payload<br/>
4.1.1.1.2.1. Random Using Seed<br/>
4.1.1.1.2.2. From External File<br/>
4.1.1.2. Path<br/>
4.1.1.3. Token<br/>
4.1.2. Input<br/>
4.1.2.1. File<br/>
4.1.2.2. Item Path Listing Input<br/>
4.1.2.3. New Items Input<br/>
4.1.2.3.1. Naming<br/>
4.1.2.3.1.1. Types<br/>
4.1.2.3.1.1.1. Random<br/>
4.1.2.3.1.1.2. Ascending<br/>
4.1.2.3.1.1.1. Descending<br/>
4.1.2.3.1.2. Prefix<br/>
4.1.2.3.1.3. Radix<br/>
4.1.2.3.1.4. Offset<br/>
4.1.2.3.1.5. Length<br/>
4.1.3. Output<br/>
4.1.3.1. File<br/>
4.1.3.1. Path<br/>
4.2. Load Operations<br/>
4.2.1. Types<br/>
4.2.1.1. Create<br/>
4.2.1.1.1. Basic<br/>
4.2.1.1.2. Copy Mode<br/>
4.2.1.2. Read<br/>
4.2.1.2.1. Basic<br/>
4.2.1.2.2. Content Verification<br/>
4.2.1.3. Update<br/>
4.2.1.4. Delete<br/>
4.2.1.5. Noop<br/>
4.2.2. Byte Ranges Operations<br/>
4.2.2.1. Read<br/>
4.2.2.1.1. Random Ranges<br/>
4.2.2.1.2. Fixed Ranges<br/>
4.2.2.2. Update<br/>
4.2.2.2.1. Random Ranges<br/>
4.2.2.2.2. Fixed Ranges<br/>
4.2.2.2.2.1. Append<br/>
4.2.3. Composite Operations<br/>
4.2.3.1. Storage-Side Concatenation<br/>
4.2.3.1.1. S3 Multipart Upload<br/>
4.2.3.1.2. Swift Dynamic Large Objects<br/>
4.2.4. Recycle Mode<br/>
4.3. Load Steps<br/>
4.3.1. Identification<br/>
4.3.2. Limits<br/>
4.3.2.1. Operations Count Limit<br/>
4.3.2.2. Time Limit<br/>
4.3.2.3. Transfer Size Limit<br/>
4.3.2.4. End Of Input<br/>
4.3.3. Types<br/>
4.3.3.1. Linear<br/>
4.3.3.2. Pipeline<br/>
4.3.3.2.1. Delay Between Load Operations<br/>
4.3.3.3. Weighted Load<br/>
5. [Load Scaling](scaling)
5.1. [Rate](scaling/REAMDE.md#1-rate)<br/>
5.2. [Concurrency](scaling/REAMDE.md#2-concurrency)<br/>
5.3. [Distributed Mode](scaling/REAMDE.md#3-distributed-mode)<br/>
