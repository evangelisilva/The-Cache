# File Exchange Cache Service and Reliable Data Transport Protocols
This project involves implementing a cache service for file exchange and developing two reliable data transport protocols to facilitate secure and efficient data transfer.

### Project Overview
The project is structured into two main components:
1. Cache Service for File Exchange: A caching mechanism that stores and serves files to improve efficiency during file exchanges.
2. Reliable Data Transport Protocols: Implementations of two protocols for reliable data transfer:
  - TCP-based Protocol: Uses standard TCP for reliable data transfer.
  - UDP with Stop-and-Wait (SNW) Protocol: Implements Stop-and-Wait reliability at the application layer while using UDP as the transport protocol.

### Usage Instructions
File Placement: Ensure that the text files you wish to test are located in the appropriate _fl directories, such as client_fl, server_fl, or cache_fl.

Initiating Requests: From the root directory, initiate a GET or PUT request using the following command format:
`get File1.txt`
Note: Do not include the full path; simply specify the filename. Make sure you are executing the command from the root directory for successful file retrieval.
