package qcri.ci;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import qcri.ci.generaldatastructure.constraints.DBProfiler;
import qcri.ci.generaldatastructure.constraints.DenialConstraint;
import qcri.ci.generaldatastructure.constraints.Predicate;
import qcri.ci.generaldatastructure.db.ColumnMapping;
import qcri.ci.generaldatastructure.db.MyConnection;
import qcri.ci.generaldatastructure.db.Table;
import qcri.ci.generaldatastructure.db.Tuple;
import qcri.ci.instancedriven.SingleTupleConstantDCMining;
import qcri.ci.utils.Config;
import qcri.ci.utils.OperatorMapping;

public class ConstraintDiscovery {

	protected long startingTime;

	public Table originalTable;
	protected Table table1,table2;

	protected ArrayList<DenialConstraint> totalDCs = new ArrayList<DenialConstraint>();
	public ArrayList<DenialConstraint> exchkDCs = new ArrayList<DenialConstraint>(); //extended check constraints



	public ArrayList<Predicate> allVarPre = new ArrayList<Predicate>();//Predicate with both variables
	public ArrayList<Predicate> preSpace = new ArrayList<Predicate>(); //The space of all predicates

	public ArrayList<Predicate> consPres = new ArrayList<Predicate>();// The set of all k-frequent constant predicate


	//public ArrayList<Predicate> singleTupleVarPre = new ArrayList<Predicate>(); //Predicate in a single tuple

	public static Object mutexLock = new Object();

	//From each set of constant predicates, to the set of DCs, having those constant predicates
	private Map<Set<Predicate>,ArrayList<DenialConstraint>> consPresMap = new HashMap<Set<Predicate>,ArrayList<DenialConstraint>>();

	//From each set of constant predicates, to the pairwise tuple info!
	private Map<Set<Predicate>,Map<Set<Predicate>,Long>> cons2PairInfo = new HashMap<Set<Predicate>,Map<Set<Predicate>,Long>>();


	private Map<Set<Predicate>,Integer> cons2NumTuples = new HashMap<Set<Predicate>,Integer>();


	public DBProfiler dbPro;

	public ConstraintDiscovery(String inputDBPath, int numRows)
	{
		startingTime = System.currentTimeMillis();
		originalTable = new Table(inputDBPath, numRows);
		table1 = originalTable;
		table2 = originalTable;

		System.out.println("before find all predicate");
		findAllPredicates();
		System.out.println((System.currentTimeMillis() - startingTime )/ 1000 +
				"Number of variable predicates: " + allVarPre.size());


		findAllConsPres();
		System.out.println( (System.currentTimeMillis() - startingTime )/ 1000 +
				": Done initialization of table and predicate space");



	}


	public double expectedInitTime()
	{
		double totalExpTime = 0;

		double k10Time = 0;
		if(originalTable.getTableName().equals("TaxGenerator"))
		{
			k10Time = 220.89;
		}
		else if(originalTable.getTableName().equals("ExpHospital"))
		{
			k10Time = 53.53;
		}
		else if(originalTable.getTableName().equals("SPStock"))
		{
			k10Time = 235;
		}


		for(Set<Predicate> limit: cons2PairInfo.keySet())
		{

			ArrayList<Tuple> curTuples = new ArrayList<Tuple>();
			for(Tuple tuple: originalTable.getTuples())
			{
				boolean good = true;
				for(Predicate pre: limit)
				{
					if(!pre.check(tuple))
					{
						good = false;
						break;
					}
				}
				if(good)
				{
					curTuples.add(tuple);
				}
			}
			totalExpTime += ((double)curTuples.size() / 10000 )
					* ((double)curTuples.size() / 10000 ) * k10Time;

		}

		return totalExpTime;
	}

