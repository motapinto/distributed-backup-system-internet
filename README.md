# Distributed Backup System for the Internet

## Compilation and testing instructions:
Use javac or intellij, compile the entire src/main/java directory.
JRE 1.8.0_231 was used.

### Run Peers
For running the Peers as the first or following rings, respectively:
```
java PeerRunner <IP> <Port>
java PeerRunner <IP> <Port> <KnownIP> <KnownPort>
```
Where KnownIP/Port is the address of any ring member.

### Run Test App
```
java -jar McastSnooper.jar 224.0.0.0:4445 224.0.0.1:4446 224.0.0.2:4447

```

## Test BACKUP
```
java TestApp <peerID> BACKUP <FILENAME> <REP_DEGREE>
```
## Test RESTORE
```
java TestApp <peerID> RESTORE <FILENAME>
```
## Test DELETE
```
java TestApp <peerID> DELETE <FILENAME>
```
## Test RECLAIM
```
java TestApp <peerID> RECLAIM <DISK_SPACE>
```
## Test STATE
```
java TestApp <peerID> STATE
```

When you run a peer, it will create a peers/ folder. 
Each peer will have its own folder, which it will use to store the backed up files and documents.
The root structure will go as something like this:
> peers
> * peer0
>   * Backup
>   * Documents
>     * document.pdf
> * peer1
>   * Backup
>   * Documents


When backing up a file, you need to specify the path from the peer's root folder.
For instance, to back up peer0's pdf file with a replication degree of 1, you'll need to run
java TestApp 0 BACKUP Documents/document.pdf 1

To restore it:
java TestApp 0 RESTORE Documents/document.pdf

When retrieving the internal state of a peer, it will be printed to a peer{ID}.log file.
