/**
 * 
 */
package xhail.core;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import xhail.core.entities.Grounding;
import xhail.core.entities.Problem;
import xhail.core.statements.Display;
import xhail.core.statements.Example;
import xhail.core.statements.ModeB;
import xhail.core.statements.ModeH;
import xhail.core.terms.Atom;
import xhail.core.terms.Clause;
import xhail.core.terms.Literal;
import xhail.core.terms.Placemarker;
import xhail.core.terms.Scheme;
import xhail.core.terms.SchemeTerm;
import xhail.core.terms.Variable;

/**
 * @author stefano
 *
 */
public class Utils {

	private static final Set<String> filters;

	static {
		filters = new LinkedHashSet<>();
		filters.add("#hide.");
		filters.add("#show display_fact/1.");
		filters.add("#show covered_example/2.");
		filters.add("#show uncovered_example/2.");
		// filters.add("#show use_clause_literal/2.");
	}

	public static String[] combine(Path program, String... args) {
		if (null == program)
			throw new IllegalArgumentException("Illegal 'program' argument in Solution.Builder.combine(Path, String...): " + program);
		String[] result = new String[1 + (null == args ? 0 : args.length)];
		result[0] = program.normalize().toString();
		for (int i = 0; i < args.length; i++)
			result[1 + i] = args[i];
		return result;
	}

	public static Collection<String> convert(Collection<Clause> clauses) {
		if (null == clauses)
			throw new IllegalArgumentException("Illegal 'clauses' argument in Utils.convert(Collection<Clause>): " + clauses);
		Set<String> result = new LinkedHashSet<>();
		int c = 0;
		for (Clause clause : clauses) {
			result.addAll(convert(c, clause));
			c += 1;
		}
		return result;
	}

	public static String convert(Display display) {
		if (null == display)
			throw new IllegalArgumentException("Illegal 'display' argument in Utils.convert(Display): " + display);
		String alternate = display.asAlternate();
		return String.format("display_fact(%s):-%s.", alternate, alternate);
	}

	public static Collection<String> convert(Example example) {
		if (null == example)
			throw new IllegalArgumentException("Illegal 'example' argument in Utils.Converter(Example): " + example);
		Atom atom = example.getAtom();
		boolean negated = example.isNegated();
		String yes = negated ? "not " : "";
		String not = negated ? "" : "not ";
		Set<String> result = new LinkedHashSet<>();
		result.add(String.format("%% %s", example.toString()));
		result.add(String.format("#maximize[ %s%s =%d @%d ].", negated ? "not " : "", atom, example.getWeight(), example.getPriority()));
		if (!example.isDefeasible())
			result.add(String.format(":-%s%s.", not, atom));
		result.add(String.format("covered_example(%s,%s):-%s%s.", negated ? "true" : "false", atom, yes, atom));
		result.add(String.format("uncovered_example(%s,%s):-%s%s.", negated ? "true" : "false", atom, not, atom));
		return result;
	}

	public static Collection<String> convert(Grounding grounding) {
		if (null == grounding)
			throw new IllegalArgumentException("Illegal 'grounding' argument in Utils.convert(Grounding): " + grounding);
		Set<String> result = new TreeSet<>(filters);
		result.add("#show use_clause_literal/2.");
		return result;
	}

	public static String convert(int id, Atom head, Collection<Literal> body) {
		if (id < 0)
			throw new IllegalArgumentException("Illegal 'id' argument in Utils.convert(int, Atom, Collection<Literal>): " + id);
		if (null == head)
			throw new IllegalArgumentException("Illegal 'head' argument in Utils.convert(int, Atom, Collection<Literal>): " + head);
		if (null == body)
			throw new IllegalArgumentException("Illegal 'body' argument in Utils.convert(int, Atom, Collection<Literal>): " + body);
		String result = String.format("%s:-use_clause_literal(%d,0)", head, id);
		int pos = 1;
		Set<String> types = new HashSet<>();
		for (Literal literal : body) {
			String vars = "";
			for (Variable variable : literal.getVariables()) {
				vars += "," + variable.getIdentifier();
				types.add(String.format(",%s(%s)", variable.getType().getIdentifier(), variable.getIdentifier()));
			}
			result += String.format(",try_clause_literal(%d,%d%s)", id, pos, vars);
			pos += 1;
		}
		for (String type : types)
			result += type;
		result += ".";
		return result;
	}

