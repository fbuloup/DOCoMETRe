package fr.univamu.ism.docometre.analyse.editors.functioneditor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import fr.univamu.ism.docometre.Activator;

public final class CustomerFunctionReader {
	
	public static String read(String functionFullPath) {
		return read(FileSystems.getDefault().getPath(functionFullPath));
	}
	
	public static String read(Path functionPath) {
		try {
			List<String> lines = Files.readAllLines(functionPath, StandardCharsets.UTF_8);
			StringBuffer content = new StringBuffer();
			int i = 0;
			for (String line : lines) {
				String localLine = line.replaceAll("\\\\t", "\t");
				localLine = localLine.replaceAll("\\\\n", "");
				content.append(localLine);
				if(i != lines.size() - 1) content.append("\n");
				i++;
			}
			return content.toString();
		} catch (IOException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return "";
	}
	

}
