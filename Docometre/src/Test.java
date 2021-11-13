import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Test {

	public static void main(String[] args) {
		try {
			java.lang.Process process = Runtime.getRuntime().exec("ping -c 1 -W 10 192.168.0.188");
			process.waitFor();
			String line;
			BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String outString = "";
			while((line = out.readLine()) != null){
				outString = outString + line + "\n";
			}
			out.close();
			System.out.println(outString);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

}
