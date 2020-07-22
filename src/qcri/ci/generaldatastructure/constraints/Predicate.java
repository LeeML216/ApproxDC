package qcri.ci.generaldatastructure.constraints;

import java.util.HashSet;
import java.util.Set;

import qcri.ci.generaldatastructure.db.*;
import qcri.ci.utils.OperatorMapping;

/**
 * This class is for denial constraint
 * @author xchu
 *
 */
public class Predicate
{
	//Example: EQ(t2.Mid,t1.Eid)
	
	Table table;
	String name;	//the name of this predicate, EQ or GT or LT
	
	
	
	int numArgs;	//number of arguments of this predicate
	int[] rowNum;	//The row number for the arguments.  {2,1}
	int[] colNum;	//The column position for the argument. {2,1}

	public int getIndex() {
		return index_pre;
	}

	public void setIndex(int index) {
		this.index_pre = index;
	}

	int index_pre;
	
	boolean secondCons;	//if the second argument is constant or not
	String cons;	//constant of the second argument if so
	
	//This constructor is for 2 arguments, both of them are cells
	public Predicate(Table table,String name, int row1, int row2, int col1, int col2)
	{
		this.table = table;
		this.name = name;
		
		numArgs = 2;
		rowNum = new int[2];
		colNum = new int[2];
		rowNum[0] = row1;
		rowNum[1] = row2;
		colNum[0] = col1;
		colNum[1] = col2;
		cons = null;
		secondCons = false;
		index_pre = 0;
		//initSatisfied();
	}
	public Predicate(Table table, String name, int row1, int col1, String cons)
	{
		this.table = table;
		this.name = name;
		
		numArgs = 1;
		rowNum = new int[2];
		colNum = new int[2];
		rowNum[0] = row1;
		colNum[0] = col1;
		rowNum[1] = -1;
		colNum[1] = -1;
		this.cons = cons;
		secondCons = true;
		index_pre = 0;
		//initSatisfied();
	}
	
	public Predicate(Predicate p)
	{
		this.table = p.table;
		this.name = p.name;
		numArgs = p.numArgs;
		rowNum = new int[2];
		colNum = new int[2];
		rowNum[0] = p.rowNum[0];
		rowNum[1] = p.rowNum[1];
		colNum[0] = p.colNum[0];
		colNum[1] = p.colNum[1];
		cons = p.cons;
		secondCons = p.secondCons;
		index_pre = 0;
	}
	
	/**
	 * if the second argument of the predicate is a constant, or a cell
	 * @return
	 */
	public boolean isSecondCons()
	{
		return secondCons;
	}
	/**
	 * Get the constant second argument
	 * @return
	 */
	public String getCons()
	{
		return cons;
	}
	
