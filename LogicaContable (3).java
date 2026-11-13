import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

public class LogicaContable {
    public static ArrayList<String> codigosCuentas = new ArrayList<>();
    public static ArrayList<String> nombresCuentas = new ArrayList<>();
    public static ArrayList<String> tiposCuentas = new ArrayList<>();
    public static ArrayList<Double> saldosCuentas = new ArrayList<>();

    public static ArrayList<Integer> numAsientos = new ArrayList<>();
    public static ArrayList<String> fechasMovimientos = new ArrayList<>();
    public static ArrayList<String> cuentasDebe = new ArrayList<>();
    public static ArrayList<String> cuentasHaber = new ArrayList<>();
    public static ArrayList<Double> importesMovimientos = new ArrayList<>();
    public static ArrayList<String> conceptosMovimientos = new ArrayList<>();

    public static final String SIN_CUENTA = "---";

    public static final int T_DEBE = 0, T_HABER = 1, T_SALDO = 2, T_NDEBE = 3, T_NHABER = 4;
    public static final double[] TOT = new double[5];

    private static final String ARCHIVO_CSV = "Cuentas.csv";
    private static final String ARCHIVO_SUBGRUPOS = "subgrupos.csv";
    private static final String ARCHIVO_TRANSACCIONES = "transacciones.csv";
    private static final String ARCHIVO_DIARIO_TXT = "libro_diario.txt";
    private static final String ARCHIVO_MAYOR_TXT = "libro_mayor.txt";
    private static final String ARCHIVO_ESTADOS_TXT = "estados_financieros.txt";

    private static final String FUERA_DE_COMILLAS = "(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
    private static final String COMA_FUERA_DE_COMILLAS = "," + FUERA_DE_COMILLAS;
    private static final Pattern DIACRITICOS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private static final int C_TODOS = 0, C_FECHAS = 1, C_TIPO_EXACTO = 2, C_TIPO_CONTIENE = 3, C_CUENTA = 4, C_CONCEPTO = 5;
    private static final int MODO_ER = 0, MODO_BAL = 1;

    private static final ArrayList<String> SUB_CODIGO = new ArrayList<>();
    private static final ArrayList<String> SUB_TIPO = new ArrayList<>();
    private static final ArrayList<String> SUB_DESCRIPCION = new ArrayList<>();

    private static final String[] RESPALDO_SUBGRUPOS = {
        "1.01,ACTIVO CIRCULANTE", "1.02,ACTIVO NO CIRCULANTE", "1.03,ACTIVO NO CIRCULANTE - INTANGIBLE",
        "1.04,ACTIVO NO CIRCULANTE", "2.01,PASIVO A CORTO PLAZO", "2.02,PASIVO A LARGO PLAZO",
        "3.01,CAPITAL CONTRIBUIDO", "3.02,CAPITAL GANADO", "4.01,INGRESOS - VENTAS",
        "4.02,DEDUCCIONES SOBRE VENTAS", "4.03,OTROS INGRESOS", "5.01,COMPRAS Y COSTO DE VENTAS",
        "5.02,DEDUCCIONES SOBRE COMPRAS", "5.03,GASTOS DE VENTA", "5.04,GASTOS DE ADMINISTRACIÓN",
        "5.05,GASTOS FINANCIEROS", "5.06,GASTOS POR IMPUESTOS", "5.07,OTROS GASTOS"
    };

    private static final String[] CLAVES_CONTRA_ACTIVO = {
        "depreciacion acumulada", "amortizacion acumulada", "estimacion para cuentas incobrables",
        "provision para cuentas incobrables", "estimacion para deudores incobrables"
    };
    private static final String[] CLAVES_CONTRA_CAPITAL = {
        "perdidas integrales", "perdidas acumuladas", "perdida acumulada", "retiros", "dividendos decretados"
    };

    public static String normalizarTexto(String texto) {
        if (texto == null) return "";
        String nfd = Normalizer.normalize(texto, Normalizer.Form.NFD);
        return DIACRITICOS.matcher(nfd).replaceAll("").toLowerCase().trim();
    }

    public static String limpiarTexto(String texto) {
        if (texto == null) return "";
        return texto.replace("\"", "'").replace(";", ",").replace("\r", " ").replace("\n", " ").trim();
    }

    private static boolean contieneAlguna(String texto, String[] claves) {
        for (String c : claves) if (texto.contains(c)) return true;
        return false;
    }

    private static String repetir(char c, int n) { return String.valueOf(c).repeat(n); }

    private static String centrar(String texto, int ancho) {
        return texto.length() >= ancho ? texto : repetir(' ', (ancho - texto.length()) / 2) + texto;
    }

    private static String ajustar(String texto, int ancho) {
        if (texto == null) return "";
        return texto.length() <= ancho ? texto : texto.substring(0, Math.max(0, ancho - 3)) + "...";
    }