	public static Collection<String> convert(int id, Clause clause) {
		if (id < 0)
			throw new IllegalArgumentException("Illegal 'id' argument in Utils.convert(int, Clause): " + id);
		if (null == clause)
			throw new IllegalArgumentException("Illegal 'clause' argument in Utils.convert(int, Clause): " + clause);
		Set<String> result = new LinkedHashSet<>();
		result.add(String.format("clause(%d).", id));
		result.add(String.format("literal(%d,1..%d).", id, clause.getSize()));
		result.add(String.format("clause_level(%d,0):-use_clause_literal(%d,0).", id, id));
		for (int pos = 1; pos <= clause.getSize(); pos++)
			result.add(String.format("clause_level(%d,%d):-use_clause_literal(%d,%d).", id, clause.getBody(pos).getLevel(), id, pos));
		for (int lvl = 0; lvl < clause.getLevels(); lvl++)
			result.add(String.format(":-not clause_level(%d,%d),clause_level(%d,%d).", id, lvl, id, 1 + lvl));
		Atom head = clause.getHead();
		result.add(String.format("#minimize[ use_clause_literal(%d,0) =%d @%d ].", id, head.getWeight(), head.getPriority()));
		for (int pos = 1; pos <= clause.getSize(); pos++) {
			Literal literal = clause.getBody(pos);
			result.add(String.format("#minimize[ use_clause_literal(%d,%d) =%d @%d ].", id, pos, literal.getWeight(), literal.getPriority()));
		}
		result.add("");
		result.add(convert(id, head, clause.getBody()));
		for (int pos = 1; pos <= clause.getSize(); pos++)
			result.addAll(convert(id, pos, clause.getBody(pos)));
		return result;
	}

	public static Collection<String> convert(int id, int pos, Literal literal) {
		if (id < 0)
			throw new IllegalArgumentException("Illegal 'id' argument in Utils.convert(int, int, Literal): " + id);
		if (pos < 1)
			throw new IllegalArgumentException("Illegal 'pos' argument in Utils.convert(int, int, Literal): " + pos);
		if (null == literal)
			throw new IllegalArgumentException("Illegal 'literal' argument in Utils.convert(int, int, Literal): " + literal);
		Set<String> result = new LinkedHashSet<>();
		String vars = "";
		Set<String> types = new HashSet<>();
		for (Variable variable : literal.getAtom().getVariables()) {
			vars += "," + variable.getIdentifier();
			types.add(String.format(",%s(%s)", variable.getType().getIdentifier(), variable.getIdentifier()));
		}
		String statement = String.format("try_clause_literal(%d,%d%s):-not use_clause_literal(%d,%d)", id, pos, vars, id, pos);
		for (String type : types)
			statement += type;
		statement += ".";
		result.add(statement);
		statement = String.format("try_clause_literal(%d,%d%s):-use_clause_literal(%d,%d)", id, pos, vars, id, pos);
		for (String type : types)
			statement += type;
		statement += String.format(",%s", literal);
		statement += ".";
		result.add(statement);
		return result;
	}

	public static Collection<String> convert(ModeH mode) {
		if (null == mode)
			throw new IllegalArgumentException("Illegal 'mode' argument in Utils.convert(ModeH): " + mode);
		String typing = "";
		String listing = "";
		Set<Variable> vars = new HashSet<>();
		SchemeTerm scheme = mode.getScheme();
		Atom atom = (Atom) scheme.generalises(vars);
		Set<String> result = new LinkedHashSet<>();
		for (Placemarker placemarker : scheme.getPlacemarkers()) {
			typing += " :" + placemarker.asVariable();
			listing += "," + placemarker.asVariable();
		}
		
		result.add(String.format("%% %s", mode.toString()));
		result.add(String.format("%d { abduced_%s%s } %d.", mode.getLower(), atom, typing, mode.getUpper()));
		result.add(String.format("#minimize[ abduced_%s =%d @%d%s ].", atom, mode.getWeigth(), mode.getPriority(), typing));
		result.add(String.format("%s:-abduced_%s%s.", atom, atom, listing));
		return result;
	}

