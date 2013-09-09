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

import java.io.FileInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import javax.sql.RowSetMetaData;
import javax.sql.rowset.RowSetMetaDataImpl;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleConnectionWrapper;
import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.rowset.OracleCachedRowSet;

/**
 * Oracle database layer
 * 
 * CachedResultSet column extension needs Oracle JDBC
 * 
 */
public class DatabaseOracle {

  // Logger
  static private Log log = Log.getInstance();

  // static properties for JDBC
  static private Properties properties = new Properties();

  // database handle
  private OracleConnection link = null;

  // save last Exception
  private Exception lastException = null;

  // add banner to every query
  private String banner = "/* JDBC */";

  // format to merge query (1) with banner (2) over string.format
  private String bannerFormat = "%1s %2s";

  // connection timeout
  private static int timeoutConnect = 10;

  public DatabaseOracle() {
    // nothing to do
  }

  public OracleConnection getLink() {
    return this.link;
  }

  // read property file into class attribut "properties"
  // which will be used for JDBC
  static public void setPropertyFile(String file) {
    try {
      properties.load(new FileInputStream(file));
    } catch (Exception e) {
      log.warn("Could not load property file: " + e.getMessage());
    }
  }

  public boolean connect(String url, String username, String password) {
    try {
      Class.forName("oracle.jdbc.OracleDriver");
    } catch (java.lang.ClassNotFoundException e) {
      // log.debug("Exception (DB->OracleDriverNotFound): " + e.getMessage());
      setLastException(e);
      return false;
    }

    try {
      OracleDataSource ods = new OracleDataSource();
      ods.setURL(url);
      ods.setUser(username);
      ods.setPassword(password);

      // set login timeout
      ods.setLoginTimeout(timeoutConnect);

      // JDBC properties
      ods.setConnectionProperties(properties);

      OracleConnection temp = (OracleConnection) ods.getConnection();

      link = new OracleConnectionWrapper(temp);

    } catch (SQLException e) {
      // log.debug("Exception (DB->connect): " + e.getMessage());
      setLastException(e);
      return false;
    }

    if (isConnected()) {
      try {
        link.setAutoCommit(false);
      } catch (SQLException e) {
        // log.debug("Exception (DB->setAutoCommit): " + e.getMessage());
        setLastException(e);
        return false;
      }
      return true;

    } else {
      this.link = null;
      return false;
    }

  }

  public void disconnect() {
    if (!isConnected()) {
      this.link = null;
      return;
    }

    try {
      this.link.rollback();
      this.link.close();
    } catch (Exception e) {
      log.debug("Exception (DB->disconnect): " + e.getMessage());
      setLastException(e);
    }

  }


  public boolean isConnected() {

    if (this.link == null) {
      return false;
    }

    try {
      if (!this.link.isClosed()) {
        return true;
      } else {
        return false;
      }
    } catch (SQLException e) {
      e.printStackTrace();
      setLastException(e);
      return false;
    }
  }

  private void setLastException(Exception e) {
    lastException = e;
  }

  public Exception getLastException() {
    return lastException;
  }

  private String addBanner(String query) {
    return String.format(bannerFormat, query, banner);
  }


  public void setConnectTimeout(int seconds) {
    timeoutConnect = seconds;
  }



  public OracleCachedRowSet getResults(String query, Object[] bindVars) {
    if (!isConnected()) {
      // log.fatal("Exception (DB->getResults): no DB connection" );
      setLastException(null);
      return null;
    }

    query = addBanner(query);

    ResultSet rs = null;
    OracleCachedRowSet crs = null;
    try {
      crs = new OracleCachedRowSet();

      PreparedStatement pstmt = this.link.prepareStatement(query);
      setBinds(pstmt, bindVars);
      rs = pstmt.executeQuery();

      crs.populate(rs);

    } catch (SQLException e) {
      log.warn("Exception (DB->getResults): " + e.getMessage());
    }
    return crs;
  }


  public OracleCachedRowSet getReportingResults(Integer dbID, String query) {
    if (!isConnected()) {
      // log.fatal("Exception (DB->getReportingResults): no DB connection" );
      setLastException(null);
      return null;
    }

    query = addBanner(query);

    ResultSet rs = null;
    OracleCachedRowSet crs = null;
    try {

      Statement stmt = this.link.createStatement();
      rs = stmt.executeQuery(query);

      crs = convertToCachedRowSet(dbID, rs);

      rs.close();
      stmt.close();

    } catch (Exception e) {
      log.warn("Exception (DB->getReportingResults): " + e.getMessage());
      setLastException(e);
      return null;
    }

    return crs;
  }


