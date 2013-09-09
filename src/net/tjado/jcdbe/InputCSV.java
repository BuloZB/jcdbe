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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.ini4j.Ini;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import au.com.bytecode.opencsv.CSVReader;

/**
 * InputCSV reads the data with the help of OpenCSV from CSV files
 */
public class InputCSV implements Input {

  // instance object (singleton)
  private static final InputCSV INSTANCE = new InputCSV();

  // Logger
  private static Log log = Log.getInstance();

  // section name of the config (ini) file
  private String sectionName = "input:csv";

  // DatabaseList instance
  private DatabaseList dbList = new DatabaseList();

  // queries which should be executed
  private Map<Integer, String> queries = null;

  // input file which contains all necessary database information in CSV format
  private String csvInputFile = null;

  // CSV delimiter
  private char csvDelimiter = ',';

  // private constructor -> singleton
  private InputCSV() {/************** nothing in constructor **************/}

  // get instance of this class
  public static InputCSV getInstance() {
    return INSTANCE;
  }

  // specify CLI arguments of required information for this module
  public void setCLI(Options o) {
    o.addOption("if", "inputFile", true, "path to csv input file");
    o.addOption("q", "query", true, "SQL query to execute");
    o.addOption("oh", "oracleHome", true, "If set then thick client will be used");
  }

  // validates the required/optional parameters for Input
  public void validateParameters(CommandLine cli, Ini ini) {
    log.debug("[INPUT] Validating parameters");

    // temporary string for storing/checking parameter values
    String checkParam = null;

    //
    //
    // INI parameters

    checkParam = ini.get(sectionName, "delimiter");
    if (checkParam != null && (checkParam.length() == 1)) {
      csvDelimiter = checkParam.charAt(0);
      log.debug("[INPUT] CSV delimiter parameter successful parsed from config file");
    } else if (checkParam == null) {
      log.debug("[INPUT] CSV delimiter parameter not set in config file (using default)");
    } else {
      log.warn("[INPUT] CSV delimiter parameter in config file has wrong length (using default)");
    }


    //
    //
    // CLI arguments

    if (cli.hasOption("inputFile")) {
      csvInputFile = cli.getOptionValue("inputFile");
      log.debug("[INPUT] CSV input file parameter successful parsed from CLI");
    } else {
      log.fatal("[INPUT] CSV input file parameter not specified");
      System.exit(1);
    }

    if (cli.hasOption("query")) {
      queries = new HashMap<Integer, String>();
      queries.put(1, cli.getOptionValue("query"));
      log.debug("[INPUT] SQL Query parameter successful parsed from CLI");
    } else {
      log.fatal("[INPUT] SQL Query not specified");
      System.exit(1);
    }

  }

  public boolean init() {

    // open file handler over CSVReader class
    CSVReader reader = null;
    try {
      reader = new CSVReader(new FileReader(csvInputFile), csvDelimiter);
    } catch (FileNotFoundException e) {
      log.fatal("[INPUT] CSV file not found");
      return false;
    }

    // read CSV file by line
    // and insert every correct line (url,username,password) into the DatabaseList object
    try {

      String[] line;
      int lineNumber = 0;

      while ((line = reader.readNext()) != null) {
        lineNumber++;

        if (line.length == 3) {
          // add database to DatabaseList
          Integer id = dbList.insert(line[0], line[1], line[2], null);
          log.debug("CSV DB: " + id + " " + line[0]);
        } else {
          log.debug("CSV line " + lineNumber + " not equals 3 columns: skipped.");
        }
      }

    } catch (IOException e) {
      log.fatal("[INPUT] IOException: " + e.getMessage());
      return false;
    }

    // reader is not needed after input initialization -> close
    try {
      reader.close();
    } catch (IOException e) {
      log.fatal("[INPUT] IOException during file handler close: " + e.getMessage());
    }

    return true;
  }

  public void close() {
    return;
  }

  public DatabaseList getDatabaseList() {
    return dbList;
  }

  public Map<Integer, String> getQueries() {
    return queries;
  }

}
