import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Importador {
	public static void main(String[] args) {
		if(args.length < 3) {
			System.out.println("Argumentos invalidos.");
			System.out.println("O uso correto é <caminho do arquivo> <caminho do json> <nome do modulo>");
			System.exit(1);
		}
		
		try {
			String fileName = args[0];
			String jsonName = args[1];
			String moduleName = args[2];
			JsonObject completeBundleJson = getJsonFromFile(jsonName, moduleName);
			JsonObject moduleBundleJson = completeBundleJson.getAsJsonObject(moduleName).getAsJsonObject("page");
			List<String> newFile = processFilesAskingNewKeyBundles(fileName, moduleBundleJson);
			completeBundleJson.getAsJsonObject(moduleName).add("page", moduleBundleJson);
			writeNewFiles(completeBundleJson, newFile, fileName, jsonName);
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private static List<String> processFilesAskingNewKeyBundles(String fileName, JsonObject bundleJson) throws FileNotFoundException,
			IOException {
		FileReader fileReader;
		BufferedReader bufferedReader;

		String line = null;
		Scanner reader = new Scanner(System.in);

		List<String> newFile = new ArrayList<>();

		fileReader = new FileReader(fileName);
		bufferedReader = new BufferedReader(fileReader);

		Map<String, String> alreadyPastStrings = new HashMap<String, String>();
		Boolean aberto = false;

		while ((line = bufferedReader.readLine()) != null) {
			aberto = processLine(bundleJson, line, reader, newFile, alreadyPastStrings, aberto);
		}

		reader.close();
		bufferedReader.close();
		return newFile;
	}

	private static Boolean processLine(JsonObject bundleJson, String line, Scanner reader, List<String> newFile,
			Map<String, String> alreadyPastStrings, Boolean aberto) {
		String myLine = "";
		Integer beggin = null;
		Integer last = null;
		for (int i = 0; i < line.length(); i++) {
			char a = line.charAt(i);
			if (a == '<')
				aberto = true;
			else if (a == '>')
				aberto = false;
			else if (!aberto && a != '\t') {
				myLine += a;

				if (beggin == null) {
					beggin = i;
				}
				last = i;
			}
		}
		myLine = myLine.trim();
		if (myLine.length() > 0 && !myLine.startsWith("{{")) {
			String replacement = getKeyToValue(reader, alreadyPastStrings, myLine);
			line = replaceValueForKey(bundleJson, line, myLine, beggin, last, replacement);
		}

		newFile.add(line);
		return aberto;
	}

	private static String replaceValueForKey(JsonObject bundleJson, String line, String myLine, Integer beggin, Integer last,
			String replacement) {
		if (!replacement.equals("")) {
			line = line.substring(0, beggin - 1) + " data-bundle=\"" + replacement + "\">" + line.substring(last + 1);
			if (!bundleJson.has(replacement)) {
				if (replacement.contains(".")) {
					bundleJson.get("field").getAsJsonObject().addProperty(replacement.substring(replacement.lastIndexOf(".") + 1), myLine);
				} else {
					bundleJson.addProperty(replacement, myLine);
				}
			}
		}
		return line;
	}

	private static String getKeyToValue(Scanner reader, Map<String, String> alreadyPastStrings, String myLine) {
		String replacement = "";
		if (alreadyPastStrings.containsKey(myLine)) {
			replacement = alreadyPastStrings.get(myLine);
		} else {
			System.out.print("Key for value \"" + myLine + "\" : ");
			replacement = reader.nextLine();
			alreadyPastStrings.put(myLine, replacement);
		}
		return replacement;
	}

	private static void writeNewFiles(JsonObject bundleJson, List<String> newFile, String fileName, String fileJson) throws IOException {
		FileWriter fileWriter = new FileWriter(fileName);
		FileWriter fileWriterJson = new FileWriter(fileJson);

		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		BufferedWriter bufferedWriterJson = new BufferedWriter(fileWriterJson);

		for (String asd : newFile) {
			bufferedWriter.write(asd + "\n");
		}
		bufferedWriterJson.write(bundleJson.toString());

		bufferedWriter.close();
		bufferedWriterJson.close();
	}

	private static JsonObject getJsonFromFile(String jsonName, String moduleName) throws FileNotFoundException, IOException {
		JsonObject ko = null;
		List<String> jsonString = Files.readAllLines(Paths.get(jsonName), Charset.defaultCharset());
		String hue = "";
		for (String concat : jsonString) {
			hue += concat;
		}
		ko = new JsonParser().parse(hue).getAsJsonObject();
		return ko;
	}
}
