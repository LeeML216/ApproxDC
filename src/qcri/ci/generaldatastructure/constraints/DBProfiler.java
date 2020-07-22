package qcri.ci.generaldatastructure.constraints;

import qcri.ci.generaldatastructure.db.*;
import qcri.ci.utils.Config;
import qcri.ci.utils.FileUtil;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import ca.pfv.spmf.frequentpatterns.apriori_optimized.AlgoApriori_saveToFile;
import ca.pfv.spmf.frequentpatterns.apriori_optimized.Itemset;
import ca.pfv.spmf.frequentpatterns.fpgrowth_saveToFile.AlgoFPGrowth;

public class DBProfiler {

	private Table table;
	private NewTable newTable;
	ArrayList<Map<String,Set<Tuple>>>  colProfiler = new ArrayList<Map<String,Set<Tuple>>> ();
	ArrayList<Map<String,Set<NewTuple>>>  colProfiler2 = new ArrayList<> ();
	

	public DBProfiler(Table table)
	{
		this.table = table;
		profiling();
		this.table.dbPro = this;
	}

	public DBProfiler(NewTable table)
	{
		this.newTable = table;
		profiling2();
		this.newTable.dbPro = this;
	}
	
	
	private void profiling()
	{
		int numCols = table.getNumCols();
		for(int i = 0 ; i < numCols; i++)
		{
			Map<String,Set<Tuple>>  temp = new HashMap<String,Set<Tuple>>();
			colProfiler.add(temp);
		}
		for(Tuple tuple: table.getTuples())
		{
			for(int i = 0 ; i < numCols; i++)
			{
				String value = tuple.getCell(i).getValue();
				if(!colProfiler.get(i).containsKey(value))
				{
					Set<Tuple> a = new HashSet<Tuple>();
					a.add(tuple);
					colProfiler.get(i).put(value, a);
				}
				else
				{
					Set<Tuple> a = colProfiler.get(i).get(value);
					a.add(tuple);
					colProfiler.get(i).put(value, a);
				}			
			}
		}
	}

	private void profiling2()
	{
		int numCols = newTable.getNumCols();
		for(int i = 0 ; i < numCols; i++)
		{
			Map<String,Set<NewTuple>>  temp = new HashMap<>();
			colProfiler2.add(temp);
		}
		for(NewTuple tuple: newTable.getTuples())
		{
			for(int i = 0 ; i < numCols; i++)
			{
				String value = tuple.getCell(i).getStringValue();
				if(!colProfiler2.get(i).containsKey(value))
				{
					Set<NewTuple> a = new HashSet<>();
					a.add(tuple);
					colProfiler2.get(i).put(value, a);
				}
				else
				{
					Set<NewTuple> a = colProfiler2.get(i).get(value);
					a.add(tuple);
					colProfiler2.get(i).put(value, a);
				}
			}
		}
	}
	
	
	
	public boolean equalOrNot(int col, String s1, String s2)
	{
		Map<String,Set<Tuple>> temp = colProfiler.get(col);
		if(temp.get(s1) == temp.get(s2))
			return true;
		else 
			return false;
	}

	public boolean equalOrNot2(int col, String s1, String s2)
	{
		Map<String,Set<NewTuple>> temp = colProfiler2.get(col);
		if(temp.get(s1) == temp.get(s2))
			return true;
		else
			return false;
	}
	
	
	
	/**
	 * The colNum is starting from 1
	 * @param colNum
	 * @return
	 */
	public Set<Set<Tuple>> getColumnGrouping(int colNum)
	{
		Set<Set<Tuple>> result = new HashSet<Set<Tuple>>();
		for(Set<Tuple> temp: colProfiler.get(colNum-1).values())
			result.add(new HashSet<Tuple>(temp));
		return result;
	}
	
