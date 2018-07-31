# Branches

The branching rules in the Mongoose project are simple:
![branching](../../images/branching.png)
1. There's a separate "archive" branch for each release with name "release-vX.Y.Z" where "X.Y.Z" is a version number.
2. The integration branch is named "integration". All feature PRs should base on the integration branch.
3. The PR/merge to the master branch is performed when the release decision is made (all tests are ok, documentation is added, etc).

# Code Style

## General
* Indent code with TAB having width of 4 characters
* Code line width: 120 characters
* If interface is named `Foo` then:
  * Abstract implementation should be named as `FooBase`
  * Default concrete implementation should be names as `FooImpl`
* Any field/local variable should be *final* if possible

## Exception Handling

The threads are not used in the usual way (*fibers* are used instead for multitasking purposes). Therefore, having an
`InterruptedException` thrown means that the run was interrupted externally. To stop the run, it's necessary to pass
the specific exception to the uppermost level of the call stack. However, the `InterruptedException` is a checked
exception and usually couldn't be passed outward. The specific unchecked `InterruptRunException` is used for this
purpose. This imposes the restrictions on the exceptions handling:

* If the `InterruptedException` is caught the `InterruptRunException` should be thrown:
    ```java
    try {
        foo(); // may throw an InterruptedException
    } catch(final InterruptedException e) {
        throw new InterruptRunException(e);
    }
    ```

* The following exceptions catching should be avoided as far as special `InterruptRunException` may be swallowed
occasionally:
    1. `Throwable`
    2. `Exception`
    3. `RuntimeException`

## Performance
Take care about the performance in the critical places:
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

# Testing

## Unit Tests

```bash
./gradlew clean test
```

## Integration Tests

```bash
./gradlew clean integrationTest
```

## System Tests

The system tests use the [JUnit parameterization](https://github.com/junit-team/junit4/wiki/Parameterized-tests). The parameter values are taken from the environment. The list of the system tests parameters below:

| Parameter Name | Acceptable Values                      | Values Meaning
|----------------|----------------------------------------|----------------
| STORAGE_TYPE   | s3, atmos, fs, swift                   | (the same)
| RUN_MODE       | local, distributed                     | 1, 2
| CONCURRENCY    | unlimited, single, small, medium, high | 0, 1, 10, 100, 1000
| ITEM_SIZE      | empty, small, medium, large, huge      | 0, 10KB, 1MB, 100MB, 10GB

To run the system tests for the particular case use the commands like
below:

```bash
export MONGOOSE_VERSION=testing
export STORAGE_TYPE=s3
export RUN_MODE=distributed
export CONCURRENCY=medium
export ITEM_SIZE=small
./gradlew clean systemTest --tests com.emc.mongoose.system.CircularAppendTest
```

Note that some system tests will not run for some parameter values. The acceptable parameter values are declared explicitly in the `.travis.yml` file.

### Containerized Tests

Since v4.0.0 all system tests are containerized. To run a system test locally it's necessary to
prepare 3 testing Docker images manually:

```bash
docker build \
    -f docker/Dockerfile \
    -t emcmongoose/mongoose:testing \
    .
docker push emcmongoose/mongoose:testing
```

```bash
docker build \
    --build-arg MONGOOSE_VERSION=testing \
    -f docker/Dockerfile.scripting-groovy \
    -t emcmongoose/mongoose-scripting-groovy:testing \
    .
docker push emcmongoose/mongoose-scripting-groovy:testing
```

```bash
docker build \
    --build-arg MONGOOSE_VERSION=testing \
    -f docker/Dockerfile.scripting-jython \
    -t emcmongoose/mongoose-scripting-jython:testing \
    .
docker push emcmongoose/mongoose-scripting-jython:testing
```

Also it's necessary to supply the testing image version to the system
test via environment variable `MONGOOSE_VERSION`:

```bash
export MONGOOSE_VERSION=testing
export STORAGE_TYPE=atmos
export DRIVER_COUNT=distributed
export CONCURRENCY=medium
export ITEM_SIZE=small
./gradlew clean systemTest --tests com.emc.mongoose.system.CreateNoLimitTest
```

# Releasing

1. Ensure all [tests](testing.md) are OK
2. Ensure the new version documentation is ready
3. Create the corresponding version branch `release-v<X>.<Y>.<Z>` and set the version in the configuration to
   <X>.<Y>.<Z>
4. Share the testing build with QE and repeat this step until the qualification is OK
5. Create/replace the corresponding VCS tags:
   ```bash
   git tag -d latest
   git push origin :refs/tags/latest
   git tag -a <X>.<Y>.<Z> -m <X>.<Y>.<Z>
   git tag -a latest -m <X>.<Y>.<Z>
   git push --tags --force
   ```
6. Create the pull request to the `master` branch
7. Merge the pull request if reviewed and approved
   *Note*:
   > This is a no-return point. Making any further changes will require a new version!
8. Upload the artifacts to the Central Maven repo:
   1.
    ```bash
    ./gradlew clean uploadArchives
    ```
   2. Go to the https://oss.sonatype.org/#stagingRepositories find the corresponding repository, close and then release
      it.
9. Update the projects depending on the Mongoose's API (storage drivers, at least)
