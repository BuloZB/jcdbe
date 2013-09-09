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

import javax.sql.rowset.CachedRowSet;

import org.ini4j.Ini;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * OutputDummy stores no data.
 */
public class OutputDummy implements Output {

  // instance object (singleton)
  private static final OutputDummy INSTANCE = new OutputDummy();

  // private constructor -> singleton
  private OutputDummy() {/************** nothing in constructor **************/
  }

  // get instance of this class
  public static OutputDummy getInstance() {
    return INSTANCE;
  }

  public void setCLI(Options o) {}

  public void validateParameters(CommandLine cli, Ini ini) {}

  public boolean init() {
    return true;
  }

  public void close() {
    return;
  }

  public boolean prepareDatabase(Integer taskDBID) {
    return true;
  }

  public boolean prepareQuery(Integer taskDBID, Integer queryID) {
    return true;
  }
  
  public void setDatabaseStatus(Integer taskDBID, String status) {
    return;
  }

  public void setQueryStatus(Integer taskDBID, Integer queryID, String status) {
    return;
  }

  public void saveResult(CachedRowSet rs, Integer taskDBID, Integer queryID) {
    return;
  }
}
