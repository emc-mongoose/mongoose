* Indent code with TAB having width of 4 characters
* Code line width: 100 characters
* Long line example:
```java
nextSubTask = new BasicPartialMutableDataIoTask<>(
    ioType, nextPart, srcPath, dstPath, equalPartsCount, this
);
```

* If something may be declared as *final* it should be declared as *final*
* Take care about the performance in the critical places:
  * Avoid *frequent* objects instantiation
  * Avoid unnecessary *frequent* allocation
  * Avoid *frequent* method calls if possible
  * Avoid deep call stack if possible
  * Avoid I/O threads blocking
  * Avoid anonymous classes in the time-critical code
  * Avoid non-static inner classes in the time-critical code
  * Use thread locals (encryption, string builders)
  * Use buffering, buffer everything
  * Use batch processing if possible
