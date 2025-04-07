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
package fr.univamu.ism.docometre.analyse.matlabeditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.NumberRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.SWT;

import fr.univamu.ism.docometre.DocometreApplication;
import fr.univamu.ism.docometre.ThemeColors;

public final class MatlabCodeScanner extends RuleBasedScanner {
	
	public static String[] FUNCTIONS = {
	        "abs", "acos", "acosh", "acot" ,"acoth", "acsc", "acsch",
	        "airfoil" ,"all" ,"andrew" ,"angle", "angle" ,"ans" ,"any", "arith",
	        "asec", "asech", "asin", "asinh", "atan", "atan2", "atanh", "auread",
	        "auwrite", "axes", "axis", "balance", "bar","bartlett", "bench", "bessel",
	        "bessela", "besselap", "besself", "besseli", "besselj", "besselk", "bessely", "beta",
	        "betacore", "betainc", "betaln", "bilinear", "blackman", "blanks", "blt", "bone",
	        "boxcar", "brighten", "bucky", "buttap", "butter", "buttonv", "buttord", 
	        "cart2pol", "cart2sph", "caxis", "cceps", "cd", "cdf2rdf", "cedit", "ceil",
	        "census", "censusex", "cheb1ap", "cheb1ord", "cheb2ap", "cheb2ord", "chebwin", 
	        "cheby1", "cheby2", "choices", "choicex", "chol", "cinvert", "cla", "clabel", "clc",
	        "clear", "clf", "clg", "clock", "close", "cohere", "colmmd", "colon",
	        "colormap", "colormenu", "colperm", "colstyle", "comet", "comet3", "compan", "compass",
	        "computer", "cond", "condest", "conj", "contour", "contour3", "contourc", "contrast",
	        "conv", "conv2", "conv2", "conv2", "convmtx", "cool", "copper", "corrcoef",
	        "corrcoef", "cos", "cosh", "cot", "coth", "cov", "cov", "cplxdemo",
	        "cplxgrid", "cplxmap", "cplxpair", "cplxpair", "cplxroot", "cputime","cross","csc",
	        "csch", "csd","cumprod", "cumsum", "cylinder", "czt", "cztdemo",
	        "datalist", "date", "dbclear", "dbcont", "dbdown", "dbquit", "dbstack", "dbstatus",
	        "dbstep", "dbstop", "dbtype", "dbup", "dc2sc", "dct", "deblank", "debug",
	        "dec2hex", "decimate", "deconv", "deconv", "del2", "delete", "delsq", "delsqdemo",
	        "delsqshow", "demo", "demod", "det", "detrend", "dftmtx", "diag", "diary",
	        "diff", "dir", "diric", "disp", "dmperm", "dos", "drawnow", "earthex",
	        "earthmap", "echo", "eig", "eigmovie", "ellip", "ellipap", "ellipj", "ellipk",
	        "ellipke", "ellipord", "eps", "erf", "erfc",
	        "erfcore", "erfcx", "erfinv", "error", "errorbar", "etime", "etree", "etreeplot",
	        "eval", "exist", "exp", "expm", "expm1", "expm2", "expm3", "eye",
	        "fclose", "feather", "feof", "ferror", "feval", "fft", "fft", "fft2",
	        "fft2", "fftdemo", "fftfilt", "fftshift", "fftshift", "fftshift", "fgetl", "fgets",
	        "figtext", "figure", "fill", "fill3", "filtdemo", "filter", "filter", "filter2",
	        "filtfilt", "filtic", "find", "findstr", "finite", "fir1", "fir2", "firls",
	        "fitdemo", "fitfun", "fix", "flag", "fliplr", "flipud", "floor", "flops",
	        "fmin", "fmins", "fopen", "foptions", "format", "fourier2", "fplot",
	        "fplotdemo", "fprintf", "fread", "freqs", "freqspace", "freqz", "frewind", "fscanf",
	        "fseek", "ftell", "full", "funm", "fwrite", "fzero", "gallery",
	        "gamma", "gammainc", "gammaln", "gca", "gcd", "gcf", "get", "getenv",
	        "getframe", "ginput", "gplot", "gradient", "gray", "graymon", "grid",
	        "griddata", "grpdelay", "gtext", "hadamard", "hamming", "hankel", "hanning", "hardcopy",
	        "help", "hess", "hex2dec", "hex2num", "hidden", "highlight", "hilb", "hilbert",
	        "hint", "hist", "hold", "home", "hostid", "hot", "hsv", "hsv2rgb", 
	        "humps", "icubic", "idct", "ident", "ifft", "ifft",
	        "ifft2", "ifft2", "iffuse", "imag", "image", "imagedemo", "imageext", "imagesc",
	        "impinvar", "impz", "imread", "imtext", "imwrite", "inf", "info", "input",
	        "inquire", "int2str", "interp", "interp1", "interp1", "interp2", "interp3", "interp4",
	        "interp5", "interpft", "intfilt", "intro", "inv", "inverf", "invfreqs", "invfreqz",
	        "invhilb", "isempty", "isglobal", "ishold", "isieee", "isinf", "isletter", "isnan",
	        "issparse", "isstr", "isunix", "jet", "kaiser", "keyboard", "knot",
	        "kron", "lalala", "lasterr", "lcm", "legend", "length", "levinson", "life", "lifeloop2",
	        "lin2mu", "line", "linspace", "load", "loadwave", "log", "log10", "log2",
	        "loglog", "logm", "logspace", "lookfor", "lorenz", "lorenzeq", "lotka", "lower",
	        "lp2bp", "lp2bs", "lp2hp", "lp2lp", "lpc", "ls", "lscov", "lu",
	        "magic","man", "mathlist", "matlabro", "max", "mean", "medfilt1", "median",
	        "membrane", "memory", "menu", "mesh", "meshc", "meshdom", "meshgrid", "meshz",
	        "meta", "min", "mkpp", "mmove2", "moddemo", "modulate", "more", "movie",
	        "moviein", "mu2lin", "nalist", "nan", "nargchk", "nargin", "nargout", "nestdiss",
	        "nested", "newplot", "nextpow2", "nnls", "nnz", "nonzeros", "norm", "normest",
	        "null", "num2str", "numgrid", "nzmax", "ode23", "ode23p", "ode45", "odedemo",
	        "ones", "orient", "orth", "pack", "paren", "pascal", "patch", "path",
	        "pause", "pcolor", "peaks", "penny", "pi", "pink", "pinv", "planerot",
	        "plot", "plot3", "pol2cart", "polar", "poly", "poly2rc", "polyder", "polyfit",
	        "polyline", "polymark", "polystab", "polyval", "polyvalm", "pow2", "ppval", "print",
	        "printopt", "prism", "prod", "prony", "psd", "punct", "puzzle", "pwd",
	        "qr", "qrdelete", "qrinsert", "quad", "quad8", "quad8stp", "quaddemo", "quadstp",
	        "quake", "quit", "quiver", "qz", "rand", "randn", "randperm", "rank",
	        "rat", "rats", "rbbox", "rc2poly", "rceps", "rcond", "readme", "real",
	        "realmax", "realmin", "relop2", "rem", "remez", "remezord", "resample", "reset",
	        "reshape", "resi2", "residue", "residuez", "rgb2hsv", "rgbplot", "rjr",
	        "roots", "rose", "rosser", "rot90", "round", "rref", "rrefmovie", "rsf2csf",
	        "save", "savewave", "sawtooth", "saxis", "sc2dc", "schur", "script", "sec",
	        "sech", "semilogx", "semilogy", "sepdemo", "sepplot", "set", "setstr", "shading",
	        "shg", "showwind", "sig1help","sig2help", "sigdemo1", "sigdemo2", "sign","sin",
	        "sinc", "sinh", "size", "slash", "slice", "sort", "sos2ss", "sos2tf",
	        "sos2zp", "sound", "sounddemo", "soundext", "spalloc", "sparlist", "sparse", "sparsfun",
	        "sparsity", "spaugment", "spconvert", "spdiags", "specgram", "specials", "spectrum", "specular",
	        "speye", "spfun", "sph2cart", "sphere", "spinmap", "spiral", "spline", "spline",
	        "spline2d", "spones", "spparms", "sprandn", "sprandsym", "sprank", "sprintf", "spy",
	        "spypart", "sqdemo2", "sqrt", "sqrtm", "square", "ss2sos", "ss2tf", "ss2zp",
	        "sscanf", "stairs", "std", "stem", "stem", "stmcb", "str2mat", "str2num",
	        "strcmp", "strings", "strips", "subplot", "subscribe", "subspace", "sum", "sunspots",
	        "superquad", "surf", "surface", "surfc", "surfl", "surfnorm", "svd", "swapprev",
	        "symbfact", "symmmd", "symrcm", "table1", "table2", "tan", "tanh", "tempdir",
	        "tempname", "terminal", "text", "tf2ss", "tf2zp", "tfe", "tffunc", "tic",
	        "title", "toc", "toeplitz", "trace", "trapz", "treelayout", "treeplot", "triang",
	        "tril", "triu", "type", "uicontrol", "uigetfile", "uimenu", "uiputfile", "uisetcolor",
	        "uisetfont", "unix", "unmesh", "unmkpp", "unwrap", "unwrap", "upper", "vander",
	        "vco", "ver", "version", "vibes", "view", "viewmtx", "waterfall", "what",
	        "whatsnew", "which", "white", "whitebg", "who", "whos", "why",
	        "wilkinson", "xcorr", "xcorr2", "xcov", "xlabel", "xor", "xyzchk", "ylabel",
	        "yulewalk", "zerodemo", "zeros", "zlabel", "zp2sos", "zp2ss", "zp2tf", "zplane" 
	    };
	
