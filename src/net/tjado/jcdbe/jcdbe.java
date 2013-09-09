/*
 * This file is part of JCDBE - Java Connect Database Engine
 * 
 * Copyright (C) 2013  Tjado Mäcke
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * 
 */

package net.tjado.jcdbe;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;


/**<pre>
 * JCDBE - Java Central DataBase Engine
 * 
 * Main structure for connecting to a large amount of databases
 * DB connections/query executions will be done threaded.
 * 
 * 
 * Project Overview:
 * 
 *  jcdbe.java                  -> main
 *  jcdbeTest.java              -> simple JDBC test class
 *  DatabaseThreadSlave.java    -> worker object for slave threads
 *  
 *  DatabaseOracle.java         -> Oracle Database Layer
 *  DatabaseList.java           -> synchronized access to HashMap with all DB infos
 *  
 *  Input.java                  -> Interface specification for input
 *      InputCSV.java                   -> CSV input
 *      InputDatabase.java              -> DB input
 *  
 *  Output.java                 -> Interface specification for output
 *      OutputCSV.java                  -> CSV output
 *      OutputDatabase.java             -> DB output
 *      OutputDummy.java                -> Dummy output
 *  
 *  Log.java                    -> Logging Helper Class for log4j (from apache)
 *  
 * </pre>
 * 
 * @author  Tjado Mäcke <tjado@maecke.de>
 * @version 0.4
 * 
 */


public class jcdbe {

  // Logger
  private static Log log = Log.getInstance();

  // start time will be saved to this variable for benchmark
  private static long measureTimeStart = 0;

  // queue for (Runnable) jobs
  private static ArrayBlockingQueue<Runnable> workQueue = null;

  // input class of database list (InputDatabase, InputCSV)
  private static String inputClass = null;

  // output class of results (OutputDatabase, OutputCSV)
  private static String outputClass = null;

  // working directory
  private static String workingDir = System.getProperty("user.dir");

  // default max threads in pool (running + non-running)
  private static int threadMax = 50;

  // default maximum parallel running threads
  private static int threadRun = 25;

  // default thread keep alive time
  private static int threadTTL = 10;

  // default log4j path to property file
  private static String log4jPropertyFile = "config/log4j.properties";

  // default log4j path to property file
  private static String jdbcPropertyeFile = "config/jdbc.properties";

  // default jdbc prefix
  private static String jdbcPrefix = "jdbc:oracle:thin:@";

  // Session Data Unit (SDU) size for Oracle Net8 handshake (e.g. setting this for Oracle CMAN bug
  // 13989986)
  private static Integer sduSize = null;

  // advanced debugging
  public static boolean advDebugging = false;

  public static void main(String[] args) throws Exception {
    // print banner
    System.out.println("------------------------------------------");
    System.out.println("| Java Central DataBase Engine v0.4 beta |");
    System.out.println("------------------------------------------");

    // path to the ini configuration
    String configPath = workingDir + "/config/jcdbe.ini";

    // If the first CLI argument doesn't begin with "-" it must be a config file.
    // We need to do this, to have the possibility to specify other ini config files
    // The ini config file is necessary for the input/output classes, which again specify the
    // required CLI arguments
    if (args.length != 0 && !args[0].startsWith("-")) {
      configPath = args[0];
      // remove first argument from CLI arguments array, so it won't be validated by CLI argument
      // parser
      args = Arrays.copyOfRange(args, 1, args.length);
    }

    // initialize the ini configuration to get required parameters
    Ini config = initConfig(configPath);

    // initialize Logger
    log.init(log4jPropertyFile);

    // setting jdbc property file
    DatabaseOracle.setPropertyFile(jdbcPropertyeFile);

    // declare the input/output classes
    Input input =
        (Input) Class.forName(inputClass).getDeclaredMethod("getInstance")
            .invoke(null, (Object[]) null);
    Output output =
        (Output) Class.forName(outputClass).getDeclaredMethod("getInstance")
            .invoke(null, (Object[]) null);

    // declare options and parser for the CLI arguments
    Options options = new Options();
    CommandLineParser parser = new PosixParser();

    // add "help" CLI argument
    options.addOption("h", "help", false, "print this usage information");

    // add further CLI arguments by the input/output classes
    input.setCLI(options);
    output.setCLI(options);

    CommandLine cli = null;
    try {
      // parse the CLI arguments
      cli = parser.parse(options, args);

      if (cli.hasOption("help") || cli.getOptions().length == 0) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar jcdbe.jar [options]", options);

        System.exit(1);
      }
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("java -jar jcdbe.jar [options]", options);

      System.exit(1);
    }

