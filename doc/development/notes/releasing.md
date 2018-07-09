# Versions And Compatibility

Mongoose uses the [semantic versioning](http://semver.org/). The
following interfaces are mentioned as the subject of the backward
compatibility:
1. API
2. Output files containing the metrics
3. Item list files
4. Scenario files format
5. Configuration options

# Steps

1. Ensure all [tests](testing.md) are OK
2. Ensure the new version documentation is ready
3. Create the corresponding version branch `release-v<X>.<Y>.<Z>` and set the version in the configuration to
   <X>.<Y>.<Z>
4. Share the testing build with QE and repeat this step until the qualification is OK
5. Create/replace the corresponding VCS tags:
   ```bash
   git tag -a <X>.<Y>.<Z> -m <X>.<Y>.<Z>
   git tag -a latest -m <X>.<Y>.<Z>
   git push --tags --force
   ```
6. Create the pull request to the `master` branch
7. Merge the pull request if reviewed and approved
8. Upload the artifacts to the Central Maven repo:
   1.
    ```bash
    ./gradlew clean uploadArchives
    ```
   2. Go to the https://oss.sonatype.org/#stagingRepositories find the corresponding repository, close and then release
      it.
9. Update the projects depending on the Mongoose's API (storage drivers, at least)
