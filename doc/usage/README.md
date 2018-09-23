# Contents

1. Input<br/>
1.1. [Defaults](defaults/README.md)<br/>
1.2. [Configuration](configuration/README.md)<br/>
1.2.1. [CLI](configuration/README.md#11-cli)<br/>
1.2.2. [Reference Table](configuration/README.md#12-reference-table)<br/>
1.2.3. [Parameterizing](configuration/README.md#2-parameterization)<br/>
1.2.4. Storage Drivers<br/>
1.2.4.1. [Netty Based](../../storage/driver/coop/netty/README.md)<br/>
1.2.4.1.1. [HTTP Storage Drivers](../../storage/driver/coop/netty/http/README.md)<br/>
1.2.4.1.1.1. [Atmos](../../storage/driver/coop/netty/http/atmos/README.md)<br/>
1.2.4.1.1.2. [S3](../../storage/driver/coop/netty/http/s3/README.md)<br/>
1.2.4.1.1.3. [Swift](../../storage/driver/coop/netty/http/swift/README.md)<br/>
1.2.4.2. [Filesystem](../../storage/driver/coop/nio/fs/README.md)<br/>
1.3. [Scenarios](../input/README.md#3-scenarios)<br/>
2. Output<br/>
2.1. [General](../output/general/README.md)<br/>
2.2. [Metrics](../output/metrics/README.md)<br/>
3. Load Generation<br/>
3.1. Items<br/>
3.1.1. Types<br/>
3.1.1.1. Data<br/>
3.1.1.1.1. Size<br/>
3.1.1.1.1.1. Fixed<br/>
3.1.1.1.1.2. Random<br/>
3.1.1.1.2. Payload<br/>
3.1.1.1.2.1. Random Using Seed<br/>
3.1.1.1.2.2. From External File<br/>
3.1.1.2. Path<br/>
3.1.1.3. Token<br/>
3.1.2. Input<br/>
3.1.2.1. File<br/>
3.1.2.2. Item Path Listing Input<br/>
3.1.2.3. New Items Input<br/>
3.1.2.3.1. Naming<br/>
3.1.2.3.1.1. Types<br/>
3.1.2.3.1.1.1. Random<br/>
3.1.2.3.1.1.2. Ascending<br/>
3.1.2.3.1.1.1. Descending<br/>
3.1.2.3.1.2. Prefix<br/>
3.1.2.3.1.3. Radix<br/>
3.1.2.3.1.4. Offset<br/>
3.1.2.3.1.5. Length<br/>
3.1.3. Output<br/>
3.1.3.1. File<br/>
3.1.3.1. Path<br/>
3.2. Load Operations<br/>
3.2.1. Types<br/>
3.2.1.1. Create<br/>
3.2.1.1.1. Basic<br/>
3.2.1.1.2. Copy Mode<br/>
3.2.1.2. Read<br/>
3.2.1.2.1. Basic<br/>
3.2.1.2.2. Content Verification<br/>
3.2.1.3. Update<br/>
3.2.1.4. Delete<br/>
3.2.1.5. Noop<br/>
3.2.2. Byte Ranges Operations<br/>
3.2.2.1. Read<br/>
3.2.2.1.1. Random Ranges<br/>
3.2.2.1.2. Fixed Ranges<br/>
3.2.2.2. Update<br/>
3.2.2.2.1. Random Ranges<br/>
3.2.2.2.2. Fixed Ranges<br/>
3.2.2.2.2.1. Append<br/>
3.2.3. Composite Operations<br/>
3.2.3.1. Storage-Side Concatenation<br/>
3.2.3.1.1. S3 Multipart Upload<br/>
3.2.3.1.2. Swift Dynamic Large Objects<br/>
3.2.4. Recycle Mode<br/>
3.3. Load Steps<br/>
3.3.1. Identification<br/>
3.3.2. Limits<br/>
3.3.2.1. Operations Count Limit<br/>
3.3.2.2. Time Limit<br/>
3.3.2.3. Transfer Size Limit<br/>
3.3.2.4. End Of Input<br/>
3.3.3. Types<br/>
3.3.3.1. Linear<br/>
3.3.3.2. [Pipeline](../../load/step/pipeline/README.md)<br/>
3.3.3.3. [Weighted Load](../../load/step/weighted/README.md)<br/>
4. [Load Scaling](scaling)
4.1. [Rate](scaling/REAMDE.md#1-rate)<br/>
4.2. [Concurrency](scaling/REAMDE.md#2-concurrency)<br/>
4.3. [Distributed Mode](scaling/REAMDE.md#3-distributed-mode)<br/>