    // output engine config after initializing of Logger
    log.debug("[CONFIG] Working Directory: " + workingDir);
    log.debug("[CONFIG] Input class:  " + inputClass);
    log.debug("[CONFIG] Output class: " + outputClass);
    log.debug("[CONFIG] JDBC URL prefix: " + jdbcPrefix);
    log.debug("[CONFIG] Java Library Path: " + System.getProperty("java.library.path"));
    log.debug("[CONFIG] Oracle SDU size: " + sduSize);
    log.debug("[CONFIG] Max. threads: " + threadMax);
    log.debug("[CONFIG] Max. running threads: " + threadRun);
    log.debug("[CONFIG] Thread idle timeout: " + threadTTL);
    log.debug("[CONFIG] Advanced Debugging: " + advDebugging);

    // validate Input arguments
    input.validateParameters(cli, config);
    // validate Output arguments
    output.validateParameters(cli, config);

    // start benchmark time
    measureTimeStart();

    log.info("[INPUT] Initialization");
    // run input init and check if it was successfull....
    if (!input.init()) {
      log.fatal("Error during input init...");
      System.exit(2);
    }
    log.info("[OUTPUT] Initialization");
    // run output init and check if it was successfull....
    if (!output.init()) {
      log.fatal("[OUTPUT] Error during output init...");
      System.exit(3);
    }

    // init thread pool
    workQueue = new ArrayBlockingQueue<Runnable>(99999);
    ThreadPoolExecutor threads =
        new ThreadPoolExecutor(threadRun, threadMax, threadTTL, TimeUnit.SECONDS, workQueue);

    // get DatabaseList object which will manage all database infos (url, username, pw, status...)
    DatabaseList dbList = input.getDatabaseList();

    if (dbList.size() == 0) {
      log.info("[QUEUE] database list is empty... nothing do to.");
      System.exit(1);
    }

    // get all SQL queries to execute
    // Integer = Query ID
    // String = SQL Text
    Map<Integer, String> queries = input.getQueries();


    log.info("[QUEUE] Starting Threads");

    // loop thru dbList to create & execute/queue all threads
    for (Integer id : dbList) {
      try {
        // create new runnable instance
        DatabaseThreadSlave slaveThread =
            new DatabaseThreadSlave(id, dbList, queries, output, jdbcPrefix, sduSize);
        // insert runnable instance into dbList
        dbList.setThread(id, slaveThread);

        // add runnable instance into thread pool queue
        threads.execute(slaveThread);
      } catch (Exception e) {
        advDebug(e);
        log.warn("Exception in thread-starter loop (DBID: " + id + "): " + e.getMessage());
      }
    }


    //
    // waiting for all threads to complete
    //
    // the timeout handling will be done completely over JDBC
    // see docs for more information
    //

    while (!dbList.isFinished() || threads.getActiveCount() > 0) {
      Thread.sleep(500);
    }


    log.info("[QUEUE] Shutting down all threads");
    threads.shutdown();

    Thread.sleep(2000);

    log.info("[INPUT] close input...");
    input.close();

    log.info("[OUTPUT] close output...");
    output.close();

    // end time-benchmark and output
    measureTimeEnd();

