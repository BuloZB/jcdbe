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
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * Simple class for testing database connections over JDBC
 * 
 */
public class jcdbeTest {

  public static void main(String[] args) throws Exception {
    // print banner
    System.out.println("--------------------");
    System.out.println("|       TEST       |");
    System.out.println("--------------------");

    String url = "";
    String username = "";
    String password = "";

    if (args.length == 3) {
      username = args[0];
      password = args[1];
      url = args[2];
    } else {
      System.err
          .println("java -classpath jcdbe.jar net.tjado.jcdbe.jcdbeTest <username> <password> <url>");
      System.exit(10);
    }

    DatabaseOracle db = new DatabaseOracle();
    if (!db.connect(url, username, password)) {

      // get exception if connect failed
      Exception eC = db.getLastException();

      eC.printStackTrace();
      System.exit(1);
    }

    String query = "SELECT instance_name from v$instance";
    System.out.println("Result: " + db.getSingleQueryResult(query, null));

    System.out.println("Connection successfull!");



    System.out.println("==========================");
    System.out.println("jdbcInfo Start");
    System.out
        .println("Oracle MOS: How To Determine The Exact JDBC Driver Version [...] (Doc ID 467804.1)");
    System.out.println("==========================");

    Connection conn = db.getLink();
    DatabaseMetaData meta = conn.getMetaData();

    // gets driver info:
    System.out.println("\nDatabase\n==============");
    System.out.println(meta.getDatabaseProductVersion());
    System.out.println("\nJDBC\n==============");
    System.out.println(meta.getDriverName() + ": " + meta.getDriverVersion());
    System.out.println("\nConnection URL\n==============");
    System.out.println(meta.getURL());

    java.util.Properties props = System.getProperties();
    System.out.println("\nJVM\n===");
    System.out.println(props.getProperty("java.vm.vendor"));
    System.out.println(props.getProperty("java.vm.name"));
    System.out.println(props.getProperty("java.vm.version"));
    System.out.println(props.getProperty("java.version"));

    // Get environment information.
    System.out.println("\nLOCALE\n===========");
    System.out.println(Locale.getDefault());

    System.out.println("\nBOOTSTRAP (sun.boot.class.path)\n==============================\n"
        + System.getProperty("sun.boot.class.path"));
    System.out.println("\nEXTENSION PACKAGES (java.ext.dirs)\n=================================\n"
        + System.getProperty("java.ext.dirs") + "\n");
    String[] dirs = new String[5];
    int cnt = 0;
    StringTokenizer st;
    // if windows parse with ; else parse with :
    if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
      st = new StringTokenizer(System.getProperty("java.ext.dirs"), " ;");
    else
      st = new StringTokenizer(System.getProperty("java.ext.dirs"), " :");

    while (st.hasMoreTokens()) {
      dirs[cnt] = st.nextToken();
      System.out.println(dirs[cnt] + ": ");
      File folder = new File(dirs[cnt]);
      File[] listOfFiles = folder.listFiles();
      if (listOfFiles != null) for (int j = 0; j < listOfFiles.length; j++)
        System.out.println("      " + listOfFiles[j].getName());
      cnt++;
    }

    // Get CLASSPATH
    String pathseparator = props.getProperty("path.separator");
    String classpath = props.getProperty("java.class.path");
    System.out.println("\nCLASSPATH\n=========");
    String[] strarr = classpath.split(pathseparator);
    for (int i = 0; i < strarr.length; i++)
      System.out.println(strarr[i]);

    // Get LIBRARY PATH
    String libpath = props.getProperty("java.library.path");
    System.out.println("\nLIBRARYPATH\n===========");
    strarr = libpath.split(pathseparator);
    for (int i = 0; i < strarr.length; i++)
      System.out.println(strarr[i]);

    System.out.println("==========================");
    System.out.println("jdbcInfo End");
    System.out.println("==========================");


    System.exit(0);
  }

}
