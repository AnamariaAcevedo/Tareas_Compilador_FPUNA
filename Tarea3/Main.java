import java.io.*;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        String rutaFuente  = "fuente.txt";
        String rutaSalida  = "salida.xml";
        String contenido;

        try {
            BufferedReader lector = new BufferedReader(new InputStreamReader(new FileInputStream(rutaFuente), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String linea;
            while ((linea = lector.readLine()) != null) {
                sb.append(linea).append("\n");
            }
            lector.close();
            contenido = sb.toString();
        } catch (IOException e) {
            System.err.println("Error al leer '" + rutaFuente + "': " + e.getMessage());
            System.exit(1);
            return;
        }

        System.out.println("Archivo fuente: " + rutaFuente);
        System.out.println();

        AnalizadorLexico lexico     = new AnalizadorLexico(contenido);
        AnalizadorSintactico analizador = new AnalizadorSintactico(lexico);

        String xml = analizador.analizar();

        if (!analizador.hayErrores() && xml != null && !xml.isEmpty()) {
            try (PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(
                        new FileOutputStream(rutaSalida), StandardCharsets.UTF_8))) {
                pw.print(xml);
                pw.println();              
            } catch (IOException e) {
                System.err.println("Error al escribir '" + rutaSalida + "': " + e.getMessage());
                System.exit(2);
            }
            System.out.println("\nArchivo XML generado: " + rutaSalida);
            System.out.println("\n" + xml);
        } else if (analizador.hayErrores()) {
            System.err.println("\nNo se generó XML debido a errores sintácticos.");
        }

        System.exit(analizador.hayErrores() ? 1 : 0);
    }
}
