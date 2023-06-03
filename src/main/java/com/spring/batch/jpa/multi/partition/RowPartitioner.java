package com.spring.batch.jpa.multi.partition;

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

public class RowPartitioner implements Partitioner{
	
	@Override
	public Map<String, ExecutionContext> partition(int gridSize) {
		
		int min = 1;
		int max = 1000;
		int targetSize = (max - min) / gridSize + 1;
		System.out.println("Target Size: "+ targetSize);
		
		Map<String,ExecutionContext> result =  new HashMap<>();
		
		int number = 0;
		int start = min;
		int end = start + targetSize-1;
		
		while(start <= max) {
			ExecutionContext value = new ExecutionContext();
			result.put("Partition_No_"+number, value);
			
			if(end >= max) {
				end = max;
			}
			
			value.putInt("minValue", start);
			value.putInt("mazValue", end);
			start += targetSize;
			end += targetSize;
			number++;
		}

		System.out.println("Partition Result Is: "+result.toString());
		return result;
	}

}
