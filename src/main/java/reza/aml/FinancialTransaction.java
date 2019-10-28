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

public class FinancialTransaction {

	
	public int name;
	public FinancialNode sender; // account sending the funds
	public FinancialNode receiver; // account receiving the funds
	public int amount;
	public int time;
	public int weight;
	
	
	public String toString()
	{
		// #transaction-id, sender-node, receiver-node, amount, time 
		return "[" + name + " " + sender + " " + receiver + " " + amount + " " + time + "]";
	}
	
	
	public String toStringFile()
	{
		// #transaction-id, sender-node, receiver-node, amount, time 
		return name + "," + sender + "," + receiver + "," + amount + "," + time;
	}
	
}
