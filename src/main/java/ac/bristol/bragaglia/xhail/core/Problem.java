/**
 * 
 */
package ac.bristol.bragaglia.xhail.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;

import ac.bristol.bragaglia.xhail.predicates.Atom;
import ac.bristol.bragaglia.xhail.predicates.Literal;

/**
 * A class to hold the annex data of example directives.
 * 
 * @author stefano
 */
public class Problem extends Model {

	public class ExampleData {

		private Integer priority;

		private Integer weight;

		public ExampleData(Integer weight, Integer priority) {
			this.priority = priority;
			this.weight = weight;
		}

		public String asData() {
			return String.format(" =%d @%d", getWeight(), getPriority());
		}

		public String asData(int maxPriority) {
			return String.format(" =%d @%d", getWeight(), 1 + maxPriority + getPriority());
		}

		public Integer getPriority() {
			return null != priority ? priority : 1;
		}

		public Integer getWeight() {
			return null != weight ? weight : 1;
		}

		public boolean isMute() {
			return null == weight && null == priority;
		}

		@Override
		public String toString() {
			String result = "";
			if (null != weight)
				result += String.format(" =%d", weight);
			if (null != priority)
				result += String.format(" @%d", priority);
			return result;
		}

	}

	/**
	 * A class to hold the annex data of mode body directives.
	 * 
	 * @author stefano
	 */
	public class ModeBodyData {

		private Integer bound;

		private Integer priority;

		private Integer weight;

		public ModeBodyData(Integer bound, Integer weight, Integer priority) {
			this.bound = bound;
			this.priority = priority;
			this.weight = weight;
		}

		public String asData() {
			return String.format(" =%d @%d", getWeight(), getPriority());
		}

		public Integer getBound() {
			return null != bound ? bound : 1;
		}

		public Integer getPriority() {
			return null != priority ? priority : 1;
		}

		public Integer getWeight() {
			return null != weight ? weight : 1;
		}

		@Override
		public String toString() {
			String result = "";
			if (null != bound)
				result += String.format(" :%d", bound);
			result += asData();
			return result;
		}

	}

	/**
	 * A class to hold the annex data of mode head directives.
	 * 
	 * @author stefano
	 */
	public class ModeHeadData {

		private Integer lower;

		private Integer priority;

		private Integer upper;

		private Integer weight;

		public ModeHeadData(Integer lower, Integer upper, Integer weight, Integer priority) {
			if (null == lower && null != upper)
				throw new IllegalArgumentException("Illegal 'lower' argument in ModeHeadData(Integer, Integer, Integer, Integer): " + lower);
			this.lower = lower;
			this.priority = priority;
			this.upper = upper;
			this.weight = weight;
		}

		public String asData() {
			return String.format(" =%d @%d", getWeight(), getPriority());
		}

		public String asLower() {
			if (null == lower || null == upper)
				return "";
			else if (lower <= upper)
				return lower + " ";
			else
				return upper + " ";
		}

		public String asUpper() {
			if (null == lower || null == upper)
				return "";
			else if (lower <= upper)
				return " " + upper;
			else
				return " " + lower;
		}

		public Integer getPriority() {
			return null != priority ? priority : 1;
		}

		public Integer getWeight() {
			return null != weight ? weight : 1;
		}

		public int maxPriority(int maxPriority) {
			int result = getPriority();
			if (maxPriority > result)
				result = maxPriority;
			return result;
		}

		@Override
		public String toString() {
			String result = "";
			if (null != upper) {
				result += " :";
				if (null != lower)
					result += lower + "-";
				result += upper;
			}
			result += asData();
			return result;
		}

	}

	/**
	 * The constant prefix for abducibles.
	 */
	public static final String TAG_ABDUCE = "abd_";

	/**
	 * The constant prefix for types.
	 */
	public static final String TAG_TYPE = "typ_";

	private boolean display;

	private Map<String, Set<Integer>> displays;

