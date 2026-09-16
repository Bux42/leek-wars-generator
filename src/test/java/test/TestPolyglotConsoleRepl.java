package test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.leekwars.generator.polyglot.PolyglotConsole;

/**
 * REPL polyglot de la console interactive : etat persistant entre lignes, rendu en notation native,
 * reroutage console.log/print, erreurs. Pendant de la console LeekScript ({@link com.leekwars.websocket}).
 */
public class TestPolyglotConsoleRepl {

	private static final class Logs implements PolyglotConsole.LogSink {
		final List<String> lines = new ArrayList<>();
		@Override public void log(int level, String message) { lines.add(message); }
	}

	private String run(PolyglotConsole c, String code) throws Exception {
		return c.execute(code).display;
	}

	@Test
	public void jsBasics() throws Exception {
		try (PolyglotConsole c = new PolyglotConsole("js", new Logs())) {
			Assert.assertEquals("2", run(c, "1 + 1"));
			Assert.assertEquals("[1, 2, 3]", run(c, "[1, 2, 3]"));
			Assert.assertEquals("{ a: 1, b: 'hi' }", run(c, "({a: 1, b: 'hi'})"));
			Assert.assertEquals("'hi'", run(c, "'hi'"));
			Assert.assertEquals("undefined", run(c, "undefined"));
			Assert.assertEquals("true", run(c, "1 < 2"));
		}
	}

	@Test
	public void jsStatePersistsVar() throws Exception {
		try (PolyglotConsole c = new PolyglotConsole("js", new Logs())) {
			run(c, "var x = 5");
			Assert.assertEquals("6", run(c, "x + 1"));
		}
	}

	@Test
	public void jsStatePersistsLet() throws Exception {
		try (PolyglotConsole c = new PolyglotConsole("js", new Logs())) {
			run(c, "let y = 3");
			Assert.assertEquals("6", run(c, "y * 2"));
		}
	}

	@Test
	public void jsStatePersistsFunction() throws Exception {
		try (PolyglotConsole c = new PolyglotConsole("js", new Logs())) {
			run(c, "function add(a, b) { return a + b; }");
			Assert.assertEquals("7", run(c, "add(3, 4)"));
		}
	}

	@Test
	public void jsConsoleLog() throws Exception {
		Logs logs = new Logs();
		try (PolyglotConsole c = new PolyglotConsole("js", logs)) {
			run(c, "console.log('hello', 42)");
			Assert.assertEquals(1, logs.lines.size());
			Assert.assertEquals("hello 42", logs.lines.get(0));
		}
	}

	@Test
	public void jsError() throws Exception {
		try (PolyglotConsole c = new PolyglotConsole("js", new Logs())) {
			try {
				run(c, "nope()");
				Assert.fail("should throw");
			} catch (PolyglotConsole.ConsoleException e) {
				Assert.assertNotNull(e.getMessage());
			}
		}
	}

	@Test
	public void jsOpsCounted() throws Exception {
		try (PolyglotConsole c = new PolyglotConsole("js", new Logs())) {
			PolyglotConsole.Result r = c.execute("var s = 0; for (var i = 0; i < 100; i++) s += i; s");
			Assert.assertEquals("4950", r.display);
			Assert.assertTrue("ops should be counted", r.ops > 0);
		}
	}

	@Test
	public void pythonBasics() throws Exception {
		try (PolyglotConsole c = new PolyglotConsole("python", new Logs())) {
			Assert.assertEquals("2", run(c, "1 + 1"));
			Assert.assertEquals("[1, 2, 3]", run(c, "[1, 2, 3]"));
			Assert.assertEquals("'hi'", run(c, "'hi'"));
			Assert.assertNull("None n'affiche rien", run(c, "None"));
			Assert.assertNull("un statement n'affiche rien", run(c, "z = 1"));
		}
	}

	@Test
	public void pythonStatePersists() throws Exception {
		try (PolyglotConsole c = new PolyglotConsole("python", new Logs())) {
			run(c, "x = 5");
			Assert.assertEquals("6", run(c, "x + 1"));
			run(c, "def f(n):\n    return n * n");
			Assert.assertEquals("9", run(c, "f(3)"));
		}
	}

