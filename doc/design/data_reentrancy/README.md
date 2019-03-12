# New Data Items

The tool should be able to create large objects (terabyte size, petabyte size and more) and do it concurrenctly via
thousands of simultaneous active channels. Mongoose tool pre-produces some fixed amount of custom data into the memory.
***[XOrShift](http://www.iro.umontreal.ca/~lecuyer/myftp/papers/xorshift.pdf)*** algorithm is used for data
pre-generation as far as it have been found the fastest. This data buffer acts like a circular one. Every data item is
defined by the unique 64-bit offset in the data ring:

![](https://latex.codecogs.com/png.latex?%5Cfn_cm%20offset_%7Bk%7D%20%3D%20k%5C%3Bmod%5C%3Bsize_%7Bring%7D)

# Custom Ring Buffer Data

Versions prior to 1.1.0 are using only one type of the ring buffer which is pre-filled with uniform random data. Since
v1.1.0 there's an option to specify a file which may be used as a source for the ring buffer content. The specified file
may be binary either text file. For demo and testing purposes there are two predefined files:

1. config/content/zerobytes

   The file contains 1MB of zero bytes.

2. conf/content/textexample

   The file contains ~30KB of text of "Rikki-Tikki-Tavi" tale by R. Kipling.

By default mongoose doesn't use any external file for the ring buffer filling so it falls back to using the ring buffer
filled with random data.

# Random Range Update

The data for updated ranges is produced from another ring buffer which shares the same size with the creation data ring
buffer. The seed for the update ring buffer is calculated as the seed of the creation data ring after reversing the
bytes order and reversing the bits order:

|                | hex value        | bin value                                                                       |
|----------------|------------------|---------------------------------------------------------------------------------|
| base value     | 7a42d9c483244167 | 0111 1010 0100 0010 1101 1001 1100 0100 1000 0011 0010 0100 0100 0001 0110 0111 |
| reversed bytes | 67412483c4d9427a | 0110 0111 0100 0001 0010 0100 1000 0011 1100 0100 1101 1001 0100 0010 0111 1010 |
| reversed bits  | 5e429b23c12482e6 | 0101 1110 0100 0010 1001 1011 0010 0011 1100 0001 0010 0100 1000 0010 1110 0110 |

> Note:
> The info above is correct for in-memory ring buffer filled with random data.
> The ring buffer filled from file has been introduced in v1.1.0. This kind of ring buffer uses
> xorshift algorithm applied to the whole buffer content in order to generate a next layer.

The data object is split into the fixed ranges of size depending on range sequence index as in the
example below for the data of size 16 bytes and range size increase factor (q) of 2:

| i(byte)  | 00 | 01 | 02 | 03 | 04 | 05 | 06 | 07 | 08 | 09 | 10 | 11 | 12 | 13 | 14 | 15 |
|----------|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|
| i(range) | 00 | 01 | 01 | 02 | 02 | 02 | 02 | 03 | 03 | 03 | 03 | 03 | 03 | 03 | 03 | 04 |

The count of possible ranges per one layer and the size of the range can be calculated for each
data item using geometric progression equations:

![](http://latex.codecogs.com/png.latex?%5Cdpi%7B120%7D%20%5Cfn_cm%20%5Cmathrm%7Bn_%7Br%7D%20%3D%20ceil%28log_%7Bq%7D%28s%20+%201%29%29%7D)

*Where: s - base data item size, q - range size increase ratio (=2).*

Then the range with number i has the following size:

![](http://latex.codecogs.com/png.latex?%5Cdpi%7B120%7D%20%5Cfn_cm%20%5Cmathrm%7Bs_%7Br%28i%29%7D%20%3D%20%5Cleft%5C%7B%5Cbegin%7Bmatrix%7D%20%5Cmathrm%7Bq%5E%7Bi%7D%20-%201%2C%7D%20%26%20%5Cmathrm%7Bi%20%3C%20n_%7Br%7D%20-%201%7D%20%5C%5C%20%5Cmathrm%7Bs-q%5E%7Bn_%7Br%7D-1%7D%20+%201%2C%7D%20%26%20%5Cmathrm%7Bi%20%3D%20n_%7Br%7D%20-%201%7D%20%5Cend%7Bmatrix%7D%5Cright.%7D)

These strict constraints allow to persist the updated ranges info into compact form of binary mask.
Note that the range count depends as logarithm of size, so it grows very slowly.
For example the item with size of 1KB contains 11 ranges while item with size of 1TB contains 31
ranges.

> Note:
> Mongoose supports updating the data items of the size up to 16 exabytes (2^64) because of using
> 64-bit masks.

There are two offsets which determine the range: its position relative to the base data item content (p(i)):

![](http://latex.codecogs.com/png.latex?%5Cdpi%7B100%7D%20%5Clarge%20%5Ctexttt%20p_%7B%5Ctexttt%20i%7D%20%3D%20%5Cfrac%7B%5Ctexttt%20b_%7B%5Ctexttt%200%7D%20%5Ctexttt%20q%5E%7B%5Ctexttt%20i%7D%20-%20%5Ctexttt%201%7D%7B%5Ctexttt%20q-%20%5Ctexttt%201%7D)

and the source data ring buffer offset calculated as ring buffer offset of base data item plus p(i)1.

# Append

Append should take care about ranges update mask value in order to not to broke the data
verification on reading.

# Verification

The data verification occurs during read back the data wriiten and possibly modified several times
after that. The tool reproduces the content of the data item using the information on its data ring,
ring offset and updated ranges mask. Below is the example of the updated ranges mask calculation
for a data item having the size of 128 bytes.

| range index (i) ->                       | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | layer/mask(hex) |
|------------------------------------------|---|---|---|---|---|---|---|---|---|---|-----------------|
| w/o range updates:                       | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0/0             |
| after 1st single random range update:    | 0 | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0/8             |
| after 2nd single random range update:    | 0 | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 1 | 0 | 0/108           |
| after next several random range updates: | 1 | 1 | 0 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | 0/13b           |
| fully filled 1st layer:                  | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 0/3ff           |
| 2nd layer updated:                       | 0 | 0 | 0 | 1 | 0 | 1 | 0 | 0 | 0 | 0 | 1/28            |
| fully filled 2nd layer:                  | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1/3ff           |
| 3rd layer updated:                       | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 1 | 0 | 0 | 2/84            |

> Note:
> A user should be sure about the same data ring is used for data verification as the one used for
> data creation/modification in another run.

# Enhancement Proposal

The new approach addresses the following issues:

1. Fixed range boundaries.
2. Strong range size dependency on the range position.
3. Inability to specify the average updated byte count to the total object's byte count ratio ("updates density").

## Generation

* Use circular buffer of any (custom) data.
* A circular buffer start offset for the object with a specified ID (X) is a hash function result from this ID (k = hash(X)).

  ![](https://latex.codecogs.com/png.latex?%5Cfn_cm%20offset_%7Bk%7D%20%3D%20k%5C%3Bmod%5C%3Bsize_%7Bring%7D)

## Modification

1. Determine the modification counter (m) for the object which has been (possibly) already updated.

   The modification number is 0 initially.

2. The configured ranges count is ***N*** and ranges count to update per request is ***n***.

   A user is able to control "average updates density" which may be calculated as:

   ![](http://latex.codecogs.com/png.latex?%5Cdpi%7B100%7D%20%5Clarge%20%5Crho%20%3D%20%5Cfrac%7Bn%7D%7BN%7D)

3. Calculate the average range size SN:

   ![](http://latex.codecogs.com/png.latex?%5Cdpi%7B100%7D%20%5Clarge%20S_N%20%3D%20%5Cfrac%7BS%7D%7BN%7D)

   Where ***S*** is the total object size.

4. For each range (i) calculate the pseudo-random and re-entrant values of the range start offset ***O(i)***:

   * O(0) = 0

   * O(i > 0) = O(i + 1) + f(S(N), i)

   Where f(S(N), i) is a hash function which generates a reproducible value in the range of:

   ![](http://latex.codecogs.com/png.latex?%5Cdpi%7B100%7D%20%5Clarge%20%5Cleft%20%5B%20-%5Cfrac%7BS_N%7D%7B2%7D%3B%20%5Cfrac%7BS_N%7D%7B2%7D%20%5Cright%20%5D)

   So the byte ranges boundaries will not overlap.

5. Choose the n byte ranges using a hash function n times:

   ![](http://latex.codecogs.com/png.latex?%5Cdpi%7B100%7D%20%5Clarge%20k_%7Bi+1%7D%20%3D%20g%28k_i%2C%20X%29)

   Where ***k*** is the next byte range index which is selected for update.

   Note: there's a requirement for this hash function (g):
   it should produce each index (k) in the range of [0; n) only once.

6. Update the selected byte ranges with the data taken from the next ring buffer layer.

   The next ring buffer layer is produced using ***xorshift*** function from the previous one.

   Note: the persisted mutation counter (m = m + n) is additive so it's enough the persist the sole
   number (m) for many update invocations per object.

## Verification

1. Determine the mutation counter (m) for the object which has been (possibly) updated.

2. The layer number may be calculated as:

   L = m div N

3. The count of the byte ranges updated on this layer is:

   n = m mod N