    public static double redondear2(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private static String num(double v) {
        return String.format(Locale.US, "%,.2f", v);
    }

    private static String fmt(double v) {
        if (v < -0.004) return "(" + num(Math.abs(v)) + ")";
        return num(Math.abs(v) < 0.005 ? 0.0 : v) + " ";
    }

    public static String obtenerFechaActual() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public static String marco(int ancho, String titulo) {
        String linea = repetir('=', ancho);
        return "\n" + linea + "\n" + titulo + "\n" + linea;
    }

    private static void mostrarLineas(ArrayList<String> lineas) {
        for (String l : lineas) System.out.println(l);
    }

    public static String leer(Scanner sc, String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    public static int leerEntero(Scanner sc, String prompt, int min, int max) {
        while (true) {
            String t = leer(sc, prompt);
            if (t.isEmpty()) {
                System.out.println("-> No puede dejar este campo vacío.");
            } else if (!t.matches("-?\\d{1,9}")) {
                System.out.println("-> Error: solo se permiten números enteros (sin letras, símbolos ni decimales). Usted escribió: '" + t + "'.");
            } else {
                int v = Integer.parseInt(t);
                if (v >= min && v <= max) return v;
                System.out.println("-> Fuera de rango: ingrese un número entre " + min + " y " + max + ".");
            }
        }
    }

    public static int menu(Scanner sc, String prompt, int max, String... lineas) {
        for (String l : lineas) System.out.println(l);
        return leerEntero(sc, prompt, 1, max);
    }

    public static double convertirMonto(String texto) {
        if (texto == null) return Double.NaN;
        String s = texto.toUpperCase().replace("C$", "").replace("$", "").replace(" ", "");
        if (s.isEmpty()) return Double.NaN;
        int ultimaComa = s.lastIndexOf(',');
        int ultimoPunto = s.lastIndexOf('.');
        if (ultimaComa >= 0 && ultimoPunto >= 0) {
            if (ultimaComa > ultimoPunto) s = s.replace(".", "").replace(',', '.');
            else s = s.replace(",", "");
        } else if (ultimaComa >= 0) {
            s = s.matches("\\d{1,3}(,\\d{3})+") ? s.replace(",", "") : s.replace(',', '.');
        }
        if (!s.matches("\\d+(\\.\\d{1,2})?")) return Double.NaN;
        return Double.parseDouble(s);
    }

    public static double leerMonto(Scanner sc, String prompt) {
        while (true) {
            String t = leer(sc, prompt);
            if (t.isEmpty()) {
                System.out.println("-> No puede dejar el monto vacío.");
                continue;
            }
            double v = convertirMonto(t);
            if (Double.isNaN(v)) System.out.println("-> Monto inválido: '" + t + "'. Use solo números, con máximo 2 decimales (ej. 1500 / 1500.75 / 1,500.75).");
            else if (v <= 0) System.out.println("-> El monto debe ser mayor que cero.");
            else if (v > 999999999999.0) System.out.println("-> El monto es demasiado grande.");
            else return redondear2(v);
        }
    }

    public static String leerTextoObligatorio(Scanner sc, String prompt, int maxLargo) {
        while (true) {
            String t = limpiarTexto(leer(sc, prompt));
            if (t.isEmpty()) System.out.println("-> Este campo es obligatorio y no puede quedar vacío.");
            else if (t.length() > maxLargo) System.out.println("-> Texto demasiado largo (máximo " + maxLargo + " caracteres).");
            else return t;
        }
    }

    public static boolean leerSiNo(Scanner sc, String prompt) {
        while (true) {
            String t = normalizarTexto(leer(sc, prompt));
            if (t.equals("s") || t.equals("si")) return true;
            if (t.equals("n") || t.equals("no")) return false;
            System.out.println("-> Responda únicamente con 's' (sí) o 'n' (no).");
        }
    }

    public static boolean esBisiesto(int a) {
        return (a % 4 == 0 && a % 100 != 0) || (a % 400 == 0);
    }

    public static int diasDelMes(int mes, int anio) {
        if (mes == 4 || mes == 6 || mes == 9 || mes == 11) return 30;
        if (mes == 2) return esBisiesto(anio) ? 29 : 28;
        return 31;
    }

    public static String normalizarFecha(String texto) {
        if (texto == null) return null;
        String[] p = texto.trim().split("[/\\-.]");
        if (p.length != 3) return null;
        for (String parte : p) if (!parte.matches("\\d+")) return null;
        if (p[0].length() > 2 || p[1].length() > 2 || p[2].length() != 4) return null;
        int d = Integer.parseInt(p[0]), m = Integer.parseInt(p[1]), a = Integer.parseInt(p[2]);
        if (a < 2000 || a > 2100 || m < 1 || m > 12 || d < 1 || d > diasDelMes(m, a)) return null;
        return String.format("%02d/%02d/%04d", d, m, a);
    }

    public static int fechaAClave(String fecha) {
        String f = normalizarFecha(fecha);
        if (f == null) return -1;
        return Integer.parseInt(f.substring(6, 10)) * 10000 + Integer.parseInt(f.substring(3, 5)) * 100
                + Integer.parseInt(f.substring(0, 2));
    }

    public static String leerFecha(Scanner sc, String prompt, String porDefecto) {
        while (true) {
            String t = leer(sc, prompt);
            if (t.isEmpty() && porDefecto != null) return porDefecto;
            String f = normalizarFecha(t);
            if (f != null) return f;
            System.out.println("-> Fecha inválida. Use DD/MM/AAAA con una fecha real (ej. 01/11/2026; año entre 2000 y 2100).");
        }
    }

    private static ArrayList<String> leerLineas(String ruta) throws IOException {
        ArrayList<String> lineas = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(ruta), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = br.readLine()) != null) lineas.add(linea);
        }
        return lineas;
    }

    private static void guardarLineas(String ruta, ArrayList<String> lineas, String errorPrefijo) {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(ruta), StandardCharsets.UTF_8))) {
            for (String l : lineas) pw.println(l);
        } catch (IOException e) {
            System.out.println(errorPrefijo + e.getMessage());
        }
    }

    private static void mostrarArchivo(String ruta, String nombre) {
        try {
            mostrarLineas(leerLineas(ruta));
        } catch (IOException e) {
            System.out.println("Error al mostrar el " + nombre + ".");
        }
    }

    private static double parsearNumeroCSV(String texto) {
        try {
            String s = texto.trim().replace("C$", "").replace("$", "").replace(" ", "");
            if (s.contains(",") && !s.contains(".")) s = s.replace(",", ".");
            double v = Double.parseDouble(s);
            return (Double.isNaN(v) || Double.isInfinite(v)) ? Double.NaN : v;
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private static void agregarSubgrupo(String codigo, String tipo, String descripcion) {
        SUB_CODIGO.add(codigo);
        SUB_TIPO.add(tipo);
        SUB_DESCRIPCION.add(descripcion);
    }

    public static void cargarSubgruposCSV() {
        SUB_CODIGO.clear();
        SUB_TIPO.clear();
        SUB_DESCRIPCION.clear();
        if (new File(ARCHIVO_SUBGRUPOS).exists()) {
            try {
                boolean primeraLinea = true;
                for (String linea : leerLineas(ARCHIVO_SUBGRUPOS)) {
                    linea = linea.replace("\uFEFF", "").trim();
                    if (linea.isEmpty()) continue;
                    if (primeraLinea) {
                        primeraLinea = false;
                        if (normalizarTexto(linea).startsWith("codigo")) continue;
                    }
                    String[] p = linea.split(",", 3);
                    String codigo = p[0].trim().replace("\"", "");
                    String tipo = p.length > 1 ? limpiarTexto(p[1].replace("\"", "")) : "";
                    String descripcion = p.length > 2 ? limpiarTexto(p[2].replace("\"", "")) : tipo;
                    if (codigo.matches("\\d\\.\\d{2}") && !tipo.isEmpty() && !SUB_CODIGO.contains(codigo)) {
                        agregarSubgrupo(codigo, tipo, descripcion.isEmpty() ? tipo : descripcion);
                    } else {
                        System.out.println("   (línea de '" + ARCHIVO_SUBGRUPOS + "' ignorada: " + linea + ")");
                    }
                }
            } catch (IOException e) {
                System.out.println("-> Error al leer '" + ARCHIVO_SUBGRUPOS + "': " + e.getMessage());
            }
        } else {
            System.out.println("-> Aviso: No se encontró '" + ARCHIVO_SUBGRUPOS + "'. Se usarán los subgrupos básicos de respaldo.");
        }
        if (SUB_CODIGO.isEmpty()) {
            for (String r : RESPALDO_SUBGRUPOS) {
                String[] p = r.split(",", 2);
                agregarSubgrupo(p[0], p[1], p[1]);
            }
        } else {
            System.out.println("-> Subgrupos cargados correctamente (" + SUB_CODIGO.size() + " subgrupos).");
        }
    }

    public static void cargarCatalogoCSV() {
        if (!new File(ARCHIVO_CSV).exists()) {
            System.out.println("-> Aviso: No se encontró el archivo '" + ARCHIVO_CSV + "'. El catálogo quedará vacío (puede crear cuentas en la opción 1).");
            return;
        }
        try {
            ArrayList<String> lineas = leerLineas(ARCHIVO_CSV);
            for (ArrayList<?> lista : new ArrayList<?>[]{codigosCuentas, nombresCuentas, tiposCuentas, saldosCuentas}) lista.clear();

            int descartadas = 0;
            boolean primeraLinea = true;
            for (int n = 0; n < lineas.size(); n++) {
                String linea = lineas.get(n).replace("\uFEFF", "").trim();
                if (linea.isEmpty()) continue;
                if (primeraLinea) {
                    primeraLinea = false;
                    String l = normalizarTexto(linea);
                    if (l.contains("codigo") || l.contains("nombre")) continue;
                }

                String[] partes = linea.split((linea.contains(";") ? ";" : ",") + FUERA_DE_COMILLAS, -1);
                String codigo = "", nombre = "", tipo = "", aviso = null;
                if (partes.length < 3) {
                    aviso = " del catálogo ignorada: faltan columnas";
                } else {
                    codigo = partes[0].trim().replace("\"", "");
                    nombre = limpiarTexto(partes[1].trim().replace("\"", ""));
                    tipo = partes[2].trim().replace("\"", "");
                    if (codigo.isEmpty() || nombre.isEmpty() || tipo.isEmpty()) aviso = " del catálogo ignorada: código, nombre o tipo vacío";
                    else if (codigosCuentas.contains(codigo)) aviso = " ignorada: el código " + codigo + " está repetido";
                }
                if (aviso != null) {
                    descartadas++;
                    System.out.println("   (línea " + (n + 1) + aviso + ")");
                    continue;
                }
                insertarCuenta(codigosCuentas.size(), codigo, nombre, tipo);
            }
            ordenarCatalogoPorCodigo();
            System.out.println("-> Catálogo cargado correctamente (" + codigosCuentas.size() + " cuentas"
                    + (descartadas > 0 ? ", " + descartadas + " líneas descartadas" : "") + ").");
        } catch (IOException e) {
            System.out.println("-> Error al leer '" + ARCHIVO_CSV + "': " + e.getMessage());
        }
    }

    public static void guardarCatalogoCSV() {
        ArrayList<String> L = new ArrayList<>();
        L.add("Codigo,Nombre,Tipo,Saldo");
        for (int i = 0; i < nombresCuentas.size(); i++) {
            L.add(codigosCuentas.get(i) + ",\"" + nombresCuentas.get(i) + "\"," + tiposCuentas.get(i) + ","
                    + String.format(Locale.US, "%.2f", saldosCuentas.get(i)));
        }
        guardarLineas(ARCHIVO_CSV, L, "Error al escribir en el archivo Cuentas.csv: ");
    }

    private static void insertarCuenta(int pos, String codigo, String nombre, String tipo) {
        codigosCuentas.add(pos, codigo);
        nombresCuentas.add(pos, nombre);
        tiposCuentas.add(pos, tipo);
        saldosCuentas.add(pos, 0.0);
    }

    private static void quitarCuenta(int idx) {
        codigosCuentas.remove(idx);
        nombresCuentas.remove(idx);
        tiposCuentas.remove(idx);
        saldosCuentas.remove(idx);
    }

    public static void ordenarCatalogoPorCodigo() {
        int n = codigosCuentas.size();
        for (int i = 0; i < n - 1; i++) {
            int menor = i;
            for (int j = i + 1; j < n; j++) {
                if (codigosCuentas.get(j).compareTo(codigosCuentas.get(menor)) < 0) menor = j;
            }
            if (menor != i) {
                Collections.swap(codigosCuentas, i, menor);
                Collections.swap(nombresCuentas, i, menor);
                Collections.swap(tiposCuentas, i, menor);
                Collections.swap(saldosCuentas, i, menor);
            }
        }
    }

    private static void agregarMov(int asiento, String fecha, String cDebe, String cHaber, double importe, String concepto) {
        numAsientos.add(asiento);
        fechasMovimientos.add(fecha);
        cuentasDebe.add(cDebe);
        cuentasHaber.add(cHaber);
        importesMovimientos.add(importe);
        conceptosMovimientos.add(concepto);
    }

    public static void guardarTransaccionesCSV() {
        ArrayList<String> L = new ArrayList<>();
        L.add("Asiento,Fecha,Debe,Haber,Importe,Concepto");
        for (int i = 0; i < numAsientos.size(); i++) {
            L.add(numAsientos.get(i) + "," + fechasMovimientos.get(i) + ",\"" + cuentasDebe.get(i) + "\",\""
                    + cuentasHaber.get(i) + "\"," + String.format(Locale.US, "%.2f", importesMovimientos.get(i))
                    + ",\"" + conceptosMovimientos.get(i) + "\"");
        }
        guardarLineas(ARCHIVO_TRANSACCIONES, L, "Error al guardar transacciones: ");
    }

    private static String cargarLineaTransaccion(String linea) {
        String[] p = linea.split(COMA_FUERA_DE_COMILLAS, -1);
        if (p.length < 6) return "faltan columnas";
        int asiento;
        try {
            asiento = Integer.parseInt(p[0].trim());
        } catch (NumberFormatException e) {
            return "número de asiento inválido";
        }
        String fecha = normalizarFecha(p[1].trim());
        String cDebe = p[2].trim().replace("\"", ""), cHaber = p[3].trim().replace("\"", "");
        double importe = parsearNumeroCSV(p[4]);
        String concepto = limpiarTexto(p[5].trim().replace("\"", ""));

        if (asiento <= 0) return "número de asiento inválido";
        if (fecha == null) return "fecha inválida";
        if (Double.isNaN(importe) || importe <= 0) return "importe inválido";
        if (concepto.isEmpty()) return "concepto vacío";
        boolean debeVacio = cDebe.equals(SIN_CUENTA), haberVacio = cHaber.equals(SIN_CUENTA);
        if (debeVacio == haberVacio) return "debe tener una (y solo una) cuenta";

        String cuenta = debeVacio ? cHaber : cDebe;
        int idx = buscarIndiceCuentaPorNombre(cuenta);
        if (idx == -1) return "la cuenta '" + cuenta + "' no existe en el catálogo";
        if (debeVacio) cHaber = nombresCuentas.get(idx); else cDebe = nombresCuentas.get(idx);

        agregarMov(asiento, fecha, cDebe, cHaber, redondear2(importe), concepto);
        return null;
    }

    public static void cargarTransaccionesCSV() {
        if (!new File(ARCHIVO_TRANSACCIONES).exists()) {
            System.out.println("-> Aviso: No se encontró el archivo de transacciones previas (se empezará sin movimientos).");
            recalcularSaldosDesdeMovimientos();
            return;
        }
        try {
            ArrayList<String> lineas = leerLineas(ARCHIVO_TRANSACCIONES);
            for (ArrayList<?> lista : new ArrayList<?>[]{numAsientos, fechasMovimientos, cuentasDebe, cuentasHaber, importesMovimientos, conceptosMovimientos}) {
                lista.clear();
            }
            int descartadas = 0;
            for (int n = 1; n < lineas.size(); n++) {
                if (lineas.get(n).trim().isEmpty()) continue;
                String motivo = cargarLineaTransaccion(lineas.get(n));
                if (motivo != null) {
                    descartadas++;
                    System.out.println("   (línea " + (n + 1) + " de transacciones ignorada: " + motivo + ")");
                }
            }
            System.out.println("-> Transacciones previas cargadas (" + numAsientos.size() + " registros"
                    + (descartadas > 0 ? ", " + descartadas + " descartados" : "") + ").");
            recalcularSaldosDesdeMovimientos();
        } catch (IOException e) {
            System.out.println("-> Error al cargar transacciones: " + e.getMessage());
        }
    }

    public static void guardarTodo() {
        guardarCatalogoCSV();
        guardarTransaccionesCSV();
        guardarLineas(ARCHIVO_DIARIO_TXT, construirDiario(), "Error al generar " + ARCHIVO_DIARIO_TXT + ": ");
        guardarLineas(ARCHIVO_MAYOR_TXT, construirMayor(), "Error al generar " + ARCHIVO_MAYOR_TXT + ": ");
    }

    public static boolean esDeudora(String tipo, String nombre) {
        String t = normalizarTexto(tipo), n = normalizarTexto(nombre);
        if (t.contains("activo")) return !contieneAlguna(n, CLAVES_CONTRA_ACTIVO);
        if (t.contains("pasivo")) return false;
        if (t.contains("capital") || t.contains("patrimonio")) return contieneAlguna(n, CLAVES_CONTRA_CAPITAL);
        if (t.contains("deducciones sobre compras")) return false;
        if (t.contains("deducciones")) return true;
        return !t.contains("ingreso");
    }

    public static boolean esDeudoraIdx(int idx) {
        return esDeudora(tiposCuentas.get(idx), nombresCuentas.get(idx));
    }

    public static boolean ladoDebe(int idxCuenta, int estado) {
        return (estado == 1) == esDeudoraIdx(idxCuenta);
    }

    private static double efecto(double monto, boolean ladoDebe, boolean deudora) {
        return ladoDebe == deudora ? monto : -monto;
    }

    public static int grupoBalance(String tipo) {
        String t = normalizarTexto(tipo);
        if (t.contains("activo")) return t.contains("no circulante") ? 2 : 1;
        if (t.contains("pasivo")) return t.contains("largo plazo") ? 4 : 3;
        if (t.contains("capital") || t.contains("patrimonio")) return t.contains("ganado") ? 6 : 5;
        return 0;
    }

    public static int categoriaER(String tipo) {
        String t = normalizarTexto(tipo);
        if (t.contains("deducciones sobre ventas")) return 2;
        if (t.contains("deducciones sobre compras")) return 4;
        if (t.contains("otros ingresos")) return 11;
        if (t.contains("ingreso")) return 1;
        if (t.equals("costo de ventas")) return 5;
        if (t.contains("compras")) return 3;
        if (t.contains("gastos de venta")) return 6;
        if (t.contains("administracion")) return 7;
        if (t.contains("financiero")) return 8;
        if (t.contains("impuesto")) return 9;
        return (t.contains("gasto") || t.contains("costo")) ? 10 : 0;
    }

    public static int buscarIndiceCuentaPorNombre(String nombre) {
        if (nombre == null) return -1;
        for (int i = 0; i < nombresCuentas.size(); i++) {
            if (nombresCuentas.get(i).equalsIgnoreCase(nombre.trim())) return i;
        }
        return -1;
    }

    public static int buscarIndicePorCodigo(String codigo) {
        int bajo = 0, alto = codigosCuentas.size() - 1;
        String buscado = codigo.trim();
        while (bajo <= alto) {
            int medio = (bajo + alto) / 2;
            int cmp = codigosCuentas.get(medio).compareTo(buscado);
            if (cmp == 0) return medio;
            if (cmp < 0) bajo = medio + 1; else alto = medio - 1;
        }
        return -1;
    }

    private static int buscarIndiceNormalizado(String nombre, int excepto) {
        String buscado = normalizarTexto(nombre);
        for (int i = 0; i < nombresCuentas.size(); i++) {
            if (i != excepto && normalizarTexto(nombresCuentas.get(i)).equals(buscado)) return i;
        }
        return -1;
    }

    public static String obtenerCodigoCuenta(String nombre) {
        int idx = buscarIndiceCuentaPorNombre(nombre);
        return idx != -1 ? codigosCuentas.get(idx) : "S/C";
    }

    public static ArrayList<Integer> buscarCuentasPorTermino(String termino) {
        ArrayList<Integer> resultado = new ArrayList<>();
        String norm = normalizarTexto(termino);
        if (norm.isEmpty()) return resultado;
        String[] palabras = norm.split("\\s+");
        for (int i = 0; i < nombresCuentas.size(); i++) {
            String codigo = normalizarTexto(codigosCuentas.get(i));
            String[] palabrasCuenta = normalizarTexto(nombresCuentas.get(i) + " " + tiposCuentas.get(i)).split("[^a-z0-9]+");
            boolean todas = true;
            for (String p : palabras) {
                boolean encontrada = codigo.contains(p);
                for (int k = 0; k < palabrasCuenta.length && !encontrada; k++) {
                    encontrada = palabrasCuenta[k].startsWith(p);
                }
                if (!encontrada) { todas = false; break; }
            }
            if (todas) resultado.add(i);
        }
        return resultado;
    }

    private static int elegirCuentaDeLista(Scanner sc, ArrayList<Integer> resultados) {
        System.out.println("\n--- CUENTAS ENCONTRADAS (" + resultados.size() + ") ---");
        for (int k = 0; k < resultados.size(); k++) {
            int i = resultados.get(k);
            System.out.println("  [" + (k + 1) + "] [" + codigosCuentas.get(i) + "] " + nombresCuentas.get(i) + "  (" + tiposCuentas.get(i) + ")");
        }
        int op = leerEntero(sc, "Seleccione el número de la cuenta (1-" + resultados.size() + ") o 0 para buscar de nuevo: ", 0, resultados.size());
        return op == 0 ? -1 : resultados.get(op - 1);
    }

    public static int buscarCuentaDirecta(Scanner scanner, String promptMensaje) {
        if (nombresCuentas.isEmpty()) {
            System.out.println("-> El catálogo está vacío. Cree cuentas primero (opción 1 del menú principal).");
            return -1;
        }
        while (true) {
            String input = leer(scanner, promptMensaje);
            if (input.isEmpty()) {
                System.out.println("-> Debe ingresar un término de búsqueda válido (o 'cancelar' para volver).");
                continue;
            }
            String norm = normalizarTexto(input);
            if (norm.equals("cancelar") || norm.equals("0")) return -1;

            ArrayList<Integer> candidatas = null;
            if (norm.equals("buscar") || norm.equals("?")) {
                String termino = leer(scanner, "Ingrese una o varias palabras clave (ej. 'gastos venta', 'caja', 'iva'): ");
                if (termino.isEmpty()) continue;
                candidatas = buscarCuentasPorTermino(termino);
                if (candidatas.isEmpty()) {
                    System.out.println("-> No se encontraron cuentas con ese término. Intente con otra palabra.");
                    continue;
                }
            } else {
                int exacta = buscarIndicePorCodigo(input);
                if (exacta == -1) exacta = buscarIndiceNormalizado(input, -1);
                if (exacta != -1) return exacta;

                ArrayList<Integer> parciales = buscarCuentasPorTermino(input);
                if (parciales.size() == 1) {
                    int i = parciales.get(0);
                    System.out.println("-> Cuenta encontrada: [" + codigosCuentas.get(i) + "] " + nombresCuentas.get(i));
                    return i;
                }
                if (parciales.isEmpty()) {
                    System.out.println("-> Cuenta '" + input + "' no encontrada. Escriba 'buscar' para explorar el catálogo o 'cancelar' para volver.\n");
                    continue;
                }
                System.out.println("\n-> Se encontraron varias cuentas con ese término. Seleccione la correcta:");
                candidatas = parciales;
            }
            int elegido = elegirCuentaDeLista(scanner, candidatas);
            if (elegido != -1) return elegido;
        }
    }

    private static boolean esMovimientoDebe(int j) {
        return !cuentasDebe.get(j).equals(SIN_CUENTA);
    }

    private static String cuentaDelMovimiento(int j) {
        return esMovimientoDebe(j) ? cuentasDebe.get(j) : cuentasHaber.get(j);
    }

    private static void reiniciarTotales() {
        Arrays.fill(TOT, 0.0);
    }

    private static void acumular(double importe, boolean ladoDebe, boolean deudora) {
        int lado = ladoDebe ? T_DEBE : T_HABER;
        TOT[lado] = redondear2(TOT[lado] + importe);
        TOT[ladoDebe ? T_NDEBE : T_NHABER]++;
        TOT[T_SALDO] = redondear2(TOT[T_SALDO] + efecto(importe, ladoDebe, deudora));
    }

    private static void procesarMovimientos(ArrayList<Integer> indices, boolean deudora) {
        for (int j : indices) acumular(importesMovimientos.get(j), esMovimientoDebe(j), deudora);
    }

    public static double[] totalesAsiento(ArrayList<Double> importes, ArrayList<Boolean> esDebe) {
        reiniciarTotales();
        for (int k = 0; k < importes.size(); k++) acumular(importes.get(k), esDebe.get(k), true);
        return TOT;
    }

    public static ArrayList<ArrayList<Integer>> construirIndiceMovimientos() {
        ArrayList<ArrayList<Integer>> indice = new ArrayList<>();
        for (int i = 0; i < nombresCuentas.size(); i++) indice.add(new ArrayList<Integer>());
        for (int j = 0; j < numAsientos.size(); j++) {
            int idx = buscarIndiceCuentaPorNombre(cuentaDelMovimiento(j));
            if (idx != -1) indice.get(idx).add(j);
        }
        return indice;
    }

    public static void recalcularSaldosDesdeMovimientos() {
        ArrayList<ArrayList<Integer>> indice = construirIndiceMovimientos();
        for (int i = 0; i < saldosCuentas.size(); i++) {
            reiniciarTotales();
            procesarMovimientos(indice.get(i), esDeudoraIdx(i));
            saldosCuentas.set(i, TOT[T_SALDO]);
        }
    }

    public static boolean existeAsiento(int numero) {
        return numAsientos.contains(numero);
    }

    public static int siguienteNumeroAsiento() {
        return numAsientos.isEmpty() ? 1 : Collections.max(numAsientos) + 1;
    }

    private static ArrayList<Integer> movimientosDelAsiento(int numero) {
        ArrayList<Integer> lista = new ArrayList<>();
        for (int j = 0; j < numAsientos.size(); j++) if (numAsientos.get(j) == numero) lista.add(j);
        return lista;
    }

    private static ArrayList<Integer> todosLosIndices() {
        ArrayList<Integer> lista = new ArrayList<>();
        for (int j = 0; j < numAsientos.size(); j++) lista.add(j);
        return lista;
    }

    private static int compararMovimientos(int a, int b, int[] claves, boolean ascendente) {
        int c = Integer.compare(claves[a], claves[b]);
        if (c == 0) c = Integer.compare(numAsientos.get(a), numAsientos.get(b));
        if (!ascendente) c = -c;
        if (c != 0) return c;
        if (esMovimientoDebe(a) != esMovimientoDebe(b)) return esMovimientoDebe(a) ? -1 : 1;
        return Integer.compare(a, b);
    }

    public static void ordenarIndicesPorFecha(ArrayList<Integer> indices, boolean ascendente) {
        int[] claves = new int[numAsientos.size()];
        for (int j = 0; j < claves.length; j++) claves[j] = fechaAClave(fechasMovimientos.get(j));
        int n = indices.size();
        for (int i = 0; i < n - 1; i++) {
            int elegido = i;
            for (int k = i + 1; k < n; k++) {
                if (compararMovimientos(indices.get(k), indices.get(elegido), claves, ascendente) < 0) elegido = k;
            }
            if (elegido != i) Collections.swap(indices, i, elegido);
        }
    }

    private static boolean coincide(int j, int criterio, String texto, int desde, int hasta) {
        switch (criterio) {
            case C_FECHAS: {
                int c = fechaAClave(fechasMovimientos.get(j));
                return c >= desde && c <= hasta;
            }
            case C_TIPO_EXACTO:
            case C_TIPO_CONTIENE: {
                int idx = buscarIndiceCuentaPorNombre(cuentaDelMovimiento(j));
                if (idx == -1) return false;
                String tipo = normalizarTexto(tiposCuentas.get(idx));
                return criterio == C_TIPO_EXACTO ? tipo.equals(texto) : tipo.contains(texto);
            }
            case C_CUENTA:
                return cuentaDelMovimiento(j).equalsIgnoreCase(texto);
            case C_CONCEPTO:
                return normalizarTexto(conceptosMovimientos.get(j)).contains(texto);
            default:
                return true;
        }
    }

    private static ArrayList<Integer> filtrar(ArrayList<Integer> base, int criterio, String texto, int desde, int hasta) {
        ArrayList<Integer> resultado = new ArrayList<>();
        for (int j : base) if (coincide(j, criterio, texto, desde, hasta)) resultado.add(j);
        return resultado;
    }

    private static ArrayList<Integer> seleccionarMovimientos(int criterio, String texto, int desde, int hasta, boolean ascendente) {
        ArrayList<Integer> lista = filtrar(todosLosIndices(), criterio, texto, desde, hasta);
        ordenarIndicesPorFecha(lista, ascendente);
        return lista;
    }

    public static int verificarIntegridad(boolean mostrarSiTodoBien) {
        ArrayList<Integer> ids = new ArrayList<>();
        for (int n : numAsientos) if (!ids.contains(n)) ids.add(n);

        int problemas = 0;
        for (int id : ids) {
            reiniciarTotales();
            procesarMovimientos(movimientosDelAsiento(id), true);
            if (Math.abs(TOT[T_DEBE] - TOT[T_HABER]) > 0.005) {
                problemas++;
                System.out.println("[!] El asiento " + id + " está DESCUADRADO: Debe C$ " + num(TOT[T_DEBE])
                        + " vs Haber C$ " + num(TOT[T_HABER]));
            }
        }
        for (int i = 0; i < nombresCuentas.size(); i++) {
            if (grupoBalance(tiposCuentas.get(i)) == 0 && categoriaER(tiposCuentas.get(i)) == 0) {
                problemas++;
                System.out.println("[!] La cuenta '" + nombresCuentas.get(i) + "' tiene un tipo no reconocido ('"
                        + tiposCuentas.get(i) + "') y no aparecerá en los estados financieros.");
            }
        }
        if (problemas == 0 && mostrarSiTodoBien) {
            System.out.println("[OK] Verificación contable: todos los asientos cuadran (" + ids.size() + " asientos).");
        }
        return problemas;
    }

    public static boolean registrarAsiento(int nAsiento, String fecha, String concepto,
                                           ArrayList<String> cuentas, ArrayList<Double> importes,
                                           ArrayList<Boolean> esDebe) {
        if (existeAsiento(nAsiento)) {
            System.out.println("-> Error: el asiento " + nAsiento + " ya existe.");
            return false;
        }
        if (normalizarFecha(fecha) == null || concepto == null || concepto.trim().isEmpty()) {
            System.out.println("-> Error: fecha o concepto inválidos.");
            return false;
        }
        for (int k = 0; k < cuentas.size(); k++) {
            if (buscarIndiceCuentaPorNombre(cuentas.get(k)) == -1 || importes.get(k) <= 0) {
                System.out.println("-> Error: movimiento inválido en la cuenta '" + cuentas.get(k) + "'.");
                return false;
            }
        }
        totalesAsiento(importes, esDebe);
        double sumaDebe = TOT[T_DEBE], sumaHaber = TOT[T_HABER];
        if (sumaDebe <= 0 || sumaHaber <= 0 || Math.abs(sumaDebe - sumaHaber) > 0.005) {
            System.out.println("-> Error: el asiento no cuadra (Debe C$ " + sumaDebe + " vs Haber C$ " + sumaHaber + ").");
            return false;
        }

        String fechaOk = normalizarFecha(fecha);
        for (int pasada = 0; pasada < 2; pasada++) {
            boolean lado = (pasada == 0);
            for (int k = 0; k < cuentas.size(); k++) {
                if (esDebe.get(k) != lado) continue;
                String oficial = nombresCuentas.get(buscarIndiceCuentaPorNombre(cuentas.get(k)));
                agregarMov(nAsiento, fechaOk, lado ? oficial : SIN_CUENTA, lado ? SIN_CUENTA : oficial,
                        importes.get(k), limpiarTexto(concepto));
            }
        }
        recalcularSaldosDesdeMovimientos();
        guardarTodo();
        return true;
    }

    public static ArrayList<String> advertenciasDeSaldo(ArrayList<String> cuentas, ArrayList<Double> importes,
                                                         ArrayList<Boolean> esDebe) {
        ArrayList<String> avisos = new ArrayList<>();
        for (int k = 0; k < cuentas.size(); k++) {
            int idx = buscarIndiceCuentaPorNombre(cuentas.get(k));
            if (idx == -1) continue;
            double proyectado = saldosCuentas.get(idx) + efecto(importes.get(k), esDebe.get(k), esDeudoraIdx(idx));
            if (proyectado < -0.004) {
                avisos.add("'" + nombresCuentas.get(idx) + "' quedaría con saldo C$ " + num(proyectado)
                        + " (contrario a su naturaleza " + (esDeudoraIdx(idx) ? "deudora" : "acreedora") + ").");
            }
        }
        return avisos;
    }

    private static ArrayList<String> construirDiario() {
        ArrayList<String> L = new ArrayList<>();
        String igual = repetir('=', 106), guion = repetir('-', 106);
        String formato = "%-12s | %-45s | %-12s | %-15s | %-15s";
        L.add(igual);
        L.add("Diario General                                                                                    Hoja 1");
        L.add(igual);
        L.add(String.format(formato, "Fecha", "Detalle", "Referencia", "Debe", "Haber"));
        L.add(guion);

        if (numAsientos.isEmpty()) {
            L.add("No hay transacciones registradas en el diario.");
            return L;
        }
        ArrayList<Integer> orden = seleccionarMovimientos(C_TODOS, "", 0, 0, true);
        reiniciarTotales();
        int cantidadAsientos = 0;
        String concepto = "";
        for (int k = 0; k < orden.size(); k++) {
            int i = orden.get(k);
            int nAs = numAsientos.get(i);
            boolean primeraLinea = (k == 0) || numAsientos.get(orden.get(k - 1)) != nAs;
            if (primeraLinea) {
                if (k > 0) {
                    L.add(String.format("             * %s", concepto));
                    L.add(guion);
                }
                cantidadAsientos++;
                L.add(String.format("\n--- Asiento No. %d ---", nAs));
            }
            concepto = conceptosMovimientos.get(i);
            boolean debe = esMovimientoDebe(i);
            double imp = importesMovimientos.get(i);
            acumular(imp, debe, true);
            String cuenta = cuentaDelMovimiento(i);
            String fecha = primeraLinea ? fechasMovimientos.get(i) : "";
            String monto = "C$" + num(imp);
            if (debe) L.add(String.format(formato, fecha, ajustar(cuenta, 45), obtenerCodigoCuenta(cuenta), monto, ""));
            else L.add(String.format("%-12s |    %-42s | %-12s | %-15s | %-15s", fecha, ajustar(cuenta, 42), obtenerCodigoCuenta(cuenta), "", monto));
        }
        L.add(String.format("             * %s", concepto));
        L.add(igual);
        L.add(String.format(formato, "", "TOTALES (" + cantidadAsientos + " asientos)", "", "C$" + num(TOT[T_DEBE]), "C$" + num(TOT[T_HABER])));
        L.add(igual);
        return L;
    }

    public static void generarLibroDiario() {
        guardarLineas(ARCHIVO_DIARIO_TXT, construirDiario(), "Error al generar " + ARCHIVO_DIARIO_TXT + ": ");
        System.out.println("\n-> ¡Libro Diario generado exitosamente en '" + ARCHIVO_DIARIO_TXT + "' con estilo formal!");
        mostrarArchivo(ARCHIVO_DIARIO_TXT, "libro diario");
    }

    private static String formatoSaldoMayor(double saldo) {
        saldo = redondear2(saldo);
        return saldo < 0 ? "(" + num(Math.abs(saldo)) + ")" : num(saldo);
    }

    private static String fechaExtrema(boolean minima) {
        int mejor = -1;
        String texto = "/  /";
        for (int j = 0; j < fechasMovimientos.size(); j++) {
            int c = fechaAClave(fechasMovimientos.get(j));
            if (c == -1) continue;
            if (mejor == -1 || (minima ? c < mejor : c > mejor)) { mejor = c; texto = fechasMovimientos.get(j); }
        }
        return texto;
    }

    private static ArrayList<String> construirMayor() {
        ArrayList<String> L = new ArrayList<>();
        String igual = repetir('=', 106), guion = repetir('-', 106);
        String formato = " %-10s %-10s %-35s %12s %12s %15s";
        L.add(igual);
        L.add("  Mayor de Cuentas                                               Fecha:        " + obtenerFechaActual());
        L.add("                                                                 Hoja :        1");
        L.add("  Ejercicio:   Ejercicio Actual                                  Fecha Desde:  " + fechaExtrema(true));
        L.add("                                                                 Fecha Hasta:  " + fechaExtrema(false));
        L.add(igual);

        ArrayList<Integer> orden = seleccionarMovimientos(C_TODOS, "", 0, 0, true);
        boolean hayAfectadas = false;
        for (int i = 0; i < nombresCuentas.size(); i++) {
            String nombre = nombresCuentas.get(i);
            ArrayList<Integer> movs = filtrar(orden, C_CUENTA, nombre, 0, 0);
            if (movs.isEmpty()) continue;
            hayAfectadas = true;

            L.add("");
            L.add(String.format(formato, "Fecha", "Nro.Minuta", "Concepto", "Debe", "Haber", "Saldo"));
            L.add(igual);
            L.add(String.format("Cuenta: %-15s %-50s", codigosCuentas.get(i), nombre));
            L.add(guion);

            boolean deudora = esDeudoraIdx(i);
            reiniciarTotales();
            for (int j : movs) {
                double imp = importesMovimientos.get(j);
                boolean debe = esMovimientoDebe(j);
                acumular(imp, debe, deudora);
                String concepto = conceptosMovimientos.get(j);
                if (concepto.length() > 33) concepto = concepto.substring(0, 30) + "...";
                L.add(String.format(formato, fechasMovimientos.get(j), String.valueOf(numAsientos.get(j)), concepto,
                        debe ? num(imp) : "", debe ? "" : num(imp), formatoSaldoMayor(TOT[T_SALDO])));
            }
            L.add(guion);
            L.add(String.format(Locale.US, " %-46s %-10s %12s %12s %15s", "", "Subtotal:",
                    num(TOT[T_DEBE]), num(TOT[T_HABER]), formatoSaldoMayor(TOT[T_SALDO])));
            L.add(igual);
        }
        if (!hayAfectadas) L.add("\nNo hay cuentas con movimientos registrados en el periodo.");
        return L;
    }

    public static void generarLibroMayor() {
        guardarLineas(ARCHIVO_MAYOR_TXT, construirMayor(), "Error al generar " + ARCHIVO_MAYOR_TXT + ": ");
        System.out.println("\n-> ¡Libro Mayor generado en '" + ARCHIVO_MAYOR_TXT + "'!");
        System.out.println("\n==========================================================================================");
        System.out.println("                                LIBRO MAYOR DE CUENTAS                                    ");
        System.out.println("==========================================================================================");
        mostrarArchivo(ARCHIVO_MAYOR_TXT, "libro mayor");
    }

    private static boolean imprimirCuentaT(int i, ArrayList<Integer> movimientosDeLaCuenta) {
        if (movimientosDeLaCuenta.isEmpty()) return false;
        boolean esDeudora = esDeudoraIdx(i);
        ArrayList<Integer> orden = new ArrayList<>(movimientosDeLaCuenta);
        ordenarIndicesPorFecha(orden, true);

        ArrayList<String> lineasDebe = new ArrayList<>(), lineasHaber = new ArrayList<>();
        reiniciarTotales();
        for (int j : orden) {
            double imp = importesMovimientos.get(j);
            boolean debe = esMovimientoDebe(j);
            acumular(imp, debe, esDeudora);
            (debe ? lineasDebe : lineasHaber).add(String.format(Locale.US, "%,.2f (As.%d)", imp, numAsientos.get(j)));
        }
        double totalDebe = TOT[T_DEBE], totalHaber = TOT[T_HABER];
        String separador = "--------------------------------+--------------------------------";

        ArrayList<String> L = new ArrayList<>();
        L.add("\nCuenta: " + nombresCuentas.get(i) + " (" + codigosCuentas.get(i) + ") - " + tiposCuentas.get(i).toUpperCase());
        L.add(String.format("%-32s|%-32s", "              DEBE", "              HABER"));
        L.add(separador);
        for (int k = 0; k < Math.max(lineasDebe.size(), lineasHaber.size()); k++) {
            L.add(String.format(" %-30s| %-30s", k < lineasDebe.size() ? lineasDebe.get(k) : "",
                    k < lineasHaber.size() ? lineasHaber.get(k) : ""));
        }
        L.add(separador);
        L.add(String.format(Locale.US, " %-30s| %-30s", "Total Debe: C$ " + num(totalDebe), "Total Haber: C$ " + num(totalHaber)));

        double diferencia = redondear2(totalDebe - totalHaber);
        boolean saldada = Math.abs(diferencia) < 0.005;
        String estatus = saldada ? "Cuenta Saldada / Cerrada" : (diferencia > 0 ? "Saldo Deudor" : "Saldo Acreedor");
        String advertencia = (!saldada && ((diferencia > 0) != esDeudora))
                ? "  [!] contrario a su naturaleza " + (esDeudora ? "deudora" : "acreedora") : "";
        L.add(String.format(Locale.US, " ==> Estado Final: C$ %,.2f (%s)%s", Math.abs(diferencia), estatus, advertencia));
        mostrarLineas(L);
        return true;
    }

    public static void consultarCuentasT(Scanner scanner) {
        int op = menu(scanner, "Seleccione una opción (1-3): ", 3,
                "\n==========================================================================",
                "                     CUENTAS T (RESUMEN OFICIAL)                          ",
                "==========================================================================",
                "1. Ver TODAS las cuentas con movimientos",
                "2. Ver UNA cuenta específica (buscar por nombre o código)",
                "3. Regresar");
        ArrayList<ArrayList<Integer>> indice = construirIndiceMovimientos();
        if (op == 1) {
            boolean hayAfectadas = false;
            for (int i = 0; i < nombresCuentas.size(); i++) {
                if (imprimirCuentaT(i, indice.get(i))) hayAfectadas = true;
            }
            if (!hayAfectadas) System.out.println("\nNo hay cuentas afectadas todavía. Registre un asiento primero (opción 2).");
        } else if (op == 2) {
            int idx = buscarCuentaDirecta(scanner, "Ingrese el nombre, código, 'buscar' o 'cancelar': ");
            if (idx == -1) return;
            if (!imprimirCuentaT(idx, indice.get(idx))) {
                System.out.println("\n-> La cuenta '" + nombresCuentas.get(idx) + "' no tiene movimientos todavía.");
            }
        }
    }

    private static ArrayList<String> construirBalanza() {
        recalcularSaldosDesdeMovimientos();
        ArrayList<String> L = new ArrayList<>();
        int ancho = 113;
        L.add("");
        L.add(repetir('=', ancho));
        L.add(centrar("BALANZA DE COMPROBACIÓN", ancho));
        L.add(repetir('=', ancho));
        L.add(String.format("%-10s | %-32s | %14s | %14s | %14s | %14s",
                "Código", "Nombre de la Cuenta", "Debe", "Haber", "Saldo Deudor", "Saldo Acreedor"));
        L.add(repetir('-', ancho));

        double sumaMovDebe = 0, sumaMovHaber = 0, sumaSaldoDeudor = 0, sumaSaldoAcreedor = 0;
        ArrayList<ArrayList<Integer>> indice = construirIndiceMovimientos();
        for (int i = 0; i < nombresCuentas.size(); i++) {
            if (indice.get(i).isEmpty()) continue;
            reiniciarTotales();
            procesarMovimientos(indice.get(i), esDeudoraIdx(i));
            double movDebe = TOT[T_DEBE], movHaber = TOT[T_HABER];

            double neto = redondear2(movDebe - movHaber);
            double sDeudor = neto > 0 ? neto : 0, sAcreedor = neto < 0 ? -neto : 0;
            sumaMovDebe += movDebe;
            sumaMovHaber += movHaber;
            sumaSaldoDeudor += sDeudor;
            sumaSaldoAcreedor += sAcreedor;
            L.add(String.format(Locale.US, "%-10s | %-32s | %,14.2f | %,14.2f | %,14.2f | %,14.2f",
                    codigosCuentas.get(i), ajustar(nombresCuentas.get(i), 32), movDebe, movHaber, sDeudor, sAcreedor));
        }
        L.add(repetir('-', ancho));
        L.add(String.format(Locale.US, "%-45s | %,14.2f | %,14.2f | %,14.2f | %,14.2f",
                "TOTALES", sumaMovDebe, sumaMovHaber, sumaSaldoDeudor, sumaSaldoAcreedor));
        L.add(repetir('=', ancho));

        if (indice.isEmpty() || numAsientos.isEmpty()) {
            L.add("(Aún no hay movimientos registrados.)");
        } else if (Math.abs(sumaMovDebe - sumaMovHaber) < 0.01 && Math.abs(sumaSaldoDeudor - sumaSaldoAcreedor) < 0.01) {
            L.add("[OK] ¡Balanza de Comprobación cuadrada correctamente! (Debe = Haber y Saldos Deudores = Saldos Acreedores)");
        } else {
            L.add("[!] ADVERTENCIA: La balanza presenta diferencias. Diferencia de movimientos: C$ "
                    + num(Math.abs(sumaMovDebe - sumaMovHaber))
                    + " | Diferencia de saldos: C$ " + num(Math.abs(sumaSaldoDeudor - sumaSaldoAcreedor)));
        }
        return L;
    }

    public static void generarBalanzaComprobacion() {
        mostrarLineas(construirBalanza());
    }

    private static ArrayList<Integer> cuentasDe(int modo, int codigo, boolean soloConSaldo) {
        ArrayList<Integer> lista = new ArrayList<>();
        for (int i = 0; i < nombresCuentas.size(); i++) {
            String tipo = tiposCuentas.get(i);
            int g = (modo == MODO_ER) ? categoriaER(tipo) : grupoBalance(tipo);
            if (g == codigo && (!soloConSaldo || Math.abs(saldosCuentas.get(i)) >= 0.005)) lista.add(i);
        }
        return lista;
    }

    private static double totalCategoria(int categoria) {
        double total = 0;
        for (int i : cuentasDe(MODO_ER, categoria, false)) total += saldosCuentas.get(i);
        return redondear2(total);
    }

    public static double calcularUtilidadNeta() {
        double ventasNetas = totalCategoria(1) - totalCategoria(2);
        double costoVentas = (totalCategoria(3) - totalCategoria(4)) + totalCategoria(5);
        double utilidadOperacion = ventasNetas - costoVentas - totalCategoria(6) - totalCategoria(7);
        double antesImpuestos = utilidadOperacion + totalCategoria(11) - totalCategoria(8) - totalCategoria(10);
        return redondear2(antesImpuestos - totalCategoria(9));
    }

    private static void lineaMonto(ArrayList<String> L, int ancho, String etiqueta, double valor) {
        L.add(String.format("%-" + (ancho - 20) + "s%20s", ajustar(etiqueta, ancho - 21), "C$ " + fmt(valor)));
    }

    private static double bloqueCategoria(ArrayList<String> L, int ancho, String titulo, int categoria, boolean mostrarNegativo) {
        double total = totalCategoria(categoria);
        ArrayList<Integer> conSaldo = cuentasDe(MODO_ER, categoria, true);
        double signo = mostrarNegativo ? -1 : 1;
        if (conSaldo.isEmpty()) {
            lineaMonto(L, ancho, titulo, 0.0);
            return total;
        }
        L.add(titulo + ":");
        for (int i : conSaldo) lineaMonto(L, ancho, "     " + nombresCuentas.get(i), signo * saldosCuentas.get(i));
        lineaMonto(L, ancho, "   Total " + titulo.replace("(-) ", "").replace("(+) ", "").toLowerCase().replace("(isr)", "(ISR)"), signo * total);
        return total;
    }

    private static ArrayList<String> construirEstadoResultados() {
        recalcularSaldosDesdeMovimientos();
        ArrayList<String> L = new ArrayList<>();
        int W = 76;
        String igual = repetir('=', W), guion = repetir('-', W);
        L.add("");
        L.add(igual);
        L.add(centrar("ESTADO DE RESULTADOS", W));
        L.add(centrar("(Periodo: todos los movimientos registrados)", W));
        L.add(igual);

        double ventas = bloqueCategoria(L, W, "Ventas", 1, false);
        double dedVentas = bloqueCategoria(L, W, "(-) Devoluciones y rebajas sobre ventas", 2, true);
        double ventasNetas = redondear2(ventas - dedVentas);
        L.add(guion);
        lineaMonto(L, W, "VENTAS NETAS", ventasNetas);
        L.add("");

        double compras = bloqueCategoria(L, W, "Compras y gastos de compras", 3, false);
        double dedCompras = bloqueCategoria(L, W, "(-) Deducciones sobre compras", 4, true);
        double comprasNetas = redondear2(compras - dedCompras);
        lineaMonto(L, W, "COMPRAS NETAS", comprasNetas);
        double costoReg = bloqueCategoria(L, W, "Costo de ventas registrado", 5, false);
        double costoVentas = redondear2(comprasNetas + costoReg);
        L.add(guion);
        lineaMonto(L, W, "(-) COSTO DE VENTAS", -costoVentas);
        double utilidadBruta = redondear2(ventasNetas - costoVentas);
        L.add(igual);
        lineaMonto(L, W, utilidadBruta >= 0 ? "UTILIDAD BRUTA" : "PÉRDIDA BRUTA", utilidadBruta);
        L.add("");

        double gVenta = bloqueCategoria(L, W, "Gastos de venta", 6, false);
        double gAdmin = bloqueCategoria(L, W, "Gastos de administración", 7, false);
        double utilidadOperacion = redondear2(utilidadBruta - gVenta - gAdmin);
        L.add(guion);
        lineaMonto(L, W, "(-) TOTAL GASTOS DE OPERACIÓN", -(gVenta + gAdmin));
        L.add(igual);
        lineaMonto(L, W, utilidadOperacion >= 0 ? "UTILIDAD DE OPERACIÓN" : "PÉRDIDA DE OPERACIÓN", utilidadOperacion);
        L.add("");

        double otrosIng = bloqueCategoria(L, W, "(+) Otros ingresos", 11, false);
        double gFin = bloqueCategoria(L, W, "(-) Gastos financieros", 8, true);
        double otrosGas = bloqueCategoria(L, W, "(-) Otros gastos", 10, true);
        double antesImp = redondear2(utilidadOperacion + otrosIng - gFin - otrosGas);
        L.add(igual);
        lineaMonto(L, W, "UTILIDAD (PÉRDIDA) ANTES DE IMPUESTOS", antesImp);
        double isr = bloqueCategoria(L, W, "(-) Impuesto sobre la renta (ISR)", 9, true);
        double neta = redondear2(antesImp - isr);
        L.add(igual);
        lineaMonto(L, W, neta >= 0 ? "UTILIDAD NETA DEL PERIODO" : "PÉRDIDA NETA DEL PERIODO", neta);
        L.add(igual);
        return L;
    }

    public static void generarEstadoResultados() {
        mostrarLineas(construirEstadoResultados());
    }

    private static double seccionBalance(ArrayList<String> L, int ancho, String titulo, int grupo, boolean grupoActivo) {
        L.add(" " + titulo);
        ArrayList<Integer> conSaldo = cuentasDe(MODO_BAL, grupo, true);
        double total = 0;
        for (int i : conSaldo) {
            double v = efecto(saldosCuentas.get(i), grupoActivo, esDeudoraIdx(i));
            total += v;
            lineaMonto(L, ancho, "     " + nombresCuentas.get(i), v);
        }
        if (conSaldo.isEmpty()) L.add("     (sin saldos)");
        total = redondear2(total);
        lineaMonto(L, ancho, "   Total " + titulo.toLowerCase(), total);
        return total;
    }

    private static ArrayList<String> construirBalanceGeneral() {
        recalcularSaldosDesdeMovimientos();
        ArrayList<String> L = new ArrayList<>();
        int W = 76;
        String igual = repetir('=', W), guion = repetir('-', W);
        L.add("");
        L.add(igual);
        L.add(centrar("BALANCE GENERAL", W));
        L.add(igual);

        L.add("ACTIVO");
        double circ = seccionBalance(L, W, "Activo circulante", 1, true);
        double noCirc = seccionBalance(L, W, "Activo no circulante", 2, true);
        double totalActivo = redondear2(circ + noCirc);
        L.add(guion);
        lineaMonto(L, W, "TOTAL ACTIVO", totalActivo);
        L.add("");

        L.add("PASIVO");
        double corto = seccionBalance(L, W, "Pasivo a corto plazo", 3, false);
        double largo = seccionBalance(L, W, "Pasivo a largo plazo", 4, false);
        double totalPasivo = redondear2(corto + largo);
        L.add(guion);
        lineaMonto(L, W, "TOTAL PASIVO", totalPasivo);
        L.add("");

        L.add("CAPITAL CONTABLE");
        double contribuido = seccionBalance(L, W, "Capital contribuido", 5, false);
        double ganado = seccionBalance(L, W, "Capital ganado", 6, false);
        double utilidad = calcularUtilidadNeta();
        lineaMonto(L, W, utilidad >= 0 ? " Utilidad del periodo (del Estado de Resultados)" : " Pérdida del periodo (del Estado de Resultados)", utilidad);
        double totalCapital = redondear2(contribuido + ganado + utilidad);
        L.add(guion);
        lineaMonto(L, W, "TOTAL CAPITAL CONTABLE", totalCapital);
        L.add(igual);
        double pasivoMasCapital = redondear2(totalPasivo + totalCapital);
        lineaMonto(L, W, "TOTAL PASIVO + CAPITAL", pasivoMasCapital);
        L.add(igual);

        if (Math.abs(totalActivo - pasivoMasCapital) < 0.01) {
            L.add("[OK] Ecuación contable cumplida:  ACTIVO = PASIVO + CAPITAL");
        } else {
            L.add("[!] ADVERTENCIA: Activo y (Pasivo + Capital) no coinciden. Diferencia: C$ " + num(Math.abs(totalActivo - pasivoMasCapital)));
        }
        return L;
    }

    public static void generarBalanceGeneral() {
        mostrarLineas(construirBalanceGeneral());
    }

    private static void guardarEstadosFinancierosArchivo() {
        ArrayList<String> L = construirBalanza();
        L.addAll(construirEstadoResultados());
        L.addAll(construirBalanceGeneral());
        guardarLineas(ARCHIVO_ESTADOS_TXT, L, "Error al generar " + ARCHIVO_ESTADOS_TXT + ": ");
    }

    private static void imprimirListaMovimientos(ArrayList<Integer> indices, String titulo) {
        String igual = repetir('=', 90), guion = repetir('-', 90);
        System.out.println("\n" + igual);
        System.out.println(titulo);
        System.out.println(igual);
        if (indices.isEmpty()) {
            System.out.println("No se encontraron movimientos con los criterios especificados.");
            System.out.println(igual);
            return;
        }
        System.out.println(String.format("%-8s | %-10s | %-25s | %-25s | %-15s | %s",
                "Asiento", "Fecha", "Debe (Carga)", "Haber (Abona)", "Importe", "Concepto"));
        System.out.println(guion);
        for (int j : indices) {
            System.out.println(String.format(Locale.US, "%-8d | %-10s | %-25s | %-25s | C$ %,12.2f | %s",
                    numAsientos.get(j), fechasMovimientos.get(j), ajustar(cuentasDebe.get(j), 25),
                    ajustar(cuentasHaber.get(j), 25), importesMovimientos.get(j), conceptosMovimientos.get(j)));
        }
        reiniciarTotales();
        procesarMovimientos(indices, true);
        System.out.println(guion);
        System.out.println(String.format(Locale.US, "Movimientos mostrados: %d   |   Suma Debe: C$ %,.2f   |   Suma Haber: C$ %,.2f",
                indices.size(), TOT[T_DEBE], TOT[T_HABER]));
        System.out.println(igual);
    }

    private static void ejecutarConsulta(Scanner sc, int opcion) {
        ArrayList<Integer> lista;
        String titulo;
        switch (opcion) {
            case 1:
            case 2:
                lista = seleccionarMovimientos(C_TODOS, "", 0, 0, opcion == 2);
                titulo = opcion == 1 ? "--- MOVIMIENTOS (DE MÁS RECIENTE A MÁS ANTIGUA) ---"
                                     : "--- MOVIMIENTOS (DE MÁS ANTIGUA A MÁS RECIENTE) ---";
                break;
            case 3: {
                String fecha = leerFecha(sc, "Ingrese la fecha a buscar (DD/MM/AAAA): ", null);
                int clave = fechaAClave(fecha);
                lista = seleccionarMovimientos(C_FECHAS, "", clave, clave, true);
                titulo = "--- MOVIMIENTOS EN FECHA: " + fecha + " ---";
                break;
            }
            case 4: {
                System.out.println("\n--- FILTRAR POR CLASIFICACIÓN ---");
                ArrayList<String> tipos = new ArrayList<>();
                for (String t : tiposCuentas) if (!tipos.contains(t)) tipos.add(t);
                for (int k = 0; k < tipos.size(); k++) System.out.println("  [" + (k + 1) + "] " + tipos.get(k));

                String clave = "";
                boolean porNumero = false;
                while (true) {
                    String entrada = leer(sc, "Escriba el número de la lista o una palabra clave (ej. activo, gastos, ventas): ");
                    if (entrada.isEmpty()) {
                        System.out.println("-> Debe ingresar un número o una palabra.");
                    } else if (!entrada.matches("\\d+")) {
                        clave = entrada;
                        break;
                    } else {
                        int n = Integer.parseInt(entrada);
                        if (n >= 1 && n <= tipos.size()) { clave = tipos.get(n - 1); porNumero = true; break; }
                        System.out.println("-> Número fuera de la lista (1-" + tipos.size() + ").");
                    }
                }
                lista = seleccionarMovimientos(porNumero ? C_TIPO_EXACTO : C_TIPO_CONTIENE, normalizarTexto(clave), 0, 0, true);
                titulo = "--- MOVIMIENTOS PARA CLASIFICACIÓN: " + clave.toUpperCase() + " ---";
                break;
            }
            case 5: {
                int idx = buscarCuentaDirecta(sc, "Ingrese el nombre, código, 'buscar' o 'cancelar': ");
                if (idx == -1) return;
                String nombre = nombresCuentas.get(idx);
                lista = seleccionarMovimientos(C_CUENTA, nombre, 0, 0, true);
                if (lista.isEmpty()) {
                    System.out.println("\n-> Aviso: La cuenta '" + nombre + "' no tiene movimientos registrados en el sistema.");
                    return;
                }
                titulo = "--- MOVIMIENTOS PARA LA CUENTA: " + nombre + " ---";
                break;
            }
            case 6: {
                String desde = leerFecha(sc, "Fecha INICIAL (DD/MM/AAAA): ", null);
                String hasta = leerFecha(sc, "Fecha FINAL   (DD/MM/AAAA): ", null);
                if (fechaAClave(desde) > fechaAClave(hasta)) {
                    System.out.println("-> La fecha inicial era mayor que la final; se intercambiaron automáticamente.");
                    String tmp = desde; desde = hasta; hasta = tmp;
                }
                lista = seleccionarMovimientos(C_FECHAS, "", fechaAClave(desde), fechaAClave(hasta), true);
                titulo = "--- MOVIMIENTOS DEL " + desde + " AL " + hasta + " ---";
                break;
            }
            default: {
                String termino = leerTextoObligatorio(sc, "Palabra o frase a buscar en el concepto: ", 80);
                lista = seleccionarMovimientos(C_CONCEPTO, normalizarTexto(termino), 0, 0, true);
                titulo = "--- MOVIMIENTOS CUYO CONCEPTO CONTIENE: '" + termino + "' ---";
                break;
            }
        }
        imprimirListaMovimientos(lista, titulo);
    }

    public static void menuConsultarMovimientosAvanzado(Scanner scanner) {
        int opcion;
        do {
            opcion = menu(scanner, "Seleccione una opción (1-8): ", 8,
                    marco(50, "         MENÚ DE CONSULTA DE MOVIMIENTOS          "),
                    "1. Ordenar por fecha (De la más reciente a la más antigua)",
                    "2. Ordenar por fecha (De la más antigua a la más reciente)",
                    "3. Buscar por fecha específica",
                    "4. Filtrar por clasificación/tipo (Activo, Pasivo, Capital, etc.)",
                    "5. Filtrar por nombre o código de cuenta específica (Caja, Inventario, etc.)",
                    "6. Buscar por rango de fechas",
                    "7. Buscar por palabra en el concepto",
                    "8. Regresar al Menú Principal");
            if (opcion == 8) continue;
            if (numAsientos.isEmpty()) {
                System.out.println("\n-> Aviso: No hay movimientos registrados en el sistema todavía.");
                continue;
            }
            ejecutarConsulta(scanner, opcion);
        } while (opcion != 8);
    }

    public static void administrarCuentas(Scanner scanner) {
        int op;
        do {
            op = menu(scanner, "Seleccione una opción (1-6): ", 6,
                    marco(43, "      ADMINISTRAR CATÁLOGO DE CUENTAS      "),
                    "1. Agregar nueva cuenta",
                    "2. Modificar cuenta (nombre)",
                    "3. Eliminar cuenta (con rastreo de movimientos)",
                    "4. Mostrar todas las cuentas",
                    "5. Buscar una cuenta",
                    "6. Regresar al Menú Principal");
            switch (op) {
                case 1: agregarCuenta(scanner); break;
                case 2: modificarCuenta(scanner); break;
                case 3: eliminarCuenta(scanner); break;
                case 4: mostrarCuentas(); break;
                case 5: consultarUnaCuenta(scanner); break;
                default: break;
            }
        } while (op != 6);
    }

    private static String siguienteCodigo(String subgrupo) {
        int max = 0;
        for (String c : codigosCuentas) {
            if (c.matches("\\d\\.\\d{2}\\.\\d{2}") && c.startsWith(subgrupo + ".")) max = Math.max(max, Integer.parseInt(c.substring(5)));
        }
        return max + 1 > 99 ? null : String.format("%s.%02d", subgrupo, max + 1);
    }

    public static void agregarCuenta(Scanner scanner) {
        int grupo = menu(scanner, "Seleccione el grupo principal (1-5): ", 5,
                "\n--- CLASIFICACIÓN PRINCIPAL ---",
                "1. Activo   (códigos 1.XX.XX)",
                "2. Pasivo   (códigos 2.XX.XX)",
                "3. Capital  (códigos 3.XX.XX)",
                "4. Ingresos (códigos 4.XX.XX)",
                "5. Costos y Gastos (códigos 5.XX.XX)");

        if (SUB_CODIGO.isEmpty()) cargarSubgruposCSV();
        ArrayList<Integer> subs = new ArrayList<>();
        for (int k = 0; k < SUB_CODIGO.size(); k++) if (SUB_CODIGO.get(k).startsWith(grupo + ".")) subs.add(k);
        if (subs.isEmpty()) {
            System.out.println("-> Error: no hay subgrupos definidos para ese grupo en '" + ARCHIVO_SUBGRUPOS + "'.");
            return;
        }
        System.out.println("\n--- SUBCLASIFICACIÓN ---");
        for (int n = 0; n < subs.size(); n++) System.out.println((n + 1) + ". " + SUB_DESCRIPCION.get(subs.get(n)));
        int k = subs.get(leerEntero(scanner, "Seleccione la subclasificación (1-" + subs.size() + "): ", 1, subs.size()) - 1);

        String codigo = siguienteCodigo(SUB_CODIGO.get(k));
        if (codigo == null) {
            System.out.println("-> Error: el subgrupo " + SUB_CODIGO.get(k) + " ya está lleno (máximo 99 cuentas por subgrupo).");
            return;
        }
        String nombre = pedirNombreNuevo(scanner, "Ingrese el nombre de la nueva cuenta: ",
                "-> Error: ya existe una cuenta con ese nombre (se comparan sin tildes ni mayúsculas). Intente con otro.", -1);

        int pos = codigosCuentas.size();
        for (int i = 0; i < codigosCuentas.size(); i++) {
            if (codigosCuentas.get(i).compareTo(codigo) > 0) { pos = i; break; }
        }
        String tipo = SUB_TIPO.get(k);
        insertarCuenta(pos, codigo, nombre, tipo);

        guardarCatalogoCSV();
        System.out.println("¡Cuenta agregada y guardada en Cuentas.csv!");
        System.out.println("   Código asignado: " + codigo + " | Tipo: " + tipo
                + " | Naturaleza: " + (esDeudora(tipo, nombre) ? "Deudora" : "Acreedora"));
        System.out.println("   (Para darle un saldo inicial, registre un asiento de apertura en la opción 2.)");
    }

    private static String pedirNombreNuevo(Scanner sc, String prompt, String mensajeRepetido, int excepto) {
        while (true) {
            String nombre = leerTextoObligatorio(sc, prompt, 60);
            if (buscarIndiceNormalizado(nombre, excepto) == -1) return nombre;
            System.out.println(mensajeRepetido);
        }
    }

    private static int reasignarCuenta(ArrayList<Integer> js, String viejo, String nuevo) {
        int cambios = 0;
        for (int j : js) {
            if (cuentasDebe.get(j).equalsIgnoreCase(viejo)) { cuentasDebe.set(j, nuevo); cambios++; }
            if (cuentasHaber.get(j).equalsIgnoreCase(viejo)) { cuentasHaber.set(j, nuevo); cambios++; }
        }
        return cambios;
    }

    public static void rastrearCuenta(int idx) {
        String nombre = nombresCuentas.get(idx);
        ArrayList<Integer> movs = construirIndiceMovimientos().get(idx);
        System.out.println("\n--- RASTREO DE LA CUENTA: [" + codigosCuentas.get(idx) + "] " + nombre + " ---");
        if (movs.isEmpty()) {
            System.out.println("   Esta cuenta no tiene movimientos: no está enlazada con ningún asiento.");
            return;
        }
        ArrayList<Integer> orden = new ArrayList<>(movs);
        ordenarIndicesPorFecha(orden, true);
        for (int j : orden) {
            ArrayList<String> enlazadas = new ArrayList<>();
            for (int k : movimientosDelAsiento(numAsientos.get(j))) {
                String otra = cuentaDelMovimiento(k);
                if (k != j && !otra.equalsIgnoreCase(nombre) && !enlazadas.contains(otra)) enlazadas.add(otra);
            }
            System.out.println(String.format(Locale.US, "   Asiento %d | %s | %s C$ %,.2f | %s",
                    numAsientos.get(j), fechasMovimientos.get(j), esMovimientoDebe(j) ? "DEBE " : "HABER",
                    importesMovimientos.get(j), ajustar(conceptosMovimientos.get(j), 40)));
            System.out.println("        enlazada con: " + (enlazadas.isEmpty() ? "(ninguna)" : String.join(", ", enlazadas)));
        }
        System.out.println(String.format(Locale.US, "   Saldo actual: C$ %,.2f (%s)",
                saldosCuentas.get(idx), esDeudoraIdx(idx) ? "naturaleza deudora" : "naturaleza acreedora"));
    }

    public static void modificarCuenta(Scanner scanner) {
        int idx = buscarCuentaDirecta(scanner, "Ingrese el nombre, código, 'buscar' o 'cancelar' de la cuenta a modificar: ");
        if (idx == -1) return;

        String nombreActual = nombresCuentas.get(idx);
        System.out.println("\n--- MODIFICAR CUENTA ---");
        System.out.println("Cuenta seleccionada: [" + codigosCuentas.get(idx) + "] " + nombreActual + " (" + tiposCuentas.get(idx) + ")");
        System.out.println(String.format(Locale.US, "Saldo actual: C$ %,.2f", saldosCuentas.get(idx)));
        System.out.println("Nota: el saldo se calcula solo con los asientos; para corregirlo registre un asiento de ajuste.");

        String nuevoNombre = pedirNombreNuevo(scanner, "Nuevo nombre de la cuenta: ", "-> Error: ya existe otra cuenta con ese nombre.", idx);
        if (nuevoNombre.equals(nombreActual)) {
            System.out.println("-> El nombre es idéntico al actual; no hay cambios.");
            return;
        }
        if (!leerSiNo(scanner, "¿Confirmar el cambio de '" + nombreActual + "' a '" + nuevoNombre + "'? (s/n): ")) {
            System.out.println("-> Modificación cancelada.");
            return;
        }
        int cambios = reasignarCuenta(todosLosIndices(), nombreActual, nuevoNombre);
        nombresCuentas.set(idx, nuevoNombre);
        recalcularSaldosDesdeMovimientos();
        guardarTodo();
        System.out.println("¡Cuenta modificada con éxito! (" + cambios + " movimientos actualizados).");
    }

    public static void eliminarCuenta(Scanner scanner) {
        int idx = buscarCuentaDirecta(scanner, "Ingrese el nombre, código, 'buscar' o 'cancelar' de la cuenta a eliminar: ");
        if (idx == -1) return;

        String nombreCuenta = nombresCuentas.get(idx);
        ArrayList<Integer> movs = construirIndiceMovimientos().get(idx);

        if (movs.isEmpty()) {
            if (leerSiNo(scanner, "La cuenta '" + nombreCuenta + "' no tiene movimientos. ¿Eliminarla definitivamente? (s/n): ")) {
                quitarCuenta(idx);
                guardarCatalogoCSV();
                System.out.println("¡Cuenta eliminada con éxito y Cuentas.csv actualizado!");
            } else {
                System.out.println("-> Eliminación cancelada.");
            }
            return;
        }

        System.out.println(String.format(Locale.US, "\n¡ALERTA! La cuenta '%s' tiene %d movimientos y un saldo de C$ %,.2f.",
                nombreCuenta, movs.size(), saldosCuentas.get(idx)));
        rastrearCuenta(idx);
        System.out.println("\nPara no dejar asientos descuadrados, sus movimientos deben trasladarse a otra cuenta.");
        int op = menu(scanner, "Seleccione una opción (1-2): ", 2,
                "1. Cancelar (no eliminar nada)", "2. Trasladar los movimientos a otra cuenta y eliminar esta cuenta");
        if (op == 1) {
            System.out.println("-> Eliminación cancelada.");
            return;
        }

        int destino;
        while (true) {
            destino = buscarCuentaDirecta(scanner, "Cuenta DESTINO que recibirá los movimientos (nombre, código, 'buscar' o 'cancelar'): ");
            if (destino == -1) {
                System.out.println("-> Eliminación cancelada.");
                return;
            }
            if (destino != idx) break;
            System.out.println("-> La cuenta destino debe ser distinta de la que se elimina.");
        }
        String nombreDestino = nombresCuentas.get(destino);
        if (esDeudoraIdx(idx) != esDeudoraIdx(destino)) {
            System.out.println("[!] Cuidado: '" + nombreCuenta + "' y '" + nombreDestino + "' tienen naturaleza distinta; el saldo se verá afectado.");
        }
        if (!leerSiNo(scanner, "¿Trasladar " + movs.size() + " movimientos a '" + nombreDestino + "' y eliminar '" + nombreCuenta + "'? (s/n): ")) {
            System.out.println("-> Eliminación cancelada.");
            return;
        }
        reasignarCuenta(movs, nombreCuenta, nombreDestino);
        quitarCuenta(idx);
        recalcularSaldosDesdeMovimientos();
        guardarTodo();
        System.out.println("¡Movimientos trasladados y cuenta eliminada con éxito! Todos los archivos fueron actualizados.");
    }

    public static void mostrarCuentas() {
        recalcularSaldosDesdeMovimientos();
        System.out.println("\n--- CATÁLOGO DE CUENTAS (" + nombresCuentas.size() + ") ---");
        if (nombresCuentas.isEmpty()) {
            System.out.println("(El catálogo está vacío)");
            return;
        }
        System.out.println(String.format("%-9s | %-48s | %-34s | %-9s | %14s", "Código", "Nombre", "Tipo", "Naturaleza", "Saldo (C$)"));
        System.out.println(repetir('-', 124));
        for (int i = 0; i < nombresCuentas.size(); i++) {
            System.out.println(String.format(Locale.US, "%-9s | %-48s | %-34s | %-9s | %,14.2f",
                    codigosCuentas.get(i), ajustar(nombresCuentas.get(i), 48), ajustar(tiposCuentas.get(i), 34),
                    esDeudoraIdx(i) ? "Deudora" : "Acreedora", saldosCuentas.get(i)));
        }
    }

    public static void consultarUnaCuenta(Scanner scanner) {
        int idx = buscarCuentaDirecta(scanner, "Ingrese el nombre, código, 'buscar' o 'cancelar': ");
        if (idx == -1) return;
        System.out.println("\nCuenta: [" + codigosCuentas.get(idx) + "] " + nombresCuentas.get(idx));
        System.out.println("Tipo:   " + tiposCuentas.get(idx));
        System.out.println("Naturaleza: " + (esDeudoraIdx(idx) ? "Deudora (aumenta por el Debe)" : "Acreedora (aumenta por el Haber)"));
        rastrearCuenta(idx);
    }

    public static String respaldarInformacion() {
        recalcularSaldosDesdeMovimientos();
        guardarTodo();
        guardarEstadosFinancierosArchivo();

        String carpeta = "respaldo_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String[] archivos = {ARCHIVO_CSV, ARCHIVO_TRANSACCIONES, ARCHIVO_DIARIO_TXT, ARCHIVO_MAYOR_TXT, ARCHIVO_ESTADOS_TXT};
        try {
            Path dir = Paths.get(carpeta);
            Files.createDirectories(dir);
            for (String a : archivos) {
                Path origen = Paths.get(a);
                if (Files.exists(origen)) Files.copy(origen, dir.resolve(a), StandardCopyOption.REPLACE_EXISTING);
            }
            return carpeta;
        } catch (IOException e) {
            System.out.println("-> No se pudo crear la carpeta de respaldo: " + e.getMessage());
            return null;
        }
    }
}