	public int[] getCols()
	{
		return colNum;
	}
	public int[] getRows()
	{
		return rowNum;
	}
	/**
	 * Check if this set of cells is satisfiable by this predicate, the set of cells have been rearranged as the constraints see it
	 * @param cells
	 * @return
	 */
	private boolean check(Cell[][] cells)
	{
		
		
		//Compare two cells, using hashing
		/*if(secondCons == false)
		{
			int col1 = colNum[0];
			int col2 = colNum[1];
			if(col1 == col2)
			{
				
				boolean tag = table.dbPro.equalOrNot(col1-1, cells[rowNum[0]-1][col1-1].getValue(), cells[rowNum[1]-1][col2-1].getValue());
				if( name.equals("EQ") && tag == true)
				{
					return true;
				}
				else if(name.equals("IQ") && tag == false)
					return true;
				else if(name.equals("EQ") && tag == false)
					return false;
				else if(name.equals("IQ") && tag == true)
					return false;
			}
		}
		*/
		
		
		
		
		
		
		
		
		
		int numRows = cells.length;
		
		Cell[] args = new Cell[numArgs];
		
		if(secondCons == false)
		{
			for(int i = 0 ; i < numArgs; i++)
			{
				int row = rowNum[i];	//row 	 position in the current predicate
				int col = colNum[i];	//column position for the i th argument
				args[i] = cells[row-1][col-1];
				if(args[i].getValue().equals("FV"))
					return false;
			}
			if(name.equals("EQ"))
			{
				return args[0].isSameValue(args[1]);
			}
			else if(name.equals("GT"))
			{
				return args[0].greaterThan(args[1]);
			}
			else if(name.equals("IQ"))
			{
				return !args[0].isSameValue(args[1]);
			}
			else if(name.equals("LT"))
			{
				return (!args[0].greaterThan(args[1]) ) && (!args[0].isSameValue(args[1])); 
			}
			else if(name.equals("GTE"))
			{
				return args[0].isSameValue(args[1]) || args[0].greaterThan(args[1]);
			}
			else if(name.equals("LTE"))
			{
				return !args[0].greaterThan(args[1]);
			}
			else if(name.equals("SIM"))
			{
				return args[0].isSimilarValue(args[1]);
			}
			else
			{
				System.out.println("Unsupported predicate");
			
				System.exit(-1);
				return false;
			}
		}
		else	//Second argument is a constant
		{
			int row = rowNum[0];	//row 	 position in the current predicate
			int col = colNum[0];	//column position for the i th argument
			args[0] = cells[row-1][col-1];
			if(args[0].getValue().equals("FV"))
				return false;
			if(name.equals("EQ"))
			{
				return args[0].isSameValue(cons);
			}
			else if(name.equals("GT"))
			{
				return args[0].greaterThan(cons);
			}
			else if(name.equals("IQ"))
			{
				return !args[0].isSameValue(cons);
			}
			else if(name.equals("LT"))
			{
				return (!args[0].greaterThan(cons) ) && (!args[0].isSameValue(cons)); 
			}
			else if(name.equals("GTE"))
			{
				return args[0].isSameValue(cons) || args[0].greaterThan(cons);
			}
			else if(name.equals("LTE"))
			{
				return !args[0].greaterThan(cons);
			}
			else if(name.equals("SIM"))
			{
				return args[0].isSimilarValue(cons);
			}
			else
			{
				System.out.println("Unsupported predicate");
			
				System.exit(-1);
				return false;
			}
		}
		
		
	}

	
	public boolean check(Tuple t1)
	{
		assert(secondCons == true);
		Cell[][] cells = new Cell[2][t1.getNumCols()];
		for(int i = 0 ; i < t1.getNumCols(); i++)
		{
			if(rowNum[0] == 1)
				cells[0][i] = t1.getCell(i);
			else
				cells[1][i] = t1.getCell(i);
		}
		return check(cells);
	}

	public boolean checkSimple(Tuple t1, Tuple t2)
	{
		int index = colNum[0] - 1;
		if(name.equals("EQ"))
		{
			return t1.getCell(index).getValue().equalsIgnoreCase(t2.getCell(index).getValue());
		}
		if(name.equals("IQ"))
		{
			return !t1.getCell(index).getValue().equalsIgnoreCase(t2.getCell(index).getValue());
		}
		if(name.equals("GT"))
		{
			return Double.valueOf(t1.getCell(index).getValue()) > Double.valueOf(t2.getCell(index).getValue());
		}
		if(name.equals("LT"))
		{
			return Double.valueOf(t1.getCell(index).getValue()) < Double.valueOf(t2.getCell(index).getValue());
		}
		if(name.equals("GTE"))
		{
			return Double.valueOf(t1.getCell(index).getValue()) >= Double.valueOf(t2.getCell(index).getValue());
		}
		if(name.equals("LTE"))
		{
			return Double.valueOf(t1.getCell(index).getValue()) <= Double.valueOf(t2.getCell(index).getValue());
		}

		return false;
	}
	
	public boolean check(Tuple t1, Tuple t2)
	{
		Cell[][] cells = new Cell[2][t1.getNumCols()];
		for(int i = 0 ; i < t1.getNumCols(); i++)
		{
			cells[0][i] = t1.getCell(i);
			cells[1][i] = t2.getCell(i);
		}
		return check(cells);
		
	}
	
	/*public boolean check(int tuplePairIndex)
	{
		return satisfied.contains(tuplePairIndex);
	}*/
	public String getName()
	{
		return name;
	}
	
	/**
	 * Is this Cell, applaible to this predicate, the row,col passed in, is the row and col number as the denial constraints see it, NOT as the database see it
	 * @param row
	 * @param col
	 * @return
	 */
	public boolean isAppliable(int row, int col)
	{
		for(int i = 0 ; i < numArgs; i++)
		{
			if(rowNum[i] == row && colNum[i] == col)
				return true;
		}
		return false;
	}
	
