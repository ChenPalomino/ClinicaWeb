package metricas;

import com.sun.source.tree.*;
import com.sun.source.util.*;

import javax.tools.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Métricas de mantenibilidad sobre el código fuente (mismas reglas que se documentan en el informe):
 *  - Complejidad ciclomática de McCabe por método: 1 + if + ?: + for + for-each + while + do + case + catch + && + ||.
 *  - Líneas de código (sin vacías ni comentarios) y líneas duplicadas: bloques de 10 o más líneas normalizadas iguales.
 * Uso: java metricas.Medidor <nombre> <salida.csv> <dir|archivo>...
 */
public class Medidor {

    record Metodo(String clase, String nombre, int linea, int cc) { }

    public static void main(String[] args) throws Exception {
        String nombre = args[0];
        Path salida = Paths.get(args[1]);
        List<Path> archivos = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            Path p = Paths.get(args[i]);
            if (Files.isDirectory(p)) {
                try (Stream<Path> s = Files.walk(p)) { s.filter(x -> x.toString().endsWith(".java")).sorted().forEach(archivos::add); }
            } else {
                archivos.add(p);
            }
        }
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = jc.getStandardFileManager(null, null, StandardCharsets.UTF_8);
        JavacTask tarea = (JavacTask) jc.getTask(null, fm, d -> { }, List.of("-proc:none"), null, fm.getJavaFileObjectsFromPaths(archivos));
        List<Metodo> metodos = new ArrayList<>();
        int clases = 0;
        for (CompilationUnitTree cu : tarea.parse()) {
            LineMap lm = cu.getLineMap();
            String archivo = Paths.get(cu.getSourceFile().toUri()).getFileName().toString().replace(".java", "");
            int[] nClases = {0};
            new TreePathScanner<Void, Void>() {
                Deque<String> pila = new ArrayDeque<>();
                @Override public Void visitClass(ClassTree c, Void v) {
                    nClases[0]++;
                    pila.push(c.getSimpleName().toString().isEmpty() ? "anonima" : c.getSimpleName().toString());
                    super.visitClass(c, v);
                    pila.pop();
                    return null;
                }
                @Override public Void visitMethod(MethodTree m, Void v) {
                    if (m.getBody() != null) {
                        int cc = 1 + Contador.nz(new Contador().scan(m.getBody(), null));
                        long linea = lm.getLineNumber(((com.sun.source.util.SourcePositions) Trees.instance(tarea).getSourcePositions()).getStartPosition(cu, m));
                        metodos.add(new Metodo(archivo + "." + String.join(".", reverse(pila)), m.getName().toString(), (int) linea, cc));
                    }
                    return null;
                }
            }.scan(cu, null);
            clases += nClases[0];
        }
        int[] loc = new int[1];
        int duplicadas = duplicadas(archivos, loc);
        IntSummaryStatistics st = metodos.stream().mapToInt(Metodo::cc).summaryStatistics();
        long mayores10 = metodos.stream().filter(m -> m.cc() > 10).count();
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(salida, StandardCharsets.UTF_8))) {
            w.println("clase,metodo,linea,cc");
            metodos.stream().sorted(Comparator.comparingInt(Metodo::cc).reversed())
                   .forEach(m -> w.println(m.clase() + "," + m.nombre() + "," + m.linea() + "," + m.cc()));
        }
        System.out.printf(Locale.US, "%s|archivos=%d|clases=%d|metodos=%d|loc=%d|cc_total=%d|cc_promedio=%.2f|cc_max=%d|metodos_cc_mayor_10=%d|lineas_duplicadas=%d|duplicacion=%.2f%%%n",
                nombre, archivos.size(), clases, st.getCount(), loc[0], st.getSum(), st.getAverage(), st.getMax(), mayores10, duplicadas,
                loc[0] == 0 ? 0.0 : 100.0 * duplicadas / loc[0]);
    }

    static List<String> reverse(Deque<String> d) { List<String> l = new ArrayList<>(d); Collections.reverse(l); return l; }

    /** Cuenta los puntos de decisión dentro de un método (incluye lambdas y clases anónimas internas). */
    static class Contador extends TreeScanner<Integer, Void> {
        @Override public Integer reduce(Integer a, Integer b) { return (a == null ? 0 : a) + (b == null ? 0 : b); }
        private int mas(Tree t, Void v) { Integer r = super.scan(t, v); return r == null ? 0 : r; }
        @Override public Integer visitIf(IfTree t, Void v) { return 1 + nz(super.visitIf(t, v)); }
        @Override public Integer visitConditionalExpression(ConditionalExpressionTree t, Void v) { return 1 + nz(super.visitConditionalExpression(t, v)); }
        @Override public Integer visitForLoop(ForLoopTree t, Void v) { return 1 + nz(super.visitForLoop(t, v)); }
        @Override public Integer visitEnhancedForLoop(EnhancedForLoopTree t, Void v) { return 1 + nz(super.visitEnhancedForLoop(t, v)); }
        @Override public Integer visitWhileLoop(WhileLoopTree t, Void v) { return 1 + nz(super.visitWhileLoop(t, v)); }
        @Override public Integer visitDoWhileLoop(DoWhileLoopTree t, Void v) { return 1 + nz(super.visitDoWhileLoop(t, v)); }
        @Override public Integer visitCase(CaseTree t, Void v) {
            int propio = t.getExpressions().isEmpty() ? 0 : t.getExpressions().size();
            return propio + nz(super.visitCase(t, v));
        }
        @Override public Integer visitCatch(CatchTree t, Void v) { return 1 + nz(super.visitCatch(t, v)); }
        @Override public Integer visitBinary(BinaryTree t, Void v) {
            int propio = (t.getKind() == Tree.Kind.CONDITIONAL_AND || t.getKind() == Tree.Kind.CONDITIONAL_OR) ? 1 : 0;
            return propio + nz(super.visitBinary(t, v));
        }
        static int nz(Integer i) { return i == null ? 0 : i; }
    }

    /** Líneas de código normalizadas; un bloque de 10+ líneas iguales en dos lugares marca todas sus líneas como duplicadas. */
    static int duplicadas(List<Path> archivos, int[] loc) throws IOException {
        final int VENTANA = 10;
        List<String[]> codigo = new ArrayList<>();
        for (Path p : archivos) {
            boolean enComentario = false;
            for (String linea : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                String t = linea.trim();
                if (enComentario) { if (t.contains("*/")) enComentario = false; continue; }
                if (t.startsWith("/*")) { if (!t.contains("*/")) enComentario = true; continue; }
                if (t.isEmpty() || t.startsWith("//") || t.startsWith("*")) continue;
                loc[0]++;
                if (t.startsWith("import ") || t.startsWith("package ") || t.matches("[{}();]+") || t.startsWith("@")) continue;
                codigo.add(new String[]{p.toString(), t.replaceAll("\\s+", " ")});
            }
        }
        Map<String, List<Integer>> ventanas = new HashMap<>();
        for (int i = 0; i + VENTANA <= codigo.size(); i++) {
            String archivo = codigo.get(i)[0];
            if (!codigo.get(i + VENTANA - 1)[0].equals(archivo)) continue;
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < VENTANA; k++) sb.append(codigo.get(i + k)[1]).append('\n');
            ventanas.computeIfAbsent(sb.toString(), x -> new ArrayList<>()).add(i);
        }
        boolean[] dup = new boolean[codigo.size()];
        for (List<Integer> inicios : ventanas.values()) {
            if (inicios.size() < 2) continue;
            for (int i : inicios) for (int k = 0; k < VENTANA; k++) dup[i + k] = true;
        }
        int n = 0;
        for (boolean b : dup) if (b) n++;
        return n;
    }
}
