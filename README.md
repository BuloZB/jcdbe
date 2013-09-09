JCDBE
=====
JCDBE - Java Connect DataBase Engine is a backend engine for connecting to  
a large amount of databases to execute SQL over JDBC and collect the results

Scope: reporting (e.g. licensing, config management, ...) and database administration.  
Currently, only Oracle JDBC is integrated.

It is highly threaded to connect to a large amount of databases even in short time.  
So JCDBE is able to process more than 1000 databases in minutes depending on SQL execution time/result size/network topology.  
Testing was successfull in big enterprise networks, mostly over Oracle Connection Manager.

The design of JCDBE is modular and can be separated into main, input and output:  
"Input" needs to deliver the database list (JDBC url, username and password) and the SQL queries, which will be executed to every database in the list.  
Saving the respective SQL results is job of "Output".  
They are designed as interfaces and needs to be configured in the config/jcdbe.ini file.

#### Configuration

The main configuration is done over CLI arguments and config/jdbc.ini.   
There are few standard parameters, like amount of threads, defining the Input/Output classes, ... . The respective Input/Output module extends these by it's own parameters, e.g. the InputCSV module needs a CSV file path.

Additional config files:

* The log4j logfile adapter has it's own propertiy file in config/log4j.properties
* JDBC properties can be configured over config/jdbc.properties

## Dependencies

* jre 1.7
* ini4j (tested with 0.5.2)
* log4j (tested with 1.2.16)
* Oracle JDBC (tested with 11.2.0.3)
* OpenCSV (tested with 2.3)
* CommonsCLI (tested with 1.2)

## Build

1. Download all dependent libraries, move them into the libs/ folder
2. Remove all version numbers from the lib filenames, so they match with them in MANIFEST.MF
3. Run build.ant in Eclipse to build jcdbe.jar

## Running
    $ java -Xmx1024m -jar jcdbe.jar
    ------------------------------------------
    | Java Central DataBase Engine v0.4 beta |
    ------------------------------------------
    usage: java -jar jcdbe.jar [options]
     -h,--help                print this usage information
     -if,--inputFile <arg>    path to csv input file
     -of,--outputFile <arg>   path to csv output file
     -oh,--oracleHome <arg>   If set then thick client will be used
     -q,--query <arg>         SQL query to execute

#### Example of a input csv file 

JDBC prefix "jdbc:oracle:thin:@" will be added to every URL.

    //hostname:port/database_name,username,password
    (DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=hostname.tld)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=orcl))),username,password

## Todo
* JavaDoc
* Replacing log4j
* JDBC driver abstraction
* Input/OutputDatabase module (already existing in a JCDBE non-public version)
* Replacing ini4j with Properties?

