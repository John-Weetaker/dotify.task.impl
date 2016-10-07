package org.daisy.dotify.impl.system.common;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.daisy.dotify.api.engine.FormatterEngineFactoryService;
import org.daisy.dotify.api.tasks.InternalTask;
import org.daisy.dotify.api.tasks.TaskGroup;
import org.daisy.dotify.api.tasks.TaskGroupFactoryMakerService;
import org.daisy.dotify.api.tasks.TaskGroupSpecification;
import org.daisy.dotify.api.tasks.TaskOption;
import org.daisy.dotify.api.tasks.TaskSystem;
import org.daisy.dotify.api.tasks.TaskSystemException;
import org.daisy.dotify.api.writer.PagedMediaWriterFactoryMakerService;
import org.daisy.dotify.impl.input.Keys;


/**
 * <p>Transforms XML into braille in PEF 2008-1 format.</p>
 * <p>Transforms documents into text format.</p>
 * 
 * <p>This TaskSystem consists of the following steps:</p>
 * <ol>
	 * <li>Input Manager. Validates and converts input to OBFL.</li>
	 * <li>OBFL to PEF converter.
	 * 		Translates all characters into braille, and puts the text flow onto pages.</li>
 * </ol>
 * <p>The result should be validated against the PEF Relax NG schema using int_daisy_validator.</p>
 * @author Joel Håkansson
 */
public class DotifyTaskSystem implements TaskSystem {
	private static final Logger logger = Logger.getLogger(DotifyTaskSystem.class.getCanonicalName());
	private final String outputFormat;
	private final String context;
	private final String name;
	private final TaskGroupFactoryMakerService imf;
	private final PagedMediaWriterFactoryMakerService pmw;
	private final FormatterEngineFactoryService fe;
	
	public DotifyTaskSystem(String name, String outputFormat, String context,
			TaskGroupFactoryMakerService imf, PagedMediaWriterFactoryMakerService pmw, FormatterEngineFactoryService fe) {
		this.context = context;
		this.outputFormat = outputFormat;
		this.name = name;
		this.imf = imf;
		this.pmw = pmw;
		this.fe = fe;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<InternalTask> compile(Map<String, Object> pa) throws TaskSystemException {
		RunParameters p = RunParameters.fromMap(pa);
		HashMap<String, Object> h = new HashMap<>();
		for (Object key : p.getKeys()) {
			if (p.getProperty(key)!=null) {
				h.put(key.toString(), p.getProperty(key));
			}
		}
		
		List<InternalTask> setup = new ArrayList<>();
		String inputFormat = h.get(Keys.INPUT_FORMAT).toString();

		logger.info("Finding path...");
		for (TaskGroup g : getPath(imf, new TaskGroupSpecification(inputFormat, outputFormat, context), pa)) {
			setup.addAll(g.compile(h));
		}

		return setup;
	}

	@Override
	public List<TaskOption> getOptions() {
		return Collections.emptyList();
	}
	
	/**
	 * Finds a path for the given specifications
	 * @param input the input format
	 * @param output the output format
	 * @param locale the target locale
	 * @param parameters the parameters
	 * @return returns a list of task groups
	 */
	static List<TaskGroup> getPath(TaskGroupFactoryMakerService imf, TaskGroupSpecification def, Map<String, Object> parameters) {
		Set<TaskGroupSpecification> specs = imf.listSupportedSpecifications();
		Map<String, List<TaskGroupSpecification>> byInput = byInput(specs);

		List<TaskGroup> path = new ArrayList<>();
		List<TaskGroupSpecification> selected = getPathSpecifications(def.getInputFormat(), def.getOutputFormat(), def.getLocale(), parameters, byInput, 0);
		for (TaskGroupSpecification spec : selected) {
			path.add(imf.newTaskGroup(spec));
		}
		return path;
	}
	
	static List<TaskGroupSpecification> getPathSpecifications(String input, String output, String locale, Map<String, Object> parameters, Map<String, List<TaskGroupSpecification>> inputs, int i) {
		Map<String, List<TaskGroupSpecification>> byInput = new HashMap<>(inputs);
		TaskGroupSpecificationFilter candidates = TaskGroupSpecificationFilter.filterLocaleGroupByType(byInput.remove(input), locale);
		
		for (TaskGroupSpecification candidate : candidates.getConvert()) {
			if (candidate.getOutputFormat().equals(output)) {
				logger.info("Evaluating " + input + " -> " + candidate.getOutputFormat() + " (D:"+i+")");
				List<TaskGroupSpecification> path = new ArrayList<>();
				path.addAll(getEnhance(candidates, parameters));
				path.add(candidate);
				return path;
			} else {
				logger.info("Evaluating " + input + " -> " + candidate.getOutputFormat() + " (D:"+i+")");
				List<TaskGroupSpecification> path2 = getPathSpecifications(candidate.getOutputFormat(), output, locale, parameters, byInput, i+1);
				if (!path2.isEmpty()) {
					List<TaskGroupSpecification> path = new ArrayList<>();
					path.addAll(getEnhance(candidates, parameters));
					path.add(candidate);
					path.addAll(path2);
					return path;
				}
			}
		}
		return Collections.emptyList();
	}
	
	private static List<TaskGroupSpecification> getEnhance(TaskGroupSpecificationFilter candidates, Map<String, Object> parameters) {
		List<TaskGroupSpecification> ret = new ArrayList<>();
		for (TaskGroupSpecification candidate : candidates.getEnhance()) {
			if (matchesRequiredOptions(candidate, parameters, false)) {
				ret.add(candidate);
			}
		}
		return ret;
	}
	
	static boolean matchesRequiredOptions(TaskGroupSpecification candidate, Map<String, Object> parameters, boolean emptyReturn) {
		if (candidate.requiresKeys().isEmpty() && candidate.requiresKeyValue().isEmpty()) {
			return emptyReturn;
		}
		for (String key : candidate.requiresKeys()) {
			if (!parameters.containsKey(key)) {
				return false;
			}
		}
		for (Entry<String, String> entry : candidate.requiresKeyValue().entrySet()) {
			Object value = parameters.get(entry.getKey());
			if (value==null || !value.equals(entry.getValue())) {
				return false;
			}
		}
		return true;
	}
	
	static Map<String, List<TaskGroupSpecification>> byInput(Set<TaskGroupSpecification> specs) {
		Map<String, List<TaskGroupSpecification>> ret = new HashMap<>();
		for (TaskGroupSpecification spec : specs) {
			List<TaskGroupSpecification> group = ret.get(spec.getInputFormat());
			if (group==null) {
				group = new ArrayList<>();
				ret.put(spec.getInputFormat(), group);
			}
			group.add(spec);
		}
		return ret;
	}
	
	static void listSpecs(PrintStream out, Map<String, List<TaskGroupSpecification>> specs) {
		for (Entry<String, List<TaskGroupSpecification>> entry : specs.entrySet()) {
			out.println(entry.getKey());
			for (TaskGroupSpecification spec : entry.getValue()) {
				out.println("  " + spec.getInputFormat() + " -> " + spec.getOutputFormat() + " (" + spec.getLocale() + ")");
			}
		}
	}

}