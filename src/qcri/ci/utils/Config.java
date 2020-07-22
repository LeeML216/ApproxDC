package qcri.ci.utils;


/**
 * All the configuration parameters
 * @author xchu
 *
 */
public class Config {

	
	//The noise tolerance level for approximate DCs
	
	
	public static int numTopks = 4;
	public static int grak = 20;
	
	
	public static String getTopkHead()
	{
		StringBuilder sb = new StringBuilder();
		for(int i = 0 ; i < numTopks; i++)
		{
			int k = grak * (i+1);
			
			if(i != numTopks - 1)
				sb.append("G-PrecisionForTop-" + k + "," + "G-RecallForTop-" + k + ",");
			else
				sb.append("G-PrecisionForTop-" + k + "," + "G-RecallForTop-" + k);
		}
		
		return new String(sb);
	}
	
	
	public static int howInit = 1;
	public static boolean testScoringFunctionOnly = false;
	
	
	public static boolean enableCrossColumn = false;
	
	
	//the choice of scoring function
	public static int sc = 0;
	
	
	//differnt types of predicate space
	public static int ps = 1;
	
	
	
	
	
	
	
	public static double noiseLevel = 0;
	public static double noiseTolerance = 0;

	
	
	
	
	public static int qua = 1;
	
	
	public static int dfsLevel = 4;
	
	
	public static double kfre = 0.02; //was 0.02
	public static int numSplits = 2;
	public static int maxNumConPres = 3;
	public static boolean enableMixedDcs = true;
	
	public static boolean topkDCPruning = false;
	public static double topkDCPruningThre = 0;
	public static double coverageA = 0.5;
	
	public static double joinableThre = 0.5;
	
	
}
