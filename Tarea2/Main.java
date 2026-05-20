import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String rutaArchivo = "fuente.txt";
        String contenido;

        try {
            BufferedReader lector = new BufferedReader(new FileReader(rutaArchivo));
            StringBuilder sb = new StringBuilder();
            String linea;
            while ((linea = lector.readLine()) != null) {
                sb.append(linea).append("\n");
            }
            lector.close();
            contenido = sb.toString();
        } catch (IOException e) {
            System.err.println("Error al leer el archivo '" + rutaArchivo + "': " + e.getMessage());
            System.exit(1);
            return;
        }

        System.out.println("Archivo: " + rutaArchivo);
        System.out.println();

        AnalizadorLexico lexico         = new AnalizadorLexico(contenido);
        AnalizadorSintactico analizador = new AnalizadorSintactico(lexico);

        boolean correcto = analizador.analizar();

        System.exit(correcto ? 0 : 1);
    }
}