	public static Collection<String> convert(Problem problem) {
		if (null == problem)
			throw new IllegalArgumentException("Illegal 'problem' argument in Utils.convert(Problem): " + problem);
		Set<String> result = new TreeSet<>(filters);
		for (ModeH mode : problem.getModeHs()) {
			Scheme scheme = mode.getScheme();
			result.add(String.format("#show %s/%d.", scheme.getIdentifier(), scheme.getArity()));
			result.add(String.format("#show abduced_%s/%d.", scheme.getIdentifier(), scheme.getArity()));
			for (Placemarker placemarker : scheme.getPlacemarkers())
				result.add(String.format("#show %s/1.", placemarker.getIdentifier()));
		}
		for (ModeB mode : problem.getModeBs()) {
			Scheme scheme = mode.getScheme();
			result.add(String.format("#show %s/%d.", scheme.getIdentifier(), scheme.getArity()));
			for (Placemarker placemarker : scheme.getPlacemarkers())
				result.add(String.format("#show %s/1.", placemarker.getIdentifier()));
		}
		return result;
	}

	public static boolean dump(Problem problem, OutputStream stream) {
		if (null == problem)
			throw new IllegalArgumentException("Illegal 'problem' argument in Utils.save(Problem, OutputStream): " + problem);
		if (null == stream)
			throw new IllegalArgumentException("Illegal 'stream' argument in Utils.save(Problem, OutputStream): " + stream);
		try {
			PrintStream printer = new PrintStream(stream);
			Collection<Display> displays = problem.getDisplays();
			if (displays.size() > 0) {
				for (Display display : displays)
					printer.println(display.toString());
				printer.println();
			}
			Collection<String> background = problem.getBackground();
			if (background.size() > 0) {
				printer.println("%% B. Background");
				for (String statement : background)
					printer.println(statement);
				printer.println();
			}
			Collection<Example> examples = problem.getExamples();
			if (examples.size() > 0) {
				printer.println("%% E. Examples");
				for (Example example : examples)
					printer.println(example.toString());
				printer.println();
			}
			Collection<ModeH> modeHs = problem.getModeHs();
			Collection<ModeB> modeBs = problem.getModeBs();
			if (modeHs.size() > 0 || modeBs.size() > 0) {
				printer.println("%% M. Modes");
				for (ModeH mode : modeHs)
					printer.println(mode);
				for (ModeB mode : modeBs)
					printer.println(mode);
				printer.println();
			}
			return true;
		} catch (Exception e) {
			Logger.error("cannot stream data to process");
		}
		return false;
	}

	public static Collection<String> listAtoms(String label, Collection<Atom> atoms) {
		if (null == label || (label = label.trim()).isEmpty())
			throw new IllegalArgumentException("Illegal 'label' argument in Utils.listAtoms(String, Collection<Atom>): " + label);
		if (null == atoms)
			throw new IllegalArgumentException("Illegal 'atoms' argument in Utils.listAtoms(String, Collection<Atom>): " + atoms);
		Set<String> result = new LinkedHashSet<>();
		result.add(label + ":");
		if (0 == atoms.size())
			result.add("  -");
		else {
			Set<String> list = new TreeSet<>();
			for (Atom atom : atoms)
				list.add(" " + atom);
			String content = " ";
			for (String item : list)
				content += item;
			result.add(content);
		}
		return result;
	}

	public static Collection<String> listClauses(String label, Collection<Clause> clauses) {
		if (null == label || (label = label.trim()).isEmpty())
			throw new IllegalArgumentException("Illegal 'label' argument in Utils.listClauses(String, Collection<Clause>): " + label);
		if (null == clauses)
			throw new IllegalArgumentException("Illegal 'clauses' argument in Utils.listClauses(String, Collection<Clause>): " + clauses);
		Set<String> result = new LinkedHashSet<>();
		result.add(label + ":");
		if (0 == clauses.size())
			result.add("  -");
		else {
			Set<String> list = new TreeSet<>();
			for (Clause clause : clauses)
				list.add("  " + clause);
			result.addAll(list);
		}
		return result;
	}

	public static Collection<String> listLiterals(String label, Collection<Literal> literals) {
		if (null == label || (label = label.trim()).isEmpty())
			throw new IllegalArgumentException("Illegal 'label' argument in Utils.listLiterals(String, Collection<Atom>): " + label);
		if (null == literals)
			throw new IllegalArgumentException("Illegal 'literals' argument in Utils.listLiterals(String, Collection<Atom>): " + literals);
		Set<String> result = new LinkedHashSet<>();
		result.add(label + ":");
		if (0 == literals.size())
			result.add("  -");
		else {
			Set<String> list = new TreeSet<>();
			for (Literal literal : literals)
				list.add(" " + literal);
			String content = " ";
			for (String item : list)
				content += item;
			result.add(content);
		}
		return result;
	}

