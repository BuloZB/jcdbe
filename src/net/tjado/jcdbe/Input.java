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

import org.ini4j.Ini;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;

/**
 * Input interface
 * 
 * Input implements the data source of JCBDE: 
 *  It needs to fill a DatabaseList object and also returns the queries 
 *  which will be executed on every database in the DatabaseList object
 * 
 */
public interface Input {

  public boolean init() throws Exception;
  
  public void close();

  public void setCLI(Options o) throws Exception;

  public void validateParameters(CommandLine cli, Ini ini);


  public DatabaseList getDatabaseList();

  public Map<Integer, String> getQueries();

}
