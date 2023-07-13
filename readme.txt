Trokos
Trokos is a currency transaction service.

Compilation / Usage:

1. Compile Project:
	mvn clean package
2. Run Server:
	java -jar server-1.0-SNAPSHOT.jar {PORT} {PASSWORD FOR ENCRYPTION} {keystore-name} {keystore-password}
	ex:
	   java -jar server-1.0-SNAPSHOT.jar 23456 trojanhorseSeg10 keystore.server server

3. Run Client:
	java -jar client-1.0-SNAPSHOT.jar {IP}:{PORT} {TRUSTSTORE NAME} {keystore-name} {keystore-password} {username}
	ex:
	   java -jar client-1.0-SNAPSHOT.jar 127.0.0.1:23456 truststore.client tiago tiago10 tiago
         java -jar client-1.0-SNAPSHOT.jar 127.0.0.1:23456 truststore.client francisco francisco10 francisco
         java -jar client-1.0-SNAPSHOT.jar 127.0.0.1:23456 truststore.client duarte duarte10 duarte

The project contains:
1. Server KeyStore:
	./server-keystores/keystore.server
2. Clients KeyStores:
	./client-keystores/tiago
	./client-keystores/francisco
	./client-keystores/duarte
3. TrustStores:
	./truststore/truststore.client


Problems:
Currently the integrity of the block chain is only being confirmed by the transactions signatures and not by the signature of the block and hash dependecies.
Concurrency of the threads is not done.


Contributions:
Tiago Badalo 55311, 
Duarte Costa 54441, 
Francisco Fiaes 53711.
