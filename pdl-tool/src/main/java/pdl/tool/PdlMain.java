// Copyright 2019 Sam Shen
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package pdl.tool;

import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import pdl.PdlDiag;

public class PdlMain {
	public static void main(String[] args) {
		try {
			if (args.length == 0) {
				usage("no files");
			}
			PdlDiag parser = new PdlDiag();
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
				} else if (args[i].equals("--set")) {
					if (i + 1 == args.length) {
						usage("not enough arguments for --set");
					}
					String arg = args[i + 1];
					int equals = arg.indexOf('=');
					if (equals < 0) {
						usage("--set should be in form property=value");
					}
					parser.parse(new StringReader(arg.substring(0, equals) + ":= '" + arg.substring(equals + 1) + "'"),
							"argv[" + (i + 1) + "]");
					i++;
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
					System.out.println(parser.explain(name));
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
		System.err.println("usage: rpl [--explain property] [--set property:=value] file ...");
		System.exit(msg == null ? 0 : 1);
	}

}
