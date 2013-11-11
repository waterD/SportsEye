package lib;

import java.util.Map;
import java.util.Vector;

public class EvaluateFunc {
	
	public void CalConfusionMatrix(Map<String, Map<String, Integer>> confusionMatrix,Vector<String> labels){
	
	/*System.out.format("%-15s","");
	for(Map.Entry<String, Map<String, Integer>> entry : confusionMatrix.entrySet()){
		
		System.out.format("%-15s",entry.getKey() );
	}
	
	for(Map.Entry<String, Map<String, Integer>> entry : confusionMatrix.entrySet()){
		
		String actualClass = entry.getKey();
		Map<String,Integer> resultClass = entry.getValue();
		System.out.print("\n");
		System.out.format("%-15s", actualClass);
		
		for(Map.Entry<String, Integer> entry1 : resultClass.entrySet()){
			int times = entry1.getValue();
			System.out.format("%-15d", times);
		}
	}
	
	System.out.println("\n\n");*/
	
	
		
		//confusion matrix	
		System.out.format("%-15s","");
		for(Map.Entry<String, Map<String, Integer>> entry : confusionMatrix.entrySet()){
			
			System.out.format("%-15s",entry.getKey() );
		}
		
		
		for(Map.Entry<String, Map<String, Integer>> entry : confusionMatrix.entrySet()){
			
			int totalTimes = 0;
			for (int i = 0; i < labels.size(); i++){
				if(labels.get(i).equalsIgnoreCase(entry.getKey())){
					totalTimes++;
				} 
			}
			//System.out.println(totalTimes);
			String actualClass = entry.getKey();
			Map<String,Integer> resultClass = entry.getValue();
			System.out.print("\n");
			System.out.format("%-15s", actualClass);
			for(Map.Entry<String, Integer> entry1 : resultClass.entrySet()){
				int times = entry1.getValue();
				double percent = times/(double)totalTimes * 100;
				String s = String.format("%.2f", percent);
				System.out.format("%-15s", s + "%" + " (" + times + "/" + totalTimes + ")");
			}
		}
	}
	
	public void getAccuracy(Map<String, Map<String, Integer>> confusionMatrix,Vector<String> labels){
		
		System.out.println();
		
		int testSize = labels.size();
		
		for(Map.Entry<String, Map<String, Integer>> entry : confusionMatrix.entrySet()){
			
			int truePositive = 0;
			int trueNegative = 0;
			String actualClass = entry.getKey();
			Map<String, Integer> resultClass = entry.getValue();
			truePositive = resultClass.get(actualClass);
			
			for(Map.Entry<String, Map<String, Integer>> entry1 : confusionMatrix.entrySet()){
				if(entry1.getKey() == actualClass){
					continue;
				}
				
				Map<String, Integer> resultClass1 = entry1.getValue();
				for(Map.Entry<String, Integer> entry2 : resultClass1.entrySet()){
					if (entry2.getKey() == actualClass){
						continue;
					}
					trueNegative += entry2.getValue();
				}
			}
			
			double accuracy = (truePositive + trueNegative)/(double)testSize * 100;
			String s = String.format("%.2f", accuracy);
			System.out.println("accuracy of class " + actualClass + ": " + s + "%");
			
		}
		
	}


	public void getPrecision(Map<String, Map<String, Integer>> confusionMatrix,Vector<String> labels){
		
		System.out.println();
		
		for(Map.Entry<String, Map<String, Integer>> entry : confusionMatrix.entrySet()){
			
			int truePositive = 0;
			int falsePositive = 0;
			
			String actualClass = entry.getKey();
			Map<String, Integer> resultClass = entry.getValue();
			truePositive = resultClass.get(actualClass);
			
			for(Map.Entry<String, Map<String, Integer>> entry1 : confusionMatrix.entrySet()){
				
				if(entry1.getKey() == actualClass){
					continue;
				}
				
				Map<String, Integer> resultClass1 = entry1.getValue();
				for(Map.Entry<String, Integer> entry2 : resultClass1.entrySet()){
					if(entry2.getKey() == actualClass){
						falsePositive += entry2.getValue();
					}
				}
			}
			
			double precision = (double)truePositive/(truePositive + falsePositive) * 100;
			String s = String.format("%.2f", precision);
			System.out.println("precision of class " + actualClass + ": " + s + "%");
			
		}
	}
	
	public void getRecall(Map<String, Map<String, Integer>> confusionMatrix,Vector<String> labels){
		
		System.out.println();
		
		for(Map.Entry<String, Map<String, Integer>> entry : confusionMatrix.entrySet()){
			
			int truePositive = 0;
			int falseNegative = 0;
			
			String actualClass = entry.getKey();
			Map<String, Integer> resultClass = entry.getValue();
			truePositive = resultClass.get(actualClass);
			for(Map.Entry<String, Integer> entry1 : resultClass.entrySet()){
				if(entry1.getKey() == actualClass){
					continue;
				}
				
				falseNegative += entry1.getValue();
			}
			
			double recall = (double)truePositive/(truePositive + falseNegative) * 100;
			String s = String.format("%.2f", recall);
			System.out.println("recall of class " + actualClass + ": " + s + "%");
		}
		
	}
	
}
