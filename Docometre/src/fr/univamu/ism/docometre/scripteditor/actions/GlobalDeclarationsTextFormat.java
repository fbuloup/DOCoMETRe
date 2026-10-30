/*******************************************************************************
 * Copyright or © or Copr. Institut des Sciences du Mouvement 
 * (CNRS & Aix Marseille Université)
 * 
 * The DOCoMETER Software must be used with a real time data acquisition 
 * system marketed by ADwin (ADwin Pro and Gold, I and II) or an Arduino 
 * Uno. This software, created within the Institute of Movement Sciences, 
 * has been developed to facilitate their use by a "neophyte" public in the 
 * fields of industrial computing and electronics.  Students, researchers or 
 * engineers can configure this acquisition system in the best possible 
 * conditions so that it best meets their experimental needs. 
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 * 
 * Contributors:
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [25/03/2020]
 ******************************************************************************/
package fr.univamu.ism.docometre.scripteditor.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.univamu.ism.process.GlobalDeclarations.Constant;
import fr.univamu.ism.process.GlobalDeclarations.Content;
import fr.univamu.ism.process.GlobalDeclarations.Library;
import fr.univamu.ism.process.GlobalDeclarations.Subroutine;

/**
 * Conversion between global declarations and source code text, in ADBasic (ADwin) or C (Arduino) :
 * - libraries : #INCLUDE file ' comment, or #include <file> // comment
 * - constants : #DEFINE NAME value ' comment, or #define NAME value // comment
 * - subroutines : SUB name(parameters) ... ENDSUB, or void name(parameters) { ... }
 * Text is used to exchange global declarations with external source code through the system clipboard.
 */
public final class GlobalDeclarationsTextFormat {
	
	/**
	 * Declarations found in a text and text lines that could not be used
	 */
	public static final class ParseResult {
		
		private Content content;
		private List<String> ignoredLines;
		
		private ParseResult(Content content, List<String> ignoredLines) {
			this.content = content;
			this.ignoredLines = ignoredLines;
		}
		
		public Content getContent() {
			return content;
		}
		
		public List<String> getIgnoredLines() {
			return ignoredLines;
		}
		
	}
	
