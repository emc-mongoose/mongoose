# Unit Tests

```bash
./gradlew clean test
```

# Integration Tests

```bash
./gradlew clean integrationTest
```

# System Tests

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

## Containerized Tests

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