  // insert
  // will return number of rows inserted (usual 1), -1 for error or the value in the getColumn
  // column
  // getColumn needs to be set to null or to a column which is numeric
  public int insert(String query, Object[] bindVars, String getColumn) {
    if (!isConnected()) {
      log.fatal("Exception (DB->insert): no DB connection");
      return -1;
    }

    query = addBanner(query);

    int rows = 0;
    try {
      // declare statement variable
      PreparedStatement pstmt;

      // if getColumn is set correctly, then it will be used to return a column during the insert
      // e.g. sequences inserted by trigger or also manual
      // it is like oci_bind_by_name in PHP
      if (getColumn != null && !getColumn.equalsIgnoreCase("")) {
        // define the returning column
        String generatedColumns[] = {getColumn};
        // set the statement with the returning columns
        pstmt = link.prepareStatement(query, generatedColumns);
        // normal insert
      } else
        pstmt = link.prepareStatement(query);

      // set binds...
      setBinds(pstmt, bindVars);
      // execute statement and returns the number of rows (should be one during insert)
      rows = pstmt.executeUpdate();

      if (getColumn != null && !getColumn.equalsIgnoreCase("")) {
        // get resultset of returning columns
        ResultSet rs = pstmt.getGeneratedKeys();
        int cntRows = 0;
        while (rs.next()) {
          rows = rs.getInt(1);
          cntRows++;
        }

        if (cntRows != 1) {
          log.warn("Exception (DB->insert): cntRows != 1 (" + cntRows + ")");
          return -1;
        }

      }

      pstmt.close();
      link.commit();

      return rows;

    } catch (SQLException e) {
      log.warn("Exception (DB->insert): " + e.getMessage());

      return -1;
    }
  }


  public int update(String query, Object[] bindVars) {
    if (!isConnected()) {
      log.fatal("Exception (DB->update): no DB connection");
      return -1;
    }

    query = addBanner(query);

    int rs = 0;
    try {
      PreparedStatement pstmt = link.prepareStatement(query);

      setBinds(pstmt, bindVars);

      rs = pstmt.executeUpdate();
      pstmt.close();
      link.commit();

      return rs;

    } catch (SQLException e) {
      log.warn("Exception (DB->update): " + e.getMessage());

      return -1;
    }
  }

  public void setBinds(PreparedStatement pstmt, Object[] bindVars) throws SQLException {
    if (bindVars != null && bindVars.length > 0) {
      int i = 1;
      for (Object obj : bindVars) {
        if (obj.getClass().equals(Integer.class))
          pstmt.setInt(i, (Integer) obj);
        else
          pstmt.setString(i, (String) obj);

        i++;
      }
    }
  }


  public void setBinds(OracleCachedRowSet crs, Object[] bindVars) throws SQLException {
    if (bindVars != null && bindVars.length > 0) {
      int i = 1;
      for (Object obj : bindVars) {
        if (obj.getClass().equals(Integer.class))
          crs.setInt(i, (Integer) obj);
        else
          crs.setString(i, (String) obj);

        i++;
      }
    }
  }


  public Object getSingleQueryResult(String query, Object[] bindVars) {
    if (!isConnected()) {
      log.fatal("Exception (DB->getSingleQueryResult): no DB connection");
      return null;
    }

    query = addBanner(query);
    Object output = null;

    try {

      OracleCachedRowSet results = getResults(query, bindVars);
      if (results.size() > 0) {
        results.next();
        output = results.getObject(1);
      }
      results.close();
    } catch (SQLException e) {
      log.warn("Exception (DB->getSingleQueryResult): " + e.getMessage());
    }

    try {
      if (this.link.isClosed())
        log.warn("Exception (DB->getSingleQueryResult): lost db connection... ");
    } catch (SQLException e) {}

    return output;
  }



