//Michael Imbro
//It 114
//Java Loops HW

import java.util.*;
public class HWJavaLoops {
	public static void main(String[] args) {
		//1. Create an array/collection of numbers (initialize it with any number of numbers (more than 1) in numerical order, with or without duplicates)
		int[] numbers = {1, 2, 3, 4, 5};
		//2. Create a loop that loops over each number and shows their value.
		//3. Have the loop output only even numbers regardless of how long the array/collection is.
		for (int i: numbers) {
			if (i % 2 == 0) {
				System.out.println(i);
			}
		}
		//4. Briefly explain how you achieved the correct output:
			//i % 2 will equal 0 only when i is an even number. The if statement will make it so that the number will print only when i % 2 is 0.
	}

}