	public void initHeavyWork(int howInit)
	{
		if(howInit == 1 || howInit == 0)
		{
			//initAllTupleWiseInfoParallel();
			//initAllTupleWiseInfoParallel2();

			for(Set<Predicate> cons: cons2PairInfo.keySet())
			{
				this.buildingEviUtil(cons, allVarPre, cons2PairInfo.get(cons));

				StringBuilder sb = new StringBuilder();
				for(Predicate pre: cons)
					sb.append(pre.toString() + " &");
				System.out.println((System.currentTimeMillis() - startingTime )/ 1000 +
						"Done initialization evidence for " + sb);
			}


			System.out.println((System.currentTimeMillis() - startingTime )/ 1000 +
					"Done initialization tuple pair wise information using one machine");
		}
		else if(howInit == 2)
		{
			//Ian's code!...
			String preInfoPath = new String((new StringBuilder(originalTable.getCurrentFolder())).append("preInfo.txt"));
			try {
				synchronized(mutexLock){
					// only one thread is allowed to go in everytime
					DistributedExecutor executor = new DistributedExecutor();
					executor.execute(originalTable.getCurrentFolder(),this.originalTable, this.getAllVarPredicates(),preInfoPath);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			//At the this point, i should have the predicate info file
			//Name is "preInfo.txt", under directory preInfoPath

			parsePreInfo(preInfoPath);


			System.out.println((System.currentTimeMillis() - startingTime )/ 1000 +
					"Done initialization tuple pair wise information using Cluster");
		}


		String preInfoPath = new String((new StringBuilder(originalTable.getCurrentFolder())).append("preInfo.txt"));
		try {
			PrintWriter out = new PrintWriter(preInfoPath);
			StringBuilder sb = new StringBuilder();
			for(Predicate pre: allVarPre)
			{
				sb.append(pre.toString());
				sb.append(":");
			}
			sb.append("count");
			out.println(sb);
			//Write the preInfo into a text File
			//check if the init has been done correctly
			for(Set<Predicate> cons: cons2PairInfo.keySet())
			{
				if(cons.size()!=0)
					continue;
				Map<Set<Predicate>, Long> value = cons2PairInfo.get(cons);
				long total = 0;
				for(Set<Predicate> key: value.keySet())
				{

					sb = new StringBuilder();
					for(Predicate pre: allVarPre)
					{
						if(key.contains(pre))
						{
							sb.append("1" + ":");
						}
						else
						{
							sb.append("0" + ":");
						}
					}
					sb.append(value.get(key));
					out.println(sb);

					total += value.get(key);
				}
				System.out.println("Total Evi1: " + total);
				System.out.println("Total Evi2: " + (long) originalTable.getNumRows() * (originalTable.getNumRows() - 1));

			}
			out.close();


		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//Output the preinfo sorted:
		preInfoPath = new String((new StringBuilder(originalTable.getCurrentFolder())).append("preInfoSorted.txt"));
		try {
			PrintWriter out = new PrintWriter(preInfoPath);
			StringBuilder sb = new StringBuilder();
			for(Predicate pre: allVarPre)
			{
				sb.append(pre.toString());
				sb.append(":");
			}
			sb.append("count");
			out.println(sb);
			//Write the preInfo into a text File
			//check if the init has been done correctly
			for(Set<Predicate> cons: cons2PairInfo.keySet())
			{
				if(cons.size()!=0)
					continue;
				Map<Set<Predicate>, Long> value = cons2PairInfo.get(cons);
				//Sort them
				ArrayList<Set<Predicate>> sortedKeys = new ArrayList<Set<Predicate>>();
				for(Set<Predicate> key: value.keySet())
				{
					int i = 0;
					for(i = 0; i < sortedKeys.size(); i++)
					{
						if(value.get(key) > value.get(sortedKeys.get(i)))
						{
							sortedKeys.add(i, key);
							break;
						}
					}
					if( i == sortedKeys.size())
					{
						sortedKeys.add(key);
					}
				}


				long total = 0;
				for(Set<Predicate> key: sortedKeys)
				{

					sb = new StringBuilder();
					for(Predicate pre: allVarPre)
					{
						if(key.contains(pre))
						{
							sb.append("1" + ":");
						}
						else
						{
							sb.append("0" + ":");
						}
					}
					sb.append(value.get(key));
					out.println(sb);

					total += value.get(key);
				}
				System.out.println("Total Evi1: " + total);
				System.out.println("Total Evi2: " + (long) originalTable.getNumRows() * (originalTable.getNumRows() - 1));

			}
			out.close();


		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void parsePreInfo(String preInfoPath)
	{

		Map<Set<Predicate>,Long> tupleWiseInfo = cons2PairInfo.get(new HashSet<Predicate>());

		try {
			BufferedReader br = new BufferedReader(new FileReader(preInfoPath));
			String line = null;
			int count = -1;
			while((line = br.readLine())!=null)
			{
				if (line.equals(""))
					continue;
				if(count == -1)//first line is the head
				{
					count++;
					continue;
				}
				String[] temp = line.split(":");
				assert(temp.length == allVarPre.size() + 1);

				Set<Predicate> key = new HashSet<Predicate>();
				for(int i = 0 ;i < temp.length - 1; i++)
				{
					if(temp[i].equals("1"))
					{
						key.add(allVarPre.get(i));
					}
				}
				tupleWiseInfo.put(key, Long.valueOf(temp[temp.length - 1]));

			}
			br.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	/**
	 * This is the function where you can add all predicates that you are interested in
	 * The set of operators you are interested in
	 * Allowing comparison within a tuple?
	 * Allowing comparison cross columns?
	 */
	private void findAllPredicates()
	{
		/*String[] ops = Config.ops;

		//Order the ops such that it is paired, each pair is like (EQ,IQ) (GT,LTE)
		ArrayList<String> orderedOps = new ArrayList<String>();
		for(int i = 0 ; i < ops.length; i++)
		{
			String opName = ops[i];
			if(orderedOps.contains(opName))
				continue;
			String reverseName = OperatorMapping.getReserveName(opName);
			orderedOps.add(opName);
			orderedOps.add(reverseName);

		}
		*/
		//1. EQ, IQ for each column
		//A=,A!,B=,B!=
		int numCols = originalTable.getNumCols();
		for(int col = 1; col <= numCols; col++)
		{

			Predicate pre = new Predicate(originalTable,"EQ",1,2,col,col);
			allVarPre.add(pre);
			pre = new Predicate(originalTable,"IQ",1,2,col,col);
			allVarPre.add(pre);


			int type = originalTable.getColumnMapping().positionToType(col);
			if(type == 0 || type == 1)
			{
				pre = new Predicate(originalTable,"GT",1,2,col,col);
				allVarPre.add(pre);
				pre = new Predicate(originalTable,"LTE",1,2,col,col);
				allVarPre.add(pre);

				pre = new Predicate(originalTable,"LT",1,2,col,col);
				allVarPre.add(pre);

				pre = new Predicate(originalTable,"GTE",1,2,col,col);
				allVarPre.add(pre);
			}

		}

		//2. GL, LTE for each column with numerical values (LT, GTE)?
		for(int col =1 ; col <= numCols; col++)
		{
			/*int type = originalTable.getColumnMapping().positionToType(col);
			if(type == 0 || type == 1)
			{
				Predicate pre = new Predicate(originalTable,"GT",1,2,col,col);
				allVarPre.add(pre);
				pre = new Predicate(originalTable,"LTE",1,2,col,col);
				allVarPre.add(pre);

				pre = new Predicate(originalTable,"LT",1,2,col,col);
				allVarPre.add(pre);

				pre = new Predicate(originalTable,"GTE",1,2,col,col);
				allVarPre.add(pre);
			}*/
		}

		dbPro = new DBProfiler(originalTable);

		preSpace.addAll(allVarPre);

		if(Config.enableCrossColumn)
		{
			Set<String> eqCompa = dbPro.getEquComCols();
			Set<String> orderCompa = dbPro.getOrderComCols();

			int[] row1s = null;
			int[] row2s = null;
			if(Config.ps == 1)
			{
				row1s = new int[]{1,1};
				row2s = new int[]{1,2};
			}
			else if(Config.ps == 2)
			{
				row1s = new int[]{1,2,1,2};
				row2s = new int[]{1,2,2,1};
			}



			//3. EQ, IQ for column pairs
			for(String cols: eqCompa)
			{
				System.out.println("Joinale columns: " + cols);
				String[] colsTemp = cols.split(",");
				int col1 = originalTable.getColumnMapping().NametoPosition(colsTemp[0]);
				int col2 = originalTable.getColumnMapping().NametoPosition(colsTemp[1]);
				for(int i = 0; i <  row1s.length; i++)
				{
					int row1 = row1s[i];
					int row2 = row2s[i];
					Predicate pre = new Predicate(originalTable,"EQ",row1,row2,col1,col2);
					allVarPre.add(pre);
					//singleTupleVarPre.add(pre);
					pre = new Predicate(originalTable,"IQ",row1,row2,col1,col2);
					allVarPre.add(pre);
					//singleTupleVarPre.add(pre);
				}
			}
			// 4. GL, LTE for each column pair with numerical values
			for(String cols: orderCompa)
			{
				System.out.println("Order Comparable columns: " + cols);
				String[] colsTemp = cols.split(",");
				int col1 = originalTable.getColumnMapping().NametoPosition(colsTemp[0]);
				int col2 = originalTable.getColumnMapping().NametoPosition(colsTemp[1]);
				for(int i = 0; i <  row1s.length; i++)
				{
					int row1 = row1s[i];
					int row2 = row2s[i];
					Predicate pre = new Predicate(originalTable,"GT",row1,row2,col1,col2);
					allVarPre.add(pre);
					pre = new Predicate(originalTable,"LTE",row1,row2,col1,col2);
					allVarPre.add(pre);
					pre = new Predicate(originalTable,"LT",row1,row2,col1,col2);
					allVarPre.add(pre);
					pre = new Predicate(originalTable,"GTE",row1,row2,col1,col2);
					allVarPre.add(pre);
				}
			}

			//Add all predicates to predicate space
			row1s = new int[]{1,2,1,2};
			row2s = new int[]{1,2,2,1};
			//3. EQ, IQ for column pairs
			for(String cols: eqCompa)
			{
				System.out.println("Joinale columns: " + cols);
				String[] colsTemp = cols.split(",");
				int col1 = originalTable.getColumnMapping().NametoPosition(colsTemp[0]);
				int col2 = originalTable.getColumnMapping().NametoPosition(colsTemp[1]);
				for(int i = 0; i <  row1s.length; i++)
				{
					int row1 = row1s[i];
					int row2 = row2s[i];
					Predicate pre = new Predicate(originalTable,"EQ",row1,row2,col1,col2);
					preSpace.add(pre);
					//singleTupleVarPre.add(pre);
					pre = new Predicate(originalTable,"IQ",row1,row2,col1,col2);
					preSpace.add(pre);
					//singleTupleVarPre.add(pre);
				}
			}
			// 4. GL, LTE for each column pair with numerical values
			for(String cols: orderCompa)
			{
				System.out.println("Order Comparable columns: " + cols);
				String[] colsTemp = cols.split(",");
				int col1 = originalTable.getColumnMapping().NametoPosition(colsTemp[0]);
				int col2 = originalTable.getColumnMapping().NametoPosition(colsTemp[1]);
				for(int i = 0; i <  row1s.length; i++)
				{
					int row1 = row1s[i];
					int row2 = row2s[i];
					Predicate pre = new Predicate(originalTable,"GT",row1,row2,col1,col2);
					preSpace.add(pre);
					pre = new Predicate(originalTable,"LTE",row1,row2,col1,col2);
					preSpace.add(pre);
					pre = new Predicate(originalTable,"LT",row1,row2,col1,col2);
					preSpace.add(pre);
					pre = new Predicate(originalTable,"GTE",row1,row2,col1,col2);
					preSpace.add(pre);
				}
			}

		}



	}

	private void findAllConsPres()
	{

		Set<Set<Predicate>> consPresInputs = new HashSet<Set<Predicate>>();
		consPresInputs.add(new HashSet<Predicate>());
		consPres.clear();
		consPresMap.clear();
		cons2PairInfo.clear();



		if(Config.enableMixedDcs)
			dbPro.findkfrequentconstantpredicateEQOnly(consPresInputs);


		for(Set<Predicate> temp: consPresInputs)
		{
			StringBuilder sb = new StringBuilder();
			for(Predicate pre: temp)
				sb.append(pre.toString() + " &");
			System.out.println("Cons for Mixed: " +  sb);
			consPresMap.put(temp, new ArrayList<DenialConstraint>());
			cons2PairInfo.put(temp, new HashMap<Set<Predicate>, Long>());

		}

		System.out.println("We will do this number of FASTDCs: " + consPresInputs.size());




		//Step 3: User specifies constant predicate set

		/*consPres = new HashSet<Predicate>();
		Predicate consPre = new Predicate(originalTable,"EQ",1,1,"1");
		consPres.add(consPre);
		if(!allConsPre.contains(consPre))
		{
			allConsPre.add(consPre);
			allConsPre.add(new Predicate(originalTable,"IQ",1,1,"1"));
		}
		consPre = new Predicate(originalTable,"EQ",2,1,"1");
		consPres.add(consPre);
		if(!allConsPre.contains(consPre))
		{
			allConsPre.add(consPre);
			allConsPre.add(new Predicate(originalTable,"IQ",2,1,"1"));
		}
		consPresInputs.add(consPres);*/

	}



	/**
	 * Initialize all the tuple pair wise information for each set of constant predicates
	 */
	private void buildingEviUtil(Set<Predicate> limit, final ArrayList<Predicate> allPres,  Map<Set<Predicate>,Long> evidence)
	{


		//Limit the database according to limit
		final ArrayList<Tuple> curTuples = new ArrayList<Tuple>();
		for(Tuple tuple: originalTable.getTuples())
		{
			boolean good = true;
			for(Predicate pre: limit)
			{
				if(!pre.check(tuple))
				{
					good = false;
					break;
				}
			}
			if(good)
			{
				curTuples.add(tuple);
			}
		}

		cons2NumTuples.put(limit, curTuples.size());

		int numThreads =  Runtime.getRuntime().availableProcessors();

		ArrayList<Thread> threads = new ArrayList<Thread>();

		final ArrayList<Map<Set<Predicate>, Long>> tempPairInfo
				= new ArrayList<Map<Set<Predicate>, Long>>();


		System.out.println("We are going to use " + numThreads + " threads to deal with " + curTuples.size() + " Tuples");
		final int numRows = curTuples.size();
		int chunk = numRows / numThreads;

		for(int k = 0 ; k < numThreads; k++)
		{
			final int kTemp = k;
			final int startk = k * chunk;
			final int finalk = (k==numThreads-1)? numRows:(k+1)*chunk;
			//tempInfo.add(new HashMap<Set<Predicate>,Long>());

			Map<Set<Predicate>, Long> tempPairInfok = new HashMap<Set<Predicate>, Long>();
			tempPairInfo.add(tempPairInfok);
			Thread thread = new Thread(new Runnable()
			{
				public void run() {
					Map<Set<Predicate>, Long> tempPairInfok = tempPairInfo.get(kTemp);

					int numCols = originalTable.getNumCols();
					for(int t1 = startk ; t1 < finalk; t1++)
						for(int t2 = 0 ; t2 < numRows; t2++)
						{

							Tuple tuple1 = curTuples.get(t1);
							Tuple tuple2 = curTuples.get(t2);

							if(tuple1.tid == tuple2.tid)
								continue;

							Set<Predicate> cur = new HashSet<Predicate>();

							//The naive way

							int p = 0;

							if(Config.qua == 1)
							{
								for(int col = 1; col <= numCols; col++)
								{

									Predicate pre = allPres.get(p);

									int type = originalTable.getColumnMapping().positionToType(col);

									if(type == 2)
									{

										int rel = 0; // 0- EQ, -1 -> IQ, 1- > , 2- <
										if(dbPro.equalOrNot(col-1, tuple1.getCell(col-1).getValue(), tuple2.getCell(col-1).getValue()))
										{

											rel = 0;
											cur.add(pre);

										}
										else
										{
											cur.add(allPres.get(p+1));


											rel = -1;
										}
										p = p + 2;
									}
									else if(type == 0 || type == 1)
									{
										// if >
										if(allPres.get(p+2).check(tuple1,tuple2))
										{
											cur.add(allPres.get(p+1));
											cur.add(allPres.get(p+2));
											cur.add(allPres.get(p+5));

										}
										else // <=
										{
											// =
											if(allPres.get(p).check(tuple1, tuple2))
											{
												cur.add(allPres.get(p));
												cur.add(allPres.get(p+3));
												cur.add(allPres.get(p+5));



											}
											else
											{
												cur.add(allPres.get(p+1));
												cur.add(allPres.get(p+3));
												cur.add(allPres.get(p+4));



											}

										}

										p = p + 6;
									}
									else
									{
										System.out.println("Type not supported");
									}

								}
							}


							for(p = p ; p < allPres.size(); p+=2)
							{

								Predicate pre = allPres.get(p);
								if(pre.check(tuple1,tuple2))
								{
									cur.add(pre);
								}
								else
									cur.add(allPres.get(p+1));



							}

							if (tempPairInfok.containsKey(cur))
							{
								tempPairInfok.put(cur, tempPairInfok.get(cur)+1);


							}
							else
							{
								tempPairInfok.put(cur, (long) 1);

							}



						}
				}

			});
			thread.start();
			threads.add(thread);
		}
		for(int k = 0 ; k < numThreads; k++)
		{
			try {
				threads.get(k).join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for(int k = 0 ; k < numThreads; k++)
		{
			Map<Set<Predicate>, Long> tempPairInfok = tempPairInfo.get(k);
			for(Set<Predicate> cur: tempPairInfok.keySet())
			{
				if (evidence.containsKey(cur))
				{
					evidence.put(cur, evidence.get(cur) + tempPairInfok.get(cur));
				}
				else
				{
					evidence.put(cur, tempPairInfok.get(cur));
					//System.out.println(cur.toString());
				}

			}


		}


	}
	/**
	 * Initialize all the tuple pair wise information for each set of constant predicates
	 */
	private void initAllTupleWiseInfoParallel()
	{

		int numThreads =  Runtime.getRuntime().availableProcessors();

		ArrayList<Thread> threads = new ArrayList<Thread>();
		final ArrayList<HashMap<Set<Predicate>, Map<Set<Predicate>, Long>>> tempcons2PairInfo
				= new ArrayList<HashMap<Set<Predicate>,Map<Set<Predicate>,Long>>>();

		//final ArrayList<HashMap<Set<Predicate>, Map<Set<Predicate>, Set<TP>>>> tempcons2PairInfoTP
		//= new ArrayList<HashMap<Set<Predicate>,Map<Set<Predicate>,Set<TP>>>>();

		//final ArrayList<Map<Set<Predicate>,Long>> tempInfo = new ArrayList<Map<Set<Predicate>,Long> >();
		System.out.println("We are going to use " + numThreads + " threads");
		final int numRows = originalTable.getNumRows();
		int chunk = numRows / numThreads;

		for(int k = 0 ; k < numThreads; k++)
		{
			final int kTemp = k;
			final int startk = k * chunk;
			final int finalk = (k==numThreads-1)? numRows:(k+1)*chunk;
			//tempInfo.add(new HashMap<Set<Predicate>,Long>());

			HashMap<Set<Predicate>, Map<Set<Predicate>, Long>> tempcons2PairInfok = new HashMap<Set<Predicate>, Map<Set<Predicate>, Long>>();
			//HashMap<Set<Predicate>, Map<Set<Predicate>, Set<TP>>> tempcons2PairInfoTPk = new HashMap<Set<Predicate>, Map<Set<Predicate>, Set<TP>>>();

			for(Set<Predicate> consPres: consPresMap.keySet())
			{
				tempcons2PairInfok.put(consPres, new HashMap<Set<Predicate>,Long>());
				//tempcons2PairInfoTPk.put(consPres, new HashMap<Set<Predicate>,Set<TP>>());
			}
			tempcons2PairInfo.add(tempcons2PairInfok);
			//tempcons2PairInfoTP.add(tempcons2PairInfoTPk );
			Thread thread = new Thread(new Runnable()
			{
				public void run() {
					HashMap<Set<Predicate>, Map<Set<Predicate>, Long>> tempcons2PairInfok = tempcons2PairInfo.get(kTemp);
					//HashMap<Set<Predicate>, Map<Set<Predicate>, Set<TP>>> tempcons2PairInfoTPk = tempcons2PairInfoTP.get(kTemp);
					//Map<Set<Predicate>,Long> tupleWiseInfok = tempInfo.get(kTemp);
					int numCols = originalTable.getNumCols();
					for(int t1 = startk ; t1 < finalk; t1++)
						for(int t2 = 0 ; t2 < numRows; t2++)
						{

							Tuple tuple1 = originalTable.getTuple(t1);
							Tuple tuple2 = originalTable.getTuple(t2);

							if(tuple1.tid == tuple2.tid)
								continue;

							Set<Predicate> cur = new HashSet<Predicate>();



							//The naive way

							int p = 0;

							if(Config.qua == 1)
							{
								for(int col = 1; col <= numCols; col++)
								{

									Predicate pre = allVarPre.get(p);

									int type = originalTable.getColumnMapping().positionToType(col);

									if(type == 2)
									{

										int rel = 0; // 0- EQ, -1 -> IQ, 1- > , 2- <
										if(dbPro.equalOrNot(col-1, tuple1.getCell(col-1).getValue(), tuple2.getCell(col-1).getValue()))
										{

											rel = 0;
											cur.add(pre);

										}
										else
										{
											cur.add(allVarPre.get(p+1));


											rel = -1;
										}
										p = p + 2;
									}
									else if(type == 0 || type == 1)
									{
										// if >
										if(allVarPre.get(p+2).check(tuple1,tuple2))
										{
											cur.add(allVarPre.get(p+1));
											cur.add(allVarPre.get(p+2));
											cur.add(allVarPre.get(p+5));



										}
										else // <=
										{
											// =
											if(allVarPre.get(p).check(tuple1, tuple2))
											{
												cur.add(allVarPre.get(p));
												cur.add(allVarPre.get(p+3));
												cur.add(allVarPre.get(p+5));



											}
											else
											{
												cur.add(allVarPre.get(p+1));
												cur.add(allVarPre.get(p+3));
												cur.add(allVarPre.get(p+4));



											}

										}

										p = p + 6;
									}
									else
									{
										System.out.println("Type not supported");
									}

								}
							}


							for(p = p ; p < allVarPre.size(); p+=2)
							{

								Predicate pre = allVarPre.get(p);
								if(pre.check(tuple1,tuple2))
								{
									cur.add(pre);
								}
								else
									cur.add(allVarPre.get(p+1));



							}


							//increaseOne(cur);
							for(Set<Predicate> consPres: consPresMap.keySet())
							{
								if(!satisfyConsPres(tuple1,tuple2,consPres))
									continue;
								Map<Set<Predicate>,Long> tupleWiseInfo = tempcons2PairInfok.get(consPres);
								if (tupleWiseInfo.containsKey(cur))
								{
									tupleWiseInfo.put(cur, tupleWiseInfo.get(cur)+1);


								}
								else
								{
									tupleWiseInfo.put(cur, (long) 1);

								}


							}


						}
				}

			});
			thread.start();
			threads.add(thread);
		}
		for(int k = 0 ; k < numThreads; k++)
		{
			try {
				threads.get(k).join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for(int k = 0 ; k < numThreads; k++)
		{
			HashMap<Set<Predicate>, Map<Set<Predicate>, Long>> tempcons2PairInfok = tempcons2PairInfo.get(k);
			//HashMap<Set<Predicate>, Map<Set<Predicate>, Set<TP>>> tempcons2PairInfoTPk = tempcons2PairInfoTP.get(k);
			for(Set<Predicate> consPres: consPresMap.keySet())
			{
				Map<Set<Predicate>,Long> tupleWiseInfok = tempcons2PairInfok.get(consPres);
				//Map<Set<Predicate>,Set<TP>> tupleWiseInfoTPk = tempcons2PairInfoTPk.get(consPres);
				Map<Set<Predicate>, Long> aa = cons2PairInfo.get(consPres);
				//Map<Set<Predicate>, Set<TP>> bb = cons2PairInfoTP.get(consPres);
				for(Set<Predicate> cur: tupleWiseInfok.keySet())
				{
					if (aa.containsKey(cur))
					{
						aa.put(cur, aa.get(cur) + tupleWiseInfok.get(cur));

					}
					else
					{
						aa.put(cur, tupleWiseInfok.get(cur));
					}
					/*if(bb.containsKey(cur))
					{
						bb.get(cur).addAll(tupleWiseInfoTPk.get(cur));
					}
					else
					{
						bb.put(cur, new HashSet<TP>(tupleWiseInfoTPk.get(cur)));
					}*/
				}
			}

		}





	}

	/**
	 * Initialize all the tuple pair wise information for each set of constant predicates
	 */
	private void initAllTupleWiseInfoParallel2()
	{

		int numThreads =  Runtime.getRuntime().availableProcessors();

		ArrayList<Thread> threads = new ArrayList<Thread>();
		final ArrayList<HashMap<Set<Predicate>, Map<Set<Predicate>, Long>>> tempcons2PairInfo
				= new ArrayList<HashMap<Set<Predicate>,Map<Set<Predicate>,Long>>>();




		final int numRows = originalTable.getNumRows();
		final int numBlocks = 5; // 1-5
		final int chunkSize = originalTable.getNumRows() / numBlocks;
		System.out.println("The chunk size is: " + chunkSize);
		final int lastChunkSize = originalTable.getNumRows() - chunkSize * (numBlocks-1);
		System.out.println("The last chunk size is: " + lastChunkSize);


		Map<Integer,String> threads2Blocks = new HashMap<Integer,String>();
		numThreads = 0;
		for(int b1 = 1; b1 <= numBlocks; b1++)
			for(int b2 = b1 +1; b2 <= numBlocks; b2++)
			{
				threads2Blocks.put(numThreads, b1 + "," + b2);
				numThreads++;
			}
		for(int b = 1; b <= numBlocks; b++)
		{
			threads2Blocks.put(numThreads, b + "," + b);
			numThreads++;
		}

		System.out.println("NumBlocks: " + numBlocks + " NumThreads: " + numThreads + " thread2Blocks :" + threads2Blocks.size());


		for(int k = 0 ; k < numThreads; k++)
		{
			final int kTemp = k;
			/*final int startk = k * chunk;
			final int finalk = (k==numThreads-1)? numRows*(numRows-1):(k+1)*chunk;
			*///tempInfo.add(new HashMap<Set<Predicate>,Long>());
			String blocks = threads2Blocks.get(k);
			//System.out.println(blocks);
			final int b1 = Integer.valueOf(blocks.split(",")[0]);
			final int b2 = Integer.valueOf(blocks.split(",")[1]);

			HashMap<Set<Predicate>, Map<Set<Predicate>, Long>> tempcons2PairInfok = new HashMap<Set<Predicate>, Map<Set<Predicate>, Long>>();
			//HashMap<Set<Predicate>, Map<Set<Predicate>, Set<TP>>> tempcons2PairInfoTPk = new HashMap<Set<Predicate>, Map<Set<Predicate>, Set<TP>>>();

			for(Set<Predicate> consPres: consPresMap.keySet())
			{
				tempcons2PairInfok.put(consPres, new HashMap<Set<Predicate>,Long>());
				//tempcons2PairInfoTPk.put(consPres, new HashMap<Set<Predicate>,Set<TP>>());
			}
			tempcons2PairInfo.add(tempcons2PairInfok);
			//tempcons2PairInfoTP.add(tempcons2PairInfoTPk );
			Thread thread = new Thread(new Runnable()
			{
				public void run() {
					HashMap<Set<Predicate>, Map<Set<Predicate>, Long>> tempcons2PairInfok = tempcons2PairInfo.get(kTemp);
					//HashMap<Set<Predicate>, Map<Set<Predicate>, Set<TP>>> tempcons2PairInfoTPk = tempcons2PairInfoTP.get(kTemp);
					//Map<Set<Predicate>,Long> tupleWiseInfok = tempInfo.get(kTemp);
					int numCols = originalTable.getNumCols();
					int b1s =  0;
					int b1e = 0;
					int b2s = 0;
					int b2e = 0;
					if(b1 != numBlocks)
					{
						b1s =  ( b1-1 ) * chunkSize;
						b1e = b1 * chunkSize;
					}
					else
					{
						b1s =  ( b1-1 ) * chunkSize;
						b1e = numRows;
					}


					if(b2 != numBlocks)
					{
						b2s = (b2 -1 ) * chunkSize;
						b2e = b2 * chunkSize;
					}
					else
					{
						b2s = (b2 -1 ) * chunkSize;
						b2e = numRows;
					}
					for(int t1 = b1s ; t1 < b1e; t1++)
						for(int t2 = b2s; t2 < b2e; t2++)
						{



							//System.out.println("Tuple pair: " + t1 + ":" + t2);
							if(t1 >= t2)
								continue;

							Tuple tuple1 = originalTable.getTuple(t1);
							Tuple tuple2 = originalTable.getTuple(t2);

							Set<Predicate> cur = new HashSet<Predicate>();

							Set<Predicate> cur21 = new HashSet<Predicate>();

							boolean usingcur21 = true;



							//The naive way

							int p = 0;

							if(Config.qua == 1)
							{
								for(int col = 1; col <= numCols; col++)
								{

									Predicate pre = allVarPre.get(p);

									int type = originalTable.getColumnMapping().positionToType(col);

									if(type == 2)
									{

										int rel = 0; // 0- EQ, -1 -> IQ, 1- > , 2- <
										if(dbPro.equalOrNot(col-1, tuple1.getCell(col-1).getValue(), tuple2.getCell(col-1).getValue()))
										{

											rel = 0;
											cur.add(pre);
											if(usingcur21)
												cur21.add(pre);
										}
										else
										{
											cur.add(allVarPre.get(p+1));
											if(usingcur21)
												cur21.add(allVarPre.get(p+1));

											rel = -1;
										}
										p = p + 2;
									}
									else if(type == 0 || type == 1)
									{
										// if >
										if(allVarPre.get(p+2).check(tuple1,tuple2))
										{
											cur.add(allVarPre.get(p+1));
											cur.add(allVarPre.get(p+2));
											cur.add(allVarPre.get(p+5));

											if(usingcur21)
											{
												cur21.add(allVarPre.get(p+1));
												cur21.add(allVarPre.get(p+3));
												cur21.add(allVarPre.get(p+4));
											}

										}
										else // <=
										{
											// =
											if(allVarPre.get(p).check(tuple1, tuple2))
											{
												cur.add(allVarPre.get(p));
												cur.add(allVarPre.get(p+3));
												cur.add(allVarPre.get(p+5));

												if(usingcur21)
												{
													cur21.add(allVarPre.get(p));
													cur21.add(allVarPre.get(p+3));
													cur21.add(allVarPre.get(p+5));
												}

											}
											else
											{
												cur.add(allVarPre.get(p+1));
												cur.add(allVarPre.get(p+3));
												cur.add(allVarPre.get(p+4));

												if(usingcur21)
												{
													cur21.add(allVarPre.get(p+1));
													cur21.add(allVarPre.get(p+2));
													cur21.add(allVarPre.get(p+5));
												}

											}

										}

										p = p + 6;
									}
									else
									{
										System.out.println("Type not supported");
									}

								}
							}


							for(p = p ; p < allVarPre.size(); p+=2)
							{

								Predicate pre = allVarPre.get(p);
								if(pre.check(tuple1,tuple2))
								{
									cur.add(pre);
								}
								else
									cur.add(allVarPre.get(p+1));

								if(usingcur21)
								{
									if(pre.check(tuple2,tuple1))
									{
										cur21.add(pre);
									}
									else
										cur21.add(allVarPre.get(p+1));
								}

							}


							//increaseOne(cur);
							for(Set<Predicate> consPres: consPresMap.keySet())
							{
								if(!satisfyConsPres(tuple1,tuple2,consPres))
									continue;
								Map<Set<Predicate>,Long> tupleWiseInfo = tempcons2PairInfok.get(consPres);
								if (tupleWiseInfo.containsKey(cur))
								{
									tupleWiseInfo.put(cur, tupleWiseInfo.get(cur)+1);


								}
								else
								{
									tupleWiseInfo.put(cur, (long) 1);

								}

								if(usingcur21)
								{
									if (tupleWiseInfo.containsKey(cur21))
									{
										tupleWiseInfo.put(cur21, tupleWiseInfo.get(cur21)+1);


									}
									else
									{
										tupleWiseInfo.put(cur21, (long) 1);

									}
								}

							}


						}
				}

			});
			thread.start();
			threads.add(thread);
		}
		for(int k = 0 ; k < numThreads; k++)
		{
			try {
				threads.get(k).join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for(int k = 0 ; k < numThreads; k++)
		{
			HashMap<Set<Predicate>, Map<Set<Predicate>, Long>> tempcons2PairInfok = tempcons2PairInfo.get(k);
			for(Set<Predicate> consPres: consPresMap.keySet())
			{
				Map<Set<Predicate>,Long> tupleWiseInfok = tempcons2PairInfok.get(consPres);
				Map<Set<Predicate>, Long> aa = cons2PairInfo.get(consPres);
				for(Set<Predicate> cur: tupleWiseInfok.keySet())
				{
					if (aa.containsKey(cur))
					{
						aa.put(cur, aa.get(cur) + tupleWiseInfok.get(cur));

					}
					else
					{
						aa.put(cur, tupleWiseInfok.get(cur));
					}
				}
			}

		}





	}

	/**
	 * Test if the two tuples satisfy the constant predicates
	 * @param t1
	 * @param t2
	 * @param cons
	 * @return
	 */
	private boolean satisfyConsPres(Tuple t1, Tuple t2, Set<Predicate> cons)
	{
		if(cons.size() == 0)
			return true;

		for(Predicate consPre: cons)
		{
			int row = consPre.getRows()[0];
			int col = consPre.getCols()[0];
			String value = consPre.getCons();
			String cellV = null;
			if(row == 1)
			{
				cellV = t1.getCell(col-1).getValue();

			}
			else if(row == 2)
			{
				cellV = t2.getCell(col-1).getValue();
			}
			String op = consPre.getName();
			assert(op.equals("EQ"));

			if(!cellV.equals(value))
				return false;
		}
		return true;
	}

	public ArrayList<Predicate> getAllVarPredicates()
	{
		return allVarPre;
	}

	/**
	 * Get the reverse predicate of all predicates
	 * @param pre
	 * @return
	 */
	protected Predicate getReversePredicate(Predicate pre)
	{
		if(allVarPre.contains(pre))
		{
			int index = allVarPre.indexOf(pre);
			if(index %2 == 0)
				return allVarPre.get(index+1);
			else
				return allVarPre.get(index - 1);
		}
		else if(consPres.contains(pre))
		{
			int index = consPres.indexOf(pre);
			if(index %2 == 0)
				return consPres.get(index+1);
			else
				return consPres.get(index - 1);
		}
		else
		{
			//System.out.println("ERROR: Getting the reverse of a predicate.");
			return null;
		}
	}

	private Map<Predicate,Set<Predicate>> impliedMap = new HashMap<Predicate,Set<Predicate>>();
	/**
	 * Get the set of predicates implied by the passed-in predicate, does not include the pre itself
	 * @param pre
	 * @return
	 */
	public Set<Predicate> getImpliedPredicates(Predicate pre)
	{
		if(pre == null)
			return new HashSet<Predicate>();

		if(!impliedMap.containsKey(pre))
		{

			if(pre.isSecondCons())
			{
				Set<Predicate> result = new HashSet<Predicate>();
				for(Predicate temp: consPres)
				{
					if(temp == pre)
						continue;
					if(pre.implied(temp))
						result.add(temp);
				}
				impliedMap.put(pre, result);
				return result;
			}
			else
			{
				Set<Predicate> result = new HashSet<Predicate>();
				for(Predicate temp: allVarPre)
				{
					if(temp == pre)
						continue;
					if(pre.implied(temp))
						result.add(temp);
				}
				impliedMap.put(pre, result);
				return result;
			}


		}
		else
			return impliedMap.get(pre);
	}


	/**
	 * This function is to compute the transitive closure of the set of predicates
	 * @param pres
	 */
	private void allImplied(Set<Predicate> pres)
	{
		for(Predicate left: pres)
			for(Predicate right: pres)
			{
				if(left == right)
					continue;
				Predicate t = transitive(left,right);
				if(t != null)
				{
					pres.add(t);
					return;
				}


			}
	}

	private Predicate transitive(Predicate left, Predicate right)
	{
		if(left == null || right == null)
			return null;


		int[] rowsleft = left.getRows();
		int[] rowsright = right.getRows();
		int[] colsleft = left.getCols();
		int[] colsright = right.getCols();
		String nameleft = left.getName();
		String nameright = right.getName();

		String lc1 = rowsleft[0] + "." + colsleft[0];
		String lc2 = rowsleft[1] + "." + colsleft[1];
		String rc1 = rowsright[0] + "." + colsright[0];
		String rc2 = rowsright[1] + "." + colsright[1];

		String resultc1 = null;
		String resultc2 = null;
		String resultn = null;

		if(lc2.equals(rc1))
		{
			resultc1 = lc1;
			resultc2 = rc2;
			if(nameleft.equals("EQ") && nameright.equals("EQ"))
			{
				resultn = "EQ";
			}
			else if(nameleft.equals("EQ") && nameright.equals("GT"))
			{
				resultn = "GT";
			}
			else if(nameleft.equals("EQ") && nameright.equals("LT"))
			{
				resultn = "LT";
			}
			else if(nameleft.equals("EQ") && nameright.equals("IQ"))
			{
				resultn = "IQ";
			}
			else if(nameleft.equals("IQ") && nameright.equals("EQ"))
			{
				resultn = "IQ";
			}
			else if(nameleft.equals("GT") && nameright.equals("EQ"))
			{
				resultn = "GT";
			}
			else if(nameleft.equals("GT") && nameright.equals("GT"))
			{
				resultn = "GT";
			}
			else if(nameleft.equals("LT") && nameright.equals("EQ"))
			{
				resultn = "LT";
			}
			else if(nameleft.equals("LT") && nameright.equals("LT"))
			{
				resultn = "LT";
			}

		}
		else if(lc2.equals(rc2))
		{
			resultc1 = lc1;
			resultc2 = rc1;
			if(nameleft.equals("EQ") && nameright.equals("EQ"))
			{
				resultn = "EQ";
			}
			else if(nameleft.equals("EQ") && nameright.equals("GT"))
			{
				resultn = "LT";
			}
			else if(nameleft.equals("EQ") && nameright.equals("LT"))
			{
				resultn = "GT";
			}
			else if(nameleft.equals("EQ") && nameright.equals("IQ"))
			{
				resultn = "IQ";
			}
			else if(nameleft.equals("IQ") && nameright.equals("EQ"))
			{
				resultn = "IQ";
			}
			else if(nameleft.equals("GT") && nameright.equals("EQ"))
			{
				resultn = "LT";
			}
			else if(nameleft.equals("GT") && nameright.equals("LT"))
			{
				resultn = "GT";
			}
			else if(nameleft.equals("LT") && nameright.equals("EQ"))
			{
				resultn = "LT";
			}
			else if(nameleft.equals("LT") && nameright.equals("GT"))
			{
				resultn = "LT";
			}
		}
		else if(lc1.equals(rc1))
		{
			resultc1 = lc2;
			resultc2 = rc2;
			if(nameleft.equals("EQ") && nameright.equals("EQ"))
			{
				resultn = "EQ";
			}
			else if(nameleft.equals("EQ") && nameright.equals("GT"))
			{
				resultn = "GT";
			}
			else if(nameleft.equals("EQ") && nameright.equals("LT"))
			{
				resultn = "LT";
			}
			else if(nameleft.equals("EQ") && nameright.equals("IQ"))
			{
				resultn = "IQ";
			}
			else if(nameleft.equals("IQ") && nameright.equals("EQ"))
			{
				resultn = "IQ";
			}
			else if(nameleft.equals("GT") && nameright.equals("EQ"))
			{
				resultn = "LT";
			}
			else if(nameleft.equals("GT") && nameright.equals("LT"))
			{
				resultn = "LT";
			}
			else if(nameleft.equals("LT") && nameright.equals("EQ"))
			{
				resultn = "GT";
			}
			else if(nameleft.equals("LT") && nameright.equals("GT"))
			{
				resultn = "GT";
			}
		}
		else if(lc2.equals(rc2))
		{
			resultc1 = lc1;
			resultc2 = rc1;
			if(nameleft.equals("EQ") && nameright.equals("EQ"))
			{
				resultn = "EQ";
			}
			else if(nameleft.equals("EQ") && nameright.equals("GT"))
			{
				resultn = "LT";
			}
			else if(nameleft.equals("EQ") && nameright.equals("LT"))
			{
				resultn = "GT";
			}
			else if(nameleft.equals("EQ") && nameright.equals("IQ"))
			{
				resultn = "IQ";
			}
			else if(nameleft.equals("IQ") && nameright.equals("EQ"))
			{
				resultn = "IQ";
			}
			else if(nameleft.equals("GT") && nameright.equals("EQ"))
			{
				resultn = "GT";
			}
			else if(nameleft.equals("GT") && nameright.equals("LT"))
			{
				resultn = "GT";
			}
			else if(nameleft.equals("LT") && nameright.equals("EQ"))
			{
				resultn = "LT";
			}
			else if(nameleft.equals("LT") && nameright.equals("GT"))
			{
				resultn = "LT";
			}
		}

		if(resultc1 == null || resultc2 == null || resultn == null)
			return null;

		for(Predicate pre: allVarPre)
		{
			String c1 = pre.getRows()[0] + "." + pre.getCols()[0];
			String c2 = pre.getRows()[1] + "." + pre.getCols()[1];
			String name = pre.getName();
			if(c1.equals(resultc1) && c2.equals(resultc2) && name.equals(resultn))
			{
				return pre;
			}
			if(c1.equals(resultc2) && c2.equals(resultc1))
			{
				if(name.equals("EQ") && resultn.equals("EQ"))
					return pre;
				else if(name.equals("IQ") && resultn.equals("IQ"))
					return pre;
				else if(name.equals("GT") && resultn.equals("LT"))
					return pre;
				else if(name.equals("LT") && resultn.equals("GT"))
					return pre;
			}
		}
		return null;
	}




	public boolean linearImplication(Collection<DenialConstraint> dcs, DenialConstraint conse)
	{

		/*Set<Predicate> premise = new HashSet<Predicate>(conse.getPredicates());
		for(int k = 0; k < conse.getPredicates().size(); k++)
		{
			Predicate kpre = conse.getPredicates().get(k);
			premise.remove(kpre);
			Set<Predicate> closure = linearGetClosure(dcs,premise);
			if(closure.contains(this.getReversePredicate(kpre)))
				return true;
			premise.add(kpre);
		}
		return false;*/

		if(conse == null)
			return false;

		if(conse.isTrivial())
			return true;

	/*	Set<DenialConstraint> dcs_all = new HashSet<DenialConstraint>(dcs);
		for(DenialConstraint dc: dcs)
		{
			DenialConstraint temp = this.getSymmetricDC(dc);
			dcs_all.add(temp);
		}*/

		Set<Predicate> premise = new HashSet<Predicate>(conse.getPredicates());
		Set<Predicate> closure = linearGetClosure(dcs,premise);
		if(closure.contains(null))
			return true;
		else
			return false;
	}

	private Set<Predicate> linearGetClosure(Collection<DenialConstraint> dcs, Set<Predicate> premise)
	{
		//Init of the result
		Set<Predicate> result = new HashSet<Predicate>(premise);
		for(Predicate temp: premise)
		{
			result.addAll(this.getImpliedPredicates(temp));
		}
		//allImplied(result);


		//A list of candidate DCs, n-1 predicates are already in result
		List<DenialConstraint> canDCs = new ArrayList<DenialConstraint>();

		//From predicate, to a set of DCs, that contain the key predicate
		Map<Predicate,Set<DenialConstraint>> p2dcs = new HashMap<Predicate,Set<DenialConstraint>>();
		//From DC, to a set of predicates, that are not yet included in the closure
		Map<DenialConstraint,Set<Predicate>> dc2ps = new HashMap<DenialConstraint,Set<Predicate>>();


		//for(Predicate pre: allVarPre)
		//p2dcs.put(pre, new HashSet<DenialConstraint>());
		//Init two maps
		for(DenialConstraint dc: dcs)
		{
			for(Predicate pre: dc.getPredicates())
			{
				if(p2dcs.containsKey(pre))
				{
					Set<DenialConstraint> value = p2dcs.get(pre);
					value.add(dc);
					p2dcs.put(pre, value);
				}
				else
				{
					Set<DenialConstraint> value = new HashSet<DenialConstraint>();
					value.add(dc);
					p2dcs.put(pre, value);
				}


			}
			dc2ps.put(dc, new HashSet<Predicate>());
		}
		for(Predicate pre: p2dcs.keySet())
		{

			if(result.contains(pre))
				continue;


			for(DenialConstraint tempDC: p2dcs.get(pre))
			{
				Set<Predicate> value = dc2ps.get(tempDC);
				value.add(pre);
				dc2ps.put(tempDC, value);
			}
		}
		//Init the candidate list
		for(DenialConstraint dc: dcs)
		{
			if(dc2ps.get(dc).size() == 1)
				canDCs.add(dc);
			if(dc2ps.get(dc).size() == 0)
			{
				result.add(null);
				return result;
			}
		}

		//Do the main loop of adding to the list
		while(!canDCs.isEmpty())
		{
			DenialConstraint canDC = canDCs.remove(0);

			Set<Predicate> tailSet = dc2ps.get(canDC);

			//Why can be 0?
			if(tailSet.size() == 0)
				continue;

			Predicate tail = tailSet.iterator().next();

			Predicate reverseTail = this.getReversePredicate(tail);


			Set<Predicate> toBeProcessed = new HashSet<Predicate>();
			toBeProcessed.add(reverseTail);
			toBeProcessed.addAll(this.getImpliedPredicates(reverseTail));
			toBeProcessed.removeAll(result);


			for(Predicate reverseTailImp: toBeProcessed)
			{
				if(p2dcs.containsKey(reverseTailImp))
				{
					Set<DenialConstraint> covered = p2dcs.get(reverseTailImp);
					Set<DenialConstraint> toBeRemoved = new HashSet<DenialConstraint>();
					for(DenialConstraint tempDC: covered)
					{
						Set<Predicate> value = dc2ps.get(tempDC);
						value.remove(reverseTailImp);
						if(value.size() == 1)
						{
							canDCs.add(tempDC);
							toBeRemoved.add(tempDC);
						}
						else if(value.size() == 0)
						{
							result.add(null);
							return result;
						}

					}
					covered.removeAll(toBeRemoved);
				}




			}

			result.addAll(toBeProcessed);

			//allImplied(result);

		}



		return result;
	}

	/**
	 * Get the minimal DC for each DC
	 * @param dcs
	 */
	protected int initMinimalDC(List<DenialConstraint> dcs)
	{

		int result = 0;
		for(DenialConstraint dc: dcs)
		{

			boolean temp = dc.initMostSuc();
			if(temp)
			{
				System.out.println(dc.toString() + " is affected!");
				result++;
			}

		}
		return result;
	}

	/**
	 * Remove from the set, the set of trivial DCs
	 * @param dcs
	 * @return the number of trivial DCs
	 */
	protected int removeTriviality(List<DenialConstraint> dcs)
	{
		Set<DenialConstraint> toBeRemoved = new HashSet<DenialConstraint>();
		for(DenialConstraint dc: dcs)
		{
			if(dc.isTrivial())
				toBeRemoved.add(dc);
		}
		dcs.removeAll(toBeRemoved);
		return toBeRemoved.size();
	}


	/**
	 * Remove from the set, the set of DCs that is a superset of some other DC in the set
	 * @param dcs
	 * @return the number of DCs removed
	 */
	protected int removeSubset(List<DenialConstraint> dcs)
	{

		Set<DenialConstraint> toBeRemoved = new HashSet<DenialConstraint>();
		int result  = 0;
		boolean changed = true;
		while(changed)
		{
			changed = false;
			for(DenialConstraint dc1: dcs)
			{
				for(DenialConstraint dc2: dcs)
				{
					if(dc1 == dc2)
						continue;

					if(dc2.getPredicates().containsAll(dc1.getPredicates()))
					{
						toBeRemoved.add(dc2);
					}
				}

				if(toBeRemoved.size() > 0)
				{
					dcs.removeAll(toBeRemoved);
					result += toBeRemoved.size();
					toBeRemoved.clear();
					changed = true;
					break;
				}

			}
		}


		return result;
	}

	/**
	 *
	 * @param dcs
	 * @return
	 */
	protected int minimalCover(List<DenialConstraint> dcs)
	{
		int result = 0;


		int sizeBefore = -1;
		while(sizeBefore != dcs.size())
		{
			sizeBefore = dcs.size();
			for(int i = 0 ; i < dcs.size(); i++)
			{
				Set<DenialConstraint> ante = new HashSet<DenialConstraint>(dcs);
				ante.remove(dcs.get(i));
				//if(implicationTesting(ante, dcs.get(i)))
				if(linearImplication(ante, dcs.get(i)))
				{
					//System.out.println("The DC has been removed due to implication: " + dcs.get(i));
					dcs.remove(i);
					result++;
					break;
				}
			}
		}

		//The following approach is obviously wrong
		/*Set<DenialConstraint> toBeRemoved = new HashSet<DenialConstraint>();
		Set<DenialConstraint> ante = new HashSet<DenialConstraint>(dcs);
		for(DenialConstraint dc: dcs)
		{
			ante.remove(dc);
			if(linearImplication(ante,dc))
			{
				toBeRemoved.add(dc);
			}
			ante.add(dc);
		}
		dcs.removeAll(toBeRemoved);
		result = toBeRemoved.size();*/

		return result;
	}

	protected int minimalCoverAccordingtoInterestingess(List<DenialConstraint> dcs)
	{
		int result = 0;
		Set<DenialConstraint> toBeRemoved = new HashSet<DenialConstraint>();
		Set<DenialConstraint> tempDCs = new HashSet<DenialConstraint>(dcs);
		for(int i = dcs.size() - 1; i >=0; i--)
		{
			tempDCs.remove(dcs.get(i));
			if(this.linearImplication(tempDCs, dcs.get(i)))
			{
				toBeRemoved.add(dcs.get(i));
			}
		}

		dcs.removeAll(toBeRemoved);
		result = toBeRemoved.size();
		return result;
	}

	private Map<Predicate,Predicate> symMap = new HashMap<Predicate,Predicate>();
	private Predicate getSymmetricPredicate(Predicate pre)
	{
		if(symMap.containsKey(pre))
			return symMap.get(pre);


		Predicate result = null;

		if(pre.getName().equals("EQ") || pre.getName().equals("IQ"))
			return pre;

		for(Predicate temp: allVarPre)
		{
			if(pre.equalExceptOp(temp))
			{
				if(pre.getName().equals("GT") && temp.getName().equals("LT"))
					result =  temp;
				else if(pre.getName().equals("LT") && temp.getName().equals("GT"))
					result = temp;
				else if(pre.getName().equals("GTE") && temp.getName().equals("LTE"))
					result =  temp;
				else if(pre.getName().equals("LTE") && temp.getName().equals("GTE"))
					result = temp;
				else
					continue;
			}
		}
		if(result != null)
		{
			symMap.put(pre, result);
			symMap.put(result, pre);
		}
		return result;

	}

	/**
	 * Get symmetric predicate using substitution
	 * @param pre
	 * @return
	 */
	private Predicate getSymmetricPredicate2(Predicate pre)
	{
		if(symMap.containsKey(pre))
			return symMap.get(pre);


		Predicate result = null;

		int[] rows = pre.getRows();

		if(rows[0] != rows[1])
		{
			if(pre.getName().equals("EQ") || pre.getName().equals("IQ"))
				return pre;

			for(Predicate temp: allVarPre)
			{
				if(pre.equalExceptOp(temp))
				{
					if(pre.getName().equals("GT") && temp.getName().equals("LT"))
						result =  temp;
					else if(pre.getName().equals("LT") && temp.getName().equals("GT"))
						result = temp;
					else if(pre.getName().equals("GTE") && temp.getName().equals("LTE"))
						result =  temp;
					else if(pre.getName().equals("LTE") && temp.getName().equals("GTE"))
						result = temp;
					else
						continue;
				}
			}
		}
		else
		{
			for(Predicate temp: preSpace)
			{
				if(temp == pre)
					continue;

				if(temp.getRows()[0] != temp.getRows()[1])
					continue;

				if(temp.getName() != pre.getName())
					continue;

				if(temp.getRows()[0]  != pre.getRows()[0])
				{
					result = temp;
					break;
				}

			}
		}

		if(result != null)
		{
			symMap.put(pre, result);
			symMap.put(result, pre);
		}
		return result;

	}

	protected int removeSymmetryDCs(List<DenialConstraint> dcs)
	{
		Set<DenialConstraint> toBeRemoved = new HashSet<DenialConstraint>();
		for(int i = 0 ; i < dcs.size(); i++)
			for(int k = 0; k < dcs.size(); k++)
			{
				if(k > i)
				{
					DenialConstraint curDC = dcs.get(i);
					DenialConstraint afterDC = dcs.get(k);


					//Determine if curDC and afterDC are symmetric
					boolean tag = true;
					ArrayList<Predicate> pres1 = curDC.getPredicates();
					ArrayList<Predicate> pres2 = afterDC.getPredicates();

					if(pres1.size() != pres2.size())
						continue;

					for(int j = 0 ; j < pres1.size(); j++)
					{
						Predicate pre1 = pres1.get(j);

						Predicate symPre1 = this.getSymmetricPredicate2(pre1);
						if(!pres2.contains(symPre1))
						{
							tag = false;
							break;
						}
					}

					if(tag == true)
					{
						toBeRemoved.add(curDC);
						break;
					}

				}
			}

		dcs.removeAll(toBeRemoved);
		return toBeRemoved.size();
	}
	protected DenialConstraint getSymmetricDC(DenialConstraint dc)
	{
		ArrayList<Predicate> curPres = new ArrayList<Predicate>();
		for(Predicate pre: dc.getPredicates())
		{
			Predicate sym = this.getSymmetricPredicate2(pre);
			curPres.add(sym);
		}

		return new DenialConstraint(2,curPres,this);
	}



	protected long runningTime; // in seconds
	public List<DenialConstraint> discover()
	{

		for(Set<Predicate> consPresInput: consPresMap.keySet())
		{
			ArrayList<DenialConstraint> dcs = consPresMap.get(consPresInput);
			Map<Set<Predicate>,Long> tuplewiseinfo = cons2PairInfo.get(consPresInput);
			//Map<Set<Predicate>,Set<TP>> tuplewiseinfoTP = cons2PairInfoTP.get(consPresInput);

			discoverInternal(dcs,tuplewiseinfo);
			System.out.println((System.currentTimeMillis() - startingTime)/1000 + ":" +
					"--------Cons Done-----");

		}
		postprocess();

		runningTime = (System.currentTimeMillis() - startingTime )/ 1000;

		dc2File();
		writeStats();

		System.out.println((System.currentTimeMillis() - startingTime)/1000 + ":" +
				"*************Done Constraints Discovery******************");
		return totalDCs;
	}


	public void discoverEXCHKS()
	{
		dbPro.getEXCHKDCs(exchkDCs,consPres);



		//Get the interestingness score for each DCs
		for(DenialConstraint dc: exchkDCs)
		{
			double coverage = 0;

			for(Tuple tuple: originalTable.getTuples())
			{
				int satNumPres = 0;
				for(Predicate pre: dc.getPredicates())
					if(pre.check(tuple))
					{
						satNumPres++;
					}

				coverage += (double)(satNumPres+1) / dc.getPredicates().size();
			}
			coverage = coverage / originalTable.getNumRows();


			if(coverage > 1)
				coverage = 1;

			//just coverage
			dc.interestingness = coverage;


			//System.out.println(dc.toString());
		}
		//rank the DCs according to their interestingness score
		Collections.sort(exchkDCs, new Comparator<DenialConstraint>()
		{
			public int compare(DenialConstraint arg0,
					DenialConstraint arg1) {
				if(arg0.interestingness > arg1.interestingness)
					return -1;
				else if(arg0.interestingness < arg1.interestingness)
					return 1;
				else
					return 0;

			}

		});


		System.out.println((System.currentTimeMillis() - startingTime )/ 1000 +
				": Number of Extend check constraints Before Implication: " + exchkDCs.size());

		this.minimalCoverAccordingtoInterestingess(exchkDCs);
		System.out.println((System.currentTimeMillis() - startingTime )/ 1000 +
				": Number of Extend check constraints After first Implication: " + exchkDCs.size());
		minimalCover(exchkDCs);

		System.out.println((System.currentTimeMillis() - startingTime )/ 1000 +
				": Number of Extend check constraints After total Implication: " + exchkDCs.size());


		dc2File();
	}

	/**
	 * Child class must override this method
	 * @param consPresInput
	 */
	protected void discoverInternal(ArrayList<DenialConstraint> dcs, Map<Set<Predicate>,Long> tuplewiseinfo)
	{
	}

	/**
	 * Add all DCs from each constant predicate, to all DCs, and do a subset pruning
	 */
	private void postprocess()
	{
		for(Set<Predicate> consPresInput: consPresMap.keySet())
		{
			//The set of implied var predicates, by constant predicates.
			//Example: t1.A = a, t2.A = a, implied t1.A = t2.A
			Set<Predicate> impliedVarPres = new HashSet<Predicate>();
			Set<Integer> cols = new HashSet<Integer>();
			for(Predicate pre: consPresInput)
			{
				int col = pre.getCols()[0];
				cols.add(col);
			}
			for(Predicate pre: allVarPre)
			{
				if(pre.getName().equals("EQ") && pre.getCols()[0] == pre.getCols()[1] && cols.contains(pre.getCols()[0]) )
				{
					impliedVarPres.add(pre);
				}
			}

			//Add implied var predicates to DCs
			ArrayList<DenialConstraint> dcs = consPresMap.get(consPresInput);
			for(DenialConstraint dc: dcs)
			{
				dc.addPredicates(consPresInput);
				dc.addPredicates(impliedVarPres);
			}

			//Eliminate of trivial constant DCs
			this.removeTriviality(dcs);
			//remove implied var predicates from Dcs
			/*for(DenialConstraint dc: dcs)
			{
				dc.removePredicates(impliedVarPres);
			}*/

			//Add them to the total DCs

			//Modify the interestingness of DCs 
			for(DenialConstraint dc: dcs)
			{
				//double rate = (double)cons2NumTuples.get(consPresInput) / originalTable.getNumRows();
				double rate = 1;
				dc.interestingness = dc.interestingness * Math.sqrt(rate);
			}

			totalDCs.addAll(dcs);
		}
		System.out.println((System.currentTimeMillis() - startingTime )/ 1000 +
				"FASTDC: The total number of DCs before subset pruning is: " + totalDCs.size());
		//remove subset from totalDCs
		this.removeSubset(totalDCs);
		System.out.println((System.currentTimeMillis() - startingTime )/ 1000 +
				"FASTDC: The total number of DCs after subset pruning is: " + totalDCs.size());

		//Rank the totalDCs according to their interestingness score
		Collections.sort(totalDCs, new Comparator<DenialConstraint>()
		{
			public int compare(DenialConstraint arg0,
					DenialConstraint arg1) {
				if(arg0.interestingness > arg1.interestingness)
					return -1;
				else if(arg0.interestingness < arg1.interestingness)
					return 1;
				else
					return 0;

			}

		});

		//Remove Symmetric DCs
		int symReduction = this.removeSymmetryDCs(totalDCs);
		System.out.println((System.currentTimeMillis() - startingTime )/ 1000 +
				"FASTDC: The total number of DCs after symmetric reduction is: " + totalDCs.size());

	}

	/*int numTotalEvi = 0;
	public int getTotalEvi()
	{
		
			return numTotalEvi;
	}*/
	public Set<DenialConstraint> getAllDCs()
	{
		return new HashSet<DenialConstraint>(totalDCs);
	}
	private void dc2File()
	{

		String dcPath = null;
		dcPath = new String((new StringBuilder(originalTable.getCurrentFolder())).append("ALLDCS"));
		/*if(originalTable.inputDBPath.endsWith("inputDBGolden"))
		{
			dcPath = new String((new StringBuilder(originalTable.getCurrentFolder())).append("dcs_g"));
		}
		else if(originalTable.inputDBPath.endsWith("inputDB"))
		{
			dcPath = new String((new StringBuilder(originalTable.getCurrentFolder())).append("dcs_s"));
		}
		else if(originalTable.inputDBPath.endsWith("inputDBAll"))
		{
			dcPath = new String((new StringBuilder(originalTable.getCurrentFolder())).append("dcs_s"));
		}
		else if(originalTable.inputDBPath.endsWith("inputDBNoise"))
		{
			dcPath = new String((new StringBuilder(originalTable.getCurrentFolder())).append("dcs_s_noise"));
		}*/

		try {
			PrintWriter out = new PrintWriter(new FileWriter(dcPath));
			out.println("The set of One tuple Constant DCs: ");
			for(DenialConstraint dc: exchkDCs)
			{
				out.println(dc.interestingness + ":" + dc.toString());
			}
			out.println("********************************************");
			out.println("The set of Denial Constraints:");
			for(DenialConstraint dc: totalDCs)
			{
				out.println(dc.interestingness + ":" + dc.toString());
			}
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void writeStats()
	{
	}

	protected class PR
	{
		public double precision;
		public double recall;

		public PR(double p, double r)
		{
			this.precision = p;
			this.recall = r;
		}
	}
	protected PR getPRTopk(int k,String dcFile, ArrayList<DenialConstraint> dcs)
	{
		//Compare golden and sample output
		//Read from golden standard, and read from the current dcs
		String golden = new String((new StringBuilder(originalTable.getCurrentFolder())).append("dcs_Human"));
		//String sample  = new String((new StringBuilder(originalTable.getCurrentFolder())).append("dcs_AfterImp"));
		String sample = dcFile;


		Set<String> allGoodDCs = new HashSet<String>();
		int count = -1;
		try {
			BufferedReader brg = new BufferedReader(new FileReader(golden));
			String line = null;
			while((line = brg.readLine())!=null)
			{
				if(count == -1)//first line is the head
				{
					count++;
					continue;
				}
				allGoodDCs.add(line.split(":")[1]);
			}
			brg.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//Construct dcs from human input
		Set<DenialConstraint> allGoodDCs_dcs = new HashSet<DenialConstraint>();
		for(String dc: allGoodDCs)
		{
			String[] pres_s = dc.substring(4,dc.length()-1).split("&");
			ArrayList<Predicate> dcs_Pres = new ArrayList<Predicate>();
			for(String pre_s: pres_s)
			{
				for(Predicate pre: allVarPre)
				{
					if(pre.toString().equals(pre_s))
					{
						dcs_Pres.add(pre);
						break;
					}

				}
			}
			allGoodDCs_dcs.add(new DenialConstraint(2,dcs_Pres,this));

		}


		Set<DenialConstraint> allGoodDCs_dcs_sym = new HashSet<DenialConstraint>(allGoodDCs_dcs);
		for(DenialConstraint dc: allGoodDCs_dcs)
		{
			DenialConstraint temp = this.getSymmetricDC(dc);
			allGoodDCs_dcs_sym.add(temp);
		}

		int numGoodInK = 0;
		count = -1;
		try {
			BufferedReader brs = new BufferedReader(new FileReader(sample));
			String line = null;
			while((line = brs.readLine())!=null)
			{
				if(count == -1)//first line is the head
				{
					count++;
					continue;
				}
				if(count < k)
				{
					String cur = line.split(":")[1];
					DenialConstraint thisDC = null;
					for(DenialConstraint dc: dcs)
					{
						if(dc.toString().equals(cur))
						{
							thisDC = dc;
							break;
						}
					}
					//assert(totalDCs.get(count).toString().equals(cur));
					//if(allGoodDCs.contains(cur))
					if(this.linearImplication(allGoodDCs_dcs_sym, thisDC))
					{

						for(DenialConstraint dc: allGoodDCs_dcs_sym)
						{
							if(thisDC.getPredicates().containsAll(dc.getPredicates())
									&& dc.getPredicates().size() == thisDC.getPredicates().size())
							{

								//System.out.println("K " + k + ":" + thisDC.toString());
								numGoodInK ++;
								break;
							}
						}


					}
					count++;
				}

			}
			brs.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		double p = (double)numGoodInK / k;
		double r = (double)numGoodInK / allGoodDCs.size();
		if(p > 1)
			p = 1;
		if(r > 1)
			r = 1;
		return new PR(p,r  );
	}

	protected PR getPRTopk(int k,String dcFile)
	{
		//Compare golden and sample output
		//Read from golden standard, and read from the current dcs
		String golden = new String((new StringBuilder(originalTable.getCurrentFolder())).append("dcs_Human"));
		//String sample  = new String((new StringBuilder(originalTable.getCurrentFolder())).append("dcs_AfterImp"));
		String sample = dcFile;


		Set<String> allGoodDCs = new HashSet<String>();
		int count = -1;
		try {
			BufferedReader brg = new BufferedReader(new FileReader(golden));
			String line = null;
			while((line = brg.readLine())!=null)
			{
				if(count == -1)//first line is the head
				{
					count++;
					continue;
				}
				allGoodDCs.add(line.split(":")[1]);
			}
			brg.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//Construct dcs from human input
		Set<DenialConstraint> allGoodDCs_dcs = new HashSet<DenialConstraint>();
		for(String dc: allGoodDCs)
		{
			String[] pres_s = dc.substring(4,dc.length()-1).split("&");
			ArrayList<Predicate> dcs_Pres = new ArrayList<Predicate>();
			for(String pre_s: pres_s)
			{
				for(Predicate pre: allVarPre)
				{
					if(pre.toString().equals(pre_s))
					{
						dcs_Pres.add(pre);
						break;
					}

				}
			}
			allGoodDCs_dcs.add(new DenialConstraint(2,dcs_Pres,this));

		}

		if(k == Config.grak)
		{
			long hahaha = (long) (originalTable.getNumRows() * originalTable.getNumRows() * Config.noiseTolerance);
			System.out.println("The max number of vios is " + hahaha);
			//System.out.println("The max number of vios is " + originalTable.getNumRows() * 0.002);
			for(DenialConstraint dc: allGoodDCs_dcs)
			{
				int numVios = 0;
				for(Set<Predicate> info: cons2PairInfo.get(new HashSet<Predicate>()).keySet())
				{
					Set<Predicate> temp = new HashSet<Predicate>(dc.getPredicates());

					temp.retainAll(info);

					//this is a NE
					if(temp.size() == dc.getPredicates().size())
					{
						//count = count - originalTable.getNumRows();
						numVios += cons2PairInfo.get(new HashSet<Predicate>()).get(info);
						continue;
					}

				}
				System.out.println("The number of vios for " + dc.toString() + " is " + numVios);

			}
		}


		Set<DenialConstraint> allGoodDCs_dcs_sym = new HashSet<DenialConstraint>(allGoodDCs_dcs);
		for(DenialConstraint dc: allGoodDCs_dcs)
		{
			DenialConstraint temp = this.getSymmetricDC(dc);
			allGoodDCs_dcs_sym.add(temp);
		}

		int numGoodInK = 0;
		count = -1;
		DenialConstraint thisDC = null;
		try {
			BufferedReader brs = new BufferedReader(new FileReader(sample));
			String line = null;
			while((line = brs.readLine())!=null)
			{
				if(count == -1)//first line is the head
				{
					count++;
					continue;
				}
				if(count < k)
				{
					String cur = line.split(":")[1];
					for(DenialConstraint dc: totalDCs)
					{
						if(dc.toString().equals(cur))
						{
							thisDC = dc;
							break;
						}
					}
					//assert(totalDCs.get(count).toString().equals(cur));
					//if(allGoodDCs.contains(cur))
					if(this.linearImplication(allGoodDCs_dcs_sym, thisDC))
					{
						for(DenialConstraint dc: allGoodDCs_dcs_sym)
						{
							if(thisDC.getPredicates().containsAll(dc.getPredicates())
									&& dc.getPredicates().size() == thisDC.getPredicates().size())
							{

								System.out.println("K " + k + ":" + thisDC.toString());
								numGoodInK ++;
								break;
							}
						}

						//System.out.println("Top" + k + ": " + cur + " : is implied by golden standard");
						//System.out.println("KKKK " + k + ":" + thisDC.toString());
						//numGoodInK ++;
					}
					count++;
				}

			}
			brs.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//System.out.println("This DC Exception : "  + thisDC.toString());
			e.printStackTrace();
		}

		double p = (double)numGoodInK / k;
		double r = (double)numGoodInK / allGoodDCs.size();
		if(p > 1)
			p = 1;
		if(r > 1)
			r = 1;
		return new PR(p,r  );
	}


}
