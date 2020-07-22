package qcri.ci.generaldatastructure.db;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import qcri.ci.generaldatastructure.constraints.DBProfiler;
import qcri.ci.generaldatastructure.constraints.DenialConstraint;
import qcri.ci.utils.Config;

public class NewTable {

	public String inputDBPath;
	private ColumnMapping colMap;
	private String schema;

	private int numRows;
	private int numCols;
	private List<NewTuple> tuples = new ArrayList<>();


	public DBProfiler dbPro;

	public String tableName;
	public NewTable(String inputDBPath, int numRows)
	{
		this.inputDBPath = inputDBPath;
		this.numRows = numRows;
		initFromFile();

		//tableName = inputDBPath.split("/")[1];
		tableName = "default";
	}
	public NewTable(NewTable table)
	{
		this.inputDBPath = table.inputDBPath;
		this.colMap = new ColumnMapping(table.colMap.getColumnHead());
		this.schema = table.schema;
		this.numRows = 0;
		this.numCols = table.numCols;
		this.tableName = table.tableName;
		for(NewTuple tuple: table.tuples)
		{
			tuples.add(new NewTuple(tuple));
		}
	}
	private boolean isInteger(String s) {
	    try { 
	        Integer.parseInt(s); 
	    } catch(NumberFormatException e) { 
	        return false; 
	    }
	    // only got here if we didn't return false
	    return true;
	}
	private boolean isDouble(String s) {
	    try { 
	        Double.parseDouble(s);
	    } catch(NumberFormatException e) { 
	        return false; 
	    }
	    // only got here if we didn't return false
	    return true;
	}
	private void initFromFile()
	{
		try 
		{
			BufferedReader br =new BufferedReader(new FileReader(inputDBPath));
			String line = null;
			int temp = 0;
			while((line = br.readLine()) != null)
			{
				if(temp == 0)
				{
					//First line is the columns information
					colMap = new ColumnMapping(line);
					schema = line;
					String[] columns = line.split(",");
					this.numCols = columns.length;
					temp ++;
				}
				else
				{
					String[] colValues = line.split(",");
					//Length checking
					if (colValues.length != this.numCols)
					{
						//System.out.println("Input Database Error");
						continue;
					}
					boolean nullColumn = false;
					//NULl column checking
					for(String value: colValues)
					{
						if(value.equals("") || value.equals("?")
								|| value.contains("?"))
						{
							//System.out.println("NULL columns");
							nullColumn = true;
							break;
						}
					}
					if(nullColumn)
					{
						continue;
					}
					
					//Type checking
					boolean tag = true;
					for(int i = 0 ; i < numCols; i++)
					{
						String coliValue = colValues[i];
						
						int type = colMap.positionToType(i+1);
						if(type == 0 && !isInteger(coliValue))
						{
							tag = false;
							break;
						}
						else if(type == 1 && ! isDouble(coliValue))
						{
							tag = false;
							break;
						}
					}
					if(tag == false)
						continue;
					NewTuple tuple = new NewTuple(colValues,colMap,temp);
					tuples.add(tuple);
					temp ++;
					if(temp > numRows)
					{
						break;
					}
					
				}
			}
			br.close();
			numRows = tuples.size();
			System.out.println("NumRows:  " + numRows + " NumCols:" + numCols);
		} 
		catch (FileNotFoundException e)
		{
			
			e.printStackTrace();
		} 
		catch (IOException e) {
			
			e.printStackTrace();
		}
	}
	
	
	
	public void initFromFileBulkLoad(MyConnection conn)
	{
		try 
		{
			List<String[]> allColValues = new ArrayList<String[]>();
			BufferedReader br =new BufferedReader(new FileReader(inputDBPath));
			String line = null;
			int temp = 0;
			while((line = br.readLine()) != null)
			{
				if(temp == 0)
				{
					//First line is the columns information
					colMap = new ColumnMapping(line);
					schema = line;
					String[] columns = line.split(",");
					this.numCols = columns.length;
					String tableName = inputDBPath.split("/")[inputDBPath.split("/").length - 2];
					conn.createTable(tableName, colMap);
					temp ++;
				}
				else
				{
					String[] colValues = line.split(",");
					if (colValues.length != this.numCols)
					{
						System.out.println("Input Database Error");
						continue;
					}
					NewTuple tuple = new NewTuple(colValues,colMap,temp);
					String[] colValuesTemp = new String[colValues.length];
					for(int i = 0; i < colValues.length; i++)
					{
						if(colMap.positionToType(i+1) == 2)
							colValuesTemp[i] = "'" + colValues[i] + "'";
						else 
							colValuesTemp[i] = colValues[i];
					}
						
					tuples.add(tuple);
					String tableName = inputDBPath.split("/")[inputDBPath.split("/").length - 2];
					//conn.insertTable(tableName, colValuesTemp);
					allColValues.add(colValuesTemp);
					temp ++;
					
				}
			}
			br.close();
			numRows = tuples.size();
			conn.builInsertTable(getTableName(), allColValues);
			System.out.println("NumRows:  " + numRows);
		} 
		catch (FileNotFoundException e)
		{
			
			e.printStackTrace();
		} 
		catch (IOException e) {
			
			e.printStackTrace();
		}
	}
	public ColumnMapping getColumnMapping()
	{
		return colMap;
	}
	public int getNumRows()
	{
		return numRows;
	}
	public int getNumCols()
	{
		return numCols;
	}
	public List<NewTuple> getTuples()
	{
		return tuples;
	}
	public NewTuple getTuple(int t)
	{
		return tuples.get(t);
	}
	
