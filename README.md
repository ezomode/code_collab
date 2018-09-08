#### Demo IntelliJ IDEA plugin for multi-user editing
##### Features:
- Allows two-user editing over local network 
- Adds a "Collaborate" menu in the MainMenu to control connectivity
- User has an option to start own session or join remote session
- During a session, only one party is able edit (WRITER); Another party (READER) will be put into read-only mode
- READER is able to grab the write lock through the menu - becomes a WRITER

##### Limitations:
- While in read-only mode, internal exceptions will are thrown upon edit attempts
- Functional tests are in draft state
