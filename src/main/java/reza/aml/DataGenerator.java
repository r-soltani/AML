/* 
   Copyright 2019 Reza Soltani

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

/* 
 * Implementation of 'A new algorithm for money laundering detection based on structural similarity' research paper. 
 * Reza Soltani, Uyen Trang Nguyen, Yang Yang, Mohammad Faghani, Alaa Yagoub and Aijun An, "A new algorithm for money laundering detection based on structural similarity," 2016 IEEE 7th Annual Ubiquitous Computing, Electronics & Mobile Communication Conference (UEMCON), New York, NY, 2016, pp. 1-7.
 * doi: 10.1109/UEMCON.2016.7777919
 * keywords: {financial data processing;globalisation;money laundering detection;structural similarity;financial transactions;global market;money laundering transactions;financial data;ML activities;ML groups;Receivers;Topology;Clustering methods;Government;Clustering algorithms;Network topology;Money laundering;money laundering detection;graph theory;structural similarity},
 * URL: http://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=7777919&isnumber=7777798
 */

package reza.aml;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class DataGenerator {

	//TODO: test with large num of nodes and transactions
	
	
	static int nodeSize = 10000; // total number of nodes in the system
	
	
	// regular transactions
	static int transactionSize = 200;
	
	
	static int TransactionCount = 1; // This is there because clean transactions are repeated.    
	static int TransactionCountHigh = 3; //  This is there because clean transactions are repeated.    
	
	
	// transcation value
	static int transactionValueMin = 1;   // minimum amount
	static int transactionValueMax = 5000; // maximum amount
	
	// transaction timeframe
	static int transactionTimeMin = 1;
	static int transactionTimeMax = 24;

	
	// ML transactions
	static int MLPatternsSize = 200; // number of ML patterns
	
	
	
	// case 1
		// # of rows
		static int MLIntermediates = 1; // number of neighbours each ML account should have .. to be changed
		static int MLIntermediatesHigh = 5; // number of neighbours each ML account should have .. to be changed
		
		// # of columns (depth)
		static int MLIntermeediatesDepth = 1; //    min depth, has to be >= 1 to be detected by AML, current implementation only detect depth of 1
		static int MLIntermeediatesDepthHigh = 1; // max depth

		
		static int MLtransactionTimeMin = 1; 	// ML transaction time frame
		static int MLtransactionTimeMax = 2; 	// ML transaction time frame
		
	 	static int MLtransactionAmountInput = 10000; // Amount being sent from sender to intermediate
		static int MLtransactionAmountOutput = 9900; // Amount being sent from intermediate to receiver
		
		static int MLTransactionCount = 6; // Number of transactions from sender to intermediate and intermediate to receiver. This is there because ML activities are repeated.    
		static int MLTransactionCountHigh = 12; // Number of transactions from sender to intermediate and intermediate to receiver. This is there because ML activities are repeated.    
		
		
// ---------------------------	

// // case 3
//	// # of rows
	// this is a complex version. this case is not supported in current version of paper 
//	static int MLIntermediates = 1; // number of neighbours each ML account should have .. to be changed
//	static int MLIntermediatesHigh = 5; // number of neighbours each ML account should have .. to be changed
//	
//	// # of columns (depth)
//	static int MLIntermeediatesDepth = 1; //    min depth, has to be >= 1 to be detected by AML, current implementation only detect depth of 1
//	static int MLIntermeediatesDepthHigh = 3; // max depth
	
// ---------------------------	
//	// case 2
	// this case is not supported in current version of paper 
//	// # of rows
//	static int MLIntermediates = 1; // number of neighbours each ML account should have .. to be changed
//	static int MLIntermediatesHigh = 1; // number of neighbours each ML account should have .. to be changed
//	
//	// # of columns (depth)
//	static int MLIntermeediatesDepth = 1; //    min depth, has to be >= 1 to be detected by AML, current implementation only detect depth of 1
//	static int MLIntermeediatesDepthHigh = 5; // max depth

	
// older version
	static int MLSize = 15; // number of ML transactions
	static int MLTrans = 2;  // number of transactions per ML account 
    static int MLNeighbor = 2; // number of neighbours each ML account should have .. to be changed
    
	
// create nodes
// create random transactions 
// create ML pattern
   
    // current version
	public static int[] generateData() throws IOException{
		double time1_1 =  System.currentTimeMillis();
		Integer i = 0;
		//int Size = 20;
		
		
		// tracks total number of ML accounts generated
		int totalMLAccounts = 0;
		
		
	
		ArrayList<FinancialNode> nodes = new ArrayList<FinancialNode>();
		ArrayList<FinancialTransaction> transactions = new ArrayList<FinancialTransaction>();
		
		
		
		/*
		 *  Create Nodes
		 *  -------------------
		 */
		
		
		System.out.println( "Start of Data Generation... v1" );
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println(dateFormat.format(date));

		
		System.out.println("Nodes...");
		for (i = 1; i <= nodeSize; i++)
		{
			FinancialNode node = new FinancialNode();
			node.name = i;
			//System.out.println(node.toString());
			nodes.add(node);
		}
		double time1_2 =  System.currentTimeMillis();
		
		
		
		/*
		 *  Create Transactions
		 *  -----
		 */ 
		double time2_1 =  System.currentTimeMillis();
		System.out.println("Transactions...");
		
		for (i = 1; i <= transactionSize; i++)
		{
			// To make the transactions more realistic. there may be multiple transactions happening between two accounts
			int transactionCountInstance = randInt(TransactionCount, TransactionCountHigh);
			int sender = randInt(0, nodes.size()-1);
            int receiver = sender;   // to be changed in the next statement
           
            // dont create a transaction from an account to itself
         	while (sender == receiver)
         				   receiver = randInt(0, nodes.size()-1);
         	
			for (int j=1; j<=transactionCountInstance; j++)
			{
				FinancialTransaction transaction = new FinancialTransaction();
				transaction.name = i*10+j;
				transaction.sender = nodes.get(sender);
	            transaction.receiver = nodes.get(receiver);  
	      				
				// for now all transactions are within a specified range 
				transaction.amount = randInt(transactionValueMin, transactionValueMax);
				
				// for now all tranx are between hour 1 and hour 24, omitting minute and seconds  
				transaction.time =  randInt(transactionTimeMin, transactionTimeMax);
				
				//System.out.println(transaction.toString());
				transactions.add(transaction);	
			}
			
		}
		
		double time2_2 =  System.currentTimeMillis();	
		
		
		/*
		 *  Create ML Nodes and Transactions
		 *  -----
		 */ 
		
		// changes nodes entirely?
		double time3_1 =  System.currentTimeMillis();
		System.out.println("ML Transactions...");
		int j = 0;
		ArrayList<Integer> allIntermediates = new ArrayList<Integer>();
		ArrayList<Integer> allSenderandReceivers = new ArrayList<Integer>();
		
		
		for (i = 1; i <= MLPatternsSize; i++)
		{
		
			// assumption senders and receivers can be involved in multiple ml activities
			// hense the use of randInt function.
			// variables sender and receiver are index only not ids 
			
			// assumption: a new sender/receiver cannot be an intermediate for an existing pattern 
			
			int sender = randInt(0, nodes.size()-1);
			while (allIntermediates.contains(sender))  //ensure sender is not an intermediate already..
				sender = randInt(0, nodes.size()-1);
			
			int receiver = sender;
			while (sender == receiver || allIntermediates.contains(receiver)) //ensure receiver is not an intermediate or sender already..
				receiver = randInt(0, nodes.size()-1);

			ArrayList<Integer> currentIntermediates = new ArrayList<Integer>();
			
			allSenderandReceivers.add(sender);
			allSenderandReceivers.add(receiver);
			
			
			
			int MLintermediate = randInt(MLIntermediates, MLIntermediatesHigh);  // number of rows
			
		
			
			System.out.println("ML Pattern #" + i);
			System.out.println("Sender " + nodes.get(sender) + " Receiver " + nodes.get(receiver));
			System.out.println("Intermediates ");
				
			
			for (j=1;j<=MLintermediate; j++) 	// row
			{
				int intermediateDepth =  randInt(MLIntermeediatesDepth, MLIntermeediatesDepthHigh);  // number of rows
				
				int prevIntermediate = sender;
				
	
				
				for (int k=1; k <= intermediateDepth; k++) // depth
				{
					
						
					
						int intermediate = sender;
				
						// new intermediate is not current sender or receiver or existing intermediate
						// assumption: new intermediate is not a sender or a receiver from another money laundering pattern 
						// assumption: new intermediate is not an intermediate from another money laundering pattern.. why isn't that allowed? realistically it should be allowed.
						int counter = 0;
						while  (intermediate == sender 
								|| intermediate == receiver
								|| currentIntermediates.contains(intermediate)
								|| allIntermediates.contains(intermediate)
								|| allSenderandReceivers.contains(intermediate))
						{	 
							counter++;
							intermediate = randInt(0, nodes.size()-1);
							
							if (counter > nodeSize*10)  // x10 should be a good estimate for exhausting possibilities
							{
								// at this point there are not other nodes to be used as intermediate, so break and inform user
								System.out.println("Warning: there are not enough nodes to assign an intermediate money launderer");
								return null;
							}
						}
						
						// track total number of ML accounts
						totalMLAccounts++;
						
						
						// is it necessary to have two separate lists of intermediates?
						currentIntermediates.add(intermediate);   
						allIntermediates.add(intermediate);
						
						
						
						// assign transaction sender/receivers 
						
						
						int tranSource = -1, tranDestination = -1;
						
						// if reached end of depth, get receiver
						if (intermediateDepth == k)
							tranDestination = receiver;
						
						
						// if beginning of depth 
						if (prevIntermediate == sender)
							tranSource = sender;
						else
							tranSource = prevIntermediate; // if in the middle or end of loop
						
						
						System.out.println("Row # " + j + " depth #" + k + " intermediate " + nodes.get(intermediate));
						
						
						// make edges u->v->w
						// for each intermediate add incoming and outgoing transactions. Each intermediate has a different number of transactions.	
						// TODO: random num of transactions may change in future for depth > 1 for consistency
						int MLNumberOfTransactions = randInt(MLTransactionCount, MLTransactionCountHigh);
						for (int n=1;n<=MLNumberOfTransactions;n++)
						{
							// pattern: w00x000y000z0 , w = pattern index, x = row, y = intermediate index, z = transaction index
							int id = (100000000 * i) + (1000000 * j) + (1000 * k) + (10 * n); // give large ids for transactions so it easy to differenciate
			
							// u->v
							FinancialTransaction transaction = new FinancialTransaction();
							transaction.name = id;
							transaction.sender = nodes.get(tranSource);
					        transaction.receiver = nodes.get(intermediate);
							transaction.amount = MLtransactionAmountInput;
							transaction.time =  MLtransactionTimeMin;
							//System.out.println(transaction.toString());
							transactions.add(transaction);
							
							// if we have reached the end of depth, make a transaction from current immediate to receiver
							if (tranDestination > -1)
							{
								//v->w
								FinancialTransaction transaction2 = new FinancialTransaction();
								transaction2.name = id+1;
								transaction2.sender = nodes.get(intermediate);
								transaction2.receiver = nodes.get(tranDestination);
								transaction2.amount = MLtransactionAmountOutput;
								transaction2.time =  MLtransactionTimeMax;
								//System.out.println(transaction.toString());
								transactions.add(transaction2);
							}
							
						}
						
						// store previous intermediate for future transactions
						prevIntermediate = intermediate;
						
						
						
				}
				
			
			}
			System.out.println("");		
		
			
		
			
			
		}
		double time3_2 =  System.currentTimeMillis();
		
		
	
		// write output
			try {
		
	
				
				FileWriter fw = new FileWriter("transactions.txt");
				 
				for (i = 0; i < transactions.size(); i++) {
					fw.write(transactions.get(i).toStringFile());
					fw.write(System.getProperty("line.separator"));
				}
			 
				fw.close();
				
				fw = new FileWriter("nodes.txt");
				 
				for (i = 0; i < nodes.size(); i++) {
					fw.write(nodes.get(i).toStringFile());
					fw.write(System.getProperty("line.separator"));
				}
			 
				fw.close();
			 
			
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		
	
			
			
			
			FileWriter fw = new FileWriter("output_summary.txt");
			 
			fw.write("\nFinished generating data... \nTimeframe.. \nNode creation:" 
					+ (double) (time1_2-time1_1) + "\nTransaction creation:"+ 
					(double) (time2_2-time2_1) + "\nML transaction:"+ (double) (time3_2-time3_1) );
					fw.write("\nTotal ML accounts generated: " + totalMLAccounts);
					fw.write("\nTotal ML groups generated: " + MLPatternsSize);
					fw.write("\nTotal number of nodes: " + nodeSize);
					fw.write("\nTotal number of clean transactions: " + transactionSize);

			fw.close();
			

			return new int[]{MLPatternsSize, totalMLAccounts};
		// now save to file
		
		
	}
	
	
	private static int randInt(int min, int max) {

	    // NOTE: Usually this should be a field rather than a method
	    // variable so that it is not re-seeded every call.
	    Random rand = new Random();

	    // nextInt is normally exclusive of the top value,
	    // so add 1 to make it inclusive
	    int randomNum = rand.nextInt((max - min) + 1) + min;

	    return randomNum;
	}
	
	
}