	public Set<String> getPairColumnsCommonValues(int colL, int colR)
	{
		Set<String> keySet1 = colProfiler.get(colL -1 ).keySet();
		Set<String> keySet2 = colProfiler.get(colR-1 ).keySet();
		
		Set<String> result = new HashSet<String>(keySet1);
		result.retainAll(keySet2);
		return result;
		
	}
	public Set<Tuple> getTupleSet(int col, String value)
	{
		return colProfiler.get(col-1).get(value);
	}
	
	/**
	 * Get column pairs that are comparable by equality/inequality
	 * @return
	 */

	public Set<String> getEquComCols()
	{
		Set<String> result = new HashSet<String>();
		ColumnMapping cm = table.getColumnMapping();
		
		String[] allNames = cm.getColumnNames();
		for(int i = 0 ; i < allNames.length; i++)
		{
			for(int j = 0 ; j < allNames.length; j++)
			{
				if(i >= j)
					continue;
				
				String columnName = allNames[i];
				String name = allNames[j];
				
				
				int type = cm.NametoType(columnName);
				int thisType = cm.NametoType(name);
				if(thisType != type)
					continue;
				
				//Now this column has the same type as the input column
				int col1 = cm.NametoPosition(columnName);
				int col2 = cm.NametoPosition(name);
				Map<String,Set<Tuple>> map1 = colProfiler.get(col1-1);
				Map<String,Set<Tuple>> map2 = colProfiler.get(col2-1);
				
				Set<String> temp1 = new HashSet<String>(map1.keySet());
				Set<String> temp2 = new HashSet<String>(map2.keySet());
				Set<String> all = new HashSet<String>(temp1);
				all.addAll(temp2);
				temp1.retainAll(temp2);
		
				//double jarDis = (double)count / (table.getNumRows() * table.getNumRows());
				double jarDis = (double)temp1.size() / all.size();
				//if(jarDis > 0 && (temp1.size() == table.getNumRows() || temp2.size() == table.getNumRows()))
				if(jarDis > Config.joinableThre)	
					result.add(columnName + "," + name);
				
			}
		}
		return result;
	}


	public Set<String> getEquComCols2()
	{
		Set<String> result = new HashSet<String>();
		ColumnMapping cm = newTable.getColumnMapping();

		String[] allNames = cm.getColumnNames();
		for(int i = 0 ; i < allNames.length; i++)
		{
			for(int j = 0 ; j < allNames.length; j++)
			{
				if(i >= j)
					continue;

				String columnName = allNames[i];
				String name = allNames[j];


				int type = cm.NametoType(columnName);
				int thisType = cm.NametoType(name);
				if(thisType != type)
					continue;

				//Now this column has the same type as the input column
				int col1 = cm.NametoPosition(columnName);
				int col2 = cm.NametoPosition(name);
				Map<String,Set<NewTuple>> map1 = colProfiler2.get(col1-1);
				Map<String,Set<NewTuple>> map2 = colProfiler2.get(col2-1);

				Set<String> temp1 = new HashSet<String>(map1.keySet());
				Set<String> temp2 = new HashSet<String>(map2.keySet());
				Set<String> all = new HashSet<String>(temp1);
				all.addAll(temp2);
				temp1.retainAll(temp2);

				//double jarDis = (double)count / (table.getNumRows() * table.getNumRows());
				double jarDis = (double)temp1.size() / all.size();
				//if(jarDis > 0 && (temp1.size() == table.getNumRows() || temp2.size() == table.getNumRows()))
				if(jarDis > Config.joinableThre)
					result.add(columnName + "," + name);

			}
		}
		return result;
	}
	
	/**
	 * Get column pairs that are comparable by order, i.e. ">, < "
	 * @return
	 */
	