	/**
	 * Get the row, and col number from the other cell from the point of view of the constraints
	 * @param row
	 * @param col
	 * @return
	 */
	public int theOtherCellRow(int row, int col)
	{
		if(rowNum[0] == row && colNum[0] == col)
			return rowNum[1];
		else 
			return rowNum[0];
	}
	/**
	 * Get the row, col number from the other cell from the point of view of the constraint
	 * @param row
	 * @param col
	 * @return
	 */
	public int theOtherCellCol(int row, int col)
	{
		if(rowNum[0] == row && colNum[0] == col)
			return colNum[1];
		else 
			return colNum[0];
	}
	
	/**
	 * Get the reverse name if the second argument is a constant
	 * @return
	 */
	public String getReverseName()
	{
		assert(secondCons == true);
		if(name.equals("EQ"))
			return "IQ";
		else if(name.equals("GT"))
			return "LTE";
		else if(name.equals("LT"))
			return "GTE";
		else if(name.equals("IQ"))
			return "EQ";
		else if(name.equals("GTE"))
			return "LT";
		else if(name.equals("LTE"))
			return "GT";
		else if(name.equals("SIM"))
			return "IQ";
		else 
			return null;
	}
	/**
	 * Get the reverse name if the second argument is not a constant
	 * @param row
	 * @param col
	 * @return
	 */
	public String getReverseName(int row, int col)
	{
		assert(secondCons == false);
		if(rowNum[0] == row && colNum[0] == col)
		{
			if(name.equals("EQ"))
				return "IQ";
			else if(name.equals("GT"))
				return "LTE";
			else if(name.equals("LT"))
				return "GTE";
			else if(name.equals("IQ"))
				return "EQ";
			else if(name.equals("GTE"))
				return "LT";
			else if(name.equals("LTE"))
				return "GT";
			else if(name.equals("SIM"))
				return "IQ";
			else 
				return null;
		}
		else if(rowNum[1] == row && colNum[1] == col)
		{
			//return name;
			if(name.equals("EQ"))
				return "IQ";
			else if(name.equals("GT"))
				return "GTE";
			else if(name.equals("LT"))
				return "LTE";
			else if(name.equals("IQ"))
				return "EQ";
			else if(name.equals("GTE"))
				return "GT";
			else if(name.equals("LTE"))
				return "LT";
			else if(name.equals("SIM"))
				return "IQ";
			else 
				return null;
		}
		else
			return null;
	}
	
	
	/**
	 * This function tests if this predicate contradicts the passed in predicate
	 * @param p
	 * @return
	 */
	public boolean contradict(Predicate p)
	{
		if(secondCons != p.secondCons)
			return false;
		
		if(secondCons == true)
		{
			return contradictC(p);
		}
		else
		{
			return contradictV(p);
		}
		
	}
	private boolean contradictV(Predicate p)
	{
		if(rowNum[0]==p.rowNum[0] && rowNum[1] == p.rowNum[1]
				&& colNum[0]==p.colNum[0] && colNum[1] == p.colNum[1])
			{
				if(name.equals("EQ") && p.name.equals("IQ"))
					return true;
				if(name.equals("IQ") && p.name.equals("EQ"))
					return true;
				if(name.equals("GT") && p.name.equals("LTE"))
					return true;
				if(name.equals("LT") && p.name.equals("GTE"))
					return true;
				if(name.equals("GTE") && p.name.equals("LT"))
					return true;
				if(name.equals("LTE") && p.name.equals("GT"))
					return true;
				if(name.equals("SIM") && p.name.equals("IQ"))
					return true;
				//More:
				if(name.equals("EQ") && p.name.equals("GT"))
					return true;
				if(name.equals("EQ") && p.name.equals("LT"))
					return true;
				if(name.equals("GT") && p.name.equals("EQ"))
					return true;
				if(name.equals("GT") && p.name.equals("LT"))
					return true;
				if(name.equals("LT") && p.name.equals("EQ"))
					return true;
				if(name.equals("LT") && p.name.equals("GT"))
					return true;
			}
			
			if(rowNum[0]==p.rowNum[1] && rowNum[1] == p.rowNum[0]
					&& colNum[0]==p.colNum[1] && colNum[1] == p.colNum[0])
				{
					System.out.println("We should never hit here in checking predicate contradictory!");
					if(name.equals("EQ") && p.name.equals("IQ"))
						return true;
					if(name.equals("IQ") && p.name.equals("EQ"))
						return true;
					if(name.equals("GT") && p.name.equals("GTE"))
						return true;
					if(name.equals("LT") && p.name.equals("LTE"))
						return true;
					if(name.equals("GTE") && p.name.equals("GT"))
						return true;
					if(name.equals("LTE") && p.name.equals("LT"))
						return true;
					if(name.equals("SIM") && p.name.equals("IQ"))
						return true;
					
					//more
					
					if(name.equals("EQ") && p.name.equals("GT"))
						return true;
					if(name.equals("EQ") && p.name.equals("LT"))
						return true;
					if(name.equals("GT") && p.name.equals("EQ"))
						return true;
					if(name.equals("GT") && p.name.equals("GT"))
						return true;
					if(name.equals("LT") && p.name.equals("EQ"))
						return true;
					if(name.equals("LT") && p.name.equals("LT"))
						return true;
				}

			return false;
	}
	private boolean contradictC(Predicate p)
	{
		//If not the same row, and not the same column
		if(rowNum[0] != p.rowNum[0] || colNum[0] != p.colNum[0])
			return false;
		String constant1 = cons;
		String constant2 = p.cons;
		//Enumerate 36 scenarios, where two constant predicates contradicting each other
		if(name.equals("EQ") && p.name.equals("EQ"))
		{
			if(!constant1.equals(constant2))
				return true;
		}
		else if(name.equals("EQ") && p.name.equals("IQ"))
		{
			if(constant1.equals(constant2))
				return true;
		}
		else if(name.equals("EQ") && p.name.equals("GT"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v1 <= v2)
				return true;
		}
		else if(name.equals("EQ") && p.name.equals("LTE"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v1 > v2)
				return true;
		}
		else if(name.equals("EQ") && p.name.equals("LT"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v1 >= v2)
				return true;
		}
		else if(name.equals("EQ") && p.name.equals("GTE"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v1 < v2)
				return true;
		}
		else if(name.equals("IQ") && p.name.equals("EQ"))
		{
			if(constant1.equals(constant2))
				return true;
		}
		else if(name.equals("IQ") && p.name.equals("IQ"))
		{
			
		}
		else if(name.equals("IQ") && p.name.equals("GT"))
		{
			
		}
		else if(name.equals("IQ") && p.name.equals("LTE"))
		{
			
		}
		else if(name.equals("IQ") && p.name.equals("LT"))
		{
			
		}
		else if(name.equals("IQ") && p.name.equals("GTE"))
		{
			
		}
		else if(name.equals("GT") && p.name.equals("EQ"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v2 <= v1)
				return true;
		}
		else if(name.equals("GT") && p.name.equals("IQ"))
		{
			
		}
		else if(name.equals("GT") && p.name.equals("GT"))
		{
			
		}
		else if(name.equals("GT") && p.name.equals("LTE"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v2 <= v1)
				return true;
		}
		else if(name.equals("GT") && p.name.equals("LT"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v2 <= v1)
				return true;
		}
		else if(name.equals("GT") && p.name.equals("GTE"))
		{
			
		}
		else if(name.equals("LTE") && p.name.equals("EQ"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v2 > v1)
				return true;
		}
		else if(name.equals("LTE") && p.name.equals("IQ"))
		{
			
		}
		else if(name.equals("LTE") && p.name.equals("GT"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v2 >= v1)
				return true;
		}
		else if(name.equals("LTE") && p.name.equals("LTE"))
		{
			
		}
		else if(name.equals("LTE") && p.name.equals("LT"))
		{
			
		}
		else if(name.equals("LTE") && p.name.equals("GTE"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v2 > v1)
				return true;
		}
		else if(name.equals("LT") && p.name.equals("EQ"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v2 >= v1)
				return true;
		}
		else if(name.equals("LT") && p.name.equals("IQ"))
		{
			
		}
		else if(name.equals("LT") && p.name.equals("GT"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v2 >= v1)
				return true;
		}
		else if(name.equals("LT") && p.name.equals("LTE"))
		{
			
		}
		else if(name.equals("LT") && p.name.equals("LT"))
		{
			
		}
		else if(name.equals("LT") && p.name.equals("GTE"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v2 >= v1)
				return true;
		}
		else if(name.equals("GTE") && p.name.equals("EQ"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v2 < v1)
				return true;
		}
		else if(name.equals("GTE") && p.name.equals("IQ"))
		{
			
		}
		else if(name.equals("GTE") && p.name.equals("GT"))
		{
			
		}
		else if(name.equals("GTE") && p.name.equals("LTE"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v2 < v1)
				return true;
		}
		else if(name.equals("GTE") && p.name.equals("LT"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v2 <= v1)
				return true;
		}
		else if(name.equals("GTE") && p.name.equals("GTE"))
		{
			
		}
		
			
		
		return false;
	}
	