	@Test
	public void pythonPrint() throws Exception {
		Logs logs = new Logs();
		try (PolyglotConsole c = new PolyglotConsole("python", logs)) {
			run(c, "print('hello', 42)");
			Assert.assertEquals(1, logs.lines.size());
			Assert.assertEquals("hello 42", logs.lines.get(0));
		}
	}

	@Test
	public void typescriptTranspiles() throws Exception {
		try (PolyglotConsole c = new PolyglotConsole("ts", new Logs())) {
			Assert.assertEquals("3", run(c, "const a: number = 1; const b: number = 2; a + b"));
		}
	}

	@Test
	public void typescriptStatePersistsAcrossLines() throws Exception {
		// transpileModule ne fait pas de resolution de noms : `n + 1` ligne 2 (n vient de la ligne 1)
		// transpile sans diagnostic, et l'etat du contexte JS persiste -> le REPL TS reste continu.
		try (PolyglotConsole c = new PolyglotConsole("ts", new Logs())) {
			run(c, "const n: number = 41");
			Assert.assertEquals("42", run(c, "n + 1"));
		}
	}

	/**
	 * Plafonds propres a la console (/exec du chat : 100 000 instructions et 1 s au lieu des 20 M et
	 * 5 s d'un combat). JS est coupe par la limite d'instructions ; Python ne l'est PAS (GraalPy ignore
	 * sandbox.MaxStatements) et c'est le budget wall-clock qui le borne. Dans les deux cas, une boucle
	 * infinie est interrompue bien avant les 5 s du watchdog par defaut, et du code borne passe.
	 */
	@Test
	public void consoleLimitsCutInfiniteLoop() throws Exception {
		for (String language : new String[] { "js", "python" }) {
			String loop = language.equals("js") ? "while (true) {}" : "while True: pass\n";
			long start = System.currentTimeMillis();
			try (PolyglotConsole c = new PolyglotConsole(language, 100_000, 1_000, new Logs())) {
				c.execute(loop);
				Assert.fail(language + " : la boucle infinie aurait du etre coupee");
			} catch (PolyglotConsole.ConsoleException e) {
				Assert.assertTrue(language + " : " + e.getMessage(), e.getMessage().startsWith("Execution interrupted"));
				Assert.assertTrue(language + " : coupee avant le watchdog par defaut", System.currentTimeMillis() - start < 3_000);
			}
			String bounded = language.equals("js") ? "let s = 0; for (let i = 0; i < 1000; i++) s += i; s" : "sum(range(1000))";
			try (PolyglotConsole c = new PolyglotConsole(language, 100_000, 1_000, new Logs())) {
				Assert.assertEquals("499500", c.execute(bounded).display);
			}
		}
	}

	/** Un bloc entier (/exec du chat) : plusieurs instructions, puis la valeur de la derniere expression. */
	@Test
	public void pythonBlockLikeANotebookCell() throws Exception {
		try (PolyglotConsole c = new PolyglotConsole("python", 100_000, 1_000, new Logs())) {
			Assert.assertEquals("45", c.executeBlock("s = 0\nfor i in range(10):\n    s += i\ns").display);
			Assert.assertEquals("1024", c.executeBlock("2 ** 10").display);
			Assert.assertNull("sans expression finale, rien a afficher", c.executeBlock("x = 3\ny = 4").display);
			Assert.assertNull("None ne s'affiche pas", c.executeBlock("print('a')\nNone").display);
			long ops = c.executeBlock("2 ** 10").ops;
			Assert.assertTrue("une ligne simple reste tres peu couteuse : " + ops, ops < 50);
		}
		try (PolyglotConsole c = new PolyglotConsole("js", 100_000, 1_000, new Logs())) {
			Assert.assertEquals("45", c.executeBlock("let s = 0\nfor (let i = 0; i < 10; i++) s += i\ns").display);
		}
	}
}
