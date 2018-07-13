# Contents

1. [Roles](#1-roles)<br/>
2. [Priorities](#2-priorities)<br/>
&nbsp;2.1. [Limitations](#21-limitations)<br/>
3. [Scopes](#3-scopes)<br/>
4. [Versions](#4-versions)<br/>
5. [Issue Reporting](#5-issue-reporting)<br/>
&nbsp;5.1. [Defect](#51-defect)<br/>
&nbsp;5.2. [Feature](#52-feature)<br/>
&nbsp;&nbsp;5.2.1. [Input](#521-input)<br/>
&nbsp;&nbsp;5.2.2. [Lifecycle](#522-lifecycle)<br/>

# 1. Roles

| Name | Responsibilities | Current Assignees
|------|------------------|------------------
| User | Report issues in the [expected way](#5-issue-reporting) | N/A
| Developer | <ul><li>Development</li><li>Testing</li><li>Automation</li><li>Documentation</li></ul> | <ul><li>Veronika Kochugova</li><li>Andrey Kurilov</li><ul>
| Owner | <ul><li>[*Next* version scope](#3-scopes) definition</li><li>Roadmap definition</li><li>User interaction</li></ul> | Andrey Kurilov
| Manager | The explicit [*next* version scope](#3-scopes) approval | ********

# 2. Priorities

| Priority | Description | Version to Accept
|----------|-------------|------------------
| P0       | Critical defects w/o workaround: <ul><li>Crash</li><li>Hang</li><li>Not functioning</li><li>Functioning incorrectly</li><li>Performance degradation</li></ul> | bugfix
| P1       | <ul><li>Non-critical defects</li><li>Workaround is available for the users</li></ul> | next
| P2       | New feature | next
| P3       | <ul><li>Non-release: <ul><li>Demo</li><li>Investigation</li><li>Release scope definition</li></ul><li>Improvements: <ul><li>Performance</li><li>Usability</li><li>Cosmetic</li></ul></li></ul> | backlog

## 2.1. Limitations

The corresponding impact probability/frequency is not taken into account in the process currently. For example, all
defects are assumed to be equally frequently occurring and affecting same users, regardless the particular scenario/
use case. This approach is used due to the lack of the sufficient statistical information about the Mongoose usage.

# 3. Scopes

| Name    | Version Number | Description | Scope Priority Threshold |
|---------|----------------|-------------|-------|
| latest  | &lt;X&gt;.&lt;Y&gt;.&lt;Z&gt; | The latest released version | N/A
| bugfix  | &lt;X&gt;.&lt;Y&gt;.&lt;Z+1&gt; | The version which is considered to be released ASAP<br/>Interrupts the tasks for the *next* | P0 |
| next    | &lt;X&gt;.&lt;Y+1&gt;.0 | The next version which is considered to include the new features and fixes for the non-critical bugs | P1*, P2**
| backlog | N/A | Backlog equivalent | P3

(*)  P1 tasks are acceptable for the *next* scope until the corresponding version release

(**) P2 tasks are acceptable for the *next* scope until [PM](#1-roles) approves the scope

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
      Leave only the essential things to reproduce: try to check if possible if the bug is reproducible w/o distributed
      mode, different concurrency level, item data size, etc.
   3. The scenario file used.
      Don't clutter with large scenario files. Simplify the scenario leaving only the essential things.
2. Expected behavior.
   Specify the reference to the particular documentation part describing the expected behavior, if possible.
3. Observed behavior.
   Error message, errors.log output file, etc.

## 5.2. Feature

### 5.2.1. Input

A requester should supply the information necessary to deliver any new
functionality.

1. **Introduction***<br/>
   Purpose. Which particular problem should be solved with Mongoose?
   1. Links<br/>
      The links to the related documents and literature.
2. Requirements
   1. **Functional***<br/>
      The list of the *functional* requirements. The list should be numbered in order to make it easy to refer to the
      particular requirements item.
      1. **Mandatory Requirements***
      2. Additional Requirements
      3. Possible Enhancements
   2. **Performance***
3. Limitations<br/>
   List of limitations.
4. Proposal
    1. Design<br/>
       Describe the possible way to get the required functionality working.
    2. Input
       1. Configuration<br/>
          Describe the possible new configuration options involved.
       2. Other Input<br/>
          Any other input required. Scenarios, external files, etc.
    4. Output
       1. Standard Output
       2. Log Files
       3. Other Output

(*) The items mandatory to specify for a requester are highlighted with **bold** text

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