	/**
	 * Test whether the current predicate implies the passed-in predicate
	 * @param p
	 * @return
	 */
	public boolean implied(Predicate p)
	{
		if(table != p.table)
			return false;
		if(secondCons != p.secondCons)
			return false;
		
		
		if(secondCons == true)
			return impliedC(p);
		else
			return impliedV(p);
	
	}

	private boolean impliedV(Predicate p)
	{
		if(rowNum[0] != p.rowNum[0])
			return false;
		if(rowNum[1] != p.rowNum[1])
			return false;
		if(colNum[0] != p.colNum[0])
			return false;
		if(colNum[1] != p.colNum[1])
			return false;
		
		
		
		if(name.equals("EQ") && p.name.equals("GTE"))
			return true;
		if(name.equals("EQ") && p.name.equals("LTE"))
			return true;
		
		
		if(name.equals("GT") && p.name.equals("GTE"))
			return true;
		if(name.equals("GT") && p.name.equals("IQ"))
			return true;
		if(name.equals("LT") && p.name.equals("LTE"))
			return true;
		if(name.equals("LT") && p.name.equals("IQ"))
			return true;
		
		
		
		return false;
	}
	private boolean impliedC(Predicate p)
	{
		//If not the same row, and not the same column
		if(rowNum[0] != p.rowNum[0] || colNum[0] != p.colNum[0])
			return false;
		String constant1 = cons;
		String constant2 = p.cons;
		if(name.equals("EQ") && p.name.equals("EQ"))
		{
			if(constant1.equals(constant2))
				return true;
		}
		else if(name.equals("EQ") && p.name.equals("IQ"))
		{
			if(!constant1.equals(constant2))
				return true;
		}
		else if(name.equals("EQ") && p.name.equals("GT"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v1 > v2)
				return true;
		}
		else if(name.equals("EQ") && p.name.equals("LTE"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v1 <= v2)
				return true;
		}
		else if(name.equals("EQ") && p.name.equals("LT"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v1 < v2)
				return true;
		}
		else if(name.equals("EQ") && p.name.equals("GTE"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v1 >= v2)
				return true;
		}
		else if(name.equals("IQ") && p.name.equals("EQ"))
		{
			
		}
		else if(name.equals("IQ") && p.name.equals("IQ"))
		{
			
		}
		else if(name.equals("IQ") && p.name.equals("GT"))
		{
			
		}
		else if(name.equals("IQ") && p.name.equals("LTE"))
		{
			
		}
		else if(name.equals("IQ") && p.name.equals("LT"))
		{
			
		}
		else if(name.equals("IQ") && p.name.equals("GTE"))
		{
			
		}
		else if(name.equals("GT") && p.name.equals("EQ"))
		{
			
		}
		else if(name.equals("GT") && p.name.equals("IQ"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v2 <= v1)
				return true;
		}
		else if(name.equals("GT") && p.name.equals("GT"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v1 >= v2)
				return true;
		}
		else if(name.equals("GT") && p.name.equals("LTE"))
		{
		}
		else if(name.equals("GT") && p.name.equals("LT"))
		{
		}
		else if(name.equals("GT") && p.name.equals("GTE"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v1 >= v2)
				return true;
		}
		else if(name.equals("LTE") && p.name.equals("EQ"))
		{
			
		}
		else if(name.equals("LTE") && p.name.equals("IQ"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v2 >  v1)
				return true;
		}
		else if(name.equals("LTE") && p.name.equals("GT"))
		{
			
		}
		else if(name.equals("LTE") && p.name.equals("LTE"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v1 <= v2)
				return true;
		}
		else if(name.equals("LTE") && p.name.equals("LT"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v1 < v2)
				return true;
		}
		else if(name.equals("LTE") && p.name.equals("GTE"))
		{
			
		}
		else if(name.equals("LT") && p.name.equals("EQ"))
		{
			
		}
		else if(name.equals("LT") && p.name.equals("IQ"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v2 >= v1)
				return true;
		}
		else if(name.equals("LT") && p.name.equals("GT"))
		{
			
		}
		else if(name.equals("LT") && p.name.equals("LTE"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v1 <= v2)
				return true;
		}
		else if(name.equals("LT") && p.name.equals("LT"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v1 <= v2)
				return true;
		}
		else if(name.equals("LT") && p.name.equals("GTE"))
		{
			
		}
		else if(name.equals("GTE") && p.name.equals("EQ"))
		{
			
		}
		else if(name.equals("GTE") && p.name.equals("IQ"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v2 < v1)
				return true;
		}
		else if(name.equals("GTE") && p.name.equals("GT"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v2 < v1)
				return true;
		}
		else if(name.equals("GTE") && p.name.equals("LTE"))
		{
			
		}
		else if(name.equals("GTE") && p.name.equals("LT"))
		{
			
		}
		else if(name.equals("GTE") && p.name.equals("GTE"))
		{
			double v1 = Double.valueOf(constant1);
			double v2 = Double.valueOf(constant2);
			if(v2 <= v1)
				return true;
		}
		return false;
		
		
	}
	public String toString()
	{
		if(secondCons == false)
		{
			StringBuilder sb = new StringBuilder();
			//sb.append(name + "(");
			sb.append("t" + rowNum[0] + "." + table.getColumnMapping().posiionToName(colNum[0]));
			//sb.append(",");
			sb.append(OperatorMapping.nameToOperator(name));
			sb.append("t" + rowNum[1] + "." + table.getColumnMapping().posiionToName(colNum[1]));
			//sb.append(")");
			return new String(sb);
		}
		else
		{
			StringBuilder sb = new StringBuilder();
			//sb.append(name + "(");
			sb.append("t" + rowNum[0] + "." + table.getColumnMapping().posiionToName(colNum[0]));
			//sb.append(",");
			sb.append(OperatorMapping.nameToOperator(name));
			sb.append(cons);
			//sb.append(")");
			return new String(sb);
		}
		
	}