  // converts a ResultSet into a ChacedRowSet to handle all results without DB connection
  // enhanced feature: custom column extension (e.g. add query_id or instance_name before result
  // columns)
  public OracleCachedRowSet convertToCachedRowSet(Object firstColVal, ResultSet rs,
      String firstColName, Integer firstColType, String firstColTypeName, Integer firstColSize)
      throws Exception {
    OracleCachedRowSet crs = new OracleCachedRowSet();
    ResultSetMetaData rsmd = rs.getMetaData();
    RowSetMetaData rsmdNew = new RowSetMetaDataImpl();

    // column count from the database result
    int columnCount = rsmd.getColumnCount();
    // column count which will be added to the database result (custom columns)
    int columnCountCustom = 1;

    // set column count for the new metadata structure
    rsmdNew.setColumnCount(columnCount + columnCountCustom);

    // add first custom column
    rsmdNew.setColumnName(1, firstColName);
    rsmdNew.setColumnType(1, firstColType);
    rsmdNew.setColumnTypeName(1, firstColTypeName);
    rsmdNew.setColumnDisplaySize(1, firstColSize);

    // add all further columns from the metadata database result
    for (int i = 1; i <= columnCount; i++) {
      rsmdNew.setColumnName(i + columnCountCustom, rsmd.getColumnName(i));
      rsmdNew.setColumnType(i + columnCountCustom, rsmd.getColumnType(i));
      rsmdNew.setColumnTypeName(i + columnCountCustom, rsmd.getColumnTypeName(i));
      rsmdNew.setColumnDisplaySize(i + columnCountCustom, rsmd.getColumnDisplaySize(i));
    }

    // set the new metadata object to the cached row set
    crs.setMetaData((RowSetMetaData) rsmdNew);

    // value of first column (instance_name)
    // String instance_name = getInstanceName();

    // process all rows from the database result
    while (rs.next()) {
      // insert new row
      crs.moveToInsertRow();

      // update value of every first custom column in new row
      crs.updateObject(1, firstColVal);

      // update all further columns from database result in new row
      for (int i = 1; i <= columnCount; i++) {
        // update respective row...
        crs.updateObject(i + columnCountCustom, rs.getObject(i));
      }

      // make the updated "persistent" to the new cached object
      crs.insertRow();
    }

    return crs;
  }

  // convertToCachedRowSet overload
  public OracleCachedRowSet convertToCachedRowSet(Object firstColVal, ResultSet rs)
      throws Exception {
    return convertToCachedRowSet(firstColVal, rs, "DATABASE_NAME_JCDBE", Types.VARCHAR, "VARCHAR2",
        38);
  }


  public String getColumnCRC(ResultSetMetaData rsmd) throws SQLException {
    int columnCount = rsmd.getColumnCount();
    String[][] columnDefinition = new String[columnCount][3];
    for (int i = 0; i < columnCount; i++) {
      int j = i + 1;
      columnDefinition[i][0] = rsmd.getColumnName(j);
      columnDefinition[i][1] = rsmd.getColumnTypeName(j);
      columnDefinition[i][2] = String.valueOf(rsmd.getColumnDisplaySize(j));
    }

    String sortedColumnDefinition = "";
    for (final String[] s : sortArray(columnDefinition)) {
      sortedColumnDefinition += s[0] + s[1] + s[2];
    }

    return getCRC(sortedColumnDefinition);
  }


  public String getCRC(String str) {
    CRC32 c = new CRC32();
    c.update(str.getBytes());

    return String.valueOf(c.getValue());
  }


  // Sort a two dimensional array based on one column
  // http://stackoverflow.com/questions/4907683/sort-a-two-dimensional-array-based-on-one-column
  public String[][] sortArray(String[][] arr) {
    Arrays.sort(arr, new Comparator<String[]>() {
      @Override
      public int compare(final String[] entry1, final String[] entry2) {
        final String s1 = entry1[0];
        final String s2 = entry2[0];
        return s1.compareTo(s2);
      }
    });

    return arr;
  }

  public String getDatabaseName(String connectDescriptor) {
    if (connectDescriptor == null) {
      return "URL_IS_NULL";
    }

    Integer patternFlags = Pattern.CASE_INSENSITIVE;
    String patternOracle = ".*(SID|SERVICE_NAME)\\s*=\\s*(?<NAME>[a-zA-Z0-9_\\-\\.]+).*";

    Pattern pattern = Pattern.compile(patternOracle, patternFlags);

    Matcher m = pattern.matcher(connectDescriptor);
    if (m.matches()) {
      if (m.group("NAME") != null) {
        return m.group("NAME");
      }
      return connectDescriptor + "(M)";
    }
    return connectDescriptor + "(NM)";
  }
  
  
  public String prepareURL(String connectDescriptor, String prefix, Integer sduSize) {

    // set URL to prefix...
    String jdbcURL = prefix;
    
    // ... and add the complete oracle connect descriptor
    // if SOURCE_ROUTE=on then the connection will be routed over CMAN
    // so we add the SDU size, if set, to the connect descriptor
    if (sduSize != null && connectDescriptor.matches("(.*)SOURCE_ROUTE(\\s*)=(\\s*)on(.*)"))
      jdbcURL += connectDescriptor.replaceFirst("(?i)DESCRIPTION\\s*=", "DESCRIPTION=(SDU=" + sduSize + ")");
    else
      jdbcURL += connectDescriptor;

    return jdbcURL;
  }
  
}
