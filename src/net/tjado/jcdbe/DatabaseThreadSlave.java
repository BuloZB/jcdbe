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

import java.util.Map;

import oracle.jdbc.rowset.OracleCachedRowSet;

/**
 * Runnable thread object created by JCDBE-main for each database in DatabaseList
 * 
 */
public class DatabaseThreadSlave implements Runnable {

  private Integer dbID = 0;
  private String dbName = null;
  private String url = null;
  private String username = null;
  private String password = null;
  private DatabaseList dbList = null;
  private DatabaseOracle db = null;

  private Map<Integer, String> queries = null;

  private Log log = Log.getInstance();
  private Output output = null;

  public DatabaseThreadSlave(Integer id, DatabaseList list, Map<Integer, String> queries,
      Output output, String jdbcPrefix, Integer sduSize) {

    dbID = id;
    dbList = list;

    db       = dbList.getDatabaseHandle(dbID);
    url      = db.prepareURL(dbList.getURL(dbID), jdbcPrefix, sduSize);
    username = dbList.getUsername(dbID);
    password = dbList.getPassword(dbID);

    this.queries = queries;
    this.output = output;

    dbName = db.getDatabaseName(url);
  }


  public void run() {
    log.info(dbID, "Start thread: " + dbName);


    try {

      output.prepareDatabase(dbID);
      this.connectRemoteDatabase();
      this.processQueries();
      output.setDatabaseStatus(dbID, "OK");

    } catch (Exception e) {

      log.warn(dbID, e.getMessage());
      output.setDatabaseStatus(dbID, e.getMessage());

    }

    // set thread state
    dbList.setFinish(dbID);

    db.disconnect();
    log.info(dbID, "End of thread: " + dbName);
  }


  private void connectRemoteDatabase() throws Exception {

    if (url == null || username == null || password == null) {
      throw new Exception("CONNECT_ERROR: url/username/password not set");
    }

    // connect
    if (!db.connect(url, username, password)) {
      throw new Exception("CONNECT_ERROR - DB: " + dbName + "; Exception: " + db.getLastException());
    }

  }

  private void processQueries() throws Exception {

    // loop thru all queriey -> execute query and save result with Output* class
    for (Map.Entry<Integer, String> entry : queries.entrySet()) {

      Integer queryID = entry.getKey();
      String query = entry.getValue();

      // execute query and collect results
      // dbID will be inserted as first column value
      OracleCachedRowSet results = db.getReportingResults(dbID, query);

      if (results == null && db.getLastException() != null) {
        output.setQueryStatus(dbID, queryID, db.getLastException().getMessage());
        continue;
      } else if (results == null && db.getLastException() == null) {
        output.setQueryStatus(dbID, queryID, "ERROR_CONNECTION_INTERRUPT");
        continue;
      }

      // write result to output if not empty
      if (results != null && results.size() > 0) {
        try {
          output.saveResult(results, dbID, queryID);
        } catch (Exception e) {
          // saveResult should do all the error handling
          // but just in case, we catch the exception and print stack trace for debugging
          log.warn("Output->saveResult: " + e.getMessage());
          e.printStackTrace();

          output.setQueryStatus(dbID, queryID, "SAVERESULT_EXCEPTION");
        }
        // no rows ...
      } else if (results != null && results.size() == 0) {
        output.setQueryStatus(dbID, queryID, "RESULT_EMPTY");
        log.info(dbID, "No rows in ResultSet");
      }

    }
  }


}
