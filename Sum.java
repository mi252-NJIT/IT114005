//Michael Imbro
//IT 114 - 005
//Convert Recursion to Loop HW

class Sum {
	public static int sum(int num) {
		int output = 0;
		while (num > 0) {
			output += (num--);
		}
		return output;
	}
	
	public static void main(String[] args) {
		System.out.println(sum(10));
	}
	
}