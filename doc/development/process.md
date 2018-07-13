# Contents

1. [Roles](#1-roles)
2. [Priorities](#2-priorities)
3. [Scopes](#3-scopes)
4. [Versions](#4-versions)
5. [Issue Reporting](#5-issue-reporting)
5.1. [Defect](#51-defect)
5.2. [Feature](#52-feature)
5.2.1. [Input](#521-input)
5.2.2. [Lifecycle](#522-lifecycle)

# 1. Roles

| Name | Responsibilities | Current Assignees
|------|------------------|------------------
| User | Report issues in the [expected way](#5-issue-reporting) | N/A
| Developer | <ul><li>Development</li><li>Testing</li><li>Automation</li><li>Documentation</li></ul> | <ul><li>Veronika Kochugova</li><li>Andrey Kurilov</li><ul>
| Owner | <ul><li>*Next* version scope definition</li><li>Roadmap definition</li><li>User interaction</li></ul> | Andrey Kurilov
| Manager | The explicit *next* version scope approval | ********

# 2. Priorities

| Priority | Description | Version to Accept
|----------|-------------|------------------
| P0       | Critical defects w/o workaround: <ul><li>Crash</li><li>Hang</li><li>Not functioning</li><li>Functioning incorrectly</li><li>Performance degradation</li></ul> | bugfix
| P1       | <ul><li>Non-critical defects</li><li>Workaround is available for the users</li></ul> | next
| P2       | New feature | next
| P3       | <ul><li>Non-release: <ul><li>Demo</li><li>Investigation</li><li>Release scope definition</li></ul><li>Improvements: <ul><li>Performance</li><li>Usability</li><li>Cosmetic</li></ul></li></ul> | backlog

***Note***:
> A Mongoose developer should not rely on the bug probability/frequency

# 3. Scopes

| Name    | Version Number | Description | Scope Priority Threshold |
|---------|----------------|-------------|-------|
| latest  | &lt;X&gt;.&lt;Y&gt;.&lt;Z&gt; | The latest released version | N/A
| bugfix  | &lt;X&gt;.&lt;Y&gt;.&lt;Z+1&gt; | The version which is considered to be released ASAP<br/>Interrupts the tasks for the *next* | P0 |
| next    | &lt;X&gt;.&lt;Y+1&gt;.0 | The next version which is considered to include the new features and fixes for the non-critical bugs | P1*, P2**
| backlog | N/A | Backlog equivalent | P3

(*)  P1 tasks are acceptable for the *next* version until the release

(**) P2 tasks are acceptable for the *next* version until [PM](#1-roles) approves the scope

# 4. Versions

Mongoose uses the [semantic versioning](http://semver.org/). The following interfaces are mentioned as the subject of
the backward compatibility:
1. API
2. Output files containing the metrics
3. Item list files
4. Scenario files format
5. Configuration options

# 5. Issue Reporting

## 5.1. Defect

When reporting a defect make sure the ticket contains the following info:

1. Specific conditions.
   1. The mongoose version used.
   2. The particular CLI command.

      *Leave only the essential things to reproduce: try to check if possible if the bug is reproducible using
 single local storage driver, different concurrency level, item data size, etc.*

   3. The scenario file used.

      *Note that the default scenario is used implicitly. Please don't clutter with large bedsheet scenario files.
      Simplify the scenario leaving only the essential things.*

2. Expected behavior.

   *Specifying the reference to the particular documentation part describing the expected behavior would be great.*

3. Observed behavior.

   *Error message, errors.log output file, etc.*

## 5.2. Feature

### 5.2.1. Input

A requester should supply the information necessary to deliver any new
functionality.

1. Introduction

   **Mandatory for a requester**

   Purpose. Which particular problem should be solved with Mongoose?

   1. Links

      *Optional for a requester*

      The links to the related documents and literature.

2. Requirements

   1. Functional

      **Mandatory for a requester**

      The list of the **functional** requirements. The list should be numbered in order to make it easy to refer to the
      particular requirements item.

      1. Mandatory Requirements

         **Mandatory for a requester**

      2. Additional Requirements

         *Optional for a requester*

      3. Possible Enhancements

         *Optional for a requester*

   2. Performance

      **Mandatory for a requester**

3. Limitations

   *Optional for a requester*

   List of limitations.

4. Proposal

    *Optional for a requester*

    1. Design

       Describe the possible way to get the required functionality working.

    3. Input

       1. Configuration

          Describe the possible new configuration options involved.

       2. Other Input

          Any other input required. Scenarios, external files, etc.

    4. Output

       1. Standard Output
       2. Log Files
       3. Other Output


### 5.2.2. Lifecycle

1. Requested
2. Requirements Available
2. Specification Available
3. Estimated
4. Approved by [PM](#1-roles) = included into the *next* version scope
5. Under Development
6. Tested Manually
7. Usage Documentation Available
8. Released
9. Covered With Automated Tests
10. Enhanced