	public Set<String> getOrderComCols()
	{
		Set<String> result = new HashSet<String>();
		ColumnMapping cm = table.getColumnMapping();
		
		String[] allNames = cm.getColumnNames();
		for(int i = 0 ; i < allNames.length; i++)
		{
			for(int j = 0 ; j < allNames.length; j++)
			{
				if(i >= j)
					continue;
				
				String columnName = allNames[i];
				String name = allNames[j];
				
				
				int type = cm.NametoType(columnName);
				int thisType = cm.NametoType(name);
				if(thisType != type)
					continue;
				
				if(type == 2)
					continue;
				
				//Now this column has the same type as the input column
				int col1 = cm.NametoPosition(columnName);
				int col2 = cm.NametoPosition(name);
				
				double[] data1 = new double[table.getNumRows()];
				double[] data2 = new double[table.getNumRows()];
				
				for(int t = 0 ; t < table.getNumRows(); t++)
				{
					Tuple tuple = table.getTuple(t);
					data1[t] = Double.valueOf(tuple.getCell(col1-1).getValue());
					data2[t] = Double.valueOf(tuple.getCell(col2-1).getValue());
				
				}
				double mean1 = getMean(data1);
				double mean2 = getMean(data2);
				double var1 = this.getStandardDeviation(data1);
				double var2 = this.getStandardDeviation(data2);
				
				if( mean1 < mean2 && mean2 / mean1 < 10)
				{
					result.add(columnName + "," + name);
				}
				else if(mean1 > mean2 && mean1 / mean2  < 10)
				{
					result.add(columnName + "," + name);
				}
				/*else if(var1 < var2 && var2/ var1 < 10)
				{
					result.add(columnName + "," + name);
				}
				else if(var1 > var2 && var1/var2 < 10)
				{
					result.add(columnName + "," + name);
				}*/
					
			}
		}
		
		if(table.getTableName().equals("Other"))
		{
			result.clear();
			result.add("High" + "," + "A");
			
			return result;
		}
		
		else 
			return result;
	}


	public Set<String> getOrderComCols2()
	{
		Set<String> result = new HashSet<String>();
		ColumnMapping cm = newTable.getColumnMapping();

		String[] allNames = cm.getColumnNames();
		for(int i = 0 ; i < allNames.length; i++)
		{
			for(int j = 0 ; j < allNames.length; j++)
			{
				if(i >= j)
					continue;

				String columnName = allNames[i];
				String name = allNames[j];


				int type = cm.NametoType(columnName);
				int thisType = cm.NametoType(name);
				if(thisType != type)
					continue;

				if(type == 2)
					continue;

				//Now this column has the same type as the input column
				int col1 = cm.NametoPosition(columnName);
				int col2 = cm.NametoPosition(name);

				double[] data1 = new double[newTable.getNumRows()];
				double[] data2 = new double[newTable.getNumRows()];

				for(int t = 0 ; t < newTable.getNumRows(); t++)
				{
					NewTuple tuple = newTable.getTuple(t);
					data1[t] = tuple.getCell(col1-1).getDoubleValue();
					data2[t] = tuple.getCell(col2-1).getDoubleValue();

				}
				double mean1 = getMean(data1);
				double mean2 = getMean(data2);
				double var1 = this.getStandardDeviation(data1);
				double var2 = this.getStandardDeviation(data2);

				if( mean1 < mean2 && mean2 / mean1 < 10)
				{
					result.add(columnName + "," + name);
				}
				else if(mean1 > mean2 && mean1 / mean2  < 10)
				{
					result.add(columnName + "," + name);
				}
				/*else if(var1 < var2 && var2/ var1 < 10)
				{
					result.add(columnName + "," + name);
				}
				else if(var1 > var2 && var1/var2 < 10)
				{
					result.add(columnName + "," + name);
				}*/

			}
		}

		if(newTable.getTableName().equals("Other"))
		{
			result.clear();
			result.add("High" + "," + "A");

			return result;
		}

		else
			return result;
	}
	