	public void removeTuple(Set<Tuple> toBeRemoved)
	{
		tuples.removeAll(toBeRemoved);
		numRows = tuples.size();
	}
	public void retainTuple(Set<Tuple> toBeRetained)
	{
		tuples.retainAll(toBeRetained);
		numRows = tuples.size();
	}
	public void insertTuples(Set<NewTuple> toBeInserted)
	{
		tuples.addAll(toBeInserted);
		numRows = tuples.size();
	}
	/**
	 * Get cell for a row and col, both starting from 0
	 * @param row
	 * @param col
	 * @return
	 */
	/*public Cell getCell(int row, int col)
	{
		return tuples.get(row).getCell(col);
	}*/
	/*public void setCell(int row, int col, String newValue)
	{
		tuples.get(row).getCell(col).setValue(newValue);
	}*/
	public String getTableName()
	{
		return inputDBPath.split("/")[inputDBPath.split("/").length - 2];
	}
	public String getSchema()
	{
		return schema;
	}
	public String getCurrentFolder()
	{
		int index = inputDBPath.indexOf("inputDB");
		StringBuilder sb = new StringBuilder(inputDBPath.substring(0, index));
		return new String(sb);
	}
	
	

	/**
	 * @deprecated
	 * return the number of violations of this dc, actually this becomes find the intersection of different sets
	 * @param dc
	 * @return
	 */
	public int getNumVios(DenialConstraint dc)
	{
		/*int countFail = 0;
		int tuplePairCount = 0;
		for(int t1 = 0 ; t1 < numRows; t1++)
			for(int t2 = 0; t2 < numRows; t2++)
			{
				if(t1 == t2)
					continue;
				tuplePairCount++;
				boolean allSatis = true;
				ArrayList<Predicate> predicates = dc.getPredicates();
				for(int p = 0 ; p < predicates.size(); p++)
				{
					if(!predicates.get(p).check(tuplePairCount))
					{
						allSatis = false;
						break;
					}
					
				}
				if(allSatis)
				{
					countFail++;
				}
			}
		return countFail;*/
		
		/*ArrayList<Predicate> predicates = dc.getPredicates();
		ArrayList<Set<Integer>> all = new ArrayList<Set<Integer>>();
		for(int i = 0 ; i < predicates.size(); i++)
			all.add(predicates.get(i).getVios());
		Set<Integer> intersect  = new HashSet<Integer>(all.get(0));
		for(int i = 1; i < all.size(); i++)
		{
			intersect.retainAll(all.get(i));
			if(intersect.size() == 0)
				return 0;
		}
		return intersect.size();*/
		
		return -1;
	}
	
