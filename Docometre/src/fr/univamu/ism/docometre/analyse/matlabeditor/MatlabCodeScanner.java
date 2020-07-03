package fr.univamu.ism.docometre.analyse.matlabeditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.SWT;

import fr.univamu.ism.docometre.DocometreApplication;
import fr.univamu.ism.docometre.editors.WordsDetector;

public final class MatlabCodeScanner extends RuleBasedScanner {
	
//	private static String[] IMPORT = {"IMPORT"};
//	private static String[] INCLUDE = {"#INCLUDE"};
//	private static String[] DEFINE = {"#DEFINE", "FPAR_", "DATA_"};
//	private static String[] DECLARE = {"DIM", "AS", "LONG", "FLOAT", "SHORT", "INTEGER", "STRING"};
//	private static String[] SEGMENTS = {"LOWINIT:", "INIT:", "EVENT:", "FINISH:"};
//	private static String[] RESERVED_WORDS = {"for", "FIFO_CLEAR", "FIFO_EMPTY", "IF", "ELSE", "ENDIF", "THEN", "INC", "GLOBAL_DELAY", "DO", "UNTIL", "FOR", "TO", "NEXT", "SHIFT_RIGHT", "OR", "AND"};
	
	static public String[] FUNCTIONS = {
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
	
	static public String[] RESERVED_WORDS = {"break", "case", "catch", "continue", "else", "elseif", "end", "for", "function", "global", "if", "otherwise", "persistent", "return", "switch", "try", "while"};
	
	
//		 // Add rule for whitespace
//	    rules.add(new WhitespaceRule(new IWhitespaceDetector() {
//	      public boolean isWhitespace(char c) {
//	        return Character.isWhitespace(c);
//	      }
//	    }));
		
	
//	public static RuleBasedScanner getSegmentScanner() {
//		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
//		List<IRule> rules= new ArrayList<IRule>();
//		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.RED), 
//				   									DocometreApplication.getColor(DocometreApplication.WHITE), 
//				   									SWT.NORMAL, 
//				   									DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
//		IToken token = new Token(attribute);
//        WordRule wordRule = new WordRule(new WordsDetector(SEGMENTS), token);
//        rules.add(wordRule);
//        ruleBasedScanner.setRules(rules.toArray(new IRule[rules.size()]));
//        return ruleBasedScanner;
//	}
	
//	public static RuleBasedScanner getDefineScanner() {
//		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
//		List<IRule> rules= new ArrayList<IRule>();
//		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.BLUE),
//				 									DocometreApplication.getColor(DocometreApplication.WHITE), 
//				 									SWT.NORMAL,
//				 									DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
//		IToken token = new Token(attribute);
//        WordRule wordRule = new WordRule(new WordsDetector(DEFINE), token);
//        rules.add(wordRule);
//        IRule[] commentRules = getCommentRules();
//        for (int i = 0; i < commentRules.length; i++) rules.add(commentRules[i]);
//        NumberRule numberRule = new NumberRule(token);
//	    rules.add(numberRule);
//        ruleBasedScanner.setRules(rules.toArray(new IRule[rules.size()]));
//        return ruleBasedScanner;
//	}
//	
//	public static RuleBasedScanner getImportScanner() {
//		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
//		List<IRule> rules= new ArrayList<IRule>();
//		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.BLUE),
//				 									DocometreApplication.getColor(DocometreApplication.WHITE), 
//				 									SWT.NORMAL,
//				 									DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
//		IToken token = new Token(attribute);
//        WordRule wordRule = new WordRule(new WordsDetector(IMPORT), token);
//        rules.add(wordRule);
//        IRule[] commentRules = getCommentRules();
//        for (int i = 0; i < commentRules.length; i++) rules.add(commentRules[i]);
//        ruleBasedScanner.setRules(rules.toArray(new IRule[rules.size()]));
//        return ruleBasedScanner;
//	}
//	
//	public static RuleBasedScanner getIncludeScanner() {
//		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
//		List<IRule> rules= new ArrayList<IRule>();
//		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.BLUE),
//				 									DocometreApplication.getColor(DocometreApplication.WHITE), 
//				 									SWT.NORMAL,
//				 									DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
//		IToken token = new Token(attribute);
//        WordRule wordRule = new WordRule(new WordsDetector(INCLUDE), token);
//        rules.add(wordRule);
//        IRule[] commentRules = getCommentRules();
//        for (int i = 0; i < commentRules.length; i++) rules.add(commentRules[i]);
//        ruleBasedScanner.setRules(rules.toArray(new IRule[rules.size()]));
//        return ruleBasedScanner;
//	}
	
	public static RuleBasedScanner getFunctionsScanner() {
		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
		List<IRule> rules= new ArrayList<IRule>();
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.BLUE),
				 									DocometreApplication.getColor(DocometreApplication.WHITE), 
				 									SWT.NORMAL,
				 									DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
        WordRule wordRule = new WordRule(new WordsDetector(FUNCTIONS), token);
        rules.add(wordRule);
        IRule[] commentRules = getCommentRules();
        for (int i = 0; i < commentRules.length; i++) rules.add(commentRules[i]);
        ruleBasedScanner.setRules(rules.toArray(new IRule[rules.size()]));
        return ruleBasedScanner;
	}
	
