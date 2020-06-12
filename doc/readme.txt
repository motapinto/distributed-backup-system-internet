+-----------+
| Compiling |
+-----------+
Using javac or intellij, compile the entire src/main/java directory.
JRE 1.8.0_231 was used.

+-----------+
|  Running  |
+-----------+
For running the Peers as the first or following rings, respectively:

java PeerRunner <IP> <Port>
java PeerRunner <IP> <Port> <KnownIP> <KnownPort>

Where KnownIP/Port is the address of any ring member.

For running the Test App:
java TestApp <peerID> BACKUP <FILENAME> <REP_DEGREE>
java TestApp <peerID> RESTORE <FILENAME>
java TestApp <peerID> DELETE <FILENAME>
java TestApp <peerID> RECLAIM <DISK_SPACE>
java TestApp <peerID> STATE


When you run a peer, it will create a peers/ folder. 
Each peer will have its own folder, which it will use to store the backed up files and documents.
The root structure will go as something like this
peers/
|
|- peer0/
|  |-- Backup/
|  |-- Documents/
|      |-- document.pdf
|
|- peer1/
   |-- Backup/
   |-- Documents/

When backing up a file, you need to specify the path from the peer's root folder.
For instance, to back up peer0's pdf file with a replication degree of 1, you'll need to run
java TestApp 0 BACKUP Documents/document.pdf 1

To restore it:
java TestApp 0 RESTORE Documents/document.pdf

When retrieving the internal state of a peer, it will be printed to a peer{ID}.log file.




