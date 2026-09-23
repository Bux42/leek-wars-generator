package test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.leekwars.generator.fight.entity.EntityAI;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.leek.LeekLog;
import com.leekwars.generator.polyglot.PolyglotEntityAI;
import com.leekwars.generator.polyglot.PolyglotSandbox;
import com.leekwars.generator.polyglot.TypeScriptTranspiler;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;

/**
 * BANC LeekScript vs Python vs JavaScript : 10 workloads varies, executes par le VRAI moteur
 * (LeekScript compile en bytecode JVM, JS/Python en Graal sous l'image isolate), mesures en
 * OPERATIONS (le budget du joueur) et en TEMPS MUR.
 *
 * <p>Purement diagnostique : imprime un tableau, n'echoue jamais sur un chiffre.
 *
 * <p>Lecture des colonnes :
 * <ul>
 * <li><b>ops</b> : ce que le moteur FACTURE au poireau (getOperations). Guest = evenements
 *     comptes par l'instrument * opsFactor (5.0 en Python, 0.6 en JS) ; LeekScript = ses ops natives.</li>
 * <li><b>brut</b> : le compte d'evenements guest AVANT facteur (statements en Python,
 *     statements + expressions en JS).</li>
 * <li><b>ms</b> : mediane / min de {@value #RUNS} executions apres WARMUP tours de chauffe ;
 *     la courbe tour 1 -&gt; tour 2 -&gt; a chaud est imprimee a part.</li>
 * </ul>
 *
 * <p>UN LANGAGE GUEST PAR JVM : {@code BENCH_LANG=js} ou {@code BENCH_LANG=python} (defaut).
 * L'image isolate ne se charge qu'une fois par process : demander les deux dans la meme JVM
 * bascule le second en isolate EXTERNE (autre process), ce qui fausserait les temps.
 * Sous-ensemble de workloads : {@code BENCH_ONLY=1,3,7}.
 */
public class TestBenchLanguages extends FightTestBase {

	private static final int WARMUP = Integer.parseInt(System.getenv().getOrDefault("BENCH_WARMUP", "20"));
	private static final int RUNS = 7;
	/** Budget large : on mesure le cout des algos, pas la limite d'ops. cores*1M = 1e9. */
	private static final int BENCH_CORES = 1000;

	private final String lang = System.getenv().getOrDefault("BENCH_LANG", "python");

	private Leek leek1;

	@Override
	protected void createLeeks() {
		leek1 = new Leek(1, "Bench", 0, 300, 500, 6, 7, 100, 100, 10, 50, 10, 0, 0, BENCH_CORES, 50,
			0, false, 0, 0, "", 0, "", "", "", 0);
		fight.getState().addEntity(0, leek1);
		fight.getState().addEntity(1, defaultLeek(2, "Bench2"));
	}

	/** Facteur de calibration applique par PolyglotEntityAI.opsFactor : ops facturees = brut * facteur. */
	private double opsFactor() {
		return "js".equals(lang) ? 0.6 : 5.0;
	}

	// ---------------------------------------------------------------- sources guest

	/** Emballe le corps du workload dans la fonction turn() du langage guest, terminee par {@code tail}. */
	private String wrap(String body, String tail) {
		if ("js".equals(lang)) {
			return "function turn() {\n" + body + "\nreturn " + tail + ";\n}";
		}
		return "def turn():\n" + indent(body) + "\n    return " + tail + "\n";
	}

	private String guestBody(Bench b) {
		return "js".equals(lang) ? b.js() : b.py();
	}

	private String guestCheck(Bench b) {
		return "js".equals(lang) ? b.checkJs() : b.checkPy();
	}

	// ---------------------------------------------------------------- runners

	/** Resultat d'un langage sur un workload : ops facturees, ms medianes/min, ms des tours 1 et 2. */
	private record Res(long ops, double ms, double minMs, double firstMs, double secondMs, String error) {
		boolean ok() { return error == null; }
	}