	public static boolean save(Grounding grounding, OutputStream stream) {
		if (null == grounding)
			throw new IllegalArgumentException("Illegal 'grounding' argument in Utils.save(Grounding, OutputStream): " + grounding);
		if (null == stream)
			throw new IllegalArgumentException("Illegal 'stream' argument in Utils.save(Grounding, OutputStream): " + stream);
		try {
			PrintStream printer = new PrintStream(stream);
			for (String filter : convert(grounding))
				printer.println(filter);
			printer.println();
			printer.println("%%% B. Background");
			for (String statement : grounding.getBackground())
				printer.println(statement);
			for (Display display : grounding.getDisplays())
				printer.println(convert(display));
			printer.println();
			printer.println("%%% E. Examples");
			for (Example example : grounding.getExamples())
				for (String statement : convert(example))
					printer.println(statement);
			printer.println();
			printer.println("%%% M. Modes");
			printer.println("{ use_clause_literal(V1,0) }:-clause(V1).");
			printer.println("{ use_clause_literal(V1,V2) }:-clause(V1),literal(V1,V2).");
			printer.println();
			for (String statement : convert(grounding.getGeneralisation()))
				printer.println(statement);
			printer.println();
			printer.close();
			return true;
		} catch (Exception e) {
			Logger.error("cannot stream data to process");
		}
		return false;
	}

	public static boolean save(Grounding grounding, Path path) {
		if (null == grounding)
			throw new IllegalArgumentException("Illegal 'grounding' argument in Utils.save(Grounding, Path): " + grounding);
		if (null == path)
			throw new IllegalArgumentException("Illegal 'path' argument in Utils.save(Grounding, Path): " + path);
		try {
			Path folder = Paths.get(".", "temp").toAbsolutePath().normalize();
			if (!Files.exists(folder))
				Files.createDirectory(folder);
			Path file = folder.resolve(path.getFileName());
			try {
				return save(grounding, new FileOutputStream(file.toFile()));
			} catch (IOException e) {
				Logger.error(String.format("cannot write to '%s' file (do we have rights?)", path.getFileName().toString()));
			}
		} catch (IOException e) {
			Logger.warning(false, "cannot create 'temp' folder (do we have rights?)");
			System.err.println(e);
		}
		return false;
	}

	public static boolean save(Problem problem, OutputStream stream) {
		if (null == problem)
			throw new IllegalArgumentException("Illegal 'problem' argument in Utils.save(Problem, OutputStream): " + problem);
		if (null == stream)
			throw new IllegalArgumentException("Illegal 'stream' argument in Utils.save(Problem, OutputStream): " + stream);
		try {
			PrintStream printer = new PrintStream(stream);
			for (String filter : convert(problem))
				printer.println(filter);
			printer.println();
			printer.println("%%% B. Background");
			for (String statement : problem.getBackground())
				printer.println(statement);
			for (Display display : problem.getDisplays())
				printer.println(convert(display));
			printer.println();
			printer.println("%%% E. Examples");
			for (Example example : problem.getExamples())
				for (String statement : convert(example))
					printer.println(statement);
			printer.println();
			printer.println("%%% M. Modes");
			for (ModeH mode : problem.getModeHs())
				for (String statement : convert(mode))
					printer.println(statement);
			printer.println();
			printer.close();
			return true;
		} catch (Exception e) {
			Logger.error("cannot stream data to process");
		}
		return false;
	}

	public static boolean save(Problem problem, Path path) {
		if (null == problem)
			throw new IllegalArgumentException("Illegal 'problem' argument in Utils.save(Problem, Path): " + problem);
		if (null == path)
			throw new IllegalArgumentException("Illegal 'path' argument in Utils.save(Problem, Path): " + path);
		try {
			Path folder = Paths.get(".", "temp").toAbsolutePath().normalize();
			if (!Files.exists(folder))
				Files.createDirectory(folder);
			Path file = folder.resolve(path.getFileName());
			try {
				return save(problem, new FileOutputStream(file.toFile()));
			} catch (IOException e) {
				Logger.error(String.format("cannot write to '%s' file (do we have rights?)", path.getFileName().toString()));
			}
		} catch (IOException e) {
			Logger.warning(false, "cannot create 'temp' folder (do we have rights?)");
			System.err.println(e);
		}
		return false;
	}

}