	private static IRule[] getCommentRules() {
		List<IRule> rules= new ArrayList<IRule>();
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.GREEN), 
													DocometreApplication.getColor(DocometreApplication.WHITE), 
													SWT.NORMAL, 
													DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
		IRule commentRule1 = new EndOfLineRule("%", token);
		IRule commentRule2 = new EndOfLineRule("%%", token);
        rules.add(commentRule1);
        rules.add(commentRule2);
        return rules.toArray(new IRule[rules.size()]);
	}
	
	public static RuleBasedScanner getCommentScanner() {
		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
        ruleBasedScanner.setRules(getCommentRules());
        return ruleBasedScanner;
	}
	
//	public static RuleBasedScanner getDeclareScanner() {
//		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
//		List<IRule> rules= new ArrayList<IRule>();
//		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.ORANGE),
//				 									DocometreApplication.getColor(DocometreApplication.WHITE), 
//				 									SWT.NORMAL,
//				 									DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
//		IToken token = new Token(attribute);
//        WordRule wordRule = new WordRule(new WordsDetector(DECLARE), token);
//        rules.add(wordRule);
//        IRule[] commentRules = getCommentRules();
//        for (int i = 0; i < commentRules.length; i++) rules.add(commentRules[i]);
////        rules.add(getWhiteSpaceRule());
//        ruleBasedScanner.setRules(rules.toArray(new IRule[rules.size()]));
//        return ruleBasedScanner;
//	}
	
//	private static WhitespaceRule getWhiteSpaceRule() {
//		WhitespaceRule whitespaceRule = new WhitespaceRule(new IWhitespaceDetector() {
//			@Override
//			public boolean isWhitespace(char c) {
//				return Character.isWhitespace(c);
//			}
//		});
//	    return whitespaceRule;
//	}
	
	public static RuleBasedScanner getReservedWordsScanner() {
		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
		List<IRule> rules= new ArrayList<IRule>();
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.ORANGE),
				 									DocometreApplication.getColor(DocometreApplication.WHITE), 
				 									SWT.NORMAL,
				 									DocometreApplication.getFont(DocometreApplication.COURIER_NEW));
		IToken token = new Token(attribute);
        WordRule wordRule = new WordRule(new WordsDetector(RESERVED_WORDS), token);
        rules.add(wordRule);
        IRule[] commentRules = getCommentRules();
        for (int i = 0; i < commentRules.length; i++) rules.add(commentRules[i]);
//        rules.add(getWhiteSpaceRule());
        ruleBasedScanner.setRules(rules.toArray(new IRule[rules.size()]));
        return ruleBasedScanner;
	}
	
//	public static RuleBasedScanner getParFparScanner() {
//		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
//		List<IRule> rules= new ArrayList<IRule>();
//		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.BLUE),
//				 									DocometreApplication.getColor(DocometreApplication.WHITE), 
//				 									SWT.NORMAL,
//				 									DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
//		IToken token = new Token(attribute);
//		String[] parfpar = new String[160];
//		for (int i = 1; i <= 80; i++) parfpar[i-1] = "PAR_" + i;
//		for (int i = 1; i <= 80; i++) parfpar[80 + i-1] = "FPAR_" + i;
//        WordRule wordRule = new WordRule(new WordsDetector(parfpar), token);
//        rules.add(wordRule);
//        ruleBasedScanner.setRules(rules.toArray(new IRule[rules.size()]));
//        return ruleBasedScanner;
//	}

}
