import java.util.ArrayList;
import java.util.List;

public class AnalizadorSintactico {
    private final AnalizadorLexico lexico;
    private Token tokenActual;
    private final List<String> errores;
    private int contadorErrores;
    private int nivelIndent;

    public AnalizadorSintactico(AnalizadorLexico lexico) {
        this.lexico          = lexico;
        this.errores         = new ArrayList<>();
        this.contadorErrores = 0;
        this.nivelIndent     = 0;                  
        this.tokenActual     = lexico.siguienteToken();
    }

    private String indent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nivelIndent; i++) sb.append('\t');
        return sb.toString();
    }

    private String sinComillas(String lexema) {
        if (lexema.length() >= 2 && lexema.charAt(0) == '"' && lexema.charAt(lexema.length() - 1) == '"') {
            return lexema.substring(1, lexema.length() - 1);
        }
        return lexema;
    }

    public String analizar() {                        
        System.out.println("=== Análisis Sintáctico Descendente Recursivo ===\n");

        String xml = analizarJson();                        

        System.out.println();
        if (contadorErrores == 0) {
            System.out.println(">>> RESULTADO: El archivo fuente es SINTÁCTICAMENTE CORRECTO. <<<");
        } else {
            System.out.println(">>> RESULTADO: Se encontraron " + contadorErrores + " error(es) sintáctico(s). <<<");
        }

        return xml;                                              
    }

    public boolean hayErrores()       { return contadorErrores > 0; }
    public List<String> getErrores()  { return errores; }

    private String analizarJson() {                         
        if (tokenActual.tipo == TipoToken.EOF) {
            reportarError("El archivo está vacío. Se esperaba '{' o '['.");
            return "";
        }
        String xml = analizarElemento();                       
        consumir(TipoToken.EOF);
        return xml;                                            
    }

    private String analizarElemento() {                         
        if (tokenActual.tipo == TipoToken.L_LLAVE) {
            return analizarObjeto();                             
        } else if (tokenActual.tipo == TipoToken.L_CORCHETE) {
            return analizarArray();                             
        } else {
            reportarError("Se esperaba '{' o '[' para iniciar un elemento, " + "pero se encontró '" + tokenActual.lexema + "'.");
            panicMode(TipoToken.R_LLAVE, TipoToken.R_CORCHETE, TipoToken.COMA, TipoToken.EOF);
            return "";                                          
        }
    }

    private String analizarObjeto() {                           
        consumir(TipoToken.L_LLAVE);
        String xml = "";                                         

        if (tokenActual.tipo == TipoToken.R_LLAVE) {
            consumir(TipoToken.R_LLAVE);

        } else if (tokenActual.tipo == TipoToken.LITERAL_CADENA) {
            xml = analizarListaAtributos();                     

            if (!consumirConPanic(TipoToken.R_LLAVE, "Se esperaba '}' para cerrar el objeto", TipoToken.R_LLAVE, TipoToken.R_CORCHETE, TipoToken.COMA, TipoToken.EOF)) {
                if (tokenActual.tipo == TipoToken.R_LLAVE) consumir(TipoToken.R_LLAVE);
            }

        } else if (tokenActual.tipo != TipoToken.EOF) {
            reportarError("Se esperaba un atributo (cadena) o '}' dentro del objeto, " + "pero se encontró '" + tokenActual.lexema + "'.");
            panicMode(TipoToken.R_LLAVE, TipoToken.EOF);
            if (tokenActual.tipo == TipoToken.R_LLAVE) consumir(TipoToken.R_LLAVE);
        }

        return xml;                                             
    }

    private String analizarArray() {                            
        consumir(TipoToken.L_CORCHETE);

        if (tokenActual.tipo == TipoToken.R_CORCHETE) {
            consumir(TipoToken.R_CORCHETE);
            return null;                                      

        } else if (tokenActual.tipo == TipoToken.L_LLAVE || tokenActual.tipo == TipoToken.L_CORCHETE) {
            String xml = analizarListaElementos();             

            if (!consumirConPanic(TipoToken.R_CORCHETE, "Se esperaba ']' para cerrar el array", TipoToken.R_CORCHETE, TipoToken.R_LLAVE, TipoToken.COMA, TipoToken.EOF)) {
                if (tokenActual.tipo == TipoToken.R_CORCHETE) consumir(TipoToken.R_CORCHETE);
            }
            return xml;                                         

        } else if (tokenActual.tipo != TipoToken.EOF) {
            reportarError("Se esperaba un elemento ('{' o '[') o ']' dentro del array, " + "pero se encontró '" + tokenActual.lexema + "'.");
            panicMode(TipoToken.R_CORCHETE, TipoToken.EOF);
            if (tokenActual.tipo == TipoToken.R_CORCHETE) consumir(TipoToken.R_CORCHETE);
        }

        return "";                                              
    }
    
    private String analizarListaElementos() {                    
        StringBuilder xml = new StringBuilder();
        xml.append(analizarItem());                             

        while (tokenActual.tipo == TipoToken.COMA) {
            consumir(TipoToken.COMA);

            if (tokenActual.tipo == TipoToken.L_LLAVE || tokenActual.tipo == TipoToken.L_CORCHETE) {
                xml.append(analizarItem());                      
            } else {
                reportarError("Se esperaba un elemento ('{' o '[') después de ',', " + "pero se encontró '" + tokenActual.lexema + "'.");
                panicMode(TipoToken.R_CORCHETE, TipoToken.R_LLAVE, TipoToken.EOF);
                break;
            }
        }

        return xml.toString();                                  
    }

    private String analizarItem() {
        String abre   = indent() + "<item>\n";
        nivelIndent++;
        String contenido = analizarElementoInterno();  
        nivelIndent--;
        String cierra = indent() + "</item>\n";
        return abre + contenido + cierra;
    }
   
    private String analizarElementoInterno() {
        if (tokenActual.tipo == TipoToken.L_LLAVE) {
            return analizarObjeto();
        } else if (tokenActual.tipo == TipoToken.L_CORCHETE) {
            return analizarArray() != null ? analizarArray() : "";
        } else {
            reportarError("Se esperaba '{' o '[' dentro del array, " + "pero se encontró '" + tokenActual.lexema + "'.");
            panicMode(TipoToken.R_LLAVE, TipoToken.R_CORCHETE, TipoToken.COMA, TipoToken.EOF);
            return "";
        }
    }

    private String analizarListaAtributos() {                    
        StringBuilder xml = new StringBuilder();
        xml.append(analizarAtributo());                         

        while (tokenActual.tipo == TipoToken.COMA) {
            consumir(TipoToken.COMA);

            if (tokenActual.tipo == TipoToken.LITERAL_CADENA) {
                xml.append(analizarAtributo());               

            } else if (tokenActual.tipo != TipoToken.R_LLAVE && tokenActual.tipo != TipoToken.EOF) {
                reportarError("Se esperaba el nombre de un atributo (cadena) después de ',', " + "pero se encontró '" + tokenActual.lexema + "'.");
                panicMode(TipoToken.LITERAL_CADENA, TipoToken.R_LLAVE, TipoToken.EOF);
                if (tokenActual.tipo == TipoToken.LITERAL_CADENA) {
                    xml.append(analizarAtributo());         
                } else {
                    break;
                }
            } else {
                reportarError("Coma (',') extra antes de '}'.");
                break;
            }
        }

        return xml.toString();                              
    }

    private String analizarAtributo() {                          
        String tag = analizarNombreAtributo();                  

        if (tokenActual.tipo == TipoToken.DOS_PUNTOS) {
            consumir(TipoToken.DOS_PUNTOS);
            return analizarValorAtributo(tag);                  
        } else {
            reportarError("Se esperaba ':' después del nombre del atributo '" + tokenActual.lexema + "'.");
            panicMode(TipoToken.COMA, TipoToken.R_LLAVE, TipoToken.EOF);
            return "";                                          
        }
    }

    private String analizarNombreAtributo() {                   
        if (tokenActual.tipo != TipoToken.LITERAL_CADENA) {
            reportarError("Se esperaba un nombre de atributo (cadena), " + "pero se encontró '" + tokenActual.lexema + "'.");
            panicMode(TipoToken.DOS_PUNTOS, TipoToken.COMA, TipoToken.R_LLAVE, TipoToken.EOF);
            return "_error";                                     
        }
        String nombre = sinComillas(tokenActual.lexema);        
        consumir(TipoToken.LITERAL_CADENA);
        return nombre;                                           
    }
    
    private String analizarValorAtributo(String tag) {           
        switch (tokenActual.tipo) {
            case L_LLAVE: {
                String abre = indent() + "<" + tag + ">\n";
                nivelIndent++;
                String contenido = analizarObjeto();             
                nivelIndent--;
                String cierra = indent() + "</" + tag + ">\n";
                return abre + contenido + cierra;
            }
   
            case L_CORCHETE: {
                String abre = indent() + "<" + tag + ">\n";
                nivelIndent++;
                String contenido = analizarArray();             
                nivelIndent--;
                if (contenido == null) {
                    return indent() + "<" + tag + "></" + tag + ">\n";
                }
                String cierra = indent() + "</" + tag + ">\n";
                return abre + contenido + cierra;
            }

            case LITERAL_CADENA: {
                String val = tokenActual.lexema;
                consumir(TipoToken.LITERAL_CADENA);
                return indent() + "<" + tag + ">" + val + "</" + tag + ">\n";
            }
            case LITERAL_NUM: {
                String val = tokenActual.lexema;
                consumir(TipoToken.LITERAL_NUM);
                return indent() + "<" + tag + ">" + val + "</" + tag + ">\n";
            }
            case PR_TRUE: {
                consumir(TipoToken.PR_TRUE);
                return indent() + "<" + tag + ">true</" + tag + ">\n";
            }
            case PR_FALSE: {
                consumir(TipoToken.PR_FALSE);
                return indent() + "<" + tag + ">false</" + tag + ">\n";
            }
            case PR_NULL: {
                consumir(TipoToken.PR_NULL);
                return indent() + "<" + tag + ">null</" + tag + ">\n";
            }

            default:
                reportarError("Valor de atributo inválido: '" + tokenActual.lexema + "'. " + "Se esperaba un objeto, array, cadena, número, true, false o null.");
                panicMode(TipoToken.COMA, TipoToken.R_LLAVE, TipoToken.R_CORCHETE, TipoToken.EOF);
                return "";
        }
    }

    private void consumir(TipoToken esperado) {
        if (tokenActual.tipo == esperado) {
            tokenActual = lexico.siguienteToken();
        } else {
            reportarError("Se esperaba '" + descripcionToken(esperado)
                    + "' pero se encontró '" + tokenActual.lexema + "'.");
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
