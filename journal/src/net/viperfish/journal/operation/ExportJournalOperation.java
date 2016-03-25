package net.viperfish.journal.operation;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import net.viperfish.journal.framework.InjectedOperation;
import net.viperfish.journal.framework.Journal;
import net.viperfish.utils.file.IOFile;
import net.viperfish.utils.file.TextIOStreamHandler;
import net.viperfish.utils.serialization.JsonGenerator;

class ExportJournalOperation extends InjectedOperation {

	static {
		generator = new JsonGenerator();
	}

	private static final JsonGenerator generator;
	private IOFile outputTarget;

	public ExportJournalOperation(String outputFile) {
		outputTarget = new IOFile(new File(outputFile), new TextIOStreamHandler());
	}

	@Override
	public void execute() {
		List<Journal> allJournals = db().getAll();
		Journal[] toExport = allJournals.toArray(new Journal[1]);
		for (Journal i : toExport) {
			i.setId(null);
		}
		try {
			String result = generator.toJson(toExport);
			outputTarget.write(result, StandardCharsets.UTF_16);
		} catch (JsonGenerationException | JsonMappingException e) {
			throw new RuntimeException(e);
		}
	}

}