	public static String[] RESERVED_WORDS = {"break", "case", "catch", "continue", "else", "elseif", "end", "for", "function", "global", "if", "otherwise", "persistent", "return", "switch", "try", "while"};
	
	private static class MatlabWordDetector implements IWordDetector {
		@Override
		public boolean isWordStart(char c) {
			return Character.isLetter(c);
		}
		@Override
		public boolean isWordPart(char c) {
			return Character.isLetter(c);
		}
	}
	
	private static class MyNumberRule extends NumberRule {
		
		private char[] operators = new char[] {'=', '+' , '*', '/', '%', '-', '(', ':'};
		
		public MyNumberRule(IToken token) {
			super(token);
		}
		
		@Override
		public IToken evaluate(ICharacterScanner scanner) {
			int c = scanner.read();
			if (Character.isDigit((char)c)) {
				scanner.unread();
				scanner.unread();
				c = scanner.read();
				if(!isOperator((char) c)) return Token.UNDEFINED;
			} else scanner.unread();
			return super.evaluate(scanner);
		}

		private boolean isOperator(char c) {
			boolean isOperator = Character.isWhitespace(c);
			for (char operaror : operators) {
				isOperator = isOperator || c == operaror;
			}
			return isOperator;
		}
	}
	
	public static RuleBasedScanner getMatlabCodeScanner() {
		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
		List<IRule> rules = new ArrayList<IRule>();
		
		rules.addAll(getMatlabScanner());
		rules.addAll(getNumbersScanner());
		
		ruleBasedScanner.setRules(rules.toArray(new IRule[rules.size()]));
		return ruleBasedScanner;
	}
	
	
	private static List<IRule> getMatlabScanner() {
		List<IRule> rules= new ArrayList<IRule>();
        
        WordRule wordRule = new WordRule(new MatlabWordDetector(), geDefaultTextAttributeToken());
        
        for (int i = 0; i < FUNCTIONS.length; i++) {
        	wordRule.addWord(FUNCTIONS[i], getFunctionsTextAttributeToken());
		}
        
        for (int i = 0; i < RESERVED_WORDS.length; i++) {
        	wordRule.addWord(RESERVED_WORDS[i], getReservedWordsTextAttributeToken());
		}

        
        rules.add(wordRule);
        return rules;
	}
	
