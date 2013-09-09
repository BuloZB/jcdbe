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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * This class handles the synchronized access of the database list which also includes login
 * information, Runnable object for threading, db handle etc...
 * 
 * Conditionally thread-safe
 * 
 */
public class DatabaseList implements Iterable<Integer>, Iterator<Integer> {

  // HashMap to store the database list internally
  private Map<Integer, Map<String, Object>> databaseList =
      new HashMap<Integer, Map<String, Object>>();

  // Current number of databases in list
  private int databaseListSize = 0;

  // Iterator current position of the internal id
  private int currentPosition = 0;
  
  public DatabaseList() {/************** nothing in constructor **************/}
  
  public synchronized int insert(String url, String username, String password, Integer extDatabaseId) {

    // define next id
    int newID = ++databaseListSize;

    Map<String, Object> db = new HashMap<String, Object>();

    // extDatabaseId to identify the database outside JCDBE if necessary
    // e.g. primary key of the respective table in an unrelated db schema
    if (extDatabaseId == null) {
      db.put("EXTERNAL_ID", newID);
    } else {
      db.put("EXTERNAL_ID", extDatabaseId);
    }

    // connection parameter
    db.put("URL", url.trim());
    db.put("USERNAME", username.trim());
    db.put("PASSWORD", password.trim());

    // database handle for later forced termination
    db.put("DATABASE_HANDLE", new DatabaseOracle());

    // last update time
    db.put("LAST_UPDATE", System.currentTimeMillis());

    // field for indicating that the database thread was processed
    db.put("FINISHED", false);

    databaseList.put(newID, db);

    return newID;
  }

  public synchronized boolean isFinished() {
    for (int i = 1; i <= databaseListSize; i++) {
      Boolean finished = (Boolean) get(i, "FINISHED");
      if (finished == false) {
        return false;
      }
    }

    return true;
  }

  private synchronized Object get(Integer id, String key) {
    if (databaseList.containsKey(id)) {
      Map<String, Object> db = databaseList.get(id);
      if (db.containsKey(key)) {
        return db.get(key);
      }
    }
    
    return null;
  }

  private synchronized boolean set(Integer id, String key, Object value) {
    if (databaseList.containsKey(id)) {
      Map<String, Object> db = databaseList.get(id);

      // if String, then trim it
      if (value instanceof String) {
        value = ((String) value).trim();
      }

      db.put(key, value);
      db.put("LAST_UPDATE", System.currentTimeMillis());
      databaseList.put(id, db);
      return true;
    }
    
    return false;
  }

  public void setFinish(Integer id) {
    set(id, "FINISHED", true);
  }

  public boolean setThread(Integer id, Runnable thread) {
    return set(id, "THREAD", thread);
  }

  public String getURL(Integer id) {
    return (String) get(id, "URL");
  }

  public String getUsername(Integer id) {
    return (String) get(id, "USERNAME");
  }

  public String getPassword(Integer id) {
    return (String) get(id, "PASSWORD");
  }

  public Integer getLastUpdate(Integer id) {
    return (Integer) get(id, "LAST_UPDATE");
  }

  public Runnable getThread(Integer id) {
    return (Runnable) get(id, "THREAD");
  }

  public DatabaseOracle getDatabaseHandle(Integer id) {
    return (DatabaseOracle) get(id, "DATABASE_HANDLE");
  }

  public Integer getExternalId(Integer id) {
    return (Integer) get(id, "EXTERNAL_ID");
  }

  
  public int size() {
    return databaseListSize;
  }
  
  
  public boolean hasNext() {
    if (currentPosition < databaseListSize)
      return true;
    else
      return false;
  }

  public Integer next() {
    if (currentPosition == databaseListSize) throw new NoSuchElementException();

    currentPosition++;
    return currentPosition;
  }

  public void remove() {
    return;
  }

  public Iterator<Integer> iterator() {
    return this;
  }

}