	/**
	 * The set of example directives of this problem (plus annexes).
	 */
	private Map<Literal, ExampleData> examples;

	/**
	 * The set of body mode directives of this problem (plus annexes, the
	 * priority is explicit (the <code>Integer</code>) because we need body
	 * modes sorted by priority in the deductive phase).
	 */
	private Map<Integer, Map<Literal, ModeBodyData>> modebodies;

	/**
	 * The set of head mode directives of this problem (plus annexes).
	 */
	private Map<Atom, ModeHeadData> modeheads;

	private Model model;

	/**
	 * The set of types defined in this problem.
	 */
	private Set<Atom> types;

	/**
	 * Default constructor. Generates and empty problem.
	 */
	public Problem() {
		super();
		this.display = false;
		this.displays = new TreeMap<>();
		this.examples = new TreeMap<>();
		this.modebodies = new TreeMap<>();
		this.modeheads = new TreeMap<>();
		this.types = new TreeSet<>();
		assert invariant() : "Illegal state in Problem()";
	}

	public boolean addDisplay(String name, int arity) {
		if (null == name || (name = name.trim()).isEmpty())
			throw new IllegalArgumentException("Illegal 'name' argument in Model.addDisplay(String, int): " + name);
		if (arity < 0)
			throw new IllegalArgumentException("Illegal 'arity' argument in Model.addDisplay(String, int): " + arity);
		Set<Integer> set = displays.get(name);
		if (null == set) {
			set = new HashSet<>();
			displays.put(name, set);
		}
		boolean result = set.add(arity);
		if (result)
			update();
		assert invariant() : "Illegal state in Model.addDisplay(String, int)";
		return result;
	}

	public void addDisplayAll() {
		display = true;
		assert invariant() : "Illegal state in Problem.addDisplayAll()";
	}

	/**
	 * Adds the given example directive to this problem. If the problem did not
	 * contain the given statement, the <code>modified</code> flag is set.
	 * 
	 * @param example
	 *            the example directive to add
	 * @return <code>true</code> if the problem did not already contain the
	 *         given statement, <code>false</code> otherwise
	 */
	public boolean addExample(Literal fact, Integer weight, Integer priority) {
		if (null == fact)
			throw new IllegalArgumentException("Illegal 'fact' argument in ConcreteProblem.addExample(Literal, Integer, Integer): " + fact);
		ExampleData value = new ExampleData(weight, priority);
		ExampleData previous = examples.put(fact, value);
		boolean result = (null == previous || previous.equals(value));
		if (result)
			update();
		assert invariant() : "Illegal state in Problem.addExample(Literal, Integer, Integer)";
		return result;
	}

	/**
	 * Adds the given body mode directive to this problem. If the problem did
	 * not contain the given statement, the <code>modified</code> flag is set.
	 * 
	 * @param show
	 *            the body mode directive to add
	 * @return <code>true</code> if the model did not already contain the given
	 *         statement, <code>false</code> otherwise
	 */
	public boolean addModeBody(Literal body, Integer bound, Integer weight, Integer priority) {
		if (null == body)
			throw new IllegalArgumentException("Illegal 'body' argument in Problem.addModeBody(Literal, Integer, Integer, Integer): " + body);
		findTypes(body.atom());
		int p = null != priority ? priority : 1;
		Map<Literal, ModeBodyData> bodies = modebodies.get(p);
		if (null == bodies) {
			bodies = new LinkedHashMap<>();
			modebodies.put(p, bodies);
		}
		ModeBodyData value = new ModeBodyData(bound, weight, priority);
		ModeBodyData previous = bodies.put(body, value);
		boolean result = (null == previous || previous.equals(value));
		if (result)
			update();
		assert invariant() : "Illegal state in Problem.addModeBody(Literal, Integer, Integer, Integer)";
		return result;
	}

