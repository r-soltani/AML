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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.impl.util.StringLogger;
import org.neo4j.tooling.GlobalGraphOperations;


public class App 
{

	//  ALGORITHM PARAMETERS
	static double degreeConstant = 0.1; // step 3 - bscore  (weight of edges), .8 for at least 7 incoming and outgoing  
	
	//  ALGORITHM PARAMETERS
	static double densePairConstant = 0.2;  // step 4 - SHRINK (similarity of two nodes), affected by the weight of the edges (i.e. balance and number of incoming and outgoing). 0.23 is two (sender, receiver) common neighbour

	//  FINANCIAL PARAMETERS
	static double amountThreshold = 10000;  // transaction should be 10000 
	static double allowedAmountDifference = 100.0; // transactions can be different as much as the following amount
	static double allowedTimeDifference = 2; // transactions can be apart as much as the following value
		
	//  DATA PARAMETERS
	static boolean generateData = true;  // should the framework generate data. Once data is generated disable this flag to avoid overwriting your data!
	static boolean generateDataAndExit = false; // exit after generation. Only generate data
	
	static boolean demo = false; // bypasses all checks and display the entire graph . This feature is no longer used
	static boolean experimentActive = false;  // activate part 3.5 or not. not used in current version of paper due to low accuracy for all topologies. 
	
	enum TransactionTypes implements RelationshipType
	{
		SEND, RECEIVE
	}

