package agencias;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class ExtractSQLAgencia {

	public static void main(String[] args) throws IOException {
		readFiles();
	}

	private static void readFiles() throws IOException {
		StringBuilder builder = new StringBuilder();

		File dir = new File("/home/nedel/workspace/ast2classtransformer-test/src/main/java/agencias/files");
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			for (File child : directoryListing) {
				readFile(child, builder);
			}
		}

		System.out.println(builder.toString());
	}

	private static void readFile(File file, StringBuilder builder) throws IOException {
		List<String> lines = Files.readAllLines(Paths.get(file.getPath()), Charset.defaultCharset());

		String bankId = getBankId(lines.get(0));
		ListIterator<String> iterator = lines.listIterator(3);

		while (iterator.hasNext()) {
			String line = iterator.next();
			String[] split = line.split("\\|");

			if (split.length == 4) {
				String agenciaId = StringUtils.leftPad(split[0], 4, "0").trim();
				String noAgencia = split[1].replace("'", "''").trim();
				String uf = split[2].trim();
				String municipio = split[3].replace("'", "''").trim();

				builder.append(
						"INSERT INTO DBSIG.TB_AGENCIA_BANCARIA (CO_AGENCIA_BANCARIA, CO_BANCO, NO_AGENCIA, CO_UF, NO_MUNICIPIO, ST_REGISTRO_ATIVO) VALUES ('" + agenciaId + "', '"
								+ bankId + "', '" + noAgencia + "', '" + uf + "', '" + municipio + "', 'S');\n");
			}
		}
	}

	private static String getBankId(String bankName) {
		Pattern p = Pattern.compile("\\d+");
		Matcher m = p.matcher(bankName);

		while (m.find()) {
			return m.group();
		}

		return null;
	}
}
