import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

class Lexer {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            Scanner sc = new Scanner(System.in);

            System.out.print("Archivo fuente: ");
            String src = sc.nextLine().trim();

            System.out.print("Archivo salida: ");
            String dst = sc.nextLine().trim();

            sc.close();

            args = new String[]{ src, dst };
        }

        BufferedReader entrada = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), StandardCharsets.UTF_8));
        PrintWriter salida = new PrintWriter(new OutputStreamWriter(new FileOutputStream(args[1]), StandardCharsets.UTF_8));

        int numeroDeLinea = 0;

        String linea;

        while ((linea = entrada.readLine()) != null) {
            numeroDeLinea++;
            procesarLinea(linea, numeroDeLinea, salida);
        }

        salida.println("EOF");

        entrada.close();
        salida.close();

        System.out.println("Terminado.");
    }

    static void procesarLinea(String linea, int numeroDeLinea, PrintWriter salida) {
        String tokensEncontrados = "";

        int i = 0;

        while (i < linea.length()) {
            char c = linea.charAt(i);

            if (c == ' ' || c == '\t') {
                i++;
                continue;
            }

            if (c == '{') { tokensEncontrados += " L_LLAVE";    i++; continue; }
            if (c == '}') { tokensEncontrados += " R_LLAVE";    i++; continue; }
            if (c == '[') { tokensEncontrados += " L_CORCHETE"; i++; continue; }
            if (c == ']') { tokensEncontrados += " R_CORCHETE"; i++; continue; }
            if (c == ',') { tokensEncontrados += " COMA";       i++; continue; }
            if (c == ':') { tokensEncontrados += " DOS_PUNTOS"; i++; continue; }

            if (c == '"') {
                i++;

                while (i < linea.length() && linea.charAt(i) != '"') {
                    if (linea.charAt(i) == '\\') {
                        i += 2;
                    } else {
                        i++;
                    }
                }

                if (i >= linea.length()) {
                    salida.println("Error en línea " + numeroDeLinea + ": cadena sin cerrar");
                    return;
                }

                i++;
                tokensEncontrados += " STRING";
                continue;
            }

            if (Character.isDigit(c)) {
                while (i < linea.length() && Character.isDigit(linea.charAt(i))) i++;

                if (i < linea.length() && linea.charAt(i) == '.') {
                    i++;

                    if (i >= linea.length() || !Character.isDigit(linea.charAt(i))) {
                        salida.println("Error en línea " + numeroDeLinea + ": número mal formado");
                        return;
                    }

                    while (i < linea.length() && Character.isDigit(linea.charAt(i))) i++;
                }

                if (i < linea.length() && (linea.charAt(i) == 'e' || linea.charAt(i) == 'E')) {
                    i++;

                    if (i < linea.length() && (linea.charAt(i) == '+' || linea.charAt(i) == '-')) i++;

                    if (i >= linea.length() || !Character.isDigit(linea.charAt(i))) {
                        salida.println("Error en línea " + numeroDeLinea + ": exponente mal formado");
                        return;
                    }

                    while (i < linea.length() && Character.isDigit(linea.charAt(i))) i++;
                }

                tokensEncontrados += " NUMBER";
                continue;
            }

            if (Character.isLetter(c)) {
                int inicio = i;

                while (i < linea.length() && Character.isLetterOrDigit(linea.charAt(i))) i++;

                String palabra = linea.substring(inicio, i).toLowerCase();

                if      (palabra.equals("true"))  tokensEncontrados += " PR_TRUE";
                else if (palabra.equals("false")) tokensEncontrados += " PR_FALSE";
                else if (palabra.equals("null"))  tokensEncontrados += " PR_NULL";
                else {
                    salida.println("Error en línea " + numeroDeLinea + ": palabra desconocida '" + palabra + "'");
                    return;
                }
                continue;
            }

            salida.println("Error en línea " + numeroDeLinea + ": carácter inválido '" + c + "'");
            return;
        }

        if (!tokensEncontrados.isEmpty()) {
            String indentacion = obtenerIndentacion(linea);
            salida.println(indentacion + tokensEncontrados.trim());
        }
    }

    static String obtenerIndentacion(String linea) {
        int i = 0;

        while (i < linea.length() && (linea.charAt(i) == ' ' || linea.charAt(i) == '\t')) i++;

        return linea.substring(0, i);
    }
}
 