	private static final Pattern ADBASIC_INCLUDE = Pattern.compile("^\\s*#include\\s+([^']+?)\\s*('.*)?$", Pattern.CASE_INSENSITIVE);
	private static final Pattern ADBASIC_DEFINE = Pattern.compile("^\\s*#define\\s+(\\S+)\\s*(.*)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern ADBASIC_SUB = Pattern.compile("^\\s*sub\\s+([A-Za-z_]\\w*)\\s*(?:\\((.*)\\))?\\s*(?:'.*)?$", Pattern.CASE_INSENSITIVE);
	private static final Pattern ADBASIC_END_SUB = Pattern.compile("^\\s*end\\s*sub\\b.*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern ADBASIC_FUNCTION = Pattern.compile("^\\s*function\\s+([A-Za-z_]\\w*).*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern ADBASIC_END_FUNCTION = Pattern.compile("^\\s*end\\s*function\\b.*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern ADBASIC_COMMENT = Pattern.compile("^\\s*('|rem\\b).*$", Pattern.CASE_INSENSITIVE);
	
	private static final Pattern C_INCLUDE = Pattern.compile("^\\s*#\\s*include\\s*(<[^>]*>|\"[^\"]*\")\\s*(.*)$");
	private static final Pattern C_DEFINE = Pattern.compile("^\\s*#\\s*define\\s+(\\S+)\\s*(.*)$");
	private static final Pattern C_FUNCTION = Pattern.compile("^\\s*((?:[A-Za-z_]\\w*[\\s\\*&]+)+?)([A-Za-z_]\\w*)\\s*\\(([^)]*)\\)\\s*(\\{.*|;.*)?$");
	private static final List<String> C_KEYWORDS = List.of("if", "for", "while", "switch", "return", "else", "do", "case");
	
	private static final int MAX_IGNORED_LINE_LENGTH = 50;
	
	private GlobalDeclarationsTextFormat() {
	}
	
	/**
	 * @return source code of libraries, constants and subroutines
	 */
	public static String format(Content content, boolean adbasic) {
		StringBuilder text = new StringBuilder();
		for (Library library : content.getLibraries()) {
			text.append(adbasic ? "#INCLUDE " : "#include ").append(library.getFile());
			if(!library.getComment().isEmpty()) text.append(adbasic ? "   ' " : "   // ").append(library.getComment());
			text.append("\n");
		}
		if(!content.getLibraries().isEmpty() && !content.getConstants().isEmpty()) text.append("\n");
		for (Constant constant : content.getConstants()) {
			text.append(adbasic ? "#DEFINE " : "#define ").append(constant.getName()).append(" ").append(constant.getValue());
			if(!constant.getComment().isEmpty()) text.append(adbasic ? "   ' " : "   // ").append(constant.getComment());
			text.append("\n");
		}
		for (Subroutine subroutine : content.getSubroutines()) {
			if(text.length() > 0) text.append("\n");
			if(adbasic) text.append("SUB ").append(subroutine.getName()).append("(").append(subroutine.getParameters()).append(")\n");
			else text.append("void ").append(subroutine.getName()).append("(").append(subroutine.getParameters()).append(") {\n");
			String body = subroutine.getBody().stripTrailing();
			if(!body.isEmpty()) for (String line : body.split("\\R")) text.append("\t").append(line).append("\n");
			text.append(adbasic ? "ENDSUB\n" : "}\n");
		}
		return text.toString();
	}
	
	/**
	 * Quick check before parsing : text may contain libraries, constants or subroutines
	 */
	public static boolean mayContainDeclarations(String text) {
		if(text == null) return false;
		String lowerCaseText = text.toLowerCase();
		return lowerCaseText.contains("include") || lowerCaseText.contains("define") || lowerCaseText.contains("sub") || lowerCaseText.contains("void");
	}
	
	/**
	 * Find libraries, constants and subroutines in source code. Comment lines just before a subroutine are kept at the beginning of its body.
	 */
	public static ParseResult parse(String text, boolean adbasic) {
		return adbasic ? parseADBasic(text) : parseC(text);
	}
	
	private static ParseResult parseADBasic(String text) {
		List<Library> libraries = new ArrayList<>();
		List<Constant> constants = new ArrayList<>();
		List<Subroutine> subroutines = new ArrayList<>();
		List<String> ignoredLines = new ArrayList<>();
		List<String> comments = new ArrayList<>();
		String[] lines = text.split("\\R");
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if(line.isBlank()) {
				comments.clear();
				continue;
			}
			if(ADBASIC_COMMENT.matcher(line).matches()) {
				comments.add(line.trim());
				continue;
			}
			Matcher matcher = ADBASIC_INCLUDE.matcher(line);
			if(matcher.matches()) {
				String comment = matcher.group(2) == null ? "" : matcher.group(2).substring(1).trim();
				libraries.add(new Library(matcher.group(1), comment));
				comments.clear();
				continue;
			}
			matcher = ADBASIC_DEFINE.matcher(line);
			if(matcher.matches()) {
				String[] valueAndComment = splitComment(matcher.group(2), "'", null);
				constants.add(new Constant(matcher.group(1), valueAndComment[0], valueAndComment[1]));
				comments.clear();
				continue;
			}
			matcher = ADBASIC_SUB.matcher(line);
			if(matcher.matches()) {
				int end = findLine(lines, i + 1, ADBASIC_END_SUB);
				if(end == -1) {
					ignoredLines.add(shorten(line) + " (ENDSUB ?)");
				} else {
					String parameters = matcher.group(2) == null ? "" : matcher.group(2).trim();
					subroutines.add(new Subroutine(matcher.group(1), parameters, getBody(comments, lines, i + 1, end)));
					i = end;
				}
				comments.clear();
				continue;
			}
			matcher = ADBASIC_FUNCTION.matcher(line);
			if(matcher.matches()) {
				int end = findLine(lines, i + 1, ADBASIC_END_FUNCTION);
				ignoredLines.add(shorten(line));
				if(end != -1) i = end;
				comments.clear();
				continue;
			}
			ignoredLines.add(shorten(line));
			comments.clear();
		}
		return new ParseResult(new Content(libraries, constants, subroutines), ignoredLines);
	}
	
	private static ParseResult parseC(String text) {
		List<Library> libraries = new ArrayList<>();
		List<Constant> constants = new ArrayList<>();
		List<Subroutine> subroutines = new ArrayList<>();
		List<String> ignoredLines = new ArrayList<>();
		List<String> comments = new ArrayList<>();
		String[] lines = text.split("\\R");
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			String trimmedLine = line.trim();
			if(trimmedLine.isEmpty()) {
				comments.clear();
				continue;
			}
			if(trimmedLine.startsWith("//")) {
				comments.add(trimmedLine);
				continue;
			}
			if(trimmedLine.startsWith("/*")) {
				int end = trimmedLine.contains("*/") ? i : findLine(lines, i + 1, Pattern.compile(".*\\*/.*"));
				if(end == -1) end = lines.length - 1;
				for (int j = i; j <= end; j++) comments.add(lines[j].trim());
				i = end;
				continue;
			}
			Matcher matcher = C_INCLUDE.matcher(line);
			if(matcher.matches()) {
				libraries.add(new Library(matcher.group(1), splitComment(matcher.group(2), "//", "/*")[1]));
				comments.clear();
				continue;
			}
			matcher = C_DEFINE.matcher(line);
			if(matcher.matches()) {
				if(matcher.group(1).contains("(")) ignoredLines.add(shorten(line));
				else {
					String[] valueAndComment = splitComment(matcher.group(2), "//", "/*");
					constants.add(new Constant(matcher.group(1), valueAndComment[0], valueAndComment[1]));
				}
				comments.clear();
				continue;
			}
			matcher = C_FUNCTION.matcher(line);
			if(matcher.matches() && !C_KEYWORDS.contains(matcher.group(2))) {
				String returnType = matcher.group(1).trim();
				String end = matcher.group(4);
				// Prototype : nothing to do, prototypes are generated by Arduino builder
				if(end != null && end.startsWith(";")) {
					comments.clear();
					continue;
				}
				// Opening brace on this line or on next non blank line
				int braceLine = i;
				if(end == null) {
					braceLine = i + 1;
					while(braceLine < lines.length && lines[braceLine].isBlank()) braceLine++;
					if(braceLine >= lines.length || !lines[braceLine].trim().startsWith("{")) {
						ignoredLines.add(shorten(line));
						comments.clear();
						continue;
					}
				}
				int[] closingBrace = findClosingBrace(lines, braceLine, lines[braceLine].indexOf('{'));
				if(closingBrace == null) {
					ignoredLines.add(shorten(line) + " (} ?)");
					comments.clear();
					continue;
				}
				if(!"void".equals(returnType)) ignoredLines.add(shorten(line));
				else {
					List<String> bodyLines = new ArrayList<>();
					String firstLine = lines[braceLine].substring(lines[braceLine].indexOf('{') + 1);
					if(closingBrace[0] == braceLine) bodyLines.add(firstLine.substring(0, closingBrace[1] - lines[braceLine].indexOf('{') - 1));
					else {
						bodyLines.add(firstLine);
						for (int j = braceLine + 1; j < closingBrace[0]; j++) bodyLines.add(lines[j]);
						bodyLines.add(lines[closingBrace[0]].substring(0, closingBrace[1]));
					}
					subroutines.add(new Subroutine(matcher.group(2), matcher.group(3).trim(), getBody(comments, bodyLines.toArray(new String[0]), 0, bodyLines.size())));
				}
				i = closingBrace[0];
				comments.clear();
				continue;
			}
			ignoredLines.add(shorten(line));
			comments.clear();
		}
		return new ParseResult(new Content(libraries, constants, subroutines), ignoredLines);
	}
	
	/*
	 * Position {line, column} of the brace closing the one at position {line, column}, ignoring braces in comments and strings
	 */
	private static int[] findClosingBrace(String[] lines, int line, int column) {
		int depth = 0;
		boolean inBlockComment = false;
		for (int i = line; i < lines.length; i++) {
			String currentLine = lines[i];
			for (int j = (i == line ? column : 0); j < currentLine.length(); j++) {
				char c = currentLine.charAt(j);
				if(inBlockComment) {
					if(c == '*' && j + 1 < currentLine.length() && currentLine.charAt(j + 1) == '/') {
						inBlockComment = false;
						j++;
					}
					continue;
				}
				if(c == '/' && j + 1 < currentLine.length() && currentLine.charAt(j + 1) == '/') break;
				if(c == '/' && j + 1 < currentLine.length() && currentLine.charAt(j + 1) == '*') {
					inBlockComment = true;
					j++;
					continue;
				}
				if(c == '"' || c == '\'') {
					j++;
					while(j < currentLine.length() && currentLine.charAt(j) != c) {
						if(currentLine.charAt(j) == '\\') j++;
						j++;
					}
					continue;
				}
				if(c == '{') depth++;
				if(c == '}') {
					depth--;
					if(depth == 0) return new int[] {i, j};
				}
			}
		}
		return null;
	}
	
	private static int findLine(String[] lines, int from, Pattern pattern) {
		for (int i = from; i < lines.length; i++) if(pattern.matcher(lines[i]).matches()) return i;
		return -1;
	}
	
	/*
	 * Comments followed by lines [from, to[ without their common indentation and without leading and trailing blank lines
	 */
	private static String getBody(List<String> comments, String[] lines, int from, int to) {
		List<String> bodyLines = new ArrayList<>();
		for (int i = from; i < to; i++) bodyLines.add(lines[i].stripTrailing());
		while(!bodyLines.isEmpty() && bodyLines.get(0).isBlank()) bodyLines.remove(0);
		while(!bodyLines.isEmpty() && bodyLines.get(bodyLines.size() - 1).isBlank()) bodyLines.remove(bodyLines.size() - 1);
		int indentation = Integer.MAX_VALUE;
		for (String line : bodyLines) if(!line.isBlank()) indentation = Math.min(indentation, line.length() - line.stripLeading().length());
		List<String> result = new ArrayList<>(comments);
		for (String line : bodyLines) result.add(line.isBlank() ? "" : line.substring(indentation));
		return String.join("\n", result);
	}
	
	/*
	 * {value, comment} : comment starts with commentStart (or alternativeCommentStart)
	 */
	private static String[] splitComment(String text, String commentStart, String alternativeCommentStart) {
		int index = text.indexOf(commentStart);
		int length = commentStart.length();
		if(alternativeCommentStart != null) {
			int alternativeIndex = text.indexOf(alternativeCommentStart);
			if(alternativeIndex > -1 && (index == -1 || alternativeIndex < index)) {
				index = alternativeIndex;
				length = alternativeCommentStart.length();
			}
		}
		if(index == -1) return new String[] {text.trim(), ""};
		String comment = text.substring(index + length).trim();
		if(comment.endsWith("*/")) comment = comment.substring(0, comment.length() - 2).trim();
		return new String[] {text.substring(0, index).trim(), comment};
	}
	
	private static String shorten(String line) {
		String trimmedLine = line.trim();
		return trimmedLine.length() > MAX_IGNORED_LINE_LENGTH ? trimmedLine.substring(0, MAX_IGNORED_LINE_LENGTH) + "..." : trimmedLine;
	}

}
