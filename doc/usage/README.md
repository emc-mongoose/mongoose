Topics

# 1. Input<br/>
## 1.1. [Defaults](defaults)<br/>
## 1.2. [Configuration](../interfaces/input/configuration)<br/>
### 1.2.1. [CLI](../interfaces/input/configuration#11-cli)<br/>
### 1.2.2. [Configuration Reference Table](../interfaces/input/configuration#12-reference-table)<br/>
### 1.2.3. [Parameterizing](../interfaces/input/configuration#2-parameterization)<br/>
### 1.2.4. Storage Drivers<br/>
#### 1.2.4.1. [Netty Based Storage Drivers](../../storage/driver/coop/netty)<br/>
##### 1.2.4.1.1. [HTTP Storage Drivers](../../storage/driver/coop/netty/http)<br/>
###### 1.2.4.1.1.1. [Atmos](../../storage/driver/coop/netty/http/atmos)<br/>
###### 1.2.4.1.1.2. [S3](../../storage/driver/coop/netty/http/s3)<br/>
###### 1.2.4.1.1.3. [Swift](../../storage/driver/coop/netty/http/swift)<br/>
#### 1.2.4.2. [Filesystem Storage Driver](../../storage/driver/coop/nio/fs)<br/>
## 1.3. [Scenarios](../interfaces/input/scenarios)<br/>
# 2. [Output](../interfaces/output)<br/>
## 2.1. [General Output](../interfaces/output#1-general)<br/>
### 2.1.1. [Logging](doc/interfaces/output#11-logging-subsystem)<br/>
#### 2.1.1.1. [Logs Separation By Load Step Id](doc/interfaces/output#111-load-step-id)<br/>
#### 2.1.1.2. [Console](doc/interfaces/output#112-console)<br/>
#### 2.1.1.3. [Files](doc/interfaces/output#113-files)<br/>
#### 2.1.1.4. [Configuration](doc/interfaces/output#114-log-configuration)<br/>
### 2.1.2. [Output Categories](doc/interfaces/output#12-categories)<br/>
#### 2.1.2.1. [CLI arguments log](doc/interfaces/output#121-cli-arguments)<br/>
#### 2.1.2.2. [Configuration dump](doc/interfaces/output#122-configuration-dump)<br/>
#### 2.1.2.3. [Scenario dump](doc/interfaces/output#123-scenario-dump)<br/>
#### 2.1.2.4. [3rd Party Messages](doc/interfaces/output#124-3rd-party-log-messages)<br/>
#### 2.1.2.5. [Error Messages](doc/interfaces/output#125-error-messages)<br/>
#### 2.1.2.6. [General Messages](doc/interfaces/output#126-general-messages)<br/>
#### 2.1.2.7. [Item List Files](doc/interfaces/output#127-item-list-files)<br/>
## 2.2. [Metrics Output](../interfaces/output#2-metrics)<br/>
### 2.2.1. [Load Average](../interfaces/output#21-load-average)<br/>
### 2.2.2. [Load Step Summary](../interfaces/output#22-load-step-summary)<br/>
### 2.2.3. [Operation Traces](../interfaces/output#23-operation-traces)<br/>
### 2.2.4. [Accounting Activation By The Threshold](../interfaces/output#24-threshold)<br/>
# 3. Load Generation<br/>
## 3.1. Items<br/>
### 3.1.1. [Item Types](item/types)<br/>
#### 3.1.1.1. [Data Item](item/types#1-data)<br/>
##### 3.1.1.1.1. [Data Item Size](item/types#11-size)<br/>
###### 3.1.1.1.1.1. [Fixed Size](item/types#111-fixed)<br/>
###### 3.1.1.1.1.2. [Random Size](item/types#112-random)<br/>
##### 3.1.1.1.2. [Data Item Payload](item/types#12-payload)<br/>
###### 3.1.1.1.2.1. [Random Payload Using A Seed](item/types#121-random-using-a-seed)<br/>
###### 3.1.1.1.2.2. [Custom Payload Using An External File](item/types#122-custom-using-an-external-file)<br/>
#### 3.1.1.2. [Path Item](item/types#2-path)<br/>
#### 3.1.1.3. [Token Item](item/types#3-token)<br/>
### 3.1.2. [Item Input](item/input)<br/>
#### 3.1.2.1. [Item Input File](item/input#1-file)<br/>
#### 3.1.2.2. [Item Path Listing Input](item/input#2-item-path-listing-input)<br/>
#### 3.1.2.3. [New Items Input](item/input#3-new-items-input)<br/>
##### 3.1.2.3.1. [Item Naming](item/input#31-naming)<br/>
###### 3.1.2.3.1.1. [Types](item/input#311-types)<br/>
3.1.2.3.1.1.1. [Random](item/input#3111-random)<br/>
3.1.2.3.1.1.2. [Serial](item/input#3112-serial)<br/>
###### 3.1.2.3.1.2. [Prefix](item/input#312-prefix)<br/>
###### 3.1.2.3.1.3. [Radix](item/input#313-radix)<br/>
###### 3.1.2.3.1.4. [Offset](item/input#314-offset)<br/>
###### 3.1.2.3.1.5. [Length](item/input#315-length)<br/>
### 3.1.3. [Item Output](item/output)<br/>
#### 3.1.3.1. [Item Output File](item/output#1-file)<br/>
#### 3.1.3.2. [Item Output Path](item/output#2-path)<br/>
##### 3.1.3.2.1. [Variable Path](item/output#21-variable)<br/>
###### 3.1.3.2.1.1. [Multiuser Variable Path](item/output#211-multiuser)<br/>
## 3.2. Load Operations<br/>
### 3.2.1. [Load Operation Types](load/operations/types)<br/>
#### 3.2.1.1. [Create](load/operations/types#1-create)<br/>
##### 3.2.1.1.1. [Basic](load/operations/types#11-basic)<br/>
##### 3.2.1.1.2. [Copy Mode](load/operations/types#12-copy-mode)<br/>
#### 3.2.1.2. [Read](load/operations/types#2-read)<br/>
##### 3.2.1.2.1. [Basic](load/operations/types#21-basic)<br/>
##### 3.2.1.2.2. [Content Verification](load/operations/types#22-content-verification)<br/>
#### 3.2.1.3. [Update](load/operations/types#3-update)<br/>
#### 3.2.1.4. [Delete](load/operations/types#4-delete)<br/>
#### 3.2.1.5. [Noop](load/operations/types#5-noop)<br/>
### 3.2.2. [Byte Ranges Operations](load/operations/byte_ranges)<br/>
#### 3.2.2.1. [Random Ranges](load/operations/byte_ranges#41-random-ranges)<br/>
#### 3.2.2.2. [Fixed Ranges](load/operations/byte_ranges#42-fixed-ranges)<br/>
##### 3.2.2.2.1. [Append](load/operations/byte_ranges#421-append)<br/>
### 3.2.3. [Composite Operations](load/operations/composite)<br/>
#### 3.2.3.1. [Storage-Side Concatenation](load/operations/composite#1-storage-side-concatenation)<br/>
##### 3.2.3.1.1. [S3 Multipart Upload](load/operations/composite#131-s3-multipart-upload)<br/>
##### 3.2.3.1.2. [Swift Dynamic Large Objects](load/operations/composite#132-swift-dynamic-large-objects)<br/>
### 3.2.4. [Load Operations Recycling](load/operations/recycling)<br/>
## 3.3. [Load Steps](load/steps)<br/>
### 3.3.1. [Load Step Identification](load/steps#1-identification)<br/>
### 3.3.2. [Load Step Limits](load/steps#2-limits)<br/>
#### 3.3.2.1. [Operations Count](load/steps#21-operations-count)<br/>
#### 3.3.2.2. [Time](load/steps#22-time)<br/>
#### 3.3.2.3. [Transfer Size](load/steps#23-transfer-size)<br/>
#### 3.3.2.4. [End Of Input](load/steps#24-end-of-input)<br/>
### 3.3.3. Types<br/>
#### 3.3.3.1. [Linear Load](../../load/step/linear)<br/>
#### 3.3.3.2. [Pipeline Load](../../load/step/pipeline)<br/>
#### 3.3.3.3. [Weighted Load](../../load/step/weighted)<br/>
# 4. [Load Scaling](scaling)<br/>
## 4.1. [Rate](scaling#1-rate)<br/>
## 4.2. [Concurrency](scaling#2-concurrency)<br/>
## 4.3. [Distributed Mode](scaling3-distributed-mode)<br/>
