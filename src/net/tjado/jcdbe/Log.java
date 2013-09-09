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
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Log helper class which uses apache-log4j
 * 
 * Note: log4j is currently deprecated -> replace it?! 
 * 
 * because of this helper class, filename and line numbers would always point to this file there is
 * a simple solution: l.log(Log.class.getCanonicalName(), Level.FATAL, msg, null); Described here:
 * http://stackoverflow.com/a/1486532
 * 
 */
public class Log {

  // instance object (singleton)
  private static final Log INSTANCE = new Log();
  
  // the "real" logger object
  private static Logger l = Logger.getLogger(Log.class.getName());

  //private constructor -> singleton
  private Log() {/************** nothing in constructor **************/}

  public static Log getInstance() {
    return INSTANCE;
  }

  public void init(String file) {
    Properties properties = new Properties();
    try {
      properties.load(new FileInputStream(file));
    } catch (Exception e) {
      System.out.println("Error: log4j property file exception");
      System.exit(1);
    }
    PropertyConfigurator.configure(properties);
  }

  // fatal
  public void fatal(int dbID, String msg) {
    if (dbID == 0) msg = "[MASTER] " + msg;

    this.fatal("[" + String.format("%05d", dbID) + "] " + formatString(msg));
  }

  public void fatal(String msg) {
    l.log(Log.class.getCanonicalName(), Level.FATAL, formatString(msg), null);
  }


  // warning
  public void warn(int dbID, String msg) {
    if (dbID == 0) msg = "[MASTER] " + msg;

    this.warn("[" + String.format("%05d", dbID) + "] " + formatString(msg));
  }

  public void warn(String msg) {
    l.log(Log.class.getCanonicalName(), Level.WARN, formatString(msg), null);
  }


  // info
  public void info(int dbID, String msg) {
    if (dbID == 0) msg = "[MASTER] " + msg;

    this.info("[" + String.format("%05d", dbID) + "] " + formatString(msg));
  }

  public void info(String msg) {
    l.log(Log.class.getCanonicalName(), Level.INFO, formatString(msg), null);
  }


  // debug
  public void debug(int dbID, String msg) {
    if (dbID == 0) msg = "[MASTER] " + msg;

    this.debug(formatString(msg));
  }

  public void debug(String msg) {
    l.log(Log.class.getCanonicalName(), Level.DEBUG, formatString(msg), null);
  }


  public String formatString(String str) {
    if (str != null) {
      str = str.replaceAll("\\r\\n|\\r|\\n", " ");
    }
    return str;
  }

}