	private Res runLs(String body) {
		try {
			AIFile file = new AIFile("bench_ls_" + System.nanoTime(), body + "\nreturn getOperations();",
				System.currentTimeMillis(), LeekScript.LATEST_VERSION, leek1.getId(), false);
			leek1.setAIFile(file);
			leek1.setLogs(new LeekLog(farmerLog, leek1));
			leek1.setFight(fight);
			EntityAI ai = EntityAI.build(generator, file, leek1);
			return measure(() -> {
				ai.resetCounter();
				try {
					return ((Number) ai.runIA()).longValue();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		} catch (Throwable e) {
			return new Res(-1, -1, -1, -1, -1, shortError(e));
		}
	}

	private Res runGuest(PolyglotSandbox sb, String body) {
		try {
			PolyglotEntityAI ai = new PolyglotEntityAI(lang, wrap(body, "System.operations"), sb);
			ai.setEntity(leek1);
			ai.setLogs(new LeekLog(farmerLog, leek1));
			ai.setFight(fight);
			ai.setTurnWallClockLimitMs(120_000); // banc : pas de coupure watchdog sur un gros workload
			return measure(() -> {
				ai.resetCounter();
				try {
					return ((Number) ai.runIA()).longValue();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		} catch (Throwable e) {
			return new Res(-1, -1, -1, -1, -1, shortError(e));
		}
	}

	/** Boucle de mesure commune : WARMUP tours de chauffe puis RUNS tours mesures. */
	private Res measure(java.util.function.LongSupplier turn) {
		long ops = 0;
		double first = 0, second = 0;
		List<Double> times = new ArrayList<>();
		for (int i = 0; i < WARMUP + RUNS; i++) {
			long t0 = System.nanoTime();
			ops = turn.getAsLong();
			double ms = (System.nanoTime() - t0) / 1e6;
			if (i == 0) first = ms;
			if (i == 1) second = ms;
			if (i >= WARMUP) times.add(ms);
		}
		return new Res(ops, median(times), min(times), first, second, null);
	}

	/** Controle d'equivalence : execute le workload une fois et renvoie son resultat (pas ses ops). */
	private String checkLs(String body, String expr) {
		try {
			AIFile file = new AIFile("check_ls_" + System.nanoTime(), body + "\nreturn " + expr + ";",
				System.currentTimeMillis(), LeekScript.LATEST_VERSION, leek1.getId(), false);
			leek1.setAIFile(file);
			leek1.setLogs(new LeekLog(farmerLog, leek1));
			leek1.setFight(fight);
			EntityAI ai = EntityAI.build(generator, file, leek1);
			ai.resetCounter();
			return format(ai.runIA());
		} catch (Throwable e) {
			return "ERR:" + shortError(e);
		}
	}

	private String checkGuest(PolyglotSandbox sb, String body, String expr) {
		try {
			PolyglotEntityAI ai = new PolyglotEntityAI(lang, wrap(body, expr), sb);
			ai.setEntity(leek1);
			ai.setLogs(new LeekLog(farmerLog, leek1));
			ai.setFight(fight);
			ai.setTurnWallClockLimitMs(120_000);
			ai.resetCounter();
			return format(ai.runIA());
		} catch (Throwable e) {
			return "ERR:" + shortError(e);
		}
	}

	/** Normalise 42 / 42.0 sur la meme ecriture : les trois langages ne typent pas pareil. */
	private static String format(Object value) {
		if (value instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
			return String.valueOf((long) n.doubleValue());
		}
		return String.valueOf(value);
	}

	private static String shortError(Throwable e) {
		String m = e.getMessage();
		if (m == null) m = e.getClass().getSimpleName();
		m = m.replace('\n', ' ');
		return m.length() > 70 ? m.substring(0, 70) : m;
	}

	private static double min(List<Double> v) {
		double m = Double.MAX_VALUE;
		for (double d : v) m = Math.min(m, d);
		return m;
	}

	private static double median(List<Double> v) {
		List<Double> s = new ArrayList<>(v);
		s.sort(null);
		return s.get(s.size() / 2);
	}

	private static String indent(String body) {
		StringBuilder b = new StringBuilder();
		for (String line : body.split("\n", -1)) {
			if (b.length() > 0) b.append('\n');
			b.append(line.isBlank() ? "" : "    " + line);
		}
		return b.toString();
	}

	// ---------------------------------------------------------------- workloads

	private record Bench(String label, String ls, String py, String js,
			String checkLs, String checkPy, String checkJs) {}

	private static final List<Bench> BENCHES = List.of(

		new Bench("1. boucle arithmetique 1M",
			"""
			var s = 0;
			for (var i = 0; i < 1000000; i++) { s += i; }
			""",
			"""
			s = 0
			for i in range(1000000):
			    s += i
			""",
			"""
			var s = 0;
			for (var i = 0; i < 1000000; i++) { s += i; }
			""",
			"s", "s", "s"),

		new Bench("2. matrices 60x60x60",
			"""
			var n = 60;
			var a = []; var b = [];
			for (var i = 0; i < n; i++) {
				var ra = []; var rb = [];
				for (var j = 0; j < n; j++) { push(ra, i + j); push(rb, i - j); }
				push(a, ra); push(b, rb);
			}
			var s = 0;
			for (var i = 0; i < n; i++) {
				for (var j = 0; j < n; j++) {
					var acc = 0;
					for (var k = 0; k < n; k++) { acc += a[i][k] * b[k][j]; }
					s += acc;
				}
			}
			""",
			"""
			n = 60
			a = []
			b = []
			for i in range(n):
			    ra = []
			    rb = []
			    for j in range(n):
			        ra.append(i + j)
			        rb.append(i - j)
			    a.append(ra)
			    b.append(rb)
			s = 0
			for i in range(n):
			    for j in range(n):
			        acc = 0
			        for k in range(n):
			            acc += a[i][k] * b[k][j]
			        s += acc
			""",
			"""
			var n = 60;
			var a = []; var b = [];
			for (var i = 0; i < n; i++) {
				var ra = []; var rb = [];
				for (var j = 0; j < n; j++) { ra.push(i + j); rb.push(i - j); }
				a.push(ra); b.push(rb);
			}
			var s = 0;
			for (var i = 0; i < n; i++) {
				for (var j = 0; j < n; j++) {
					var acc = 0;
					for (var k = 0; k < n; k++) { acc += a[i][k] * b[k][j]; }
					s += acc;
				}
			}
			""",
			"s", "s", "s"),

		new Bench("3. recursion fib(25)",
			"""
			function fib(n) { if (n < 2) return n; return fib(n - 1) + fib(n - 2); }
			var r = fib(25);
			""",
			"""
			def fib(n):
			    if n < 2:
			        return n
			    return fib(n - 1) + fib(n - 2)
			r = fib(25)
			""",
			"""
			function fib(n) { if (n < 2) return n; return fib(n - 1) + fib(n - 2); }
			var r = fib(25);
			""",
			"r", "r", "r"),

		new Bench("4. tableau 200k push+somme",
			"""
			var a = [];
			for (var i = 0; i < 200000; i++) { push(a, i * 3); }
			var s = 0;
			for (var i = 0; i < 200000; i++) { s += a[i]; }
			""",
			"""
			a = []
			for i in range(200000):
			    a.append(i * 3)
			s = 0
			for i in range(200000):
			    s += a[i]
			""",
			"""
			var a = [];
			for (var i = 0; i < 200000; i++) { a.push(i * 3); }
			var s = 0;
			for (var i = 0; i < 200000; i++) { s += a[i]; }
			""",
			"s", "s", "s"),

		new Bench("5. map 100k set+get",
			"""
			var m = [:];
			for (var i = 0; i < 100000; i++) { m[i] = i * 2; }
			var s = 0;
			for (var i = 0; i < 100000; i++) { s += m[i]; }
			""",
			"""
			m = {}
			for i in range(100000):
			    m[i] = i * 2
			s = 0
			for i in range(100000):
			    s += m[i]
			""",
			"""
			var m = new Map();
			for (var i = 0; i < 100000; i++) { m.set(i, i * 2); }
			var s = 0;
			for (var i = 0; i < 100000; i++) { s += m.get(i); }
			""",
			"s", "s", "s"),

		new Bench("6. chaines 5k concat+scan",
			"""
			var s = "";
			for (var i = 0; i < 5000; i++) { s += "x"; }
			var n = length(s);
			var c = 0;
			for (var i = 0; i < n; i++) { if (charAt(s, i) == "x") { c++; } }
			""",
			"""
			s = ""
			for i in range(5000):
			    s += "x"
			n = len(s)
			c = 0
			for i in range(n):
			    if s[i] == "x":
			        c += 1
			""",
			"""
			var s = "";
			for (var i = 0; i < 5000; i++) { s += "x"; }
			var n = s.length;
			var c = 0;
			for (var i = 0; i < n; i++) { if (s[i] == "x") { c++; } }
			""",
			"c + n", "c + n", "c + n"),

		new Bench("7. flottants 200k sqrt+sin",
			"""
			var s = 0.0;
			for (var i = 1; i < 200000; i++) { s += sqrt(i) * 1.5 + sin(i); }
			""",
			"""
			import math
			s = 0.0
			for i in range(1, 200000):
			    s += math.sqrt(i) * 1.5 + math.sin(i)
			""",
			"""
			var s = 0.0;
			for (var i = 1; i < 200000; i++) { s += Math.sqrt(i) * 1.5 + Math.sin(i); }
			""",
			"floor(s)", "math.floor(s)", "Math.floor(s)"),

		new Bench("8. crible 200k",
			"""
			var n = 200000;
			var sieve = [];
			for (var i = 0; i <= n; i++) { push(sieve, true); }
			var nb = 0;
			for (var i = 2; i <= n; i++) {
				if (sieve[i]) {
					nb++;
					for (var j = i * i; j <= n; j += i) { sieve[j] = false; }
				}
			}
			""",
			"""
			n = 200000
			sieve = []
			for i in range(n + 1):
			    sieve.append(True)
			nb = 0
			for i in range(2, n + 1):
			    if sieve[i]:
			        nb += 1
			        j = i * i
			        while j <= n:
			            sieve[j] = False
			            j += i
			""",
			"""
			var n = 200000;
			var sieve = [];
			for (var i = 0; i <= n; i++) { sieve.push(true); }
			var nb = 0;
			for (var i = 2; i <= n; i++) {
				if (sieve[i]) {
					nb++;
					for (var j = i * i; j <= n; j += i) { sieve[j] = false; }
				}
			}
			""",
			"nb", "nb", "nb"),

		// PRNG = MINSTD (16807 / 2^31-1) : le produit reste sous 2^53, donc identique en
		// entiers 64 bits (LS), en entiers exacts (Python) ET en doubles (JS).
		new Bench("9. tri rapide 20k",
			"""
			function qs(a, lo, hi) {
				if (lo >= hi) { return; }
				var pivot = a[floor((lo + hi) / 2)];
				var i = lo; var j = hi;
				while (i <= j) {
					while (a[i] < pivot) { i++; }
					while (a[j] > pivot) { j--; }
					if (i <= j) { var t = a[i]; a[i] = a[j]; a[j] = t; i++; j--; }
				}
				qs(a, lo, j);
				qs(a, i, hi);
			}
			var seed = 12345;
			var arr = [];
			for (var k = 0; k < 20000; k++) {
				seed = (seed * 16807) % 2147483647;
				push(arr, seed % 100000);
			}
			qs(arr, 0, 19999);
			""",
			"""
			def qs(a, lo, hi):
			    if lo >= hi:
			        return
			    pivot = a[(lo + hi) // 2]
			    i = lo
			    j = hi
			    while i <= j:
			        while a[i] < pivot:
			            i += 1
			        while a[j] > pivot:
			            j -= 1
			        if i <= j:
			            t = a[i]
			            a[i] = a[j]
			            a[j] = t
			            i += 1
			            j -= 1
			    qs(a, lo, j)
			    qs(a, i, hi)
			seed = 12345
			arr = []
			for k in range(20000):
			    seed = (seed * 16807) % 2147483647
			    arr.append(seed % 100000)
			qs(arr, 0, 19999)
			""",
			"""
			function qs(a, lo, hi) {
				if (lo >= hi) { return; }
				var pivot = a[Math.floor((lo + hi) / 2)];
				var i = lo; var j = hi;
				while (i <= j) {
					while (a[i] < pivot) { i++; }
					while (a[j] > pivot) { j--; }
					if (i <= j) { var t = a[i]; a[i] = a[j]; a[j] = t; i++; j--; }
				}
				qs(a, lo, j);
				qs(a, i, hi);
			}
			var seed = 12345;
			var arr = [];
			for (var k = 0; k < 20000; k++) {
				seed = (seed * 16807) % 2147483647;
				arr.push(seed % 100000);
			}
			qs(arr, 0, 19999);
			""",
			"arr[0] + arr[10000] + arr[19999]",
			"arr[0] + arr[10000] + arr[19999]",
			"arr[0] + arr[10000] + arr[19999]"),

		new Bench("10. BFS grille 100x100",
			"""
			var W = 100; var H = 100;
			var grid = [];
			for (var i = 0; i < W * H; i++) { push(grid, (i % 7 == 0 && i % 13 != 0) ? 1 : 0); }
			grid[0] = 0;
			var dist = [];
			for (var i = 0; i < W * H; i++) { push(dist, -1); }
			var queue = []; push(queue, 0); dist[0] = 0;
			var head = 0;
			var dx = [1, -1, 0, 0]; var dy = [0, 0, 1, -1];
			while (head < count(queue)) {
				var cur = queue[head]; head++;
				var cx = cur % W; var cy = floor(cur / W);
				for (var d = 0; d < 4; d++) {
					var nx = cx + dx[d]; var ny = cy + dy[d];
					if (nx >= 0 && nx < W && ny >= 0 && ny < H) {
						var ni = ny * W + nx;
						if (grid[ni] == 0 && dist[ni] < 0) { dist[ni] = dist[cur] + 1; push(queue, ni); }
					}
				}
			}
			""",
			"""
			W = 100
			H = 100
			grid = []
			for i in range(W * H):
			    grid.append(1 if (i % 7 == 0 and i % 13 != 0) else 0)
			grid[0] = 0
			dist = []
			for i in range(W * H):
			    dist.append(-1)
			queue = [0]
			dist[0] = 0
			head = 0
			dx = [1, -1, 0, 0]
			dy = [0, 0, 1, -1]
			while head < len(queue):
			    cur = queue[head]
			    head += 1
			    cx = cur % W
			    cy = cur // W
			    for d in range(4):
			        nx = cx + dx[d]
			        ny = cy + dy[d]
			        if nx >= 0 and nx < W and ny >= 0 and ny < H:
			            ni = ny * W + nx
			            if grid[ni] == 0 and dist[ni] < 0:
			                dist[ni] = dist[cur] + 1
			                queue.append(ni)
			""",
			"""
			var W = 100; var H = 100;
			var grid = [];
			for (var i = 0; i < W * H; i++) { grid.push((i % 7 == 0 && i % 13 != 0) ? 1 : 0); }
			grid[0] = 0;
			var dist = [];
			for (var i = 0; i < W * H; i++) { dist.push(-1); }
			var queue = [0];
			dist[0] = 0;
			var head = 0;
			var dx = [1, -1, 0, 0]; var dy = [0, 0, 1, -1];
			while (head < queue.length) {
				var cur = queue[head]; head++;
				var cx = cur % W; var cy = Math.floor(cur / W);
				for (var d = 0; d < 4; d++) {
					var nx = cx + dx[d]; var ny = cy + dy[d];
					if (nx >= 0 && nx < W && ny >= 0 && ny < H) {
						var ni = ny * W + nx;
						if (grid[ni] == 0 && dist[ni] < 0) { dist[ni] = dist[cur] + 1; queue.push(ni); }
					}
				}
			}
			""",
			"dist[9999] + dist[5000]",
			"dist[9999] + dist[5000]",
			"dist[9999] + dist[5000]")
	);

	// ---------------------------------------------------------------- test

	@Test
	public void benchLeekScriptVsGuest() throws Exception {
		initFightOnly();

		String only = System.getenv().getOrDefault("BENCH_ONLY", "");
		List<String> filter = only.isBlank() ? List.of() : List.of(only.split(","));
		String up = lang.toUpperCase();

		System.out.println("\n===== BANC LeekScript vs " + up + " (moteur reel, chauffe " + WARMUP + " tours) =====");
		System.out.printf("%-26s | %11s %12s | %11s %11s %12s | %7s %7s%n",
			"workload", "LS ops", "LS ms md/min", up + " ops", up + " brut", up + " ms md/min", "ops x", "temps x");
		System.out.println("-".repeat(113));

		List<Double> opsRatios = new ArrayList<>();
		List<Double> timeRatios = new ArrayList<>();
		StringBuilder warmupCurve = new StringBuilder();
		StringBuilder equivalence = new StringBuilder();

		try (PolyglotSandbox sb = new PolyglotSandbox(lang)) {
			int index = 0;
			for (Bench bench : BENCHES) {
				index++;
				if (!filter.isEmpty() && !filter.contains(String.valueOf(index))) continue;

				// Equivalence d'abord : un workload qui ne calcule pas la meme chose des deux
				// cotes rendrait toute comparaison de cout sans objet.
				String vLs = checkLs(bench.ls(), bench.checkLs());
				String vGuest = checkGuest(sb, guestBody(bench), guestCheck(bench));
				equivalence.append(String.format("  %-26s LS=%s %s=%s  %s%n",
					bench.label(), vLs, up, vGuest, vLs.equals(vGuest) ? "OK" : "<<< DIFFERENT"));

				Res ls = runLs(bench.ls());
				Res guest = runGuest(sb, guestBody(bench));

				String opsR = "-", timeR = "-";
				if (ls.ok() && guest.ok() && ls.ops() > 0 && ls.ms() > 0) {
					double o = (double) guest.ops() / ls.ops();
					double t = guest.ms() / ls.ms();
					opsRatios.add(o);
					timeRatios.add(t);
					opsR = String.format("x%.2f", o);
					timeR = String.format("x%.1f", t);
				}
				System.out.printf("%-26s | %11s %12s | %11s %11s %12s | %7s %7s%n",
					bench.label(),
					ls.ok() ? ls.ops() : "ERR", ls.ok() ? String.format("%.1f/%.1f", ls.ms(), ls.minMs()) : "-",
					guest.ok() ? guest.ops() : "ERR",
					guest.ok() ? Math.round(guest.ops() / opsFactor()) : "-",
					guest.ok() ? String.format("%.1f/%.1f", guest.ms(), guest.minMs()) : "-",
					opsR, timeR);
				if (!ls.ok()) System.out.println("      LS erreur : " + ls.error());
				if (!guest.ok()) System.out.println("      " + up + " erreur : " + guest.error());
				if (ls.ok() && guest.ok()) {
					warmupCurve.append(String.format("  %-26s LS %.1f -> %.1f -> %.1f ms | %s %.1f -> %.1f -> %.1f ms%n",
						bench.label(), ls.firstMs(), ls.secondMs(), ls.ms(),
						up, guest.firstMs(), guest.secondMs(), guest.ms()));
				}
			}
		}

		System.out.println("-".repeat(113));
		if (!opsRatios.isEmpty()) {
			System.out.printf("mediane : ops x%.2f | temps x%.1f  (>1 = %s consomme plus / est plus lent que LeekScript)%n",
				median(opsRatios), median(timeRatios), up);
		}
		System.out.println("\nEquivalence des resultats :");
		System.out.print(equivalence);
		System.out.println("\nCourbe de chauffe : tour 1 -> tour 2 -> mediane a chaud (ms)");
		System.out.print(warmupCurve);
		System.out.println("\nops " + up + " = evenements guest * opsFactor(" + opsFactor() + ") ; brut = compte avant facteur.\n");
	}

	/**
	 * Cout PROPRE au TypeScript : l'effacement de types au build (cf PolyglotEntityAI.build).
	 * Le JS produit repart ensuite dans le pipeline JS identique -&gt; a l'execution, TS == JS
	 * (memes ops, memes temps). Seul ce transpile s'ajoute, une fois par IA et par combat.
	 */
	@Test
	public void typescriptTranspileCost() {
		String ts = """
			type Target = { cell: number; life: number };
			function pick(list: Target[]): Target | null {
				let best: Target | null = null;
				for (const t of list) { if (best === null || t.life < best.life) best = t; }
				return best;
			}
			export function turn(): void {
				const enemies: Target[] = [{ cell: 12, life: 300 }, { cell: 40, life: 120 }];
				const target = pick(enemies);
				if (target !== null) { moveToward(target.cell); }
			}
			""";
		double[] times = new double[6];
		for (int i = 0; i < times.length; i++) {
			long t0 = System.nanoTime();
			TypeScriptTranspiler.Result r = TypeScriptTranspiler.transpile(ts, "ai.ts");
			times[i] = (System.nanoTime() - t0) / 1e6;
			if (!r.ok()) {
				System.out.println("[ts] transpile KO : " + r.firstDiagnostic().message);
				return;
			}
		}
		System.out.printf("%n===== COUT TYPESCRIPT (effacement de types au build) =====%n");
		System.out.printf("1er transpile (charge typescript.js) : %.1f ms%n", times[0]);
		System.out.printf("transpiles suivants : %.1f / %.1f / %.1f / %.1f / %.1f ms%n",
			times[1], times[2], times[3], times[4], times[5]);
		System.out.println("A l'execution, TS == JS (meme artefact) : ops et temps identiques.\n");
	}
}
