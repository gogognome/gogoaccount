/*
 * $Id: PartyCsvReader.java,v 1.3 2007-10-15 19:33:48 sanderk Exp $
 *
 * Copyright (C) 2007 Sander Kooijmans
 */
package nl.gogognome.cf.tool;

import java.util.ArrayList;

import nl.gogognome.csv.CsvFileParser;
import cf.engine.Party;

/**
 * This class reads a comma separated value (CSV) file and creates a list of
 * <code>Party</code> instances based on values in the CSV file.
 *
 * @author Sander Kooijmans
 */
public class PartyCsvReader {

    private int nrFirstLine = 0;
    private int nrLastLine = Integer.MAX_VALUE;
    
    private String colId = "{0}";
    private String colName = "{2} {1}";
    private String colAddress = "{3}";
    private String colZip = "{4}";
    private String colCity = "{5}";
    
    private String[][] values;
    
    public PartyCsvReader(String[][] values) {
        this.values = values;
        nrLastLine = values.length-1;
    }
    
    public Party[] getParties() {
        ArrayList parties = new ArrayList();

        for (int lineNr=nrFirstLine; lineNr<=nrLastLine; lineNr++) {
            String[] columns = values[lineNr];
            Party party = new Party(CsvFileParser.composeValue(colId, columns),
                    CsvFileParser.composeValue(colName, columns),
                    CsvFileParser.composeValue(colAddress, columns),
                    CsvFileParser.composeValue(colZip, columns),
                    CsvFileParser.composeValue(colCity, columns), null, null, null);
            parties.add(party);
        }
        
        return (Party[]) parties.toArray(new Party[parties.size()]);
    }
    
    
    public void setPatterns(String colId, String colName, String colAddress, String colZip,
            String colCity) {
        this.colId = colId;
        this.colName = colName;
        this.colAddress = colAddress;
        this.colZip = colZip;
        this.colCity = colCity;
    }
            
    public int getNrFirstLine() {
        return nrFirstLine;
    }
    
    public void setNrFirstLine(int nrFirstLine) {
        this.nrFirstLine = Math.min(Math.max(nrFirstLine, 0), values.length-1);
    }
    
    public int getNrLastLine() {
        return nrLastLine;
    }
    
    public void setNrLastLine(int nrLastLine) {
        this.nrLastLine = Math.min(Math.max(nrLastLine, 0), values.length-1);
    }
}