	public static RuleBasedScanner  getCommentScanner() {
		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
		List<IRule> rules= new ArrayList<IRule>();
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.GREEN), 
													ThemeColors.getBackgroundColor(), 
													SWT.NORMAL, 
													DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
		IRule commentRule1 = new EndOfLineRule("%", token);
		IRule commentRule2 = new EndOfLineRule("%%", token);
        rules.add(commentRule1);
        rules.add(commentRule2);
		
        ruleBasedScanner.setRules(rules.toArray(new IRule[rules.size()]));
		return ruleBasedScanner;
	}
	
	private static List<IRule>  getNumbersScanner() {
		List<IRule> rules= new ArrayList<IRule>();
		
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.BLACK), null, SWT.BOLD);
		
	    IToken numberToken = new Token(attribute);
	    
	    FloatingPointNumberRule floatingPointNumberRule = new FloatingPointNumberRule(numberToken);
        rules.add(floatingPointNumberRule);

	    MyNumberRule numberRule = new MyNumberRule(numberToken);
        rules.add(numberRule);
	    
        return rules;
	}
	
	private static IToken getFunctionsTextAttributeToken() {
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.BLUE),
				ThemeColors.getBackgroundColor(), SWT.NORMAL,
				DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
		return token;
	}

	private static IToken getReservedWordsTextAttributeToken() {
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.ORANGE),
				ThemeColors.getBackgroundColor(), SWT.NORMAL,
				DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
		return token;
	}
	
	private static IToken geDefaultTextAttributeToken() {
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.BLACK));
		IToken token = new Token(attribute);
		return token;
	}
	
}
