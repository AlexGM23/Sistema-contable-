import java.util.ArrayList;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Principal {
    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        Scanner scanner = crearScanner();

        System.out.println("\n[INICIANDO SISTEMA CONTABLE EMPRESARIAL...]");
        LogicaContable.cargarSubgruposCSV();
        LogicaContable.cargarCatalogoCSV();
        LogicaContable.cargarTransaccionesCSV();
        LogicaContable.verificarIntegridad(true);

        boolean salir = false;
        while (!salir) {
            try {
                int opcion = mostrarMenuPrincipal(scanner);
                switch (opcion) {
                    case 1: LogicaContable.administrarCuentas(scanner); break;
                    case 2: registrarAsientoAmigableCompuesto(scanner); break;
                    case 3: LogicaContable.consultarCuentasT(scanner); break;
                    case 4: LogicaContable.generarLibroDiario(); break;
                    case 5: LogicaContable.generarLibroMayor(); break;
                    case 6: LogicaContable.generarBalanzaComprobacion(); break;
                    case 7: LogicaContable.generarEstadoResultados(); break;
                    case 8: LogicaContable.generarBalanceGeneral(); break;
                    case 9: LogicaContable.menuConsultarMovimientosAvanzado(scanner); break;
                    case 10:
                        String carpeta = LogicaContable.respaldarInformacion();
                        System.out.println("¡Información respaldada y libros generados exitosamente!");
                        if (carpeta != null) System.out.println("   Copia de seguridad en la carpeta: " + carpeta);
                        break;
                    default:
                        LogicaContable.guardarTodo();
                        System.out.println("¡Saliendo del sistema contable. Hasta pronto!");
                        salir = true;
                        break;
                }
            } catch (NoSuchElementException | IllegalStateException e) {
                System.out.println("\n-> Entrada de datos finalizada. Guardando información y saliendo...");
                LogicaContable.guardarTodo();
                salir = true;
            } catch (Exception e) {
                System.out.println("-> Ocurrió un error inesperado: " + e.getMessage() + ". Intente de nuevo.");
            }
        }
        scanner.close();
    }

    private static Scanner crearScanner() {
        String codificacion = System.getProperty("stdin.encoding");
        if (codificacion != null) {
            try {
                return new Scanner(System.in, codificacion);
            } catch (IllegalArgumentException e) {
            }
        }
        return new Scanner(System.in);
    }

    private static int mostrarMenuPrincipal(Scanner scanner) {
        return LogicaContable.menu(scanner, "\nSeleccione una opción (1-11): ", 11,
                LogicaContable.marco(43, "       SISTEMA CONTABLE EMPRESARIAL        "),
                "1. Administrar catálogo de cuentas",
                "2. Registrar operación contable (Modo Inteligente)",
                "3. Consultar Cuentas T (Resumen Oficial)",
                "4. Generar Libro Diario (Estilo Formal TXT)",
                "5. Generar Libro Mayor (Estilo Tarjeta / Cuadro Oficial)",
                "6. Generar Balanza de Comprobación (4 Columnas)",
                "7. Generar Estado de Resultados",
                "8. Balance General",
                "9. Consultar movimientos (Menú avanzado y filtros)",
                "10. Respaldar información (Guardar CSV y TXT)",
                "11. Salir del sistema");
    }

    public static void registrarAsientoAmigableCompuesto(Scanner scanner) {
        System.out.println("\n==================================================");
        System.out.println("   REGISTRO CONTABLE INTELIGENTE (MÚLTIPLES CUENTAS)");
        System.out.println("==================================================");

        if (LogicaContable.nombresCuentas.isEmpty()) {
            System.out.println("-> No hay cuentas en el catálogo. Primero cree cuentas en la opción 1.");
            return;
        }

        int nAsiento = pedirNumeroAsiento(scanner);
        String hoy = LogicaContable.obtenerFechaActual();
        String fecha = LogicaContable.leerFecha(scanner,
                "Fecha (DD/MM/AAAA, ej. 01/11/2026) [Enter = hoy " + hoy + "]: ", hoy);
        String concepto = LogicaContable.leerTextoObligatorio(scanner,
                "Concepto o descripción detallada de lo que sucedió: ", 120);

        int modo = LogicaContable.menu(scanner, "Seleccione una opción (1-2): ", 2,
                "\n¿Cómo desea indicar los movimientos?",
                "1. Modo inteligente: usted dice si la cuenta AUMENTÓ o DISMINUYÓ y el sistema decide Debe/Haber",
                "2. Modo manual: usted elige directamente DEBE (cargo) o HABER (abono)");

        ArrayList<String> cuentas = new ArrayList<>();
        ArrayList<Double> importes = new ArrayList<>();
        ArrayList<Boolean> esDebe = new ArrayList<>();

        boolean registrado = false;
        boolean cancelado = false;
        while (!registrado && !cancelado) {
            int op = 1;
            if (!cuentas.isEmpty()) {
                mostrarResumenAsiento(cuentas, importes, esDebe);
                op = LogicaContable.menu(scanner, "Seleccione una opción (1-4): ", 4,
                        "\n¿Qué desea hacer?",
                        "1. Agregar otra cuenta al asiento",
                        "2. Deshacer el último movimiento",
                        "3. Terminar y registrar el asiento",
                        "4. Cancelar este asiento");
            }

            switch (op) {
                case 1:
                    agregarMovimiento(scanner, modo, cuentas, importes, esDebe);
                    break;
                case 2:
                    int ultimo = cuentas.size() - 1;
                    System.out.println("-> Se quitó el movimiento de '" + cuentas.get(ultimo) + "'.");
                    cuentas.remove(ultimo);
                    importes.remove(ultimo);
                    esDebe.remove(ultimo);
                    break;
                case 3:
                    registrado = intentarRegistrar(scanner, nAsiento, fecha, concepto, cuentas, importes, esDebe);
                    break;
                default:
                    if (LogicaContable.leerSiNo(scanner, "¿Seguro que desea cancelar y descartar este asiento? (s/n): ")) {
                        cancelado = true;
                        System.out.println("-> Asiento cancelado. No se guardó nada.");
                    }
                    break;
            }
        }
    }

    private static int pedirNumeroAsiento(Scanner scanner) {
        int sugerido = LogicaContable.siguienteNumeroAsiento();
        while (true) {
            String t = LogicaContable.leer(scanner, "Número de asiento [Enter = " + sugerido + "]: ");
            if (t.isEmpty()) return sugerido;
            if (!t.matches("\\d{1,6}")) {
                System.out.println("-> Error: ingrese únicamente un número entero positivo (sin letras ni símbolos).");
                continue;
            }
            int n = Integer.parseInt(t);
            if (n <= 0) System.out.println("-> El número de asiento debe ser mayor a 0.");
            else if (LogicaContable.existeAsiento(n)) System.out.println("-> El asiento " + n + " ya existe. Use otro número (el siguiente libre es " + sugerido + ").");
            else return n;
        }
    }

    private static void agregarMovimiento(Scanner scanner, int modo, ArrayList<String> cuentas,
                                          ArrayList<Double> importes, ArrayList<Boolean> esDebe) {
        System.out.println("\n--- Movimiento #" + (cuentas.size() + 1) + " ---");

        int idx = LogicaContable.buscarCuentaDirecta(scanner,
                "Ingrese el nombre o código de la cuenta (o escriba 'Buscar' / 'Cancelar'): ");
        if (idx == -1) {
            System.out.println("-> No se agregó ninguna cuenta.");
            return;
        }

        String nombreCta = LogicaContable.nombresCuentas.get(idx);
        boolean deudora = LogicaContable.esDeudoraIdx(idx);

        for (String c : cuentas) {
            if (c.equalsIgnoreCase(nombreCta)) {
                System.out.println("-> La cuenta '" + nombreCta + "' ya fue agregada a este asiento. Elija otra o deshaga el movimiento.");
                return;
            }
        }

        System.out.println("[OK] Cuenta seleccionada: " + nombreCta + " (" + LogicaContable.tiposCuentas.get(idx) + ") - naturaleza "
                + (deudora ? "DEUDORA (aumenta por el Debe)" : "ACREEDORA (aumenta por el Haber)"));

        int eleccion = (modo == 1)
                ? LogicaContable.menu(scanner, "Seleccione una opción (1 o 2): ", 2,
                        "¿Qué le pasó a esta cuenta en esta transacción?",
                        "1. Aumentó (Entró valor / Creció)",
                        "2. Disminuyó (Salió valor / Se redujo)")
                : LogicaContable.menu(scanner, "Seleccione una opción (1 o 2): ", 2,
                        "¿Dónde desea registrar esta cuenta?",
                        "1. DEBE (cargo)",
                        "2. HABER (abono)");
        boolean ladoDebe = (modo == 1) ? LogicaContable.ladoDebe(idx, eleccion) : eleccion == 1;

        double importe = LogicaContable.leerMonto(scanner, "Monto de esta cuenta (C$): ");

        cuentas.add(nombreCta);
        importes.add(importe);
        esDebe.add(ladoDebe);
        System.out.println(String.format(Locale.US, " -> Clasificado en el %s por C$ %,.2f", ladoDebe ? "DEBE" : "HABER", importe));
    }

    private static void mostrarResumenAsiento(ArrayList<String> cuentas, ArrayList<Double> importes, ArrayList<Boolean> esDebe) {
        System.out.println("\n----------------- ASIENTO EN CONSTRUCCIÓN -----------------");
        System.out.println(String.format("%-4s %-38s %15s %15s", "#", "Cuenta", "DEBE", "HABER"));
        for (int k = 0; k < cuentas.size(); k++) {
            String monto = String.format(Locale.US, "%,.2f", importes.get(k));
            String nombre = cuentas.get(k).length() > 38 ? cuentas.get(k).substring(0, 35) + "..." : cuentas.get(k);
            System.out.println(String.format("%-4d %-38s %15s %15s", k + 1, nombre, esDebe.get(k) ? monto : "", esDebe.get(k) ? "" : monto));
        }
        double[] t = LogicaContable.totalesAsiento(importes, esDebe);
        double sumaDebe = t[LogicaContable.T_DEBE], sumaHaber = t[LogicaContable.T_HABER];
        System.out.println(String.format(Locale.US, "%-43s %15s %15s", "TOTALES:", String.format(Locale.US, "%,.2f", sumaDebe), String.format(Locale.US, "%,.2f", sumaHaber)));
        double dif = LogicaContable.redondear2(sumaDebe - sumaHaber);
        if (Math.abs(dif) < 0.005) {
            System.out.println("[OK] El asiento está CUADRADO. Ya puede registrarlo.");
        } else if (dif > 0) {
            System.out.println(String.format(Locale.US, "[!] Faltan C$ %,.2f en el HABER para cuadrar.", dif));
        } else {
            System.out.println(String.format(Locale.US, "[!] Faltan C$ %,.2f en el DEBE para cuadrar.", -dif));
        }
        System.out.println("-----------------------------------------------------------");
    }

    private static boolean intentarRegistrar(Scanner scanner, int nAsiento, String fecha, String concepto,
                                             ArrayList<String> cuentas, ArrayList<Double> importes,
                                             ArrayList<Boolean> esDebe) {
        double[] t = LogicaContable.totalesAsiento(importes, esDebe);
        double sumaDebe = t[LogicaContable.T_DEBE], sumaHaber = t[LogicaContable.T_HABER];

        if (t[LogicaContable.T_NDEBE] == 0 || t[LogicaContable.T_NHABER] == 0) {
            System.out.println("\n[!] Todo asiento necesita al menos una cuenta en el DEBE y una en el HABER.");
            System.out.println("    Agregue más movimientos, deshaga alguno o cancele el asiento.");
            return false;
        }
        if (Math.abs(sumaDebe - sumaHaber) > 0.005) {
            System.out.println("\n==================================================");
            System.out.println("  ERROR: El asiento no está cuadrado.");
            System.out.println(String.format(Locale.US, " Total en el Debe:  C$ %,.2f", sumaDebe));
            System.out.println(String.format(Locale.US, " Total en el Haber: C$ %,.2f", sumaHaber));
            System.out.println(" Agregue movimientos para igualar ambos lados (no se perdió nada de lo capturado).");
            System.out.println("==================================================");
            return false;
        }

        ArrayList<String> avisos = LogicaContable.advertenciasDeSaldo(cuentas, importes, esDebe);
        if (!avisos.isEmpty()) {
            System.out.println("\n[!] AVISOS ANTES DE REGISTRAR:");
            for (String a : avisos) System.out.println("    - " + a);
            if (!LogicaContable.leerSiNo(scanner, "¿Desea registrar el asiento de todos modos? (s/n): ")) {
                System.out.println("-> El asiento sigue en construcción; puede corregirlo.");
                return false;
            }
        } else if (!LogicaContable.leerSiNo(scanner, "¿Confirmar y registrar el asiento " + nAsiento + "? (s/n): ")) {
            System.out.println("-> El asiento sigue en construcción; puede continuar editándolo.");
            return false;
        }

        boolean ok = LogicaContable.registrarAsiento(nAsiento, fecha, concepto, cuentas, importes, esDebe);
        if (ok) {
            System.out.println("\n==================================================");
            System.out.println("       ¡ASIENTO REGISTRADO EXITOSAMENTE!          ");
            System.out.println("==================================================");
            System.out.println(String.format(Locale.US, " Asiento No. %d  |  Fecha: %s", nAsiento, fecha));
            System.out.println(String.format(Locale.US, " Total Cuadrado: C$ %,.2f", sumaDebe));
            System.out.println(" Archivos 'libro_diario.txt' y 'libro_mayor.txt' actualizados.");
            System.out.println("==================================================");
        }
        return ok;
    }
}