	protected Predicate clone()
	{
		Predicate clone = null;

		try {
			clone = (Predicate) super.clone();
		}
		catch(CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}

		return clone;
	}


	
	/**
	 * Determine if the predicate is the same as the passed-in operator except the operator
	 * @param p
	 * @return
	 */
	public boolean equalExceptOp(Predicate p)
	{
		Predicate op = (Predicate)p;
		if(table != op.table)
			return false;
		if(secondCons != op.secondCons)
			return false;
		/*if(!name.equals(op.name))
			return false;*/
		if(rowNum[0] != op.rowNum[0])
			return false;
		if(rowNum[1] != op.rowNum[1])
			return false;
		if(colNum[0] != op.colNum[0])
			return false;
		if(colNum[1] != op.colNum[1])
			return false;
		if(secondCons && (!cons.equals(op.cons)))
			return false;
		return true;
	}
	
	
	@Override
	public boolean equals(Object o)
	{
		Predicate op = (Predicate)o;
		if(table != op.table)
			return false;
		if(secondCons != op.secondCons)
			return false;
		if(!name.equals(op.name))
			return false;
		if(rowNum[0] != op.rowNum[0])
			return false;
		if(rowNum[1] != op.rowNum[1])
			return false;
		if(colNum[0] != op.colNum[0])
			return false;
		if(colNum[1] != op.colNum[1])
			return false;
		if(secondCons && (!cons.equals(op.cons)))
			return false;
		return true;
	}
	
	
	public double coherence()
	{
		if(secondCons || rowNum[0] == rowNum[1])
			return 1;
		else if(colNum[0] == colNum[1])
			return 0.8;
		else
			return 0.5;
	}
}