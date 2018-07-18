# 1. Roles

| Name | Responsibilities | Current Assignees
|------|------------------|------------------
| User | Report the issues | Definitely unknown
| Developer | <ul><li>Development</li><li>Testing</li><li>Automation</li><li>Documentation</li></ul> | <ul><li>Veronika Kochugova</li><li>Andrey Kurilov</li><ul>
| Owner | <ul><li>The next version scope definition</li><li>Roadmap definition</li><li>User interaction</li></ul> | Andrey Kurilov
| Manager | The explicit scopes approval | ********

# 2. Versions

## 2.1. Backward Compatibility

The following interfaces are mentioned as the subject of the backward compatibility:
1. Input (item list files, scenario files, configuration options)
2. Output files containing the metrics
3. API

## 2.2. Numbers

Mongoose uses the [semantic versioning](http://semver.org/). This means that the ***X.Y.Z*** version notation is used:

* ***X***<br/>
    Major version number. Points to significant design and interfaces change. The *backward compatibility* is **not
    guaranteed**.
* ***Y***<br/>
    Minor version number. The *backward compatibility* is guaranteed.
* ***Z***<br/>
    Patch version number. Includes only the defect fixes.

# 3. Tasks

## 3.1. States

| State     | Description |
|-----------|-------------|
| OPEN      | All new tasks should have this state. The tasks are selected from the set of the *OPEN* tasks for the proposal and review process. The task is updated w/ the corresponding comment but left in the *OPEN* state if it's considered incomplete/incorrect. Also incomplete/incorrect task should be assigned back to the reporter.
| PROPOSED  | The task is selected for the approval by the *manager*.
| DEFERRED  | Manager has approved the task to be processed after the next major/minor (non-patch) version is released.
| ACCEPTED  | Manager approved the task to be processed before the next major/minor (non-patch) version is released.
| ESCALATED | Critical defect which interrupts all *DEFERRED*/*ACCEPTED* tasks processing. Causes the new *patch* version release ASAP.
| RESOLVED  | Task is done and the corresponding changes are merged into the `integration` branch.
| CLOSED    | Task is done and the corresponding changes are merged into the `master` branch (= version release, availability for the user).

**Note**:
> The corresponding impact probability/frequency is not taken into account in the process currently. For example, all
> defects are assumed to be equally frequently occurring and affecting same users, regardless the particular
> scenario/use case. This approach is used due to the lack of the sufficient statistical information about the Mongoose
> usage.

## 3.2. Types

### 3.2.1. Defects

| Priority     | Conditions | Target state as a result of the review
|--------------|------------|---------------------------------------
| Critical     | No workaround available **and** any of the following: <ul><li>Crash</li><li>Hang</li><li>Not functioning</li><li>Functioning incorrectly</li><li>Performance degradation</li></ul> | `ESCALATED`
| Non-critical | Not *critical* **and** not *minor* | `ACCEPTED` (for the next minor/major version)
| Minor        | Any of the following: <ul><li>Usability issue</li><li>Cosmetic</li></ul> | `OPEN` or `ACCEPTED`

**Specific properties**:

| Name                  | Applicable task type | Who is responsible to specify  | Notes
|-----------------------|----------------------|--------------------------------|-------|
| Affected version      | Defect               | Reporter: user/developer/owner | Only the *latest* version may be used for the defect reporting. The task should be *rejected* if the reported version is not *latest*.
| Fix version           | Defect               | Reviewer: developer/owner      |
| Start command/request | Defect               | Reporter: user/developer/owner | Leave only the essential things to reproduce: try to check if possible if the bug is reproducible w/o distributed mode, different concurrency level, item data size, etc.
| Scenario              | Defect               | Reporter: user/developer/owner | Don't clutter with large scenario files. Simplify the scenario leaving only the essential things.
| Steps                 | Defect               | Reporter: user/developer/owner |
| Expected behaviour    | Defect               | Reporter: user/developer/owner | The reference to the particular documentation part describing the expected behavior is preferable.
| Observed behaviour    | Defect               | Reporter: user/developer/owner | Error message, errors.log output file, etc.

### 3.2.2. Stories

**Specific properties**:

| Name                  | Applicable task type | Who is responsible to specify  | Notes
|-----------------------|----------------------|--------------------------------|-------|
| Purpose               | Story                | Reporter: user/developer/owner | Which particular problem should be solved with Mongoose? The links to the related documents and literature are encouraged.
| Requirements          | Story                | Reporter: user/developer/owner | Both functional and performance requirements are mandatory. Optionally the additional requirements/possible enhancements may be specified.
| Limitations           | Story                | Reviewer: developer/owner      |

### 3.2.3. Tasks and sub-tasks

**Specific properties**:

| Name                  | Applicable task type | Who is responsible to specify  | Notes
|-----------------------|----------------------|--------------------------------|-------|
| Version               | Task/Sub-task        | Reviewer: developer/owner      |
| Description           | Task/Sub-task        | Reporter: user/developer/owner |
