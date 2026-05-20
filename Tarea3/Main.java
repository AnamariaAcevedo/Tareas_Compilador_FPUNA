import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
        Path projectDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();

        String inputPath = projectDir.resolve("fuente.txt").toString();

        String outputPath = projectDir.resolve("fuente.xml").toString();

        String source;
        try {
            source = new String(Files.readAllBytes(Paths.get(inputPath)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("No se pudo leer fuente.txt");
            System.err.println("Asegurate de que exista en: " + projectDir);
            System.exit(1);
            return;
        }

        System.out.println("=== Traduciendo fuente.txt ===\n");

        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        if (!lexer.getErrors().isEmpty()) {
            System.err.println("--- Errores Léxicos ---");
            for (String e : lexer.getErrors()) {
                System.err.println("  " + e);
            }
            System.err.println();
        }

        Parser parser = new Parser(tokens);
        parser.parse();

        if (!parser.getErrors().isEmpty()) {
            System.err.println("--- Errores Sintácticos ---");
            for (String e : parser.getErrors()) {
                System.err.println("  " + e);
            }
            System.err.println();
        }

        boolean hasErrors = !lexer.getErrors().isEmpty() || !parser.getErrors().isEmpty();

        String xmlContent = parser.getXml();

        if (xmlContent != null && !xmlContent.isEmpty()) {
            try (PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(outputPath),
                            StandardCharsets.UTF_8))) {

                pw.print(xmlContent);
            }

            if (hasErrors) {
                System.out.println("Traducción parcial generada: fuente.xml");
            } else {
                System.out.println("Traducción exitosa -> fuente.xml");
                System.out.println("\n--- XML generado ---");
                System.out.println(xmlContent);
            }

        } else {
            System.err.println("✗ No se pudo generar XML debido a errores graves.");
        }

        System.exit(hasErrors ? 1 : 0);
    }
}