	public Set<String> getEquComColsManual()
	{
		Set<String> result = new HashSet<String>();
		ColumnMapping cm = table.getColumnMapping();
		
		String[] allNames = cm.getColumnNames();
		if(table.tableName.equalsIgnoreCase("TaxGenerator"))
		{
			
		}
		return result;
	}
	
	
	public Set<String> getOrderComColsManual()
	{
		Set<String> result = new HashSet<String>();
		ColumnMapping cm = table.getColumnMapping();
		
		String[] allNames = cm.getColumnNames();
		System.out.println("Table Name: " + table.tableName);
		if(table.tableName.equalsIgnoreCase("TaxGenerator"))
		{
			//result.add(allNames[9] + "," + allNames[11]);
			//result.add(allNames[9] + "," + allNames[12]);
		}
		
		return result;
	}
	
	
	private double getMean(double[] data)
	{
		double sum = 0;
		for(double i: data)
			sum += i;
		
		return sum / data.length;
	}
	private double getStandardDeviation(double[] data)
	{
		final int n = data.length; 
		if ( n < 2 ) 
		{ 
		return Double.NaN; 
		} 
		double avg = data[0]; 
		double sum = 0; 
		for ( int i = 1; i < data.length; i++ ) 
		{ 
		double newavg = avg + ( data[i] - avg ) / ( i + 1 ); 
		sum += ( data[i] - avg ) * ( data [i] -newavg ) ; 
		avg = newavg; 
		} 
		// Change to ( n - 1 ) to n if you have complete data instead of a sample. 
		return Math.sqrt( sum / ( n - 1 ) ); 
	}
	
	
	public ArrayList<Predicate> findkfrequentconstantpredicate()
	{
		int kfrequent = (int) (table.getNumRows() * Config.kfre);
		ArrayList<Predicate> consPres = new ArrayList<Predicate>();
		ColumnMapping cm = table.getColumnMapping();
		for(int i = 0; i < table.getNumCols(); i++)
		{
			Map<String,Set<Tuple>> tempi = colProfiler.get(i);
			for(String value: tempi.keySet())
			{
				int sizeEQ = tempi.get(value).size();
				int sizeIQ = table.getNumRows() - sizeEQ;
				if(sizeEQ >= kfrequent && sizeIQ >= kfrequent)
				{
					Predicate pre = new Predicate(table,"EQ",1,i+1,value);
					consPres.add(pre);
					pre = new Predicate(table,"IQ",1,i+1,value);
					consPres.add(pre);
				}
			}
			
			int type = cm.positionToType(i+1);
			if(type == 1)
			{
				ArrayList<Double> values = new ArrayList<Double>();
				for(String value: tempi.keySet())
				{
					values.add(Double.valueOf(value));
				}
				double min = Double.MAX_VALUE;
				double max = Double.MIN_VALUE;
				for(double value: values)
				{
					if(value < min)
						min = value;
					if(value > max)
						max = value;
				}
				double incre = (max - min) / Config.numSplits;
				//split [min,max] into 5 pieces
				double split = min + incre;
				while(split < max)
				{
					Predicate pre = new Predicate(table,"LT",1,i+1,String.valueOf(split));
					consPres.add(pre);
					pre = new Predicate(table,"GTE",1,i+1,String.valueOf(split));
					consPres.add(pre);
					split += incre;
				}
			}
			else if(type == 0)
			{
				ArrayList<Integer> values = new ArrayList<Integer>();
				for(String value: tempi.keySet())
				{
					values.add(Integer.valueOf(value));
				}
				int min = Integer.MAX_VALUE;
				int max = Integer.MIN_VALUE;
				for(int value: values)
				{
					if(value < min)
						min = value;
					if(value > max)
						max = value;
				}
				int incre = (max - min) / Config.numSplits;
				if(incre == 0)
					incre = 1;
				int split = min + incre;
				while(split < max)
				{
					Predicate pre = new Predicate(table,"LT",1,i+1,String.valueOf(split));
					consPres.add(pre);
					pre = new Predicate(table,"GTE",1,i+1,String.valueOf(split));
					consPres.add(pre);
					split += incre;
				}
			}
		}
		System.out.println("We have found: " + consPres.size() + " constant predicates");
		return consPres;
	}
	
	

