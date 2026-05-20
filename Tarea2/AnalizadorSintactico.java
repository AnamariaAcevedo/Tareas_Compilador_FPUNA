import java.util.ArrayList;
import java.util.List;

public class AnalizadorSintactico {
    private final AnalizadorLexico lexico;
    private Token tokenActual;
    private final List<String> errores;
    private int contadorErrores;

    public AnalizadorSintactico(AnalizadorLexico lexico) {
        this.lexico          = lexico;
        this.errores         = new ArrayList<>();
        this.contadorErrores = 0;
        this.tokenActual = lexico.siguienteToken();
    }

    public boolean analizar() {
        System.out.println("=== Análisis Sintáctico Descendente Recursivo ===\n");

        analizarJson();

        System.out.println();
        if (contadorErrores == 0) {
            System.out.println(">>> RESULTADO: El archivo fuente es SINTÁCTICAMENTE CORRECTO. <<<");
            return true;
        } else {
            System.out.println(">>> RESULTADO: Se encontraron " + contadorErrores + " error(es) sintáctico(s). <<<");
            return false;
        }
    }

    private void analizarJson() {
        if (tokenActual.tipo == TipoToken.EOF) {
            reportarError("El archivo está vacío. Se esperaba '{' o '['.");
            return;
        }
        analizarElemento();
        consumir(TipoToken.EOF);
    }

    private void analizarElemento() {
        if (tokenActual.tipo == TipoToken.L_LLAVE) {
            analizarObjeto();
        } else if (tokenActual.tipo == TipoToken.L_CORCHETE) {
            analizarArray();
        } else {
            reportarError("Se esperaba '{' o '[' para iniciar un elemento, " + "pero se encontró '" + tokenActual.lexema + "'.");
            panicMode(TipoToken.R_LLAVE, TipoToken.R_CORCHETE, TipoToken.COMA, TipoToken.EOF);
        }
    }

    private void analizarObjeto() {
        consumir(TipoToken.L_LLAVE);

        if (tokenActual.tipo == TipoToken.R_LLAVE) {
            consumir(TipoToken.R_LLAVE);

        } else if (tokenActual.tipo == TipoToken.LITERAL_CADENA) {
            analizarListaAtributos();

            if (!consumirConPanic(TipoToken.R_LLAVE, "Se esperaba '}' para cerrar el objeto", TipoToken.R_LLAVE, TipoToken.R_CORCHETE, TipoToken.COMA, TipoToken.EOF)) {
                if (tokenActual.tipo == TipoToken.R_LLAVE) consumir(TipoToken.R_LLAVE);
            }

        } else if (tokenActual.tipo != TipoToken.EOF) {
            reportarError("Se esperaba un atributo (cadena) o '}' dentro del objeto, " + "pero se encontró '" + tokenActual.lexema + "'.");
            panicMode(TipoToken.R_LLAVE, TipoToken.EOF);
            if (tokenActual.tipo == TipoToken.R_LLAVE) consumir(TipoToken.R_LLAVE);
        }
    }

    private void analizarArray() {
        consumir(TipoToken.L_CORCHETE); 

        if (tokenActual.tipo == TipoToken.R_CORCHETE) {
            consumir(TipoToken.R_CORCHETE);

        } else if (tokenActual.tipo == TipoToken.L_LLAVE || tokenActual.tipo == TipoToken.L_CORCHETE) {
            analizarListaElementos();

            if (!consumirConPanic(TipoToken.R_CORCHETE, "Se esperaba ']' para cerrar el array", TipoToken.R_CORCHETE, TipoToken.R_LLAVE, TipoToken.COMA, TipoToken.EOF)) {
                if (tokenActual.tipo == TipoToken.R_CORCHETE) consumir(TipoToken.R_CORCHETE);
            }

        } else if (tokenActual.tipo != TipoToken.EOF) {
            reportarError("Se esperaba un elemento ('{' o '[') o ']' dentro del array, " + "pero se encontró '" + tokenActual.lexema + "'.");
            panicMode(TipoToken.R_CORCHETE, TipoToken.EOF);
            if (tokenActual.tipo == TipoToken.R_CORCHETE) consumir(TipoToken.R_CORCHETE);
        }
    }

    private void analizarListaElementos() {
        analizarElemento();

        while (tokenActual.tipo == TipoToken.COMA) {
            consumir(TipoToken.COMA); 

            if (tokenActual.tipo == TipoToken.L_LLAVE || tokenActual.tipo == TipoToken.L_CORCHETE) {
                analizarElemento();
            } else {
                reportarError("Se esperaba un elemento ('{' o '[') después de ',', " + "pero se encontró '" + tokenActual.lexema + "'.");
                panicMode(TipoToken.R_CORCHETE, TipoToken.R_LLAVE, TipoToken.EOF);
                break;
            }
        }
    }

