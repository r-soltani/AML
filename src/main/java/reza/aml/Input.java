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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Input {

	
	public static ArrayList<String> readNodes(String path)

	{
		 // open input files
        ArrayList<String> nodes = new ArrayList<String>();
        File file = new File(path);
        BufferedReader reader = null;
        try {
        	reader = new BufferedReader(new FileReader(file));
            String text = null;
            while ((text = reader.readLine()) != null) {
            	if (!text.startsWith("#"))
            		nodes.add(text.trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
            }
        }
        
       
		return nodes;
		
	}
	
	
	
	public static ArrayList<FinancialTransaction>  readTransactions(String path)
	{
		 ArrayList<FinancialTransaction> transactions = new ArrayList<FinancialTransaction>();
		 File  file = new File(path);
		 BufferedReader  reader = null;
	        try {
	        	reader = new BufferedReader(new FileReader(file));
	            String text = null;
	            while ((text = reader.readLine()) != null) {
	            	if (!text.startsWith("#") && !text.isEmpty())
	            	{
	            	
	            	//	System.out.println(text);
	            		FinancialTransaction t = new FinancialTransaction();
	            		String[] oneTransaction = text.split(",");
	            	
	            		t.name = Integer.parseInt(oneTransaction[0]);
	            		
	            		FinancialNode sender = new FinancialNode();
	            		sender.name = Integer.parseInt(oneTransaction[1].trim());
	            		t.sender = sender;
	            		
	            		FinancialNode receiver = new FinancialNode();
	            		receiver.name = Integer.parseInt(oneTransaction[2].trim());
	            		t.receiver = receiver;
	            		
	            		t.amount = Integer.parseInt(oneTransaction[3].trim());
	            		t.time = Integer.parseInt(oneTransaction[4].trim());
	            		transactions.add(t);
	            	}
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        } finally {
	            try {
	                if (reader != null) {
	                    reader.close();
	                }
	            } catch (IOException e) {
	            }
	        }
	        
	     return  transactions;  
	        
	}
	
}
