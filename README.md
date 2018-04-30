# project05-osharif12


     For Project 5 I will be implementing the Raft consensus algorithm. 
 I thought this algorithm would be interesting to implement since it
 is a commonly used one for distributed systems. The main parts of 
 Raft I will focus on are the leadership election, log replication, 
 and safety. For the leadership election, I will have the leader send 
 out heartbeat messages in the form of Append-entry RPC’s with no data 
 to its followers. If follower doesn’t hear back for a period of time, 
 it will start election using the Raft process of sending out 
 Request-vote RPC’s. The first one that gets majority of votes will 
 be elected leader, I will also implement mechanisms to ensure split 
 votes do not occur. Upon election of a new primary, that primary will 
 ensure that all followers logs are up to date with that of the primary 
 by going to the last index where the two nodes have the same data and 
 replicating everything after that point. For log replication I will 
 ensure that each piece of data that is replicated has an index and term. 
 When a client sends a write request to the primary, that primary will 
 respond back to the client once it has heard back from a majority of 
 the secondary servers that replication was successful.  
     The primary will be responsible for committing changes to the 
 local state machine after it gets a positive response back from 
 most secondary servers that they have replicated data. For this 
 project the commit will be to a file(probably a json file) that 
 will store data, index number, and term number. In case a primary 
 or secondary goes down, when it comes back on it will go to that 
 file and retrieve all the data that has been committed. 
     This project will emulate a data store somewhat similar to project 4, 
 only it will be persistent once it is committed. I will create classes for 
 front end nodes and service nodes. The service nodes will be primary and 
 secondary servers which will handles requests from the front end and store 
 data. This project will have a few milestones, namely completion of basic 
 structure as outlined above, election algorithm, replication of data (with 
 indices, terms, commits, and data), and the testing script. By May 8th I 
 hope to have at least the basic structure of the project and the election 
 algorithm completed. By May 10 I hope to have the election algorithm completed. 
 By May 12 I hope to have the the replication completed. By May 14 I hope to 
 have everything completed including the testing. I will debug project until May 
 16. On May 16 I will demo and submit.