The branching rules in the Mongoose project are simple:
![branching](../../images/branching.png)
1. There's a separate "archive" branch for each release with name "release-vX.Y.Z" where "X.Y.Z" is a version number.
2. The integration branch is named "integration". All feature PRs should base on the integration branch.
3. The PR/merge to the master branch is performed when the release decision is made (all tests are ok, documentation is added, etc).
