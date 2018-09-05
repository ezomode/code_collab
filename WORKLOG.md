#### Thoughts:
- explore intellij sdk, build a basic plugin
- look into editor and toolbar APIs

#### Plan:
States:
    - Idle -> waits for someone to connect, or until user connects to someone
    - Writer -> If Reader joins - write changing doc to socket (changes are always pushed through observable)
    - Reader -> create doc with given path from Writer, accept doc contents from socket
    
State transitions:
    - Idle by default, current doc changes are pushed to observable
    - If is idle - accepts incomming connection - becomes Writer
    - User can enter ip_addr:port to connect to someone and transitions to Reader mode
    - User can disconnect - transitions back to Idle
    - Writer can end remote session -> Idle


editor - instance per window/file
file/contents ownership - stays with the initial Writer
create rx-subject in main service to set active editor

---------------------

#### Issues:
- error running a demo plugin: JDK/JRE issue `JDK classes seem to be not on IDEA classpath. Please ensure you run the IDE on JDK rather than JRE.`
- even though JDK is used: `Please consider switching to the bundled Java runtime that is better suited for the IDE (your current Java runtime is 1.8.0_181-b13 by Oracle Corporation).`
- project created with wizard is not properly set up, missing 'apply plugin'

#### TODOs:
+ create interaction commands - OPEN_DOC, GET_LOCK, DOC (contents from writer)
+ find way to intercept OPEN_DOC
+ find way to open editor through command
+ use klaxon for jsonification
+ look up CommandListener and AnActionListener usecases
+- implement incoming message handling
+ implement connection to remote socket
- don't apply incoming messages when in WRITER mode, also don't send them in READER mode (probably filtered already)

+ write a functest to verify readonly mode
- write a functest for document open/update incoming messages - failed so far, need to properly resolve file paths (relative to project)


#### effort:
week 1: 6-7 hours
week 2: 1.5 hours
week 2-3: 5 hours