    // rc=0
    System.exit(0);
  }


  private static Ini initConfig(String iniFile) {

    // temporary string for storing/checking parameter values
    String checkParam = null;

    Ini ini = null;
    try {
      ini = new Ini(new File(iniFile));
    } catch (InvalidFileFormatException e) {
      System.out.println("Error: Invalid format of config file!");
      System.exit(1);
    } catch (IOException e) {
      System.out.println(iniFile);
      System.out.println("Error: Config file not existent or readable!");
      System.exit(1);
    }


    // package name
    // because refactoring would be harder with hardcoded names
    String packageName = jcdbe.class.getPackage().getName();

    // input class
    try {
      inputClass = packageName + ".Input" + ini.get("main", "input").trim();
    } catch (NullPointerException e) {
      System.out.println("Error: engine input parameter not found!");
      System.exit(1);
    }
    // check if class exists
    try {
      Class.forName(inputClass);
    } catch (ClassNotFoundException e) {
      System.out.printf("Error: %s class not found!\n", inputClass);
      System.exit(1);
    }


    // output class
    try {
      outputClass = packageName + ".Output" + ini.get("main", "output").trim();
    } catch (NullPointerException e) {
      System.out.println("Error: engine output parameter not found!");
      System.exit(1);
    }
    // check if class exists
    try {
      Class.forName(outputClass);
    } catch (ClassNotFoundException e) {
      System.out.printf("Error: %s class not found!\n", outputClass);
      System.exit(1);
    }


    // log4j property file
    checkParam = ini.get("main", "log4jPropertyFile");
    if (checkParam != null) {
      log4jPropertyFile = checkParam;
    }

    // jdbc property file
    checkParam = ini.get("main", "jdbcPropertyeFile");
    if (checkParam != null) {
      jdbcPropertyeFile = checkParam;
    }

    // threadMax
    checkParam = ini.get("main", "threadMax");
    if (checkParam != null) {
      threadMax = Integer.parseInt(checkParam);
    }

    // threadRun
    checkParam = ini.get("main", "threadRun");
    if (checkParam != null) {
      threadRun = Integer.parseInt(checkParam);
    }

    // threadTTL
    checkParam = ini.get("main", "threadTTL");
    if (checkParam != null) {
      threadTTL = Integer.parseInt(checkParam);
    }


    // oracleHome
    String oracleHome = ini.get("main", "oracleHome");

    // if oracleHome is set then using thick client
    if (oracleHome != null) {
      String oraLib = oracleHome + "/lib/libocijdbc11.so";

      // check if libocijdbc11.so is found in oracle home
      File f = new File(oraLib);
      if (!f.exists()) {
        System.out.printf("Error: Thick client lib not found: %s\n", oraLib);
        System.exit(1);
      }

      // set thick client prefix
      jdbcPrefix = "jdbc:oracle:oci:@";

      // set library path for thick client
      String lib = System.getProperty("java.library.path");
      System.setProperty("java.library.path", lib + ":" + oracleHome + "/lib");
    }


    // Session Data Unit (SDU) size
    // for Oracle Net handshake (e.g. setting this for Oracle CMAN bug 13989986)
    // Will only be set for Oracle Connect Strings!
    checkParam = ini.get("main", "oracleSDU");
    if (checkParam != null) {
      try {
        Integer tempSDU = Integer.parseInt(checkParam);

        // SDU needs to be power of two
        if (tempSDU != 0 && (tempSDU & (tempSDU - 1)) == 0) {
          sduSize = tempSDU;
        }

      } catch (NumberFormatException e) {
        sduSize = null;
      }
    }


    // threadMax
    checkParam = ini.get("main", "threadMax");
    if (checkParam != null) {
      threadMax = Integer.parseInt(checkParam);
    }


    // printStackTrace
    checkParam = ini.get("main", "printStackTrace");
    if (checkParam != null) {
      advDebugging = Boolean.parseBoolean(checkParam);
    }


    // return config handler for further processing in the Input* and Output* classes
    return ini;
  }

  private static void measureTimeStart() {
    measureTimeStart = System.currentTimeMillis();
    log.info("[BENCH] Start: " + measureTimeStart);
  }

  private static void measureTimeEnd() {
    long dateEnd = System.currentTimeMillis();
    log.info("[BENCH] End: " + dateEnd);
    log.info("[BENCH] Time: " + (dateEnd - measureTimeStart) / 1000 + " seconds");
  }

  public static void advDebug(Exception e) {
    if (advDebugging) e.printStackTrace();
  }

}
