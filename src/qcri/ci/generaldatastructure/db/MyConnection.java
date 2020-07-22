package qcri.ci.generaldatastructure.db;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.List;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

public class MyConnection {

	private Connection conn;
	private String username;
	private String password;
	
	
	public MyConnection()
	{
		try {
			 
			Class.forName("org.postgresql.Driver");
 
		} catch (ClassNotFoundException e) {
 
			System.out.println("Where is your PostgreSQL JDBC Driver? "
					+ "Include in your library path!");
			e.printStackTrace();
		
		}
 
		try {
 
			conn = DriverManager.getConnection(
					"jdbc:postgresql://127.0.0.1:5432/ci", "ci",
					"ci");
 
		} catch (SQLException e) {
 
			System.out.println("Connection Failed! Check output console");
			e.printStackTrace();
 
		}
 
		if (conn != null) {
			//System.out.println("You made it, take control your database now!");
		} else {
			System.out.println("Failed to make connection!");
		}
	}
	
	public void dropTable(String tableName)
	{
		String sql = "DROP TABLE " + tableName;
		try {
			Statement st = conn.createStatement();
			st.executeUpdate(sql);
		} catch (SQLException e) {
			//e.printStackTrace();
		}
		
		
	}
	public Connection getConnection()
	{
		return conn;
	}
	
	public void createTable(String tableName, ColumnMapping cm)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE " + tableName + "(");
		String[] colNames = cm.getColumnNames();
		for(int i = 0 ; i < colNames.length; i++)
		{
			sb.append(colNames[i]);
			int type = cm.NametoType(colNames[i]);
			sb.append(" ");
			if(type == 2)
				sb.append("TEXT");
			else if(type == 0)
				sb.append("INTEGER");
			//else if(type.equalsIgnoreCase("BIT"))
			//	sb.append("BIT");
			else 
				System.out.println("Unrecognized data type");
			if(i != colNames.length - 1)
				sb.append(",");
			else 
				sb.append(")");
		}
		try {
			Statement st = conn.createStatement();
			st.executeUpdate(new String(sb));
		} catch (SQLException e) {
			System.out.println("table " + tableName + " already exist in the database");
			e.printStackTrace();
		}
	}
	
	public void builInsertTable(String tableName, List<String[]> allcolValues)
	{
		try{
			Statement st = conn.createStatement();
			for(String[] colValues: allcolValues)
			{
				StringBuilder sb = new StringBuilder();
				sb.append("INSERT INTO " + tableName +  " VALUES(");
				for(int i = 0 ; i < colValues.length; i++)
				{
					sb.append(colValues[i]);
					if(i != colValues.length - 1)
						sb.append(",");
					else
						sb.append(")");
				}
				st.addBatch(new String(sb));
			}
			st.executeBatch();
			st.close();
		}catch (SQLException e) {
			e.printStackTrace();
			System.exit(-1);

		}
		
	}
	public void bulkInsertFromFile(String tableName, String file)
	{
		try {
			CopyManager cm = new CopyManager((BaseConnection)conn);
			FileReader fileReader = new FileReader(file);
			String sql = "COPY " + tableName +" FROM STDIN WITH DELIMITER ',' ";
            cm.copyIn(sql, fileReader);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void insertTable(String tableName, String[] colValues)
	{
		
/*		INSERT INTO expdatagenerator1_predicatedb(
	            p0, p1, p2, p3, p4, p5)
	    VALUES (b'1',b'1',b'1',b'1',b'1',b'0');
*/
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO " + tableName +  " VALUES(");
		for(int i = 0 ; i < colValues.length; i++)
		{
			sb.append(colValues[i]);
			if(i != colValues.length - 1)
				sb.append(",");
			else
				sb.append(")");
		}
		try{
			Statement st = conn.createStatement();
			st.executeUpdate(new String(sb));
		}catch (SQLException e) {
			System.out.println(sb);
			e.printStackTrace();
			System.exit(-1);

		}
	}
	
	public ResultSet selectTable(String sql)
	{
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(sql);
			return rs;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public void buildIndex(String tableName, String colName)
	{
		String sql = "CREATE INDEX " + tableName + "_" + colName 
				+ " ON " + tableName + "(" + colName + ")";
		
		try {
			Statement st = conn.createStatement();
			st.executeUpdate(sql);
		} catch (SQLException e) {
			
			e.printStackTrace();
		}
		
	}
	public void closeConnection()
	{
		try {
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
