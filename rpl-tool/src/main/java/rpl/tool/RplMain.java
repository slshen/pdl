package rpl.tool;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import rpl.RplDiag;

public class RplMain {
	public static void main(String[] args) {
		try {
			if (args.length == 0) {
				usage("no files");
			}
			RplDiag parser = new RplDiag();
			List<String> explains = new ArrayList<>();
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("--explain")) {
					if (i + 1 == args.length) {
						usage("not enough arguments for --explain");
					}
					explains.addAll(Arrays.asList(args[i + 1].split("[, ]")));
					i++;
				} else if (args[i].equals("--help")) {
					usage(null);
				} else {
					parser.parse(new FileReader(args[i]), args[i]);
				}
			}
			if (explains.isEmpty()) {
				Properties properties = new Properties() {
					private static final long serialVersionUID = 1L;

					@Override
					public synchronized Enumeration<Object> keys() {
						Vector<Object> keys = new Vector<>(keySet());
						keys.sort(null);
						return keys.elements();
					}
				};
				parser.getResult().toProperties(properties);
				properties.store(System.out, null);
			} else {
				for (String name : explains) {
					parser.explain(name);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void usage(String msg) {
		if (msg != null)
			System.err.println(msg);
		System.err.println("usage: rpl --explain property file ...");
		System.exit(msg == null ? 0 : 1);
	}

}