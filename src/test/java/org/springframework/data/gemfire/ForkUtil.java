/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.gemfire;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Utility for forking Java processes.
 * 
 * @author Costin Leau
 */
public class ForkUtil {
	private static OutputStream os;
	private static String TEMP_DIR = System.getProperty("java.io.tmpdir");

	public static OutputStream cloneJVM(String argument) {
		String cp = System.getProperty("java.class.path");
		String home = System.getProperty("java.home");

		Process proc = null;
		String sp = System.getProperty("file.separator");
		String java = home + sp + "bin" + sp + "java";
		String argCp = " -cp " + cp;
		String argClass = argument;

		String cmd = java + argCp + " " + argClass;
		try {
			//ProcessBuilder builder = new ProcessBuilder(cmd, argCp, argClass);
			//builder.redirectErrorStream(true);
			proc = Runtime.getRuntime().exec(cmd);
		} catch (IOException ioe) {
			throw new IllegalStateException("Cannot start command " + cmd, ioe);
		}

		System.out.println("Started fork from command\n" + cmd);
		final Process p = proc;

		final BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		final AtomicBoolean run = new AtomicBoolean(true);

		Thread reader = new Thread(new Runnable() {

			public void run() {
				try {
					String line = null;
					do {
						while ((line = br.readLine()) != null) {
							System.out.println("[FORK] " + line);
						}
						Thread.sleep(200);
					} while (run.get());
				} catch (Exception ex) {
					// ignore and exit
				}
			}
		});

		reader.start();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.println("Stopping fork...");
				run.set(false);
				os = null;
				if (p != null)
					p.destroy();

				try {
					p.waitFor();
				} catch (InterruptedException e) {
					// ignore
				}
				System.out.println("Fork stopped");
			}
		});

		os = proc.getOutputStream();
		return os;
	}

	   public static OutputStream cacheServer(Class<?> clazz) {
	        return cacheServer(clazz.getName());
	    }
	    public static OutputStream cacheServer() {
	        return cacheServer("org.springframework.data.gemfire.fork.CacheServerProcess");
	    }

	public static OutputStream cacheServer(String className) {
//		String className = "org.springframework.data.gemfire.fork.CacheServerProcess";
		if (controlFileExists(className)) {
			deleteControlFile(className);
		}
		OutputStream os = cloneJVM(className);
		int maxTime = 30000;
		int time = 0;
		while (!controlFileExists(className) && time < maxTime) {
			try {
				Thread.sleep(500);
				time += 500;
			} catch (InterruptedException ex) {
				// ignore and move on
			}
		}
		if (controlFileExists(className)) {
			System.out.println("[FORK] Started cache server");
		}
		else {
			throw new RuntimeException("could not fork cache server");
		}
		return os;
	}

	public static void sendSignal() {
		try {
			os.write("\n".getBytes());
			os.flush();
		} catch (IOException ex) {
			throw new IllegalStateException("Cannot communicate with forked VM", ex);
		}
	}

	public static boolean deleteControlFile(String name) {
		String path = TEMP_DIR + File.separator + name;
		return new File(path).delete();
	}

	public static boolean createControlFile(String name) throws IOException {
		String path = TEMP_DIR + File.separator + name;
		return new File(path).createNewFile();
	}

	public static boolean controlFileExists(String name) {
		String path = TEMP_DIR + File.separator + name;
		return new File(path).exists();
	}
}