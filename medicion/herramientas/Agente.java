package cobertura;

import jdk.internal.org.objectweb.asm.*;
import java.io.*;
import java.lang.instrument.*;
import java.nio.file.*;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.*;

/** Agente de cobertura de líneas (equivalente simplificado a JaCoCo). args: dirClases;salida.csv;prefijo1,prefijo2 */
public class Agente {
    static final Set<String> CUBIERTAS = ConcurrentHashMap.newKeySet();
    static final List<String> IDS = Collections.synchronizedList(new ArrayList<>());

    public static void hit(int id) { CUBIERTAS.add(IDS.get(id)); }

    static boolean enAlcance(String clase, String[] prefijos) {
        for (String p : prefijos) if (clase.startsWith(p)) return true;
        return false;
    }

    public static void premain(String args, Instrumentation inst) {
        String[] a = args.split(";");
        Path clases = Paths.get(a[0]); Path salida = Paths.get(a[1]); String[] prefijos = a[2].split(",");
        inst.addTransformer(new ClassFileTransformer() {
            public byte[] transform(ClassLoader l, String nombre, Class<?> c, ProtectionDomain d, byte[] bytes) {
                if (nombre == null || !enAlcance(nombre, prefijos)) return null;
                try {
                    ClassReader cr = new ClassReader(bytes);
                    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
                    cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
                        public MethodVisitor visitMethod(int acc, String n, String desc, String sig, String[] ex) {
                            MethodVisitor mv = super.visitMethod(acc, n, desc, sig, ex);
                            return new MethodVisitor(Opcodes.ASM9, mv) {
                                int pendiente = -1;
                                void volcar() {
                                    if (pendiente < 0) return;
                                    int id = pendiente; pendiente = -1;
                                    super.visitLdcInsn(id);
                                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "cobertura/Agente", "hit", "(I)V", false);
                                }
                                public void visitLineNumber(int linea, Label inicio) {
                                    super.visitLineNumber(linea, inicio);
                                    synchronized (IDS) { pendiente = IDS.size(); IDS.add(nombre + ":" + linea); }
                                }
                                public void visitInsn(int o) { volcar(); super.visitInsn(o); }
                                public void visitIntInsn(int o, int v) { volcar(); super.visitIntInsn(o, v); }
                                public void visitVarInsn(int o, int v) { volcar(); super.visitVarInsn(o, v); }
                                public void visitTypeInsn(int o, String t) { if (o == Opcodes.NEW) { super.visitTypeInsn(o, t); volcar(); } else { volcar(); super.visitTypeInsn(o, t); } }
                                public void visitFieldInsn(int o, String a, String b, String c) { volcar(); super.visitFieldInsn(o, a, b, c); }
                                public void visitMethodInsn(int o, String a, String b, String c, boolean i) { volcar(); super.visitMethodInsn(o, a, b, c, i); }
                                public void visitInvokeDynamicInsn(String a, String b, Handle h, Object... x) { volcar(); super.visitInvokeDynamicInsn(a, b, h, x); }
                                public void visitJumpInsn(int o, Label l) { volcar(); super.visitJumpInsn(o, l); }
                                public void visitLdcInsn(Object v) { volcar(); super.visitLdcInsn(v); }
                                public void visitIincInsn(int v, int i) { volcar(); super.visitIincInsn(v, i); }
                                public void visitTableSwitchInsn(int a, int b, Label d, Label... l) { volcar(); super.visitTableSwitchInsn(a, b, d, l); }
                                public void visitLookupSwitchInsn(Label d, int[] k, Label[] l) { volcar(); super.visitLookupSwitchInsn(d, k, l); }
                                public void visitMultiANewArrayInsn(String d, int n) { volcar(); super.visitMultiANewArrayInsn(d, n); }
                            };
                        }
                    }, 0);
                    return cw.toByteArray();
                } catch (Throwable t) { System.err.println("cobertura: " + nombre + " " + t); return null; }
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> reporte(clases, salida, prefijos)));
    }

    static void reporte(Path clases, Path salida, String[] prefijos) {
        Map<String, TreeSet<Integer>> todas = new TreeMap<>();
        try (Stream<Path> s = Files.walk(clases)) {
            for (Path p : s.filter(x -> x.toString().endsWith(".class")).collect(Collectors.toList())) {
                String rel = clases.relativize(p).toString().replace('\\', '/').replace(".class", "");
                if (!enAlcance(rel, prefijos)) continue;
                String fuente = rel.contains("$") ? rel.substring(0, rel.indexOf('$')) : rel;
                new ClassReader(Files.readAllBytes(p)).accept(new ClassVisitor(Opcodes.ASM9) {
                    public MethodVisitor visitMethod(int acc, String n, String desc, String sig, String[] ex) {
                        return new MethodVisitor(Opcodes.ASM9) {
                            public void visitLineNumber(int linea, Label l) { todas.computeIfAbsent(fuente, k -> new TreeSet<>()).add(linea); }
                        };
                    }
                }, 0);
            }
            Set<String> cub = new HashSet<>();
            for (String c : CUBIERTAS) { String cl = c.substring(0, c.indexOf(':')); if (cl.contains("$")) cl = cl.substring(0, cl.indexOf('$')); cub.add(cl + c.substring(c.indexOf(':'))); }
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(salida))) {
                w.println("clase,lineas,cubiertas,no_cubiertas");
                for (var e : todas.entrySet()) {
                    int tot = e.getValue().size(); List<Integer> faltan = new ArrayList<>(); int ok = 0;
                    for (int ln : e.getValue()) { if (cub.contains(e.getKey() + ":" + ln)) ok++; else faltan.add(ln); }
                    w.println(e.getKey().replace('/', '.') + "," + tot + "," + ok + ",\"" + faltan.stream().map(String::valueOf).collect(Collectors.joining(" ")) + "\"");
                }
            }
        } catch (IOException ex) { ex.printStackTrace(); }
    }
}
