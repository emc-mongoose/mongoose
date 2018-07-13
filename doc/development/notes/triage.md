# Roles

| Name | Responsibilities
|------|-----------------
| User | Report issues in the expected way
| Developer | <ul><li>Development</li><li>Testing</li><li>Automation</li><li>Documentation</li></ul>
| Owner | <ul><li>*Next* version scope definition</li><li>Roadmap definition</li></ul>
| Manager | The explicit *next* version scope approval

# Priorities

| Priority | Description | Version to Accept
|----------|-------------|------------------
| P0       | Critical bugs w/o workaround: <ul><li>Crash</li><li>Hang</li><li>Not functioning</li><li>Functioning incorrectly</li><li>Performance degradation</li></ul> | bugfix
| P1       | <ul><li>Non-critical bugs</li><li>Workaround is available for the users</li></ul> | next
| P2       | New feature | next
| P3       | <ul><li>Non-release: <ul><li>Demo</li><li>Investigation</li><li>Release scope definition</li></ul><li>Improvements: <ul><li>Performance</li><li>Usability</li><li>Cosmetic</li></ul></li></ul> | backlog

*Note**:
> A Mongoose developer should not rely on the bug probability/frequency

# Scopes

| Name    | Version Number | Description | Scope Priority Threshold |
|---------|----------------|-------------|-------|
| latest  | &lt;X&gt;.&lt;Y&gt;.&lt;Z&gt; | The latest released version | N/A
| bugfix  | &lt;X&gt;.&lt;Y&gt;.&lt;Z+1&gt; | The version which is considered to be released ASAP<br/>Interrupts the tasks for the *next* | P0 |
| next    | &lt;X&gt;.&lt;Y+1&gt;.0 | The next version which is considered to include the new features and fixes for the non-critical bugs | P1*, P2**
| backlog | N/A | Backlog equivalent | P3

(*)  P1 tasks are acceptable for the *next* version until the release

(**) P2 tasks are acceptable for the *next* version until PM approves the scope