	/**
	 * Get all the constants in a particular column
	 * @param col
	 * @return
	 */
	/*public Set<String> getColumnValues(int col)
	{
		Set<String> colValues = new HashSet<String>();
		for(int i = 0 ; i < numRows; i++)
		{
			String value = tuples.get(i).getCell(col).getValue();
			colValues.add(value);
		}
		return colValues;
	} */
	
	
	/**
	 * Manually insert noise into this table
	 */
	/*public void insertNoise(double noiseType)
	{
		//collect the active domain
		Map<Integer,Set<String>> ad = new HashMap<Integer,Set<String>>();
		for(int i = 0; i < numCols; i++)
		{
			ad.put(i, getColumnValues(i));
		}
		System.out.println("Inserting noise using noise level: " + Config.noiseLevel);
		int countTypo = 0;
		int countDomainError = 0;
		Random rd = new Random();
		for(int i = 0; i < numRows; i++)
		{
			for(int j = 0 ; j < numCols; j++)
			{
				
				//do not introduce errors for those binary columns
				*//*
				if(colMap.posiionToName(j+1).equals("HasChild") || 
						colMap.posiionToName(j+1).equals("MaritalStatus")
						||colMap.posiionToName(j+1).equals("State") 
						||colMap.posiionToName(j+1).equals("AreaCode")		
					)

					continue;
				*//*
				if(rd.nextDouble() <= Config.noiseLevel )
				{
					
					String[] temp = ad.get(j).toArray(new String[0]);
					String randomValue = null;
					
					//create a typo
					if(new Random().nextDouble() <= noiseType)
					{
						countTypo++;
						if(colMap.positionToType(j+1) == 2)
						{
							String ij = getCell(i,j).toString();
							char a = ij.charAt(new Random().nextInt(ij.length()));
							randomValue = ij.replace(a, 'x');
						}
						else
						{
							//randomValue  = getCell(i,j).toString() + "0";
							
							String ij = getCell(i,j).toString();
							char a = ij.charAt(new Random().nextInt(ij.length()));
							randomValue = ij.replace(a, '8');
							
							//randomValue = temp[new Random().nextInt(temp.length)];
						}
					}
					else
					{
						countDomainError++;
						//create an active domain error
						randomValue = temp[new Random().nextInt(temp.length)];
					}
					
					
					//randomValue = temp[new Random().nextInt(temp.length)];
					setCell(i, j, randomValue);
				}
			}
			
		}
		System.out.println("The total number of cells changed is: typo" + countTypo);

		System.out.println("The total number of cells changed is: domain error" + countDomainError);
	
		
		
	}
	public void insertNoise(double noiseType,List<String> errors)
	{
		//collect the active domain
		Map<Integer,Set<String>> ad = new HashMap<Integer,Set<String>>();
		for(int i = 0; i < numCols; i++)
		{
			ad.put(i, getColumnValues(i));
		}
		System.out.println("Inserting noise using noise level: " + Config.noiseLevel);
		int countTypo = 0;
		int countDomainError = 0;
		
		//int numErrors = (int) (numRows * numCols * Config.noiseLevel);
		//List<String> curErrors = errors.subList(0, numErrors);
		
		for(int i = 0; i < numRows; i++)
		{
			for(int j = 0 ; j < numCols; j++)
			{
				
				//do not introduce errors for those binary columns
				*//*
				if(colMap.posiionToName(j+1).equals("HasChild") || 
						colMap.posiionToName(j+1).equals("MaritalStatus")
						||colMap.posiionToName(j+1).equals("State") 
						||colMap.posiionToName(j+1).equals("AreaCode")		
					)

					continue;
				*//*
				String haha = i + "," + j;
				if(errors.contains(haha) )
				{
					
					String[] temp = ad.get(j).toArray(new String[0]);
					String randomValue = null;
					
					//create a typo
					if(new Random().nextDouble() <= noiseType)
					{
						countTypo++;
						if(colMap.positionToType(j+1) == 2)
						{
							String ij = getCell(i,j).toString();
							char a = ij.charAt(new Random().nextInt(ij.length()));
							
							char rdC = (char)(new Random().nextInt(26) + 'a');
							
							randomValue = ij.replace(a, rdC);
						}
						else
						{
							//randomValue  = getCell(i,j).toString() + "0";
							
							String ij = getCell(i,j).toString();
							char a = ij.charAt(new Random().nextInt(ij.length()));
							
							char rdC = (char)('0' + new Random().nextInt(10));
							
							randomValue = ij.replace(a, rdC);
							
							//randomValue = temp[new Random().nextInt(temp.length)];
						}
					}
					else
					{
						countDomainError++;
						//create an active domain error
						randomValue = temp[new Random().nextInt(temp.length)];
					}
					
					
					//randomValue = temp[new Random().nextInt(temp.length)];
					
					System.out.println("ERROR: " + "row, col" + i + "," + colMap.posiionToName(j+1) + ": "
							+ "OLD: " + getCell(i,j).toString() 
							+ "NEW: " + randomValue) ;
					setCell(i, j, randomValue);
				}
			}
			
		}
		System.out.println("The total number of cells changed is: typo" + countTypo);

		System.out.println("The total number of cells changed is: domain error" + countDomainError);
	
		
		
	}*/
	/**
	 * Dump the table to the desFile
	 * @param desFile
	 */
	public void dump2File(String desFile)
	{
		PrintWriter out;
		try {
			out = new PrintWriter(new FileWriter(desFile));
			out.println(schema);
			for(int i = 0; i < numRows; i++)
			{
				String s = tuples.get(i).toString();
				out.println(s);
			}
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	
	
	
	
	
	
	
}
