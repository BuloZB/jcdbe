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
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.rowset.CachedRowSet;

import org.ini4j.Ini;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * OutputCSV stores the query results in CSV files.
 */
public class OutputCSV implements Output {

//instance object (singleton)
  private static final OutputCSV INSTANCE = new OutputCSV();

  // Logger
  private static Log log = Log.getInstance();
  
  //section name of the config (ini) file
  private String sectionName = "output:csv";

  // object for writing output csv file
  private CSVWriter output = null;

  // RowSets will be written in CSV format into this file
  private String csvOutputFile = null;

  // CSV delimiter
  private char csvDelimiter = ',';

  // if true, the first line in the output CSV file are the column names of the first result set
  private Boolean csvHeadline = true;
  
  // save all non-OK database status messages here to write them during close
  private List<String[]> errorList = new ArrayList<String[]>();

  // private constructor -> singleton
  private OutputCSV() {/************** nothing in constructor **************/}

  // get instance of this class
  public static OutputCSV getInstance() {
    return INSTANCE;
  }

  // specify CLI arguments of required information for this module
  public void setCLI(Options o) {
    // already specified options in respective Input class doesn't matter
    // so double argument specification is no issue with CLI parser 
    //   but of course, they will have the same value

    o.addOption("of", "outputFile", true, "path to csv output file");
  }

  public void validateParameters(CommandLine cli, Ini ini) {
    log.debug("[OUTPUT] Validating parameters");
    
    // temporary string for storing/checking parameter values
    String checkParam = null;

    //
    //
    // INI parameters
    
    checkParam = ini.get(sectionName, "delimiter");
    if (checkParam != null && (checkParam.length() == 1)) {
      csvDelimiter = checkParam.charAt(0);
      log.debug("[OUTPUT] CSV delimiter parameter successful parsed from config file");
    } else if (checkParam == null) {
      log.debug("[OUTPUT] CSV delimiter parameter not set in config file (using default)");
    } else {
      log.warn("[OUTPUT] CSV delimiter parameter in config file has wrong length (using default)");
    }
    
    checkParam = ini.get(sectionName, "headline");
    if( checkParam != null && (checkParam.equals("true") || checkParam.equals("false")) ) {   
        csvHeadline = Boolean.parseBoolean( checkParam );
        log.debug("[OUTPUT] Headline parameter successful parsed from config file");
    } else if ( checkParam == null ) {
        log.debug("[OUTPUT] Headline parameter not set in config file");
    } else {
        log.warn("[OUTPUT] Headline parameter in config file should only be set to true/false");
    }

    
    //
    //
    // CLI arguments

    if (cli.hasOption("outputFile")) {
      csvOutputFile = cli.getOptionValue("outputFile");
      log.debug("[OUTPUT] CSV output file parameter successful parsed from CLI");
    } else {
      log.fatal("[OUTPUT] CSV output file parameter not specified");
      System.exit(1);
    }

  }

  public boolean init() throws Exception {
    
    // open file handle with the help of CSVWriter
    try {
      output = new CSVWriter(new FileWriter(csvOutputFile), csvDelimiter);
    } catch (FileNotFoundException e) {
      log.fatal("[OUTPUT] CSV file not found");
      return false;
    }

    return true;
  }

  public void close() {
    
    synchronized (INSTANCE) {
      
      output.writeAll(errorList);
      
      try {
        output.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public boolean prepareDatabase(Integer dbID) {

    return true;
  }

  public boolean prepareQuery(Integer dbID, Integer queryID) {

    return true;
  }
  
  public void setDatabaseStatus(Integer dbID, String status) {
    if (!status.equalsIgnoreCase("OK")) {
      errorList.add(new String[] {status.replaceAll("\\s+$", "")});
    }
  }

  public void setQueryStatus(Integer dbID, Integer queryID, String status) {

    return;
  }

  public void saveResult(CachedRowSet rs, Integer dbID, Integer queryID) {

    synchronized (INSTANCE) {
      try {
        output.writeAll(rs, csvHeadline);
      } catch (SQLException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
      
      // set csvHeadline to false so only the column names of the first RowSet
      // will be written as headline in the output file 
      csvHeadline = false;
    }
  }

}