	// Main method 
	public static void main( String[] args ) throws IOException
	{
		
		// Output.txt 
		PrintStream out = new PrintStream(new FileOutputStream("output.txt"));
		System.setOut(out);
		
		    
		double timeStart =  System.currentTimeMillis();
		
		System.out.println( "Start of AML framework... v1" );
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println(dateFormat.format(date));
		int[] generationResult = new int[2];  // used for stats
		
		if (demo)
		{
			System.out.println("Demo activated");
		}
	
		if (generateData == true)
		{
			System.out.println("Generating data ...");
			try {
				generationResult = DataGenerator.generateData();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  
			if (generateDataAndExit)
			{
				System.out.println("End of generating data.");
				return;
			}
				
		}
		
		double timeStartWithoutGeneration =  System.currentTimeMillis();
		
		//if (true) return;
		System.out.println(dateFormat.format(date));
		System.out.println("------------");
		System.out.println("Start of algorithm ");
		System.out.println("Parameters:");
		System.out.println("degreeConstant:" + degreeConstant);
		System.out.println("densePairConstant:" + densePairConstant);
		System.out.println("amountThreshold:" + amountThreshold);
		System.out.println("allowedAmountDifference:" + allowedAmountDifference);
		System.out.println("allowedTimeDifference:" + allowedTimeDifference);
		
		
		// Output_summary.txt
		
		FileWriter fw = new FileWriter("output_summary.txt");
		fw.write("\n------------");
		fw.write("\nStart of algorithm ");
		fw.write("\nParameters:");
		fw.write("\ndegreeConstant:" + degreeConstant);
		fw.write("\ndensePairConstant:" + densePairConstant);
		fw.write("\namountThreshold:" + amountThreshold);
		fw.write("\nallowedAmountDifference:" + allowedAmountDifference);
		fw.write("\nallowedTimeDifference:" + allowedTimeDifference);
		fw.close();
	
		
		// Nodes.txt
		ArrayList<String> nodes = Input.readNodes("nodes.txt");
		
		// Transactions.txt
		ArrayList<FinancialTransaction> transactions = Input.readTransactions("transactions.txt");


		
		
		// System.out.println(nodes.toString());
		// System.out.println(transactions.toString());

		// ***********************************
		// step one: find matching transactions
		if (demo)
			System.out.println("----------------------- \n Showing all transactions");
		else
			System.out.println("----------------------- step 1 \n Finding matching transactions ");
		
		HashMap<FinancialTransaction, FinancialTransaction> pairs = new HashMap<FinancialTransaction, FinancialTransaction>();
		HashMap< HashMap<FinancialTransaction, FinancialTransaction>, Double> similarityOfTransactions = new HashMap< HashMap<FinancialTransaction, FinancialTransaction>, Double> ();
		double time1 =  System.currentTimeMillis();
		
		
		int i = 0;
		int j = 1;
		boolean found = false;

		
		for (i=0;i<transactions.size();i++)
		{

			for (j=0;j<transactions.size();j++)
			{

				//System.out.println(".");
				// if looking at same 
				if (i == j)
					continue;
				
				// the map pair contains transactions already processed 
				// dont look at existing items in P. Unique transactions on either side of pair <L, R>
				// needs investigation..
				if (pairs.containsKey(transactions.get(i)))
				{
					continue;
				}
				if  (pairs.containsValue(transactions.get(j)))
				{
					continue;
				}


				
				Double amountDifference = (double) Math.abs((transactions.get(i).amount - transactions.get(j).amount));
				Integer timeDifference = transactions.get(i).time - transactions.get(j).time;

				
				
			
				
				// if this is a demo or we just generated the data and want to see full graph
				if (demo)
				{ 
					pairs.put(transactions.get(i), null);
					
					continue;
				}


				// if it has the form u->v v->w
				if (transactions.get(j).sender.name == transactions.get(i).receiver.name )
	
				// amount equal or higher than 10000
				// condition 1 of trx matching algorithm 
				if (transactions.get(i).amount >= amountThreshold)
				{ 
					//System.out.println("Comparing " + transactions.get(i).amount + " and " + transactions.get(j).amount) ;

					// if sending and receiving amounts are same or similar above certain threshold ex. $100
					// condition 2 of trx matching algorithm 
					if (amountDifference <= allowedAmountDifference)
					{

						// within same timeframe. This is the simplified version
						// todo: implement time variance function
						if (timeDifference <= allowedTimeDifference)
						{
							// store so the pair of t
							pairs.put(transactions.get(i), transactions.get(j)); 
							
							System.out.println(".");
							// store the matching transactions along with their difference in terms of amount and time
							similarityOfTransactions.put(pairs, 1 / (amountDifference * timeDifference));  // include the difference between transactions
							//found = true;
							//break;
							//	break; //only one 

						}
					}

				}

			}  
		}

		double time2 =  System.currentTimeMillis();
		
		System.out.println("Matching transactions...");
		System.out.println(pairs);
	
			
		if (pairs.isEmpty())
			System.out.println("There are no matching transactions!");
	
		// ************************************************************************************
		// ************************************************************************************
		// step 2: make graph of matching transactions
		System.out.println("----------------------- step 2 \n Making graph...");

		GraphDatabaseService graphDb = null;
		GlobalGraphOperations GOp = null;

		double time2_1 =  System.currentTimeMillis();
		try {
			
			// reset database 
			removeDirectory("neo4j-store");
			
			graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( "neo4j-store" );
			GOp = GlobalGraphOperations.at(graphDb);
			registerShutdownHook( graphDb );
		}
		
		catch (Exception e)
		{
			System.out.println("Connection error:  " + e.getLocalizedMessage());
			return;
		}


		String query;
		ExecutionResult result;

		ExecutionEngine engine = new ExecutionEngine( graphDb, StringLogger.DEV_NULL );



		Iterator it = pairs.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<FinancialTransaction, FinancialTransaction> onepair = (Map.Entry) it.next();

			System.out.println("Retrieved: " + onepair.getKey() + " = " + onepair.getValue());

			try ( Transaction tx = graphDb.beginTx() )
			{


				if (demo) 
				{
					System.out.println("Demo: Ignoring second part.. ");
					System.out.println("Adding: " + onepair.getKey().sender.name  + " --> " + onepair.getKey().receiver.name);

				}
				else
					System.out.println("Adding: " + onepair.getKey().sender.name  + " --> " + onepair.getKey().receiver.name + " --> " + onepair.getValue().receiver.name );



				Label label;
				Node firstNode, secondNode, thirdNode;
				Relationship	relationship = null, relationship2 = null;


				// first node
				query =  "match (n {id: "+onepair.getKey().sender.name + "}) return n";
				System.out.println(query);
				result =  engine.execute( query );
			
				if (result.columnAs("n").hasNext())
				{
					System.out.println("Node " + onepair.getKey().sender.name + " already exist");
					firstNode =  (Node) result.columnAs("n").next();
				}
				else
				{
					firstNode = graphDb.createNode();
					System.out.println("Node " + onepair.getKey().sender.name + " is generated");
					firstNode.setProperty( "id", onepair.getKey().sender.name );
				}

				// second node
				query =  "match (n {id: "+ onepair.getKey().receiver.name + "}) return n";
				System.out.println(query);
				result = engine.execute( query );
				if (result.columnAs("n").hasNext())
				{
					System.out.println("Node " + onepair.getKey().receiver.name + " already exist");
					secondNode =  (Node) result.columnAs("n").next();
				}
				else
				{
					secondNode = graphDb.createNode();
					System.out.println("Node " + onepair.getKey().receiver.name + " is generated");
					secondNode.setProperty( "id", onepair.getKey().receiver.name );
				}

				// relationship

				query =  "START n=node(*) MATCH n-[rel:SEND]->r  WHERE n.id="+  onepair.getKey().sender.name  +" AND r.id="+  onepair.getKey().receiver.name + " RETURN rel";
				result = engine.execute(query);
				
				// if relationship already exist
				if (result.columnAs("rel").hasNext())
				{
					System.out.println("Edge " + onepair.getKey().sender.name + " to " +  onepair.getKey().receiver.name + "  already exist");
					relationship =  (Relationship) result.columnAs("rel").next();
					relationship.setProperty("weight", Integer.parseInt(relationship.getProperty("weight").toString()) +1);
					// time and amount are not updated for existing edges 
				}
				else
				{
					// if relationship(transaction) is new
					System.out.println("Edge " + onepair.getKey().sender.name + " to " +  onepair.getKey().receiver.name + "  is generated");
					relationship = firstNode.createRelationshipTo(secondNode, TransactionTypes.SEND);
					relationship.setProperty("weight", 1);
					relationship.setProperty("amount", onepair.getKey().amount);
					relationship.setProperty("time", onepair.getKey().time);
					relationship.setProperty("id", onepair.getKey().name); // set the id as the name of the tranx
				}


				if (!demo)
				{


					// third node
					query =  "match (n {id: "+ onepair.getValue().receiver.name + "}) return n";
					result = engine.execute( query );
					System.out.println(query);
					if (result.columnAs("n").hasNext())
					{
						System.out.println("Node " + onepair.getValue().receiver.name + " already exist");
						thirdNode =  (Node) result.columnAs("n").next();
					}
					else
					{
						thirdNode = graphDb.createNode();
						System.out.println("Node " + onepair.getValue().receiver.name + " is generated");
						thirdNode.setProperty( "id", onepair.getValue().receiver.name );
					}


					// relationship
					query =  "START n=node(*) MATCH n-[rel:SEND]->r  WHERE n.id="+  onepair.getKey().receiver.name  +" AND r.id="+  onepair.getValue().receiver.name + " RETURN rel";
					result = engine.execute(query);
					if (result.columnAs("rel").hasNext())
					{
						System.out.println("Edge " + onepair.getKey().receiver.name + " to " +  onepair.getValue().receiver.name + " already exists");
						relationship2 =  (Relationship) result.columnAs("rel").next();
						relationship2.setProperty("weight", Integer.parseInt(relationship2.getProperty("weight").toString()) +1);

					}
					else
					{
						System.out.println("Edge " + onepair.getKey().receiver.name + " to " +  onepair.getValue().receiver.name + "  is generated");
						relationship2 = secondNode.createRelationshipTo(thirdNode, TransactionTypes.SEND);
						relationship2.setProperty("weight", 1);
						relationship2.setProperty("amount", onepair.getValue().amount);
						relationship2.setProperty("time", onepair.getValue().time);
						relationship2.setProperty("id", onepair.getValue().name);
					}
				}


				tx.success();

			}
			catch (Exception e)
			{
				System.out.println("ERROR with adding to db..");
				e.printStackTrace();
				break;
			}
			finally 
			{


			}

			//	   break;
		}

		double time2_2 =  System.currentTimeMillis();

		

		// ************************************************************************************
		// ************************************************************************************
		// step 3: calculate balance score (B Score) of each node  (check inbound and outbound weights are the same)

//		first part of formula will be 1 if weights are same, or 0 if weights are different. meaning incoming = outgoing better. 
//		second part returns the smaller direction weight, either incoming weight, or outgoing weight. meaning higher weights better. 
//		second part is used to give more value to nodes with more transactions(weight)

		

		// calculate balance score for all nodes. only the first part is implemented
		
		// Requirement: only one edge between each node (each edge has a weight) 
		System.out.println("----------------------- step 3 \n Calculating balance score (node weights)...");
		HashMap<Node, Double> Bs = new HashMap<Node, Double>();
		ArrayList<Node> BsArray = new ArrayList<Node>();
		double time3_1 =  System.currentTimeMillis();
		Integer graphNodesQuantity = 0;
		
		try ( Transaction tx = graphDb.beginTx() )	
		{

			// get all nodes
			Iterator<Node> allNodes =  GOp.getAllNodes().iterator();

			while (allNodes.hasNext()) {
				graphNodesQuantity++;
				Node currentNode = (Node) allNodes.next();
				Iterator<Relationship> outboundRel = currentNode.getRelationships(Direction.OUTGOING).iterator();  // get outbound edges
				Iterator<Relationship> inboundRel = currentNode.getRelationships(Direction.INCOMING).iterator();   // get inbound edges

				System.out.println("Observing node " + currentNode.getProperty("id")); 

				Double B = 0.0;
				Integer sumOfOutgoing = 0;
				Integer sumOfIncoming = 0;	
				int outboundRelCount = 0, inboundRelCount = 0;


				while (outboundRel.hasNext())
				{
					outboundRelCount+=1;  // compute # of outbound
					
					//		System.out.println(outboundRel.next().getProperty("id"));
					sumOfOutgoing += Integer.parseInt(outboundRel.next().getProperty("weight").toString());  // compute total weight of outgoing edges
					//	System.out.println(">");
					//	outboundRel.next();
					//	sumOfOutgoing += 1;
				}

				//System.out.println("Inbound edges: ");
				while (inboundRel.hasNext())
				{
					inboundRelCount+=1; // compute # of inbound
					//	System.out.println(inboundRel.next().getProperty("id"));
					sumOfIncoming += Integer.parseInt(inboundRel.next().getProperty("weight").toString()); // compute total weight of incoming edges
					//inboundRel.next();
					//	sumOfIncoming += 1;
					//	System.out.println("<");
				}
				
				System.out.println("Node: " + currentNode.getProperty("id") + " #inbound " + inboundRelCount + " #outbound " + outboundRelCount);
				System.out.println("sumofincoming " + sumOfIncoming +  " sumofoutgoing " + sumOfOutgoing);
				
				// basic version: only the first part of the formula
				//B = (2 * sumOfOutgoing * sumOfIncoming) / (Math.pow(sumOfOutgoing,2) + Math.pow(sumOfIncoming, 2));
				
				// advanced version: both parts of formula
				B = (2 * sumOfOutgoing * sumOfIncoming) / (Math.pow(sumOfOutgoing,2) + Math.pow(sumOfIncoming, 2));   // compare the difference between the weights. more diff -> higher value 
				B = B * Math.log10(Math.min(sumOfOutgoing, sumOfIncoming));  // put more emphasis on nodes with higher weight
				
				System.out.println(" > Final B value " + B);
				if (demo || B >= 0)
				{
					currentNode.setProperty("B", B);
					currentNode.setProperty("sumOfIncoming", sumOfIncoming);
					currentNode.setProperty("sumOfOutgoing", sumOfOutgoing);
					currentNode.setProperty("B-with-second-term", B * Math.log10(Math.min(sumOfOutgoing, sumOfIncoming)));
					
					Bs.put(currentNode, B);
				}

			}


			// sort
			Bs = (HashMap<Node, Double>) Util.sortMapByValue(Bs);


			System.out.println("Sorted Bs: (High degree nodes) (higher than/equal to: " + degreeConstant +  " )");
			tx.success();
			Iterator BsI = Bs.entrySet().iterator();


		
			while (BsI.hasNext()) {
				Map.Entry pairs1 = (Map.Entry) BsI.next();
				Node  BNode = (Node) pairs1.getKey();
				
				// if Node has high enough B value, add it to the final list
				if ((Double) pairs1.getValue() >= degreeConstant)
					BsArray.add(BNode);
				
				System.out.println("Node: " + BNode.getProperty("id") + ": " + pairs1.getValue());

				BsI.remove(); // avoids a ConcurrentModificationException
			}

			System.out.println("High risk nodes in horizontal form: ");
			for (Node n : BsArray)
				System.out.print(n.getProperty("id") +" ");
			System.out.println("");

			
		}



		double time3_2 =  System.currentTimeMillis();
		
		
		// ***********************************
		// step 3.5 add temporary shared neighbors to intermediate nodes to satisfy step 4 algorithm
		// this is to produce better result for linear result
		

	
		if (experimentActive)
		{
				
				System.out.println("----------------------- step 3.5 ");
				
				// return intermediates in: X -> .. i.. -> Y 
				// query =  "START n=node(*) MATCH p=()-->i-->() WHERE has(i.B)  RETURN DISTINCT filter(x IN NODES(p) WHERE exists(x.B)) as o";
				//query =  "START n=node(*)  WHERE n.aa='bb' RETURN n ";
				//System.out.println(row);
				Iterator<Iterable<Node>> resultingRows = null;
				Iterator<Node> resultingSenders = null;
				Iterator<Node> resultingReceivers = null;
				
				// this is to store the sender and receivers seen
				HashMap<Pair<Node,Node> , Node> endPoints = new HashMap<Pair<Node,Node> , Node>();
				
				try(Transaction tx = graphDb.beginTx())
				{	
					//query = "MATCH p=(n)-[:SEND*]->(m) WHERE NOT ( ()-[:SEND]->(n) OR (m)-[:SEND]->() ) RETURN NODES(p)[1..-1] AS middleNodes";
					query = "MATCH p=(n)-[:SEND*]->(m) WHERE NOT ( ()-[:SEND]->(n) OR (m)-[:SEND]->() ) RETURN NODES(p)[1..-1] AS middleNodes, NODES(p)[0] AS sender, NODES(p)[-1] as receiver";
					
					result = engine.execute(query);
					
//					resultingRows =  result.columnAs("middleNodes");
//					resultingSenders =  result.columnAs("sender");
//					resultingReceivers =  result.columnAs("receiver");
//				
					
				
			//	HashSet<String> seenNodesNames = new HashSet<String>();
					
//					while (resultingSenders.hasNext())
//					{
//						Node sender = (Node) resultingSenders.next();	
//						System.out.println(">" + sender.getProperty("id"));
//						Node receiver = (Node) resultingReceivers.next();	
//						System.out.println(receiver.getProperty("id"));
//					}	
					
					

				int idC = 0;
				int idC2 = 0;
				
				// for every set 
				//  while (resultingRows.hasNext())  // used when there is only one result column
				for (Map<String, Object> row : result) 
				{
					
					    System.out.println("---- New row of nodes...");		   
						Iterable<Node> intermediates = (Iterable<Node>) row.get("middleNodes");
						
						// obtain sender and receiver nodes for this row
						Node sender = (Node) row.get("sender");
						Node receiver = (Node)  row.get("receiver");
						
						
							 // compute the number of nodes in one row
							 int sizeOfRow=0;
							 int indexOfRow=0;
							 
							 for (Node n : intermediates) {
								 if (n.hasProperty("sumOfIncoming"))  // even though the query only returns the intermediates, we are again checking for a property specific to intermediates
									 sizeOfRow++;
							 }
						
							  System.out.println("Obtained a row of intermediates with length " + sizeOfRow + " sender:" + sender.getProperty("id") + " receiver:" + receiver.getProperty("id"));
								
							 // if the row only consist of 1 node.. ex: S -> I -> R, then dont add the shared nodes as Clustering algorithm will pick up this node anyways
							 if (sizeOfRow > 1)
							 {
								
					 
								// if sender and receiver hasnt been seen before, create a new node
								// otherwise extract the same shared node from a map
									
								 Pair<Node,Node> thisEndPoint = new ImmutablePair<Node, Node>(sender, receiver);
								 
								 Node sharedNode = null;
								 if (endPoints.containsKey(thisEndPoint))
								 {
									 System.out.println("The shared point already exist");
									 sharedNode = endPoints.get(thisEndPoint);
								 }
								 else 
								 {
									 System.out.println("Creating new shared point");
									 sharedNode = graphDb.createNode();
									 endPoints.put(thisEndPoint, sharedNode);
										
									 // add label to node to distinguish it from other nodes. 
										Label SharedLabel = DynamicLabel.label("SHARED");
										sharedNode.addLabel(SharedLabel);
										sharedNode.setProperty("type", "sharedNode");
										idC++;
										sharedNode.setProperty("id", 1000000000000L+idC);
										
								 }
								 

								
								// reset relationship index counter
								idC2 = 0;
														 
							// every node in the row
							 for (Node n : intermediates) {
								 
								 	// extra check 
									if (n.hasProperty("sumOfIncoming"))
									{
									
									
						                //System.out.println(n.getProperty("id"));
		//							 if (seenNodesNames.contains(n.getProperty("id").toString())) 
		//								 continue;
		//							 
		//							 	seenNodesNames.add(n.getProperty("id").toString());
									 
									   // set the weight value for the edges. set the weight to outgoing unless its the last node of the list.
										indexOfRow++;	
										Integer weight = 0;
										 if (n.hasProperty("sumOfOutgoing"))
										    weight = (Integer) n.getProperty("sumOfOutgoing");
										 
										 if (indexOfRow == sizeOfRow)
										 {
											if (n.hasProperty("sumOfIncoming"))
												 weight = (Integer) n.getProperty("sumOfIncoming");
										 }
										 
									 
						                // create a relationship between new node and the existing nodes
						                Relationship sharedR = sharedNode.createRelationshipTo(n, TransactionTypes.SEND);
						              
										sharedR.setProperty("type", "sharedRelationship");
										idC2++;
										sharedR.setProperty("id", 1000000000+idC+idC2);
										sharedR.setProperty("weight", weight); 
										
										Relationship sharedR2= n.createRelationshipTo(sharedNode, TransactionTypes.SEND);
										sharedR2.setProperty("type", "sharedRelationship");
										idC2++;
										sharedR2.setProperty("id", 1000000000+idC+idC2+1);
										sharedR2.setProperty("weight", weight); 
										System.out.println("Creating a relationship between " + sharedNode.getProperty("id") + " to  " + n.getProperty("id") );
										System.out.println("Creating a relationship between " + n.getProperty("id") + " to  " + sharedNode.getProperty("id") );
									
										System.out.println("Created/reused shared node " +  sharedNode.getProperty("id") + 
												" with relationships " + sharedR.getProperty("id") + " from/to " + n.getProperty("id") + " with weight " + weight +  " .Degree of shared node: " + sharedNode.getDegree());								
										
								 	} // if its not an I node
									else
									{
										System.out.println("Ignored a node because its not an I node ");
										
										
									}
							 } //loop 
							 tx.success();
							
						 } // if there is only 1 node in row
						 else
						 {
								System.out.println("Ignored a row of nodes, because there is only one node in the row ");						
								
						 }
							 
				}  // while
				
				}   // tx 
						
					
//						 <Node> iNodes = (Iterator<Node>) row.nodes(); 
//						Iterator<Node> iNodes  = resultingNodes;
//						
						
//						Node sharedNode = graphDb.createNode();
//						sharedNode.setProperty("type", "sharedNode");
//						i++;
//						sharedNode.setProperty("id", 10000+idC);
//						idC2 = 0;
//						while (iNodes.hasNext())
//						{
//							Relationship sharedR = sharedNode.createRelationshipTo(iNodes.next(), TransactionTypes.SEND);
//							sharedR.setProperty("type", "sharedRelationship");
//							idC2++;
//							sharedR.setProperty("id", 1000000000+idC+idC2);
//							System.out.println("Created shared node " +  sharedNode.getProperty("id") + 
//									" with relationship " + sharedR.getProperty("id"));
//							
//						} 
							
				
		}
				
		
		
		// ***********************************
		// step 4: Find dense pairs by the means of clustering (SHRINK) 
		
		
//		σ(u,v)=(∑_(x∈Γ(u)∩Γ(v))▒〖w(x,u)w(x,v)〗)/(√(∑_(x∈Γ(u))▒〖w^2 (x,u) 〗) √(∑_(x∈Γ(v))▒〖w^2 (x,v) 〗)) 
//		∙  (∑_(x∈Γ'(u)∩Γ'(v))▒〖w(u,x)⋅w(v,x)〗)/(√(∑_(x∈Γ'(u))▒〖w^2 (x,u) 〗) √(∑_(x∈Γ'(v))▒〖w^2 (x,v) 〗))
//				
				
		// issue: what if there are dealing with multi-level intermediate nodes or linear topology

		System.out.println("----------------------- step 4 \n Calculating Similar Nodes (SHRINK)...");
		System.out.println("Compare all combinations...");

		HashMap <ArrayList<Node>, Double> DensePairs = new HashMap <ArrayList<Node>, Double>();// stores pairs and their density
		double time4_1 =  System.currentTimeMillis();
		HashMap<String, Integer> PreviousNodeU = new HashMap<String, Integer>();
		Double[][] similarityMatrix = new Double[BsArray.size()][BsArray.size()];
		
		
		try ( Transaction tx = graphDb.beginTx() )	
		{

			
			// compare every node with every other node. 
			for (i=0; i < BsArray.size(); i++)
			{
				
				Node u = BsArray.get(i);
				
				for ( j=0; j< BsArray.size(); j++)
				{

					// if comparing a node with itself  
					if (i==j) continue;  // add result


					
					Node v = BsArray.get(j);

				
					// if nodes seen before
					// todo: use the value of the previousnodeU to check for item i? ex. key=i&val=j or key=j&val=i
					if (PreviousNodeU.containsKey(i + " " + j) || PreviousNodeU.containsKey(j + " " + i))
					{
							//System.out.println("Found the same key j");
				
							//System.out.println("Found the same pair, skipping");
							//System.out.println("existing pair " + u.getProperty("id").toString() + " and " +  v.getProperty("id").toString());
							continue;		
					}
				
					// store the pair so that they are not compared again	
					PreviousNodeU.put(i + " " + j, 1);
				
					//System.out.println("New pair " + u.getProperty("id").toString() + " and " +  v.getProperty("id").toString());
					//System.out.println(PreviousNodeU);
					
					System.out.println();
					System.out.println("Considering u " + u.getProperty("id").toString() + " | v " + v.getProperty("id").toString());


					
					// u item
//					ArrayList<Node> uEndNodes = new ArrayList<Node>();
//					ArrayList<Node> uStartNodes = new ArrayList<Node>();
//
//					// v item
//					ArrayList<Node> vEndNodes = new ArrayList<Node>();
//					ArrayList<Node> vStartNodes = new ArrayList<Node>();
			


					Double termOne = -1.0;
					Double termTwo = -1.0;
					
			
				// repeat twice for term one and two.
				// round one is node from incoming edges, round two is node at the end of outgoing edges
				for (int ii=1; ii<=2;ii++){
					
					// nominator (common nodes)
					if (ii == 1)
						query = "MATCH (u { id: " +  u.getProperty("id").toString() +"})<-[a:SEND]-(x)-[b:SEND]->(v {id: " + v.getProperty("id").toString() +"}) RETURN a,b";  //incoming 
					else
						query = "MATCH (u { id: " +  u.getProperty("id").toString() +"})-[a:SEND]->(x)<-[b:SEND]-(v {id: " + v.getProperty("id").toString() +"}) RETURN a,b";  //outgoing
						
					
					System.out.println("Query " + query);
					
					result = engine.execute( query );
				
					Double nominator = 0.0;
							
					Relationship edgeXtoU = null;
					Relationship edgeXtoV = null;
        	    		
    	        	 for ( Map<String, Object> row : result)
    	        	 {
    	        	     for ( Entry<String, Object> column : row.entrySet() )
    	        	     {
    	        	    	  String rows = column.getKey() + ": " + column.getValue() + "; ";
    	        	    	  
    	        	    	  if (column.getKey().compareTo("a") == 0)
    	        	    		 edgeXtoU =  (Relationship) column.getValue();
    	        	    	  else
    	        	    		 edgeXtoV =  (Relationship) column.getValue();
    	        	     }
    	        	     
    	        	     
    	        	 	nominator += Double.parseDouble(edgeXtoU.getProperty("weight").toString()) * Double.parseDouble(edgeXtoV.getProperty("weight").toString());
					//	nominator++; // w(u,u) = 1
    	        	 	System.out.println("edge " + edgeXtoU.getProperty("id") + " and " + edgeXtoV.getProperty("id") + ". nominator so far: " + nominator);						
    	        	 }
					
					System.out.println("final nominator " + nominator);

					// denominator
					// part 1 of denominator
					if (ii == 1)
						query = "MATCH (u { id: " +  u.getProperty("id").toString() +"} )<-[a:SEND]-() RETURN a";  //incoming
					else
						query = "MATCH (u { id: " +  u.getProperty("id").toString() +"} )-[a:SEND]->() RETURN a";  //outgoing
						
					
					System.out.println("Query " + query);
					result = engine.execute( query );
					Double sumOfSquaredIncomingEdgesForU = 0.0;
					int edgeXtoUCount = 0;
					if (result.columnAs("a").hasNext())
					{
						while (result.columnAs("a").hasNext())
						{
							edgeXtoU =  (Relationship) result.columnAs("a").next();
							edgeXtoUCount++;
							sumOfSquaredIncomingEdgesForU += Math.pow(Double.parseDouble(edgeXtoU.getProperty("weight").toString()), 2.0);
							//System.out.println("edge " + edgeXtoU.getProperty("id") + " has weight " + edgeXtoU.getProperty("weight").toString());
						}
					}

					sumOfSquaredIncomingEdgesForU++;   // w(u,u) = 1, as part of spec
					System.out.println(edgeXtoUCount + " total squared weight (+1 per spec): " + sumOfSquaredIncomingEdgesForU);
					
					// part 2 of denominator
					if (ii == 1)
						query = "MATCH (v { id: " +  v.getProperty("id").toString() +"} )<-[a:SEND]-() RETURN a"; //incoming
					else
						query = "MATCH (v { id: " +  v.getProperty("id").toString() +"} )-[a:SEND]->() RETURN a"; //outgoing
					
					
					System.out.println("Query " + query);
					result = engine.execute( query );
					Double sumOfSquaredIncomingEdgesForV = 0.0;
					int edgeXtoVCount = 0;
					if (result.columnAs("a").hasNext())
					{
						while (result.columnAs("a").hasNext())
						{
							 edgeXtoV =  (Relationship) result.columnAs("a").next();
							 edgeXtoVCount++;
							 sumOfSquaredIncomingEdgesForV += Math.pow(Double.parseDouble(edgeXtoV.getProperty("weight").toString()), 2);
						//	System.out.println("edge " + edgeXtoV.getProperty("id") + " has weight " + edgeXtoV.getProperty("weight").toString());
						}
					}
					//TODO: should the following line be here??
					sumOfSquaredIncomingEdgesForV++;   // w(u,u) = 1, as part of spec
					System.out.println(edgeXtoVCount + " total squared weight (+1 per spec):  " + sumOfSquaredIncomingEdgesForV);
					
					
					

					if (ii == 1)
					{
						// computing the final value of first term 
						termOne = nominator / (Math.sqrt(sumOfSquaredIncomingEdgesForU) * Math.sqrt(sumOfSquaredIncomingEdgesForV));
						if (termOne == null) // take care of NaN and division by 0
							termOne = 0.0;
						System.out.println(termOne + " = " + nominator + " / " + "sqrt(" + sumOfSquaredIncomingEdgesForU + ") * sqrt("+sumOfSquaredIncomingEdgesForV+")");

					}
					else
					{
						// computing the final value of second term 
						termTwo = nominator / (Math.sqrt(sumOfSquaredIncomingEdgesForU) * Math.sqrt(sumOfSquaredIncomingEdgesForV));
						if (termTwo == null) // take care of NaN and division by 0
							termTwo = 0.0;
						System.out.println(termTwo + " = " + nominator + " / " + "sqrt(" + sumOfSquaredIncomingEdgesForU + ") * sqrt("+sumOfSquaredIncomingEdgesForV+")");
					}

				}
				

				Double finalResult = termOne * termTwo;

				System.out.println("Similarity value between node u and v is " + finalResult);
				
				
				// add values to a matrix for loggin
				similarityMatrix[i][j] = finalResult;
				
				
				if (finalResult >= densePairConstant)  // threshold may be 0 or .2 or something higher
				{
					ArrayList<Node> denseNodes = new ArrayList<Node>(); 
					denseNodes.add(u);
					denseNodes.add(v);
					DensePairs.put(denseNodes, finalResult);
				}

			}

		}
//			System.out.printf("%1s  %-7s   %-7s   %-6s   %-6s%n", "n", "result1", "result2", "time1", "time2");
//			System.out.printf("%1d  %7.2f   %7.1f   %4dms   %4dms%n", 5, 1000F, 20000F, 1000, 1250);
//			System.out.printf("%1d  %7.2f   %7.1f   %4dms   %4dms%n", 6, 300F, 700F, 200, 950);
//			
		// draw a matrix
		if (!pairs.isEmpty())
		{	
			System.out.println("----");
			System.out.print("  ");
			System.out.printf("\t\t   ", "");
			for (i=0;i<BsArray.size();i++)
			{
	
				System.out.printf("%-7d", BsArray.get(i).getProperty("id"));
			}

			System.out.println("");
			NumberFormat formatter = new DecimalFormat("#0.00");
			
			    int rows = similarityMatrix.length;
		        int columns = similarityMatrix[0].length;
		        String str = "|\t";
		        Double num = 0.0;
		        
		        for( i=0;i<rows;i++){
		        	System.out.printf("%-7d", BsArray.get(i).getProperty("id"));
		        	//System.out.print(BsArray.get(i).getProperty("id") + "\t");
		            for( j=0;j<columns;j++){
		            	if (similarityMatrix[i][j]!=null)
		            	{ num = similarityMatrix[i][j]; //formatter.format(similarityMatrix[i][j]);
		            		System.out.printf("%7.2f", num);}
		            	else
		            	{
		            		num = -1.0;
		            		System.out.printf("%7s", "N/A");
		            	}
		            	
		            	
		            }
		            System.out.println("");
		          
		          
		        }
		        
			System.out.println("----");
			//System.out.println(PreviousNodeU);
		}
		
			tx.success();
			}
		
		double time4_2 =  System.currentTimeMillis();
		

		
		// sort
		//DensePairs = (HashMap<ArrayList<Node>, Double>) Util.sortMapByValue(DensePairs);
		
		

		// ***********************************
		// step 5: identity groups (from dense pairs)
		// In the first step the DensePairs with the format: {List<Node>, Double}... is converted to a list of sets: {Set<Node>,...}

		System.out.println("----------------------- step 5 \n Identity groups (from dense pairs)...");
	
		Integer DensepairSize = DensePairs.size(); 
		System.out.println("Number of dense pairs to look (i.e. pairs with similarity above threshold " + densePairConstant  +  " : " + DensePairs.size() + ")");	
		
		Iterator DensePairsit = DensePairs.entrySet().iterator();
		ArrayList <ArrayList<Node>> DensePairsArray = new ArrayList <ArrayList<Node>>();// stores pairs (no similarity value)
		
		double time5_1 =  System.currentTimeMillis();
		Transaction tx = graphDb.beginTx();
		try 	
		{
				while (DensePairsit.hasNext()) {
			        Map.Entry DensePairsitItem = (Map.Entry)DensePairsit.next();  // get one { <pair>, <similarity> }
			        
			        System.out.println( DensePairsitItem.getKey() 
			        		+ " with ids " + ((ArrayList<Node>) DensePairsitItem.getKey()).get(0).getProperty("id") + " and " + ((ArrayList<Node>) DensePairsitItem.getKey()).get(1).getProperty("id")
			        		+ " have similarity value " + DensePairsitItem.getValue());   //node1,node2 = similarity 
			     	
			        ArrayList<Node> densePairSet = new ArrayList<Node>(); // create a set. each set contains 2 ids (one pair) in the beginning
			        densePairSet.add(((ArrayList<Node>) DensePairsitItem.getKey()).get(0));  // extract first id
			        densePairSet.add(((ArrayList<Node>) DensePairsitItem.getKey()).get(1)); // extract second id
			        DensePairsArray.add(densePairSet);     // put this set into collection of pairs
			        DensePairsit.remove(); // avoids a ConcurrentModificationException
			    }
				tx.success();
		}
		finally
		{
			tx.close();
		}
		
	
		 
		// at this point we have an array of sets (DensePairsArray). each set has a pair of nodes that are dense. we now combine these sets to to ML groups
		/*

			For each pair d in D
				for each item i in d
					for each unprocessed pair d’ in D (D- d)
						if i is in d’
							merge d’ into d
							remove d’ from D
		*/
	
		Transaction tx2 = graphDb.beginTx();
		try	
		{
		//ArrayList <ArrayList<Node>> MoneyLaunderingGroups = new ArrayList <ArrayList<Node>>();// stores pairs and their density
			
		int totalPairs =  DensePairsArray.size();
		System.out.println("Starting the pair merging process...");
		System.out.println(DensePairsArray);
		
		//  example DensePairsArray = (1,2)(1,3)(1,4)(2,3)(2,4)(3,4)
		// i is the index of set
		for ( i = 0; i <  DensePairsArray.size() ; i++) // For each pair d in D
		{
			System.out.println("---");
			// example densepairitem = (1, 2)
			
			// j is the item index in one set
			for (j = 0; j < DensePairsArray.get(i).size(); j++)  // for each item i in d. i = densePairItem, every densePairItem has 2 items 
			{	
				// if there are no more sets in the list to review. regular size variable cannot be used as the list shrinks when merging happens
				if (i == totalPairs) break;  
				
				ArrayList<Node> densePairItem = (ArrayList<Node>) DensePairsArray.get(i);  // (1,2)
			
				System.out.println("\n Looking at set with first two items of " + densePairItem.get(0).getProperty("id").toString() + " and " +  densePairItem.get(1).getProperty("id").toString() + ", indexes i=" + i + " j="+ j);
				
					// for each unprocessed pair d’ in D: (D - d)
				    // k is the index of unprocessed sets
			        for (int k = i+1; k <  DensePairsArray.size(); k++)  // limit should be var totalPairs ???
			        {
			        	ArrayList<Node> densePairItemCompared = (ArrayList<Node>) DensePairsArray.get(k);  // (1,3) when i=0, k=1 (first try)
			        	
			          	System.out.println("looking at pair with index(k)=" + k + " total number of sets: " + DensePairsArray.size());
			        	System.out.println("comparing node " + densePairItem.get(j).getProperty("id").toString() + " with node " +  densePairItemCompared.get(0).getProperty("id").toString()  + " and " + densePairItemCompared.get(1).getProperty("id").toString());
			        	//System.out.println(DensePairsArray);
			        	
			        	//if item 1 is in d’ :
			        	if (densePairItem.get(j).getProperty("id").toString().equalsIgnoreCase(densePairItemCompared.get(0).getProperty("id").toString()))
			        	{
			        		System.out.println("Item " + densePairItem.get(j).getProperty("id").toString() + " is same as " + densePairItemCompared.get(0).getProperty("id").toString());
			        		System.out.println("reducing number of pairs..");
			        		
			        		
			        		// if 3 is included in (1,2)
			        		if (!densePairItem.contains(densePairItemCompared.get(1)))
			        			densePairItem.add(densePairItemCompared.get(1)); // change (1,2) to (1,2,3)
			        		
			        		System.out.println("Removing " + densePairItemCompared);
			        		DensePairsArray.remove(k);
			        		k--;  // when an set is removed, another set replaces its position (i.e. k) therefore same k index should be checked
			        	//	totalPairs--;
			        	}
			        	//if item 2 is in d’ :
			        	else if (densePairItem.get(j).getProperty("id").toString().equalsIgnoreCase(densePairItemCompared.get(1).getProperty("id").toString()))
			        	{
			        		System.out.println("Node " + densePairItem.get(j).getProperty("id").toString() + " is same as second term " + densePairItemCompared.get(1).getProperty("id").toString());
			        		System.out.println("reducing number of pairs..");
				        	
			        		// if 1 is included in (1,2). it wont get here on example
			        		if (!densePairItem.contains(densePairItemCompared.get(0)))
			        			densePairItem.add(densePairItemCompared.get(0));
			        		
			         		System.out.println("Removing " + densePairItemCompared);
			        		DensePairsArray.remove(k);
			        		k--;
			        	//	totalPairs--;
			        		
			        	}
			        	else
			        		System.out.println("Item " + densePairItem.get(j).getProperty("id").toString() + " is not found in  " + densePairItemCompared);
		        		
			        }  // loops D-d (i.e. d'), iterate k
			
			        
			        DensePairsArray.set(i, densePairItem); // overwrite pair with a new group of nodes
			        
			        // print out the result after comparing one node with d' and performing the necessary merges
			        System.out.println("Result of list so far ...");
			    	System.out.println(DensePairsArray);
			    	
			} // iterate j
			
			
		} // iterate i
		
			tx2.success();
		}
		finally
		{
			tx2.close();
		}
		
		// final DensePairsArray = (a,b,m,e), (c,d)
		double time5_2 =  System.currentTimeMillis();
		
		
		// at this point the var DensePairArray has ML groups 
		// we mark the ML nodes on graph with ML attribute and color 
		Transaction tx1 = graphDb.beginTx();
		int totalMLAccountsFound = 0;  // used for stats 
		try
		{	
		// at the end DensePairsArray has the list of arrays that cooresponde to ML groups
		System.out.println("Resulting ML groups are ...");
		
		Label MLLabel = DynamicLabel.label("ML");
		
		
		for (i = 0; i< DensePairsArray.size(); i++) {
			System.out.println("ML Group #" + new Integer(i+1) + ":");
			
			for (j = 0; j< DensePairsArray.get(i).size(); j++) {
				System.out.print(DensePairsArray.get(i).get(j).getProperty("id") + " ");
				DensePairsArray.get(i).get(j).setProperty("ML", "yes");
			//	DensePairsArray.get(i).get(j).setProperty("ui.class", "ml");  // for coloring. coloring is achieved through graph styling file now
				DensePairsArray.get(i).get(j).addLabel(MLLabel);
				totalMLAccountsFound++;
			}
			System.out.println();
		}
		
		System.out.println(DensePairsArray);
		tx1.success();
		}
		finally
		{
			tx1.close();
		}
		double timeEnd=  System.currentTimeMillis();
		
		
		// MATCH (n) RETURN n
		// MATCH (n) WHERE n.ML = 1 RETURN n
		
		// end of algorithm. report result 
		
		System.out.println("Report ..");
		System.out.println("Total number of nodes: " + nodes.size());
		System.out.println("Total number of transactions: " + transactions.size());

		System.out.println("Total number of matched transactions: " + pairs.size());
		System.out.println("Total number of matched transactions nodes: " + graphNodesQuantity);
		// why is DensepairSize bigger than BsArray.size()  .. dense pair is just? because (BsArray * BsArray) - 5 is the total DensepairSize
		System.out.println("Total number of balanced scored nodes: " + BsArray.size());
		System.out.println("Total number of dense pairs: " + DensepairSize);
		System.out.println("Total number of ML groups: " + DensePairsArray.size());

		System.out.println("ML detection of group rating: " + DensePairsArray.size() + " / " + generationResult[0]);
		System.out.println("ML detection of accounts rating: " + totalMLAccountsFound + " / " + generationResult[1]);
		
		
		
		System.out.println("Timeframe ..");

		System.out.println("Total time to find matching pair (ms): " + (double) (time2 - time1));
		System.out.println("Total time to generate graph (ms): " + (double) (time2_2 - time2_1));
		System.out.println("Total time to calculate balance score (ms): " + (double) (time3_2 - time3_1));
		System.out.println("Total time to Similar Nodes (SHRINK) (ms): " + (double) (time4_2 - time4_1));
		System.out.println("Total time to identity groups (ms): " + (double) (time5_2 - time5_1));
		System.out.println("Total time of program with Data generation (ms): " + (double) (timeEnd - timeStart));
		System.out.println("Total time of program without Data generation (ms): " + (double) (timeEnd - timeStartWithoutGeneration));
		
		
		fw = new FileWriter("output_summary.txt");
		fw.write("\n------------");
		fw.write("\nReport ..");
		fw.write("\nTotal number of nodes: " + nodes.size());
		fw.write("\nTotal number of transactions: " + transactions.size());

		fw.write("\nTotal number of matched transactions: " + pairs.size());
		fw.write("\nTotal number of matched transactions nodes: " + graphNodesQuantity);
		// why is DensepairSize bigger than BsArray.size()  .. dense pair is just? because (BsArray * BsArray) - 5 is the total DensepairSize
		fw.write("\nTotal number of balanced scored nodes: " + BsArray.size());
		fw.write("\nTotal number of dense pairs: " + DensepairSize);
		fw.write("\nTotal number of ML groups: " + DensePairsArray.size());

		fw.write("\nML detection of group rating: " + DensePairsArray.size() + " / " + generationResult[0]);
		fw.write("\nML detection of accounts rating: " + totalMLAccountsFound + " / " + generationResult[1]);
		
		
		
		fw.write("\nTimeframe ..");

		fw.write("\nTotal time to find matching pair (ms): " + (double) (time2 - time1));
		fw.write("\nTotal time to generate graph (ms): " + (double) (time2_2 - time2_1));
		fw.write("\nTotal time to calculate balance score (ms): " + (double) (time3_2 - time3_1));
		fw.write("\nTotal time to Similar Nodes (SHRINK) (ms): " + (double) (time4_2 - time4_1));
		fw.write("\nTotal time to identity groups (ms): " + (double) (time5_2 - time5_1));
		fw.write("\nTotal time of program with Data generation (ms): " + (double) (timeEnd - timeStart));
		fw.write("\nTotal time of program without Data generation (ms): " + (double) (timeEnd - timeStartWithoutGeneration));
	
		
		fw.close();
		
		
		System.out.println("Shutdown db");
		graphDb.shutdown();
	}

	private static void printMatrix(double[][] m){
	    try{
	        int rows = m.length;
	        int columns = m[0].length;
	        String str = "|\t";

	        for(int i=0;i<rows;i++){
	            for(int j=0;j<columns;j++){
	                str += m[i][j] + "\t";
	            }

	            System.out.println(str + "|");
	            str = "|\t";
	        }

	    }catch(Exception e){System.out.println("Matrix is empty!!");}
	}
	
	private static Map<String, Object> removeDirectory(String storeDir) throws IOException {
		try{
			File dir = new File(storeDir);
		
		Map<String,Object> result=new HashMap<String, Object>();
		result.put("store-dir",dir);
		result.put("size", FileUtils.sizeOfDirectory(dir));
		FileUtils.deleteDirectory(dir);
		}
		catch (Exception e)
		{
			return null;
		}
		return null;
	}

	private static void registerShutdownHook( final GraphDatabaseService graphDb )
	{
		// Registers a shutdown hook for the Neo4j instance so that it
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
		// running application).
		Runtime.getRuntime().addShutdownHook( new Thread()
		{
			@Override
			public void run()
			{
				graphDb.shutdown();
			}
		} );
	}
}