	/**
	 * Adds the given head mode directive to this problem. If the problem did
	 * not contain the given statement, the <code>modified</code> flag is set.
	 * 
	 * @param show
	 *            the head mode directive to add
	 * @return <code>true</code> if the model did not already contain the given
	 *         statement, <code>false</code> otherwise
	 */
	public boolean addModeHead(Atom head, Integer min, Integer max, Integer weight, Integer priority) {
		if (null == head)
			throw new IllegalArgumentException("Illegal 'head' argument in Problem.addModeHead(Atom, Integer, Integer, Integer, Integer): " + head);
		if (min >= 0 && max < 0)
			throw new IllegalArgumentException("Illegal 'min' argument in Problem.addModeHead(Atom, Integer, Integer, Integer, Integer): " + min);
		if (max < min || (min < 0 && max >= 0))
			throw new IllegalArgumentException("Illegal 'max' argument in Problem.addModeHead(Atom, Integer, Integer, Integer, Integer): " + max);
		findTypes(head);
		ModeHeadData value = new ModeHeadData(min, max, weight, priority);
		ModeHeadData previous = modeheads.put(head, value);
		boolean result = (null == previous || previous.equals(value));
		if (result)
			update();
		assert invariant() : "Illegal state in Problem.addModeHead(Atom, Integer, Integer, Integer, Integer)";
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ac.bristol.bragaglia.xhail.Model#clear()
	 */
	@Override
	public void clear() {
		super.clear();
		examples.clear();
		modebodies.clear();
		modeheads.size();
		update();
		assert invariant() : "Illegal state in Problem.clear()";
	}

	/**
	 * Derives a model from this problem and returns it. This method clones the
	 * standard model that is included in this program; then it takes every
	 * non-standard statement, converts it into one or more standard statements
	 * and finally add them to the cloned model. The resulting augmented cloned
	 * model is finally returned.
	 * 
	 * @return the model equivalent to this problem
	 */
	public Model derive() {
		if (null == model || isModified()) {
			model = new Model(this);
			processModeHeads();
			processExamples();
			save();
		}
		assert invariant() : "Illegal state in Problem.derive()";
		return model;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Problem other = (Problem) obj;
		if (display != other.display)
			return false;
		if (displays == null) {
			if (other.displays != null)
				return false;
		} else if (!displays.equals(other.displays))
			return false;
		if (examples == null) {
			if (other.examples != null)
				return false;
		} else if (!examples.equals(other.examples))
			return false;
		if (modebodies == null) {
			if (other.modebodies != null)
				return false;
		} else if (!modebodies.equals(other.modebodies))
			return false;
		if (modeheads == null) {
			if (other.modeheads != null)
				return false;
		} else if (!modeheads.equals(other.modeheads))
			return false;
		if (model == null) {
			if (other.model != null)
				return false;
		} else if (!model.equals(other.model))
			return false;
		if (types == null) {
			if (other.types != null)
				return false;
		} else if (!types.equals(other.types))
			return false;
		return true;
	}

	/**
	 * Returns the example directives of the problem as a collection.
	 * 
	 * @return the example directives of the problem as a collection
	 */
	public Collection<String> examples() {
		Set<String> result = new TreeSet<>();
		for (Literal key : examples.keySet())
			result.add(String.format("#example %s%s.", key.toPrint(), examples.get(key).toString()));
		assert invariant() : "Illegal state in Problem.examples()";
		return result;
	}

	protected Map<Literal, ExampleData> evidendes() {
		assert invariant() : "Illegal state in Problem.evidences()";
		return examples;
	}

	/**
	 * Recursively parses the given non-<code>null</code> atom to find all its
	 * type definitions. A type definition is any term preceded by a
	 * <code>+</code>, <code>-</code> and <code>$</code>. All the type
	 * definitions found in this way are finally added to the set of type
	 * definitions of this problem.
	 * 
	 * @param atom
	 *            the atom to parse
	 */
	private void findTypes(Atom atom) {
		if (null == atom)
			throw new IllegalArgumentException("Illegal 'atom' argument in Problem.findTypes(Atom): " + atom);
		int arity = atom.arity();
		String functor = atom.name();
		if (arity > 0)
			if (1 == arity && (Atom.PAR_INPUT.equals(functor) || Atom.PAR_OUTPUT.equals(functor) || Atom.PAR_CONSTANT.equals(functor)))
				types.add(atom.get(0));
			else
				for (Atom term : atom)
					findTypes(term);
		assert invariant() : "Illegal state in Problem.findTypes(Atom)";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (display ? 1231 : 1237);
		result = prime * result + ((displays == null) ? 0 : displays.hashCode());
		result = prime * result + ((examples == null) ? 0 : examples.hashCode());
		result = prime * result + ((modebodies == null) ? 0 : modebodies.hashCode());
		result = prime * result + ((modeheads == null) ? 0 : modeheads.hashCode());
		result = prime * result + ((model == null) ? 0 : model.hashCode());
		result = prime * result + ((types == null) ? 0 : types.hashCode());
		return result;
	}

	public Model induce() {
		Model result = new Model(this);
		processExamples(result);
		assert invariant() : "Illegal state in Problem.induce()";
		return result;
	}

	/**
	 * Invariant check against the internal state.
	 * 
	 * @return <code>true</code> if this instance's state is consistent,
	 *         <code>false</code> otherwise
	 */
	private boolean invariant() {
		return (null != displays && null != examples && null != modebodies && null != modeheads);
	}

	/**
	 * Tells whether this problem is generalisable or not. A generalisable
	 * problem is a problem with at least a head mode directive. Only in this
	 * case, in fact, it is possible to have some abducibles.
	 * 
	 * @return <code>true</code> if the problem is generalisable,
	 *         <code>false</code> otherwise
	 */
	public boolean isAbducible() {
		boolean result = !modeheads.isEmpty();
		assert invariant() : "Illegal state in Problem.isAbducible()";
		return result;
	}

	public boolean isDisplayable(Atom candidate) {
		if (null == candidate)
			throw new IllegalArgumentException("Illegal 'candidate' argument in Problem.isDisplayable(Atom): " + candidate);
		String name = candidate.name();
		boolean result = displays.containsKey(name) && displays.get(name).contains(candidate.arity());
		assert invariant() : "Illegal state in Problem.isDisplayable(Atom)";
		return result;
	}

	public boolean isDisplayAll() {
		assert invariant() : "Illegal state in Problem.isDisplayAll()";
		return display;
	}

	public boolean isEmpty() {
		boolean result = display == false;
		result &= displays.isEmpty();
		result &= examples.isEmpty();
		result &= modebodies.isEmpty();
		result &= modeheads.isEmpty();
		result &= (null == model || model.isEmpty());
		assert invariant() : "Illegal state in Problem.isEmpty()";
		return result;
	}

	/**
	 * Returns the modebodies directives of the problem as a collection.
	 * 
	 * @return the modebodies directives of the problem as a collection
	 */
	public Collection<String> modebodies() {
		Set<String> result = new TreeSet<>();
		for (int priority : modebodies.keySet()) {
			Map<Literal, ModeBodyData> bodies = modebodies.get(priority);
			for (Literal body : bodies.keySet())
				result.add(String.format("#modeb %s%s.", body.toPrint(), bodies.get(body).toString()));
		}
		assert invariant() : "Illegal state in Problem.modebodies()";
		return result;
	}

	/**
	 * Returns the modehead directives of the problem as a collection.
	 * 
	 * @return the modehead directives of the problem as a collection
	 */
	public Collection<String> modeheads() {
		Set<String> result = new TreeSet<>();
		for (Atom key : modeheads.keySet())
			result.add(String.format("#modeh %s%s.", key.toPrint(), examples.get(key).toString()));
		assert invariant() : "Illegal state in Problem.modeheads()";
		return result;
	}

	/**
	 * Utility method to extract the set of body mode directives (plus annexes)
	 * from a problem.
	 * 
	 * @param problem
	 *            the problem from whom extracting the body mode directives
	 * @return the body mode directives of the problem
	 */
	public Map<Integer, Map<Literal, ModeBodyData>> modes() {
		assert invariant() : "Illegal state in Problem.modes()";
		return modebodies;
	}

	/**
	 * This utility method converts any example directive of this problem into
	 * standard statements and adds them to the given non-<code>null</code>
	 * model. Specifically, it adds the constraints for certain examples
	 * (required in both the abductive and inductive phase) and the maximization
	 * of examples coverage (required only in the abductive phase).
	 */
	private void processExamples() {
		processExamples(model);
	}

	private void processExamples(Model model) {
		if (null == model)
			throw new IllegalArgumentException("Illegal 'model' argument in Problem.processExamples(model): " + model);
		if (!examples.isEmpty()) {
			StringJoiner joiner = new StringJoiner(", ");
			for (Literal key : examples.keySet()) {
				ExampleData value = examples.get(key);
				joiner.add(key.toString() + value.asData());
				if (value.isMute())
					model.addConstraint(String.format(":- %s%s.", key.negated() ? "" : "not ", key.atom().toString()));
			}
			model.addMaximize(String.format("#maximize[ %s ].", joiner.toString()));
		}
		assert invariant() : "Illegal state in Problem.processExamples()";
	}

	/**
	 * This utility method converts any modehead directive of this problem into
	 * standard statements and adds them to the given non-<code>null</code>
	 * model.
	 */
	private void processModeHeads() {
		if (!modeheads.isEmpty()) {
			for (Atom mode : modeheads.keySet()) {
				List<String> fixes = new ArrayList<String>();
				List<String> heads = new ArrayList<String>();
				List<String> vars = new ArrayList<String>();
				List<String> types = new ArrayList<String>();
				ModeHeadData data = modeheads.get(mode);
				for (Atom term : mode)
					term.decode(fixes, heads, vars, types);
				String name = mode.name();
				String nameHead = heads.size() > 0 ? String.format("%s(%s)", name, String.join(", ", heads)) : name;
				String abduce = String.format("%s%s_%d_%d", TAG_ABDUCE, name, data.getWeight(), data.getPriority());
				String abduceHead = fixes.size() > 0 ? String.format("%s(%s)", abduce, String.join(", ", fixes)) : abduce;
				String type = String.format("%s%s", TAG_TYPE, name);
				String typeHead = vars.size() > 0 ? String.format("%s(%s)", type, String.join(", ", vars)) : type;
				model.addHide(String.format("#hide %s/%d.", type, vars.size()));
				model.addMinimize(String.format("#minimize[ %s%s : %s ].", abduceHead, data.asData(), typeHead));
				model.addClause(String.format("%s{ %s : %s }%s.", data.asLower(), abduceHead, typeHead, data.asUpper()));
				model.addClause(String.format("%s :- %s, %s.", nameHead, typeHead, abduceHead));
				if (vars.size() > 0) // var.size() == types.size()
					model.addClause(String.format("%s :- %s.", typeHead, String.join(", ", types)));
				else
					model.addClause(String.format("%s.", typeHead));
			}
		}
		assert invariant() : "Illegal state in Problem.processModeHeads()";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ac.bristol.bragaglia.xhail.Model#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		append(hideshows(), builder);
		append(constants(), builder);
		append(domains(), builder);
		append(externals(), builder);
		append(examples(), builder);
		append(modeheads(), builder);
		append(modebodies(), builder);
		append(computes(), builder);
		append(maximizes(), builder);
		append(minimizes(), builder);
		append(clauses(), builder);
		append(facts(), builder);
		append(constraints(), builder);
		assert invariant() : "Illegal state in Problem.toString()";
		return builder.toString();
	}

	/**
	 * Returns the type definitions of the problem as a collection.
	 * 
	 * @return the type definitions of the problem as a collection
	 */
	public Collection<Atom> types() {
		assert invariant() : "Illegal state in Problem.types()";
		return types;
	}

}