    private void analizarListaAtributos() {
        analizarAtributo();

        while (tokenActual.tipo == TipoToken.COMA) {
            consumir(TipoToken.COMA);

            if (tokenActual.tipo == TipoToken.LITERAL_CADENA) {
                analizarAtributo();
            } else if (tokenActual.tipo != TipoToken.R_LLAVE && tokenActual.tipo != TipoToken.EOF) {
                reportarError("Se esperaba el nombre de un atributo (cadena) después de ',', " + "pero se encontró '" + tokenActual.lexema + "'.");
                panicMode(TipoToken.LITERAL_CADENA, TipoToken.R_LLAVE, TipoToken.EOF);
                if (tokenActual.tipo == TipoToken.LITERAL_CADENA) {
                    analizarAtributo();
                } else {
                    break;
                }
            } else {
                reportarError("Coma (',') extra antes de '}'.");
                break;
            }
        }
    }

    private void analizarAtributo() {
        analizarNombreAtributo();

        if (tokenActual.tipo == TipoToken.DOS_PUNTOS) {
            consumir(TipoToken.DOS_PUNTOS);
            analizarValorAtributo();
        } else {
            reportarError("Se esperaba ':' después del nombre del atributo '" + tokenActual.lexema + "'.");
            panicMode(TipoToken.COMA, TipoToken.R_LLAVE, TipoToken.EOF);
        }
    }

    private void analizarNombreAtributo() {
        if (tokenActual.tipo != TipoToken.LITERAL_CADENA) {
            reportarError("Se esperaba un nombre de atributo (cadena), " + "pero se encontró '" + tokenActual.lexema + "'.");
            panicMode(TipoToken.DOS_PUNTOS, TipoToken.COMA, TipoToken.R_LLAVE, TipoToken.EOF);
        } else {
            consumir(TipoToken.LITERAL_CADENA);
        }
    }

    private void analizarValorAtributo() {
        switch (tokenActual.tipo) {
            case L_LLAVE:
            case L_CORCHETE:
                analizarElemento();
                break;
            case LITERAL_CADENA:
                consumir(TipoToken.LITERAL_CADENA);
                break;
            case LITERAL_NUM:
                consumir(TipoToken.LITERAL_NUM);
                break;
            case PR_TRUE:
                consumir(TipoToken.PR_TRUE);
                break;
            case PR_FALSE:
                consumir(TipoToken.PR_FALSE);
                break;
            case PR_NULL:
                consumir(TipoToken.PR_NULL);
                break;
            default:
                reportarError("Valor de atributo inválido: '" + tokenActual.lexema + "'. " + "Se esperaba un objeto, array, cadena, número, true, false o null.");
                panicMode(TipoToken.COMA, TipoToken.R_LLAVE, TipoToken.R_CORCHETE, TipoToken.EOF);
                break;
        }
    }

    private void consumir(TipoToken esperado) {
        if (tokenActual.tipo == esperado) {
            tokenActual = lexico.siguienteToken();
        } else {
            reportarError("Se esperaba '" + descripcionToken(esperado) + "' pero se encontró '" + tokenActual.lexema + "'.");
        }
    }

    private boolean consumirConPanic(TipoToken esperado, String mensajeError, TipoToken... sincronizacion) {
        if (tokenActual.tipo == esperado) {
            tokenActual = lexico.siguienteToken();
            return true;
        }
        reportarError(mensajeError + ", pero se encontró '" + tokenActual.lexema + "'.");
        panicMode(sincronizacion);
        return false;
    }

    private void panicMode(TipoToken... sincronizacion) {
        System.out.println("    [Panic Mode] Sincronizando a partir de línea " + tokenActual.linea + "...");

        while (tokenActual.tipo != TipoToken.EOF) {
            for (TipoToken tipo : sincronizacion) {
                if (tokenActual.tipo == tipo) {
                    System.out.println("    [Panic Mode] Sincronizado en '" + tokenActual.lexema + "' (línea " + tokenActual.linea + ").");
                    return;
                }
            }
            tokenActual = lexico.siguienteToken();
        }
        System.out.println("    [Panic Mode] Se alcanzó EOF sin encontrar token de sincronización.");
    }

    private void reportarError(String mensaje) {
        contadorErrores++;
        String error = String.format("ERROR SINTÁCTICO [Línea %d]: %s", tokenActual.linea, mensaje);
        errores.add(error);
        System.out.println(error);
    }

    private String descripcionToken(TipoToken tipo) {
        switch (tipo) {
            case L_CORCHETE:     return "[";
            case R_CORCHETE:     return "]";
            case L_LLAVE:        return "{";
            case R_LLAVE:        return "}";
            case COMA:           return ",";
            case DOS_PUNTOS:     return ":";
            case LITERAL_CADENA: return "cadena";
            case LITERAL_NUM:    return "número";
            case PR_TRUE:        return "true";
            case PR_FALSE:       return "false";
            case PR_NULL:        return "null";
            case EOF:            return "EOF";
            default:             return tipo.toString();
        }
    }
}