	/**
	 * Profiling each column to find the k-frequent constant predicate
	 * @param consPres
	 */
	public void getEXCHKDCs(ArrayList<DenialConstraint> exchkDCs,ArrayList<Predicate> consPres)
	{
		//ArrayList<Predicate> consPres = new ArrayList<Predicate>();
		
		
		int kfrequent = (int) (table.getNumRows() * Config.kfre);
		ColumnMapping cm = table.getColumnMapping();
		for(int i = 0; i < table.getNumCols(); i++)
		{
			Map<String,Set<Tuple>> tempi = colProfiler.get(i);
			for(String value: tempi.keySet())
			{
				int sizeEQ = tempi.get(value).size();
				int sizeIQ = table.getNumRows() - sizeEQ;
				if(sizeEQ >= kfrequent && sizeIQ >= kfrequent)
				{
					Predicate pre = new Predicate(table,"EQ",1,i+1,value);
					consPres.add(pre);
					pre = new Predicate(table,"IQ",1,i+1,value);
					consPres.add(pre);
				}
			}
			
			int type = cm.positionToType(i+1);
			if(type == 1)
			{
				ArrayList<Double> values = new ArrayList<Double>();
				for(String value: tempi.keySet())
				{
					values.add(Double.valueOf(value));
				}
				double min = Double.MAX_VALUE;
				double max = Double.MIN_VALUE;
				for(double value: values)
				{
					if(value < min)
						min = value;
					if(value > max)
						max = value;
				}
				double incre = (max - min) / Config.numSplits;
				//split [min,max] into 5 pieces
				double split = min + incre;
				while(split < max)
				{
					Predicate pre = new Predicate(table,"LT",1,i+1,String.valueOf(split));
					consPres.add(pre);
					pre = new Predicate(table,"GTE",1,i+1,String.valueOf(split));
					consPres.add(pre);
					split += incre;
				}
			}
			else if(type == 0)
			{
				ArrayList<Integer> values = new ArrayList<Integer>();
				for(String value: tempi.keySet())
				{
					values.add(Integer.valueOf(value));
				}
				int min = Integer.MAX_VALUE;
				int max = Integer.MIN_VALUE;
				for(int value: values)
				{
					if(value < min)
						min = value;
					if(value > max)
						max = value;
				}
				int incre = (max - min) / Config.numSplits;
				if(incre == 0)
					incre = 1;
				int split = min + incre;
				while(split < max)
				{
					Predicate pre = new Predicate(table,"LT",1,i+1,String.valueOf(split));
					consPres.add(pre);
					pre = new Predicate(table,"GTE",1,i+1,String.valueOf(split));
					consPres.add(pre);
					split += incre;
				}
			}
		}
		System.out.println("EXCHKS: we have found: " + consPres.size() + " constant predicates");
		if(consPres.size() == 0)
			return;
		
		
		String input = new String((new StringBuilder(table.getCurrentFolder())).append("IntegerDB"));
		String output = new String((new StringBuilder(table.getCurrentFolder())).append("FrequentItemSet"));
		//iterate over every tuple, and add the items to the database
		//the item starts from 1, the index of the predicate starts from 0, so pay attention
		try {
			PrintWriter out = new PrintWriter(new FileWriter(input));
			//for every two pairs of tuples, find all the predicates that they are satisfying
			for(Tuple tuple: table.getTuples())
			{
				StringBuilder sb = new StringBuilder();
				for(int i = 0; i < consPres.size(); i++)
				{
					
					Predicate consPre = consPres.get(i);
					if(consPre.check(tuple))
					{
						sb.append((i + 1));
						sb.append(" ");
					}
				}
				sb.deleteCharAt(sb.length() - 1);
			
				out.println(sb);
			}
			
			out.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println( "We have created the integer database");
		//Mining k-frequent predicate sets, and the right denial constraints
		AlgoApriori_saveToFile apriori = new AlgoApriori_saveToFile(consPres);
		try {
			apriori.runAlgorithm(kfrequent,input, output);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println( "We have done the apriori algorithm");
		//Get k-nonfrequent itmesets;
		ArrayList<Itemset> kemptyk_1Frequent 
		= apriori.getkEmptyAllK_1FrequentItemsets();
		//= apriori.getkEmptyAllK_1FrequentItemsetsAppro();
		
		System.out.println("The number of one tuple constant DCs is: " + kemptyk_1Frequent.size());
		for(Itemset itemSet: kemptyk_1Frequent)
		{
			int[] items = itemSet.items;
			ArrayList<Predicate> tempPres = new ArrayList<Predicate>();
			for(int item: items)
			{
				tempPres.add(consPres.get(item - 1));
			}
			DenialConstraint dc = new DenialConstraint(1,tempPres);
			exchkDCs.add(dc);
		}
		
		/*//Get k-frequent free itemsets
		ArrayList<Itemset> kFrequentFreeItemsets = apriori.getkFrequentFreeItemsets();
		System.out.println("The number of f frequent free itemset for mixed is: " + kFrequentFreeItemsets.size());
		
		
		
		
		//Get k-frequent itemsets;
		ArrayList<Itemset> frequentItemSets =  apriori.getFrequentItemSets();
		System.out.println("The number of constant predicate set for mixed is: " + frequentItemSets.size());
		for(Itemset itemSet: frequentItemSets )
		{
			int[] items = itemSet.items;
			Set<Predicate> tempSet = new HashSet<Predicate>();
			for(int item: items)
			{
				Predicate pre1 = consPres.get(item - 1);
				Predicate pre2 = new Predicate(table,pre1.getName(),2,pre1.getCols()[0],pre1.getCons());
				tempSet.add(pre1);
				tempSet.add(pre2);
			}
			
			
			consPresInputs.add(tempSet);
	
			for(Predicate tempPre: tempSet)
			{
				System.out.print(tempPre.toString());
			}
			System.out.println();
		}*/
		
	}
	
	
	public void findkfrequentconstantpredicateEQOnly(Set<Set<Predicate>> consPres)
	{

		ArrayList<Map<String, Integer>> str2int= new ArrayList<Map<String,Integer>>();
		Map<Integer,Integer> int2Col = new HashMap<Integer,Integer>();

		int kfrequent = (int) (table.getNumRows() * Config.kfre) + 1;
		String input = new String((new StringBuilder(table.getCurrentFolder())).append("IntegerDB"));
		String output = new String((new StringBuilder(table.getCurrentFolder())).append("FrequentItemSet"));


		int tag = 1;
		//Step 1, transform the string database into an integer database for frequent itemset mining
		for(int i = 0; i < table.getNumCols(); i++)
		{
			Map<String,Integer> coliInt = new HashMap<String,Integer>();


			Map<String,Set<Tuple>> coliMap = colProfiler.get(i);
			for(String str: coliMap.keySet())
			{
				coliInt.put(str, tag);
				int2Col.put(tag, i);
				tag++;
			}

			str2int.add(coliInt);
		}
		try {
			PrintWriter out = new PrintWriter(new FileWriter(input));
			//for every two pairs of tuples, find all the predicates that they are satisfying
			for(Tuple tuple: table.getTuples())
			{
				StringBuilder sb = new StringBuilder();
				for(int i = 0; i < table.getNumCols(); i++)
				{
					String value = tuple.getCell(i).getValue();
					int item = str2int.get(i).get(value);
					sb.append(item);
					if(i != table.getNumCols()-1)
						sb.append(" ");
				}
				out.println(sb);
			}
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}


		
		
		
		/*AlgoFPGrowth fpgrowth = new AlgoFPGrowth();
		try {
			fpgrowth.runAlgorithm(input, output, kfrequent);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		
		AlgoApriori_saveToFile apriori = new AlgoApriori_saveToFile();
		try {
			apriori.runAlgorithm(kfrequent,input, output);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		ArrayList<Itemset> frequentItemSets =  apriori.getkFrequentFreeItemsets();
		for(Itemset itemSet: frequentItemSets )
		{
			int[] items = itemSet.items;
			//if(items.length >= 2)
				//System.out.println("fd");
			Set<Predicate> tempSet = new HashSet<Predicate>();
			for(int item: items)
			{
				int col = int2Col.get(item);
				Map<String,Integer> coliInt = str2int.get(col);
				for(String string: coliInt.keySet())
				{
					if(coliInt.get(string) == item)
					{
						Predicate pre1 = new Predicate(table,"EQ",1,col+1,string);
						Predicate pre2 = new Predicate(table,"EQ",2,col+1,string);
						tempSet.add(pre1);
						tempSet.add(pre2);
						break;
					}
				}
				
			}
			consPres.add(tempSet);
		}
		
		//Delete the files
		//FileUtil.deleteFile(input);
		//FileUtil.deleteFile(output);
		
	}

	public void findkfrequentconstantpredicateEQOnly2(Set<Set<NewPredicate>> consPres)
	{

		ArrayList<Map<String, Integer>> str2int= new ArrayList<Map<String,Integer>>();
		Map<Integer,Integer> int2Col = new HashMap<Integer,Integer>();

		int kfrequent = (int) (table.getNumRows() * Config.kfre) + 1;
		String input = new String((new StringBuilder(table.getCurrentFolder())).append("IntegerDB"));
		String output = new String((new StringBuilder(table.getCurrentFolder())).append("FrequentItemSet"));


		int tag = 1;
		//Step 1, transform the string database into an integer database for frequent itemset mining
		for(int i = 0; i < table.getNumCols(); i++)
		{
			Map<String,Integer> coliInt = new HashMap<String,Integer>();


			Map<String,Set<Tuple>> coliMap = colProfiler.get(i);
			for(String str: coliMap.keySet())
			{
				coliInt.put(str, tag);
				int2Col.put(tag, i);
				tag++;
			}

			str2int.add(coliInt);
		}
		try {
			PrintWriter out = new PrintWriter(new FileWriter(input));
			//for every two pairs of tuples, find all the predicates that they are satisfying
			for(Tuple tuple: table.getTuples())
			{
				StringBuilder sb = new StringBuilder();
				for(int i = 0; i < table.getNumCols(); i++)
				{
					String value = tuple.getCell(i).getValue();
					int item = str2int.get(i).get(value);
					sb.append(item);
					if(i != table.getNumCols()-1)
						sb.append(" ");
				}
				out.println(sb);
			}
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}





		/*AlgoFPGrowth fpgrowth = new AlgoFPGrowth();
		try {
			fpgrowth.runAlgorithm(input, output, kfrequent);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/


		AlgoApriori_saveToFile apriori = new AlgoApriori_saveToFile();
		try {
			apriori.runAlgorithm(kfrequent,input, output);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		ArrayList<Itemset> frequentItemSets =  apriori.getkFrequentFreeItemsets();
		for(Itemset itemSet: frequentItemSets )
		{
			int[] items = itemSet.items;
			//if(items.length >= 2)
			//System.out.println("fd");
			Set<NewPredicate> tempSet = new HashSet<>();
			for(int item: items)
			{
				int col = int2Col.get(item);
				Map<String,Integer> coliInt = str2int.get(col);
				for(String string: coliInt.keySet())
				{
					if(coliInt.get(string) == item)
					{
						NewPredicate pre1 = new NewPredicate(newTable,0,1,col+1,string);
						NewPredicate pre2 = new NewPredicate(newTable,0,2,col+1,string);
						tempSet.add(pre1);
						tempSet.add(pre2);
						break;
					}
				}

			}
			consPres.add(tempSet);
		}

		//Delete the files
		//FileUtil.deleteFile(input);
		//FileUtil.deleteFile(output);

	}
	
}
