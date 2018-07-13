Triage rules

# Steps

1. An user reports a problem

   Not all users create the corresponding tickets for reporting. A developer should create the corresponding ticket if
   an user doesn't want/unable to create the ticket. However, an user should be notified that he/she ***should***
   create a ticket the next time if the ticket was not created this time.

2. A developer decides if the observed behavior contradicts to the documentation

   Not all functionality is described in the documentation. A developer should take the responsibility to decide on his
   own if the reported issue should be considered as a bug either a new feature. The further actions depend on this
   decision:

   * Bug

     The missing requirements should be added into the documentation in this case.

   * New Feature


3. A developer reproduces the bug and confirms it

   Not all bugs are reproducible. A developer should ask the user for all necessary and sufficient conditions to
   reproduce a bug. If a bug is not reproducible in any way the corresponding ticket should be closed with the
   corresponding note/reason.

4.


5.

Version Aliases

| Name   | Version Number | Description |
|--------|----------------|-------------|
| latest | <X>.<Y>.<Z>          | The latest released version
| bugfix | X.Y.{Z+1}      | The version which is considered to be released ASAP and containing the fixes for the bugs found in the *latest* version **if they have no workaround**.
| next   | X.{Y+1}.0      |
| future | N/A

| Priority | Description | Version to Accept
|----------|-------------|------------------
| 0        | <ul><li>Crash</li><li>Hang</li><li>Not functioning</li><li>Functioning incorrectly</li><li>Performance degradation</li></ul> | Bugfix
| 1        | <ul><li>New feature</li><li>Bug having a workaround</li></ul> | Next
| 2        | <ul><li>Non-release: <ul><li>Demo</li><li>Investigation</li><li>Release scope definition</li></ul><li>Improvements: <ul><li>Performance</li><li>Usability</li><li>Cosmetic</li></ul></li></ul> | Future

*Note**:
> A Mongoose developer should not rely on the bug probability/frequency

# Task Priorities

1. Current latest version bugs w/o any workaround
2. The tasks assigned for the next major version being developed currently
3. All other tasks:
    * Investigations
    * Demos
    * Future major version scope definition
