package transformer;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsModel;

public class TypeScriptMavenPropertiesExtension extends EmitterExtension {

	@Override
	public EmitterExtensionFeatures getFeatures() {
		EmitterExtensionFeatures features = new EmitterExtensionFeatures();
		features.generatesRuntimeCode = true;
		return features;
	}

	@Override
	public void emitElements(Writer writer, Settings settings, boolean exportKeyword, TsModel model) {
		PropertiesReader reader = new PropertiesReader("static-properties.properties");
		String versionProperty = reader.getProperty("project.version");

		writer.writeIndentedLine("");
		writer.writeIndentedLine("export const PROJECT_VERSION = '" + versionProperty + "'");
